package com.example.neatgrid.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder

open class SteamService(
    storeBaseUrl: String = DEFAULT_STORE_URL,
    cdnBaseUrl: String = DEFAULT_CDN_URL
) {
    private val store = storeBaseUrl.trimEnd('/')
    private val cdn = cdnBaseUrl.trimEnd('/')

    open suspend fun searchGames(query: String): List<GameMetadata> {
        if (query.isBlank()) return emptyList()
        val term = URLEncoder.encode(query.trim(), "UTF-8")
        val json = requestJson("$store/api/storesearch/?term=$term&l=english&cc=US")
        val items = json.optJSONArray("items") ?: return emptyList()
        val games = (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optLong("id", -1L).takeIf { it > 0 } ?: return@mapNotNull null
            val name =
                item.optString("name").trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            GameMetadata(
                title = name,
                summary = null,
                rating = null,
                releaseDate = null,
                genres = emptyList(),
                platforms = parsePlatforms(item.optJSONObject("platforms")),
                coverUrl = coverUrl(id.toString()),
                screenshotUrls = emptyList(),
                sourceId = "steam:$id"
            )
        }
        return withoutAddOns(games)
    }

    open suspend fun fetchGameDetails(steamId: String, basicMetadata: GameMetadata): GameMetadata {
        val appId = cleanId(steamId)
        val root = requestJson("$store/api/appdetails?appids=$appId&l=english")
        val entry = root.optJSONObject(appId)
        val data = entry?.takeIf { it.optBoolean("success") }?.optJSONObject("data")
            ?: throw IOException("Steam app $appId not found")
        val artwork = fetchArtwork(appId)
        return basicMetadata.copy(
            title = data.optString("name").trim().ifEmpty { basicMetadata.title },
            summary = data.optString("short_description").let(::plainText) ?: basicMetadata.summary,
            rating = data.optJSONObject("metacritic")?.optDouble("score")?.takeIf { !it.isNaN() }
                ?: basicMetadata.rating,
            releaseDate = data.optJSONObject("release_date")?.optString("date")?.trim()
                ?.ifEmpty { null }
                ?: basicMetadata.releaseDate,
            genres = data.optJSONArray("genres")?.let { genres ->
                (0 until genres.length()).mapNotNull {
                    genres.optJSONObject(it)?.optString("description")?.trim()?.ifEmpty { null }
                }
            }.orEmpty().ifEmpty { basicMetadata.genres },
            platforms = parsePlatforms(data.optJSONObject("platforms")).ifEmpty { basicMetadata.platforms },
            coverUrl = artwork.covers.firstOrNull()?.url ?: data.optString("header_image")
                .ifEmpty { null },
            backdropUrl = artwork.banners.firstOrNull()?.url ?: basicMetadata.backdropUrl,
            screenshotUrls = data.optJSONArray("screenshots")?.let { shots ->
                (0 until shots.length()).mapNotNull {
                    shots.optJSONObject(it)?.optString("path_thumbnail")?.ifEmpty { null }
                }
            }.orEmpty().ifEmpty { basicMetadata.screenshotUrls },
            sourceId = "steam:$appId"
        )
    }

    open suspend fun fetchArtwork(steamId: String): ArtworkUrls = withContext(Dispatchers.IO) {
        val appId = cleanId(steamId)
        val cover = coverUrl(appId)
        val hero = "$cdn/steam/apps/$appId/library_hero.jpg"
        val coverExists = async { HttpJson.exists(cover) }
        val heroExists = async { HttpJson.exists(hero) }
        ArtworkUrls(
            covers = listOfNotNull(
                ArtworkImage(
                    cover,
                    "$cdn/steam/apps/$appId/library_600x900.jpg"
                ).takeIf { coverExists.await() }
            ),
            banners = listOfNotNull(ArtworkImage(hero).takeIf { heroExists.await() })
        )
    }

    private fun coverUrl(appId: String) = "$cdn/steam/apps/$appId/library_600x900_2x.jpg"

    private suspend fun requestJson(url: String): JSONObject {
        val response = withContext(Dispatchers.IO) { HttpJson.get(url) }
        if (response.code !in 200..299) throw IOException("Steam request failed with HTTP ${response.code}")
        return JSONObject(response.body.ifBlank { "{}" })
    }

    private fun cleanId(steamId: String): String {
        return steamId.removePrefix("steam:").trim()
            .ifEmpty { throw IOException("Missing Steam app ID") }
    }

    private fun parsePlatforms(platforms: JSONObject?): List<String> {
        if (platforms == null) return emptyList()
        return listOfNotNull(
            "Windows".takeIf { platforms.optBoolean("windows") },
            "Mac".takeIf { platforms.optBoolean("mac") },
            "Linux".takeIf { platforms.optBoolean("linux") }
        )
    }

    private fun withoutAddOns(games: List<GameMetadata>): List<GameMetadata> {
        val titles = games.map { it.title.lowercase() }.toSet()
        return games.filterNot { game ->
            val title = game.title.lowercase()
            ADD_ON_WORDS.any { it in title } ||
                    titles.any { base -> base != title && title.startsWith("$base - ") }
        }
    }

    private fun plainText(html: String): String? = Jsoup.parse(html).text().trim().ifEmpty { null }

    companion object {
        const val DEFAULT_STORE_URL = "https://store.steampowered.com"
        const val DEFAULT_CDN_URL = "https://cdn.cloudflare.steamstatic.com"
        private val ADD_ON_WORDS =
            listOf("soundtrack", " ost", "artbook", "art book", "content pack", "season pass")
    }
}
