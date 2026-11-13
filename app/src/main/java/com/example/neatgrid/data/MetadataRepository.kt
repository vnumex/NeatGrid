package com.example.neatgrid.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
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

data class MetadataConfig(
    val order: List<MetadataSource>,
    val iidbApiKey: String,
    val rawgApiKey: String
)

data class MetadataSearchResult(
    val results: List<GameMetadata>,
    val warnings: List<String> = emptyList()
)

class MetadataRepository internal constructor(
    private val metadataDirectory: File,
    private val service: LaunchBoxService = LaunchBoxService(),
    private val iidbService: IidbService = IidbService(),
    private val rawgService: RawgService = RawgService(),
    private val configProvider: suspend () -> MetadataConfig
) {
    constructor(
        context: Context,
        service: LaunchBoxService = LaunchBoxService(),
        iidbService: IidbService = IidbService(),
        rawgService: RawgService = RawgService(),
        configProvider: suspend () -> MetadataConfig = defaultConfigProvider(context)
    ) : this(
        metadataDirectory = File(context.filesDir, "metadata"),
        service = service,
        iidbService = iidbService,
        rawgService = rawgService,
        configProvider = configProvider
    )

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
        if (query.isBlank()) {
            return CachedGameMetadata(
                metadata = emptyMetadata(label),
                status = MetadataCacheStatus.NOT_FOUND
            )
        }
        val config = configProvider()
        val sources = activeSources(config)
        var authError: MetadataAuthException? = null
        var lastError: Exception? = null
        var hadSuccessfulAttempt = false

        for ((index, source) in sources.withIndex()) {
            try {
                val match = lookupFromSource(source, query, preferredPlatforms, config)
                hadSuccessfulAttempt = true
                if (match != null) {
                    val enriched = backfillSummary(
                        primary = match,
                        query = query,
                        preferredPlatforms = preferredPlatforms,
                        fallbackSources = sources.drop(index + 1),
                        config = config
                    )
                    return CachedGameMetadata(
                        metadata = enriched,
                        status = MetadataCacheStatus.AUTOMATIC
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: MetadataAuthException) {
                if (authError == null) authError = e
                lastError = e
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (authError != null) throw authError
        if (!hadSuccessfulAttempt && lastError != null) throw lastError

        return CachedGameMetadata(
            metadata = emptyMetadata(label),
            status = MetadataCacheStatus.NOT_FOUND
        )
    }

    suspend fun search(query: String): MetadataSearchResult {
        val cleanQuery = MetadataMatcher.cleanQuery(query)
        if (cleanQuery.isBlank()) return MetadataSearchResult(emptyList())
        val config = configProvider()
        val sources = activeSources(config)
        val results = mutableListOf<GameMetadata>()
        val warnings = mutableListOf<String>()
        var hadSuccessfulAttempt = false
        var firstAuthError: MetadataAuthException? = null
        var lastError: Exception? = null
        for (source in sources) {
            try {
                val sourceResults = when (source) {
                    MetadataSource.IIDB -> iidbService.searchGames(cleanQuery, config.iidbApiKey)
                    MetadataSource.LAUNCHBOX -> service.searchGames(cleanQuery)
                    MetadataSource.RAWG -> rawgService.searchGames(cleanQuery, config.rawgApiKey)
                }
                hadSuccessfulAttempt = true
                results += sourceResults
            } catch (e: CancellationException) {
                throw e
            } catch (e: MetadataAuthException) {
                if (firstAuthError == null) firstAuthError = e
                lastError = e
                warnings += "${source.displayName}: ${e.message ?: "Authentication failed"}"
            } catch (e: Exception) {
                lastError = e
                warnings += "${source.displayName}: ${e.message ?: "Search failed"}"
            }
        }
        if (!hadSuccessfulAttempt && lastError != null) {
            throw firstAuthError ?: lastError
        }
        return MetadataSearchResult(results = results, warnings = warnings)
    }

    suspend fun resolve(game: GameMetadata): GameMetadata {
        val sourceId = game.sourceId ?: return game
        val source = MetadataSource.fromSourceId(sourceId)
        val config = configProvider()
        val resolved = when (source) {
            MetadataSource.IIDB -> iidbService.fetchGameDetails(sourceId, game, config.iidbApiKey)
            MetadataSource.LAUNCHBOX -> service.fetchGameDetails(sourceId, game)
            MetadataSource.RAWG -> rawgService.fetchGameDetails(sourceId, game, config.rawgApiKey)
        }
        val withSourceId = if (resolved.sourceId.isNullOrBlank()) {
            resolved.copy(sourceId = sourceId)
        } else {
            resolved
        }
        if (!withSourceId.summary.isNullOrBlank()) return withSourceId
        val fallbackSources = activeSources(config).filter { it != source }
        return backfillSummary(
            primary = withSourceId,
            query = MetadataMatcher.cleanQuery(withSourceId.title),
            preferredPlatforms = withSourceId.platforms.toSet(),
            fallbackSources = fallbackSources,
            config = config
        )
    }

    private suspend fun lookupFromSource(
        source: MetadataSource,
        query: String,
        preferredPlatforms: Set<String>,
        config: MetadataConfig
    ): GameMetadata? {
        return when (source) {
            MetadataSource.IIDB -> iidbService.resolveBestMatch(query, preferredPlatforms, config.iidbApiKey)
            MetadataSource.LAUNCHBOX -> {
                val results = service.searchGames(query)
                val match = MetadataMatcher.bestMatch(query, results, preferredPlatforms) ?: return null
                val resolved = match.sourceId?.let { service.fetchGameDetails(it, match) } ?: match
                resolved.copy(sourceId = match.sourceId)
            }
            MetadataSource.RAWG -> {
                val results = rawgService.searchGames(query, config.rawgApiKey)
                val match = MetadataMatcher.bestMatch(query, results, preferredPlatforms) ?: return null
                val resolved = match.sourceId?.let { rawgService.fetchGameDetails(it, match, config.rawgApiKey) } ?: match
                resolved.copy(sourceId = match.sourceId)
            }
        }
    }

    private suspend fun backfillSummary(
        primary: GameMetadata,
        query: String,
        preferredPlatforms: Set<String>,
        fallbackSources: List<MetadataSource>,
        config: MetadataConfig
    ): GameMetadata {
        if (!primary.summary.isNullOrBlank() || query.isBlank()) return primary
        for (source in fallbackSources) {
            val fallback = try {
                lookupFromSource(source, query, preferredPlatforms, config)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (fallback != null && !fallback.summary.isNullOrBlank()) {
                return primary.copy(
                    summary = fallback.summary,
                    genres = primary.genres.ifEmpty { fallback.genres },
                    rating = primary.rating ?: fallback.rating
                )
            }
        }
        return primary
    }

    private fun activeSources(config: MetadataConfig): List<MetadataSource> {
        return config.order.distinct().filter { source ->
            when (source) {
                MetadataSource.IIDB -> config.iidbApiKey.isNotBlank()
                MetadataSource.RAWG -> config.rawgApiKey.isNotBlank()
                MetadataSource.LAUNCHBOX -> true
            }
        }
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

private fun defaultConfigProvider(context: Context): suspend () -> MetadataConfig {
    val settingsManager = SettingsManager(context)
    return {
        MetadataConfig(
            order = settingsManager.metadataSourceOrderFlow.first(),
            iidbApiKey = settingsManager.iidbApiKeyFlow.first(),
            rawgApiKey = settingsManager.rawgApiKeyFlow.first()
        )
    }
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

    internal fun platformsMatch(preferred: String, candidate: String): Boolean {
        val preferredNames = platformNames(preferred)
        val candidateName = normalize(candidate)
        return candidateName in preferredNames
    }

    private fun platformNames(platform: String): Set<String> {
        val normalized = normalize(platform)
        return PLATFORM_ALIAS_GROUPS.firstOrNull { normalized in it } ?: setOf(normalized)
    }

    private val PLATFORM_ALIAS_GROUPS = listOf(
        setOf("psp", "sony psp", "playstation portable", "sony playstation portable"),
        setOf("playstation", "sony playstation", "ps1", "psx", "playstation 1", "sony playstation 1"),
        setOf("playstation 2", "sony playstation 2", "ps2"),
        setOf("game boy advance", "nintendo game boy advance", "gba"),
        setOf("super nintendo", "super nintendo entertainment system", "snes", "super famicom"),
        setOf("nintendo 64", "n64"),
        setOf("gamecube", "nintendo gamecube", "gc", "ngc"),
        setOf("wii", "nintendo wii"),
        setOf("nintendo ds", "nds", "ds"),
        setOf("nintendo 3ds", "3ds", "n3ds"),
        setOf("nintendo switch", "switch"),
        setOf("android")
    )

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
