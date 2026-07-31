package com.example.neatgrid.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max

enum class MetadataCacheStatus {
    AUTOMATIC,
    CUSTOM,
    NOT_FOUND
}

data class CachedGameMetadata(
    val metadata: GameMetadata,
    val status: MetadataCacheStatus
)

class MetadataRepository(
    context: Context,
    private val service: LaunchBoxService = LaunchBoxService()
) {
    private val metadataDirectory = File(context.filesDir, "metadata")

    fun readCached(packageName: String): CachedGameMetadata? {
        val file = cacheFile(packageName)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            if (json.has("metadata")) {
                CachedGameMetadata(
                    metadata = GameMetadata.fromJson(json.getJSONObject("metadata")),
                    status = runCatching {
                        MetadataCacheStatus.valueOf(json.optString("status"))
                    }.getOrDefault(MetadataCacheStatus.AUTOMATIC)
                )
            } else {
                CachedGameMetadata(
                    metadata = GameMetadata.fromJson(json),
                    status = MetadataCacheStatus.AUTOMATIC
                )
            }
        }.getOrNull()
    }

    suspend fun lookup(
        label: String,
        preferredPlatforms: Set<String> = emptySet()
    ): CachedGameMetadata {
        val query = MetadataMatcher.cleanQuery(label)
        val results = service.searchGames(query)
        val match = MetadataMatcher.bestMatch(query, results, preferredPlatforms)
            ?: return CachedGameMetadata(
                metadata = emptyMetadata(label),
                status = MetadataCacheStatus.NOT_FOUND
            )
        val resolved = match.sourceId?.let { service.fetchGameDetails(it, match) } ?: match
        return CachedGameMetadata(
            metadata = resolved.copy(sourceId = match.sourceId),
            status = MetadataCacheStatus.AUTOMATIC
        )
    }

    suspend fun search(query: String): List<GameMetadata> {
        return service.searchGames(MetadataMatcher.cleanQuery(query))
    }

    suspend fun resolve(game: GameMetadata): GameMetadata {
        return game.sourceId?.let { service.fetchGameDetails(it, game) } ?: game
    }

    suspend fun save(
        packageName: String,
        cachedMetadata: CachedGameMetadata
    ) = withContext(Dispatchers.IO) {
        if (!metadataDirectory.exists()) metadataDirectory.mkdirs()
        val json = JSONObject()
            .put("version", 3)
            .put("status", cachedMetadata.status.name)
            .put("updatedAt", System.currentTimeMillis())
            .put("metadata", cachedMetadata.metadata.toJson())
        cacheFile(packageName).writeText(json.toString())
    }

    suspend fun delete(packageName: String) = withContext(Dispatchers.IO) {
        cacheFile(packageName).delete()
    }

    private fun cacheFile(packageName: String): File {
        val hash = runCatching {
            MessageDigest.getInstance("SHA-256")
                .digest(packageName.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }.getOrElse {
            packageName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        }
        return File(metadataDirectory, "$hash.json")
    }

    private fun emptyMetadata(label: String) = GameMetadata(
        title = label,
        summary = null,
        rating = null,
        releaseDate = null,
        genres = emptyList(),
        platforms = emptyList(),
        coverUrl = null,
        screenshotUrls = emptyList()
    )
}

object MetadataMatcher {
    fun cleanQuery(query: String): String {
        return query
            .replace(Regex("\\s*\\([^)]*\\)"), "")
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .trim()
    }

    fun bestMatch(
        query: String,
        results: List<GameMetadata>,
        preferredPlatforms: Set<String> = emptySet()
    ): GameMetadata? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return null
        val platformMatches = if (preferredPlatforms.isEmpty()) {
            results
        } else {
            results.filter { result ->
                result.platforms.any { candidate ->
                    preferredPlatforms.any { preferred -> platformsMatch(preferred, candidate) }
                }
            }
        }
        if (platformMatches.isEmpty()) return null
        return platformMatches
            .map { it to score(normalizedQuery, normalize(it.title)) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= 0.58 }
            ?.first
    }

    internal fun score(normalizedQuery: String, normalizedTitle: String): Double {
        if (normalizedTitle.isEmpty()) return 0.0
        if (normalizedQuery == normalizedTitle) return 1.0
        if (normalizedQuery in normalizedTitle || normalizedTitle in normalizedQuery) return 0.85

        val queryTokens = normalizedQuery.split(' ').filter { it.isNotEmpty() }.toSet()
        val titleTokens = normalizedTitle.split(' ').filter { it.isNotEmpty() }.toSet()
        val tokenScore = queryTokens.intersect(titleTokens).size.toDouble() /
            queryTokens.union(titleTokens).size.coerceAtLeast(1)
        val editScore = 1.0 - levenshtein(normalizedQuery, normalizedTitle).toDouble() /
            max(normalizedQuery.length, normalizedTitle.length)
        return max(tokenScore, editScore)
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun platformsMatch(preferred: String, candidate: String): Boolean {
        val preferredNames = platformNames(preferred)
        val candidateName = normalize(candidate)
        return candidateName in preferredNames
    }

    private fun platformNames(platform: String): Set<String> {
        return when (normalize(platform)) {
            "psp" -> setOf("psp", "sony psp", "playstation portable")
            "playstation 1" -> setOf("playstation", "sony playstation", "ps1")
            "playstation 2" -> setOf("playstation 2", "sony playstation 2", "ps2")
            "game boy advance" -> setOf("game boy advance", "nintendo game boy advance", "gba")
            "super nintendo" -> setOf("super nintendo", "super nintendo entertainment system", "snes")
            "nintendo 64" -> setOf("nintendo 64", "n64")
            "gamecube" -> setOf("gamecube", "nintendo gamecube")
            "wii" -> setOf("wii", "nintendo wii")
            "nintendo ds" -> setOf("nintendo ds", "nds")
            "nintendo 3ds" -> setOf("nintendo 3ds", "3ds")
            else -> setOf(normalize(platform))
        }
    }

    private fun levenshtein(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        left.forEachIndexed { leftIndex, leftChar ->
            val current = IntArray(right.length + 1)
            current[0] = leftIndex + 1
            right.forEachIndexed { rightIndex, rightChar ->
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + if (leftChar == rightChar) 0 else 1
                )
            }
            previous = current
        }
        return previous[right.length]
    }
}
