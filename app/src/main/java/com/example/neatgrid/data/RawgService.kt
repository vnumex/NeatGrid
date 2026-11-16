package com.example.neatgrid.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RawgService(
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend fun searchGames(query: String, apiKey: String): List<GameMetadata> {
        if (query.isBlank()) return emptyList()
        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val json = requestJson("/games?key=$encodedKey&search=$encodedQuery&page_size=10")
        val results = json.optJSONArray("results") ?: return emptyList()
        val games = mutableListOf<GameMetadata>()
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val id = parseId(item) ?: continue
            val name = item.optNullableString("name") ?: continue
            val rating = item.optNullableDouble("rating")?.takeIf { it > 0.0 }?.let { it * 20.0 }
            games.add(
                GameMetadata(
                    title = name,
                    summary = null,
                    rating = rating,
                    releaseDate = item.optNullableString("released"),
                    genres = parseGenres(item.optJSONArray("genres")),
                    platforms = parsePlatforms(item.optJSONArray("platforms")),
                    coverUrl = item.optNullableString("background_image"),
                    screenshotUrls = parseScreenshots(item.optJSONArray("short_screenshots")),
                    sourceId = "rawg:$id",
                    backdropUrl = null
                )
            )
        }
        return games
    }

    suspend fun fetchGameDetails(
        rawgId: String,
        basicMetadata: GameMetadata,
        apiKey: String
    ): GameMetadata {
        val cleanId = rawgId.removePrefix("rawg:").trim()
        if (cleanId.isEmpty()) throw IOException("Missing RAWG game ID")
        val encodedId = URLEncoder.encode(cleanId, "UTF-8")
        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val json = requestJson("/games/$encodedId?key=$encodedKey")

        val title = json.optNullableString("name") ?: basicMetadata.title
        val summary = json.optNullableString("description_raw") ?: basicMetadata.summary
        val rating = json.optNullableDouble("rating")?.takeIf { it > 0.0 }?.let { it * 20.0 }
            ?: basicMetadata.rating
        val releaseDate = json.optNullableString("released") ?: basicMetadata.releaseDate
        val genres = parseGenres(json.optJSONArray("genres")).ifEmpty { basicMetadata.genres }
        val platforms = parsePlatforms(json.optJSONArray("platforms")).ifEmpty { basicMetadata.platforms }
        val coverUrl = json.optNullableString("background_image") ?: basicMetadata.coverUrl
        val backdropUrl = json.optNullableString("background_image_additional") ?: basicMetadata.backdropUrl
        val screenshotUrls = parseScreenshots(json.optJSONArray("short_screenshots"))
            .ifEmpty { basicMetadata.screenshotUrls }

        return basicMetadata.copy(
            title = title,
            summary = summary,
            rating = rating,
            releaseDate = releaseDate,
            genres = genres,
            platforms = platforms,
            coverUrl = coverUrl,
            screenshotUrls = screenshotUrls,
            sourceId = "rawg:$cleanId",
            backdropUrl = backdropUrl
        )
    }

    suspend fun fetchArtwork(rawgId: String, apiKey: String): ArtworkUrls {
        val cleanId = rawgId.removePrefix("rawg:").trim()
        if (cleanId.isEmpty()) throw IOException("Missing RAWG game ID")
        val encodedId = URLEncoder.encode(cleanId, "UTF-8")
        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val json = requestJson("/games/$encodedId?key=$encodedKey")
        val banners = listOfNotNull(
            json.optNullableString("background_image"),
            json.optNullableString("background_image_additional")
        ).distinct().map { ArtworkImage(it, previewUrl(it)) }
        return ArtworkUrls(banners = banners)
    }

    private fun previewUrl(url: String): String {
        return if ("/media/resize/" in url) url else url.replaceFirst(
            "/media/",
            "/media/resize/420/-/"
        )
    }

    private suspend fun requestJson(route: String): JSONObject {
        val response = withContext(Dispatchers.IO) {
            executeHttp(route)
        }
        return when (response.code) {
            in 200..299 -> JSONObject(response.body.ifBlank { "{}" })
            401, 403 -> throw MetadataAuthException(
                MetadataSource.RAWG,
                "RAWG authentication failed (HTTP ${response.code})"
            )
            else -> throw IOException("RAWG request failed with HTTP ${response.code}")
        }
    }

    private fun executeHttp(route: String): HttpResponse {
        val connection = (URL("$normalizedBaseUrl$route").openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseId(json: JSONObject): String? {
        if (!json.has("id") || json.isNull("id")) return null
        val numeric = json.optLong("id", -1L)
        if (numeric > 0L) return numeric.toString()
        return json.optNullableString("id")
    }

    private fun parseGenres(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val genres = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            val name = obj?.optNullableString("name")
                ?: if (obj == null && !array.isNull(i)) array.optString(i).trim().takeIf { it.isNotEmpty() } else null
            if (name != null) genres.add(name)
        }
        return genres.distinct()
    }

    private fun parsePlatforms(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val platforms = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val name = entry.optJSONObject("platform")?.optNullableString("name")
                ?: entry.optNullableString("name")
            if (name != null) platforms.add(name)
        }
        return platforms.distinct()
    }

    private fun parseScreenshots(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val urls = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            val url = obj?.optNullableString("image")
                ?: if (obj == null && !array.isNull(i)) array.optString(i).trim().takeIf { it.isNotEmpty() } else null
            if (url != null) urls.add(url)
        }
        return urls.distinct()
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return value.takeIf { !it.isNaN() }
    }

    private data class HttpResponse(
        val code: Int,
        val body: String
    )

    companion object {
        const val DEFAULT_BASE_URL = "https://api.rawg.io/api"
        private const val TIMEOUT_MS = 10_000
    }
}
