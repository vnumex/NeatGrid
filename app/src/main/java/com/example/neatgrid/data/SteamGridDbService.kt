package com.example.neatgrid.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

open class SteamGridDbService(baseUrl: String = DEFAULT_BASE_URL) {
    private val base = baseUrl.trimEnd('/')

    suspend fun verifyKey(apiKey: String): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(
                MetadataAuthException(
                    MetadataSource.STEAMGRIDDB,
                    "SteamGridDB API key is required"
                )
            )
        }
        return try {
            requestData("/search/autocomplete/${encode("zelda")}", apiKey)
            Result.success("Verified")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun searchGames(query: String, apiKey: String): List<GameMetadata> {
        if (query.isBlank()) return emptyList()
        val results = requestData("/search/autocomplete/${encode(query.trim())}", apiKey)
            ?: return emptyList()
        return (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optLong("id", -1L).takeIf { it > 0 } ?: return@mapNotNull null
            val name = item.optString("name").trim().ifEmpty { null } ?: return@mapNotNull null
            GameMetadata(
                title = name,
                summary = null,
                rating = null,
                releaseDate = null,
                genres = emptyList(),
                platforms = emptyList(),
                coverUrl = null,
                screenshotUrls = emptyList(),
                sourceId = "steamgriddb:$id"
            )
        }
    }

    open suspend fun fetchArtwork(gameId: String, apiKey: String): ArtworkUrls =
        fetchArtworkAt("game/${encode(gameId.removePrefix("steamgriddb:").trim())}", apiKey)

    open suspend fun fetchSteamArtwork(steamAppId: String, apiKey: String): ArtworkUrls =
        fetchArtworkAt("steam/${encode(steamAppId.removePrefix("steam:").trim())}", apiKey)

    private suspend fun fetchArtworkAt(target: String, apiKey: String): ArtworkUrls =
        coroutineScope {
            val covers = async {
                images(
                    "/grids/$target?dimensions=600x900,342x482,660x930&types=static",
                    apiKey
                )
            }
            val banners = async { images("/heroes/$target?types=static", apiKey) }
            val icons = async { images("/icons/$target?types=static", apiKey) }
            ArtworkUrls(covers = covers.await(), banners = banners.await(), icons = icons.await())
        }

    private suspend fun images(route: String, apiKey: String): List<ArtworkImage> {
        val data = requestData(route, apiKey) ?: return emptyList()
        return (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val url = item.optString("url").ifEmpty { null } ?: return@mapNotNull null
            ArtworkImage(url, item.optString("thumb").ifEmpty { url })
        }
    }

    private suspend fun requestData(route: String, apiKey: String): JSONArray? {
        val response = withContext(Dispatchers.IO) {
            HttpJson.get("$base$route", mapOf("Authorization" to "Bearer ${apiKey.trim()}"))
        }
        return when (response.code) {
            in 200..299 -> JSONObject(response.body.ifBlank { "{}" }).optJSONArray("data")
            401, 403 -> throw MetadataAuthException(
                MetadataSource.STEAMGRIDDB,
                "SteamGridDB authentication failed (HTTP ${response.code})"
            )

            404 -> null
            else -> throw IOException("SteamGridDB request failed with HTTP ${response.code}")
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        const val DEFAULT_BASE_URL = "https://www.steamgriddb.com/api/v2"
    }
}
