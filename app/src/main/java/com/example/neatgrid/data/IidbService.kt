package com.example.neatgrid.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class IidbService(
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend fun verifyKey(apiKey: String): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(MetadataAuthException(MetadataSource.IIDB, "iiDB API key is required"))
        }
        return try {
            val json = requestJson("/me/access", "GET", apiKey)
            val tier = json.optJSONObject("tier")
            val tierCode = tier?.optNullableString("code")
                ?: json.optNullableString("tier_code")
            val rps = tier?.optNullableDouble("requests_per_second")
                ?: json.optNullableDouble("requests_per_second")
            val rpsText = rps?.let { value ->
                if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            }
            val summary = when {
                tierCode != null && rpsText != null -> "Verified ($tierCode, $rpsText req/s)"
                tierCode != null -> "Verified ($tierCode)"
                else -> "Verified"
            }
            Result.success(summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGames(query: String, apiKey: String): List<GameMetadata> {
        if (query.isBlank()) return emptyList()
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val json = requestJson("/search/parents?q=$encodedQuery&limit=10", "GET", apiKey)
        val results = json.optJSONArray("results") ?: return emptyList()
        val games = mutableListOf<GameMetadata>()
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val parentId = parseParentId(item, "parent_id") ?: continue
            val name = item.optNullableString("name") ?: continue
            val year = item.optNullableInt("initial_release_year")?.takeIf { it > 0 }?.toString()
            val platforms = parsePlatforms(item)
            val coverUrl = item.optNullableString("boxart_url")
            games.add(
                GameMetadata(
                    title = name,
                    summary = null,
                    rating = null,
                    releaseDate = year,
                    genres = emptyList(),
                    platforms = platforms,
                    coverUrl = coverUrl,
                    screenshotUrls = emptyList(),
                    sourceId = "iidb:$parentId",
                    backdropUrl = null
                )
            )
        }
        return games
    }

    suspend fun resolveBestMatch(
        query: String,
        preferredPlatforms: Set<String>,
        apiKey: String
    ): GameMetadata? {
        if (query.isBlank()) return null
        val payload = JSONObject()
            .put("title", query.trim())
            .put("artwork", "recommended")
            .put("slots", JSONArray(ARTWORK_SLOTS))
            .put("candidates_per_slot", 4)
            .put("include", JSONArray(listOf("parent_metadata", "asset_metadata")))
        toConsoleId(preferredPlatforms)?.let { payload.put("preferred_console", it) }

        val json = requestJson("/resolve", "POST", apiKey, payload.toString())
        val match = json.optJSONObject("match")
        val status = match?.optNullableString("status") ?: return null
        return when (status) {
            "matched" -> {
                val parent = match.optJSONObject("parent") ?: return null
                val mapped = parseParentMetadata(parent, json.optJSONObject("artwork")) ?: return null
                if (preferredPlatforms.isNotEmpty() && mapped.platforms.isNotEmpty()) {
                    val compatible = mapped.platforms.any { candidate ->
                        preferredPlatforms.any { preferred ->
                            MetadataMatcher.platformsMatch(preferred, candidate)
                        }
                    }
                    if (!compatible) return null
                }
                mapped
            }

            "needs_choice" -> {
                val candidates = parseCandidates(json.optJSONArray("candidates"))
                val winner = MetadataMatcher.bestMatch(query, candidates, preferredPlatforms) ?: return null
                val winnerId = winner.sourceId ?: return null
                fetchGameDetails(winnerId, winner, apiKey)
            }

            else -> null
        }
    }

    suspend fun fetchGameDetails(
        parentId: String,
        basicMetadata: GameMetadata,
        apiKey: String
    ): GameMetadata {
        val cleanId = parentId.removePrefix("iidb:").trim()
        if (cleanId.isEmpty()) throw IOException("Missing iiDB parent ID")
        val encodedId = URLEncoder.encode(cleanId, "UTF-8")
        val slots = ARTWORK_SLOTS.joinToString(",")
        val route =
            "/assets/parents/$encodedId?slots=$slots&candidates_per_slot=4&include=parent_metadata,asset_metadata"
        val json = requestJson(route, "GET", apiKey)
        val parent = json.optJSONObject("match")?.optJSONObject("parent")
        val artwork = json.optJSONObject("artwork")
        val parsed = parent?.let { parseParentMetadata(it, artwork) }
        val (boxart, background, screenshots) = parseArtwork(artwork)
        val icon = parseIcon(artwork)

        return basicMetadata.copy(
            title = parsed?.title ?: basicMetadata.title,
            releaseDate = parsed?.releaseDate ?: basicMetadata.releaseDate,
            platforms = parsed?.platforms?.takeIf { it.isNotEmpty() } ?: basicMetadata.platforms,
            coverUrl = boxart ?: parsed?.coverUrl ?: basicMetadata.coverUrl,
            backdropUrl = background ?: parsed?.backdropUrl ?: basicMetadata.backdropUrl,
            screenshotUrls = screenshots.ifEmpty { basicMetadata.screenshotUrls },
            sourceId = "iidb:$cleanId",
            iconUrl = icon ?: basicMetadata.iconUrl
        )
    }

    suspend fun fetchArtwork(parentId: String, apiKey: String): ArtworkUrls {
        val cleanId = parentId.removePrefix("iidb:").trim()
        if (cleanId.isEmpty()) throw IOException("Missing iiDB parent ID")
        val encodedId = URLEncoder.encode(cleanId, "UTF-8")
        val slots = PICKER_SLOTS.joinToString(",")
        val json = requestJson(
            "/assets/parents/$encodedId?slots=$slots&candidates_per_slot=6",
            "GET",
            apiKey
        )
        val artwork = json.optJSONObject("artwork") ?: return ArtworkUrls()
        fun urls(vararg slotNames: String) = slotNames
            .flatMap { extractArtworkImages(artwork.optJSONArray(it)) }
            .distinctBy { it.url }
        return ArtworkUrls(
            banners = urls("banner", "hero"),
            icons = urls("icon", "boxart", "iisu_boxart")
        )
    }

    private suspend fun requestJson(
        route: String,
        method: String,
        apiKey: String,
        body: String? = null
    ): JSONObject {
        val trimmedKey = apiKey.trim()
        var attempt = 0
        while (true) {
            val response = withContext(Dispatchers.IO) {
                executeHttp(route, method, trimmedKey, body)
            }
            when (response.code) {
                in 200..299 -> {
                    val text = response.body.ifBlank { "{}" }
                    return JSONObject(text)
                }

                401, 403 -> {
                    throw MetadataAuthException(
                        MetadataSource.IIDB,
                        "iiDB authentication failed (HTTP ${response.code})"
                    )
                }

                429 -> {
                    if (attempt == 0) {
                        attempt++
                        val retryAfterSeconds = response.retryAfterSeconds?.coerceIn(0L, 2L) ?: 1L
                        if (retryAfterSeconds > 0L) {
                            delay(retryAfterSeconds * 1000L)
                        }
                        continue
                    }
                    throw IOException("iiDB rate limit exceeded (HTTP 429)")
                }

                else -> {
                    throw IOException("iiDB request failed with HTTP ${response.code}")
                }
            }
        }
    }

    private fun executeHttp(
        route: String,
        method: String,
        trimmedKey: String,
        body: String?
    ): HttpResponse {
        val connection = (URL("$normalizedBaseUrl$route").openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            if (trimmedKey.isNotEmpty()) {
                setRequestProperty("X-API-Key", trimmedKey)
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val retryAfter = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
            return HttpResponse(code, text, retryAfter)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCandidates(candidates: JSONArray?): List<GameMetadata> {
        if (candidates == null) return emptyList()
        val list = mutableListOf<GameMetadata>()
        for (i in 0 until candidates.length()) {
            val item = candidates.optJSONObject(i) ?: continue
            parseParentMetadata(item, null)?.let { list.add(it) }
        }
        return list
    }

    private fun parseParentMetadata(parent: JSONObject, artwork: JSONObject?): GameMetadata? {
        val id = parseParentId(parent, "id") ?: parseParentId(parent, "parent_id") ?: return null
        val name = parent.optNullableString("name") ?: return null
        val year = (parent.optNullableInt("year") ?: parent.optNullableInt("initial_release_year"))
            ?.takeIf { it > 0 }
            ?.toString()
        val platforms = parsePlatforms(parent)
        val (boxart, background, screenshots) = parseArtwork(artwork)
        val coverUrl = boxart ?: parent.optNullableString("boxart_url")
        return GameMetadata(
            title = name,
            summary = null,
            rating = null,
            releaseDate = year,
            genres = emptyList(),
            platforms = platforms,
            coverUrl = coverUrl,
            screenshotUrls = screenshots,
            sourceId = "iidb:$id",
            backdropUrl = background,
            iconUrl = parseIcon(artwork)
        )
    }

    private fun parseArtwork(artwork: JSONObject?): Triple<String?, String?, List<String>> {
        if (artwork == null) return Triple(null, null, emptyList())
        val boxart = extractArtworkUrls(artwork.optJSONArray("boxart")).firstOrNull()
        val background = extractArtworkUrls(artwork.optJSONArray("background")).firstOrNull()
        val screenshots = extractArtworkUrls(artwork.optJSONArray("screenshot"))
        return Triple(boxart, background, screenshots)
    }

    private fun parseIcon(artwork: JSONObject?): String? {
        return artwork?.let { extractArtworkUrls(it.optJSONArray("icon")).firstOrNull() }
    }

    private fun extractArtworkImages(array: JSONArray?): List<ArtworkImage> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val url = item.optNullableString("url") ?: return@mapNotNull null
            ArtworkImage(url, item.optNullableString("thumbnail_url") ?: url)
        }
    }

    private fun extractArtworkUrls(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val urls = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            val url = obj?.optNullableString("url")
                ?: if (obj == null && !array.isNull(i)) array.optString(i).takeIf { it.isNotBlank() } else null
            if (url != null) urls.add(url)
        }
        return urls
    }

    private fun parsePlatforms(json: JSONObject): List<String> {
        val platforms = mutableListOf<String>()
        json.optNullableString("primary_platform")?.let { platforms.add(it) }
        addPlatformField(json, "platform_summary", platforms)
        addPlatformField(json, "platform_list", platforms)
        return platforms.distinct()
    }

    private fun addPlatformField(json: JSONObject, key: String, target: MutableList<String>) {
        val array = json.optJSONArray(key)
        if (array != null) {
            for (i in 0 until array.length()) {
                if (!array.isNull(i)) {
                    array.optString(i).trim().takeIf { it.isNotEmpty() }?.let { target.add(it) }
                }
            }
            return
        }
        val text = json.optNullableString(key) ?: return
        text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { target.add(it) }
    }

    private fun parseParentId(json: JSONObject, key: String): String? {
        if (json.isNull(key)) return null
        val numeric = json.optLong(key, -1L)
        if (numeric > 0L) return numeric.toString()
        return json.optNullableString(key)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val value = optInt(key, Int.MIN_VALUE)
        return value.takeIf { it != Int.MIN_VALUE }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return value.takeIf { !it.isNaN() }
    }

    private data class HttpResponse(
        val code: Int,
        val body: String,
        val retryAfterSeconds: Long?
    )

    companion object {
        const val DEFAULT_BASE_URL = "https://iidb-api.iisu.network/api/v1"
        private val ARTWORK_SLOTS = listOf("boxart", "icon", "background", "screenshot")
        private val PICKER_SLOTS = listOf("boxart", "iisu_boxart", "icon", "banner", "hero")
        private const val TIMEOUT_MS = 10_000

        fun toConsoleId(preferredPlatforms: Set<String>): String? {
            if (preferredPlatforms.size != 1) return null
            return when (preferredPlatforms.single().trim().lowercase(Locale.ROOT)) {
                "psp" -> "psp"
                "playstation 1" -> "psx"
                "playstation 2" -> "ps2"
                "game boy advance" -> "gba"
                "super nintendo" -> "snes"
                "nintendo 64" -> "n64"
                "gamecube" -> "gamecube"
                "wii" -> "wii"
                "nintendo ds" -> "nds"
                "nintendo 3ds" -> "3ds"
                "nintendo switch" -> "switch"
                "android" -> "android"
                else -> null
            }
        }
    }
}
