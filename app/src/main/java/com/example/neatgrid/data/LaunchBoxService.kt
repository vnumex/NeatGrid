package com.example.neatgrid.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

class LaunchBoxService {

    suspend fun searchGames(query: String): List<GameMetadata> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlSpec = "https://gamesdb.launchbox-app.com/games/results/$encodedQuery"
        val doc = Jsoup.connect(urlSpec)
            .userAgent(USER_AGENT)
            .timeout(10000)
            .get()
        parseSearchResults(doc)
    }

    suspend fun fetchGameDetails(detailsUrlSuffix: String, basicMetadata: GameMetadata): GameMetadata = withContext(Dispatchers.IO) {
        val urlSpec = "https://gamesdb.launchbox-app.com/games/details/$detailsUrlSuffix"
        val doc = Jsoup.connect(urlSpec)
            .userAgent(USER_AGENT)
            .timeout(10000)
            .get()
        parseGameDetails(doc, basicMetadata)
    }

    fun parseSearchResults(doc: Document): List<GameMetadata> {
        val results = mutableListOf<GameMetadata>()
        val cards = doc.select("div.games-grid-card")
        for (card in cards) {
            val linkEl = card.selectFirst("a.list-item") ?: continue
            val href = linkEl.attr("href") // e.g., "/games/details/2224-sonic-advance"
            val detailsUrlSuffix = href.substringAfter("/games/details/")
            
            val titleEl = card.selectFirst("div.cardTitle h3")
            val title = titleEl?.text() ?: "Unknown Game"

            val platformEl = card.selectFirst("div.cardTitle p")
            val platform = platformEl?.text() ?: ""
            
            val coverImgEl = card.selectFirst("div.cardImgPart img")
            val coverUrl = coverImgEl?.absUrl("src")?.takeIf { it.isNotEmpty() }

            val releaseEl = card.selectFirst("div.releaseDate h5")
            val releaseDate = releaseEl?.text()

            // Parse rating: data-rateit-value (can be out of 5, e.g. 3.94)
            val rateitEl = card.selectFirst("div.rateit")
            val ratingRaw = rateitEl?.attr("data-rateit-value")?.toDoubleOrNull()
            val ratingPercentage = ratingRaw?.let { it * 20.0 } // 3.94 * 20 = 78.8%

            results.add(
                GameMetadata(
                    title = title,
                    summary = null,
                    rating = ratingPercentage,
                    releaseDate = releaseDate,
                    genres = emptyList(),
                    platforms = if (platform.isNotEmpty()) listOf(platform) else emptyList(),
                    coverUrl = coverUrl,
                    screenshotUrls = emptyList(),
                    sourceId = detailsUrlSuffix
                )
            )
        }
        return results
    }

    fun parseGameDetails(doc: Document, basicMetadata: GameMetadata): GameMetadata {
        // 1. Title (fallback to doc h1 if basicMetadata has generic title)
        val h1El = doc.selectFirst("h1")
        val title = h1El?.text()?.takeIf { it.isNotEmpty() } ?: basicMetadata.title

        // 2. Overview / Summary
        val paragraphs = doc.select("p.text-dark-100, p.text-body-lg")
        val overview = paragraphs.joinToString("\n\n") { it.text() }.trim()

        // 3. Rating
        var rating = basicMetadata.rating
        if (rating == null) {
            val fieldset = doc.selectFirst("fieldset")
            val ratingDiv = fieldset?.selectFirst("div")
            val styleAttr = ratingDiv?.attr("style") ?: "" // style="width:3.9466960356624115em;"
            val regex = Regex("width:\\s*([0-9.]+)\\s*em")
            val match = regex.find(styleAttr)
            if (match != null) {
                val ratingStars = match.groupValues[1].toDoubleOrNull()
                if (ratingStars != null) {
                    rating = ratingStars * 20.0
                }
            }
        }

        // 4. Release Date (fallback to datetime of time element)
        var releaseDate = basicMetadata.releaseDate
        if (releaseDate.isNullOrEmpty()) {
            val timeEl = doc.selectFirst("time")
            releaseDate = timeEl?.attr("datetime") ?: timeEl?.text()
        }

        // 5. Genres
        val genres = mutableListOf<String>()
        val genreDt = doc.select("dt").firstOrNull { it.text().contains("Genre", ignoreCase = true) }
        if (genreDt != null) {
            val genreDd = genreDt.nextElementSibling()
            if (genreDd != null) {
                val links = genreDd.select("a")
                if (links.isNotEmpty()) {
                    genres.addAll(links.map { it.text() })
                } else {
                    val text = genreDd.text()
                    if (text.isNotEmpty()) {
                        genres.addAll(text.split(",").map { it.trim() })
                    }
                }
            }
        }

        // 6. Platforms
        val platforms = basicMetadata.platforms.toMutableList()
        if (platforms.isEmpty()) {
            val platformDt = doc.select("dt").firstOrNull { it.text().contains("Platform", ignoreCase = true) }
            if (platformDt != null) {
                val platformDd = platformDt.nextElementSibling()
                if (platformDd != null) {
                    val links = platformDd.select("a")
                    if (links.isNotEmpty()) {
                        platforms.addAll(links.map { it.text() })
                    } else {
                        val text = platformDd.text()
                        if (text.isNotEmpty()) {
                            platforms.add(text)
                        }
                    }
                }
            }
        }

        // 7. Cover URL
        var coverUrl = basicMetadata.coverUrl
        val boxFrontHeading = doc.select("h3").firstOrNull { it.text().contains("Box - Front", ignoreCase = true) }
        if (boxFrontHeading != null) {
            val container = boxFrontHeading.nextElementSibling()
            val coverImg = container?.selectFirst("img")
            if (coverImg != null) {
                coverUrl = coverImg.absUrl("src")
            }
        }

        val screenshots = mutableListOf<String>()
        val backdropImg = doc.selectFirst("main img.opacity-40") ?: doc.selectFirst("main img[class*=opacity]")
        val backdropUrl = backdropImg?.absUrl("src")?.takeIf { it.isNotEmpty() }

        val headings = doc.select("h3")
        for (heading in headings) {
            val headingText = heading.text()
            if (headingText.startsWith("Screenshot", ignoreCase = true)) {
                val container = heading.nextElementSibling()
                if (container != null) {
                    val imgs = container.select("img")
                    for (img in imgs) {
                        val imgUrl = img.absUrl("src")
                        if (imgUrl.isNotEmpty() && !screenshots.contains(imgUrl) && imgUrl != coverUrl) {
                            screenshots.add(imgUrl)
                        }
                    }
                }
            }
        }

        return basicMetadata.copy(
            title = title,
            summary = overview.takeIf { it.isNotEmpty() } ?: basicMetadata.summary,
            rating = rating,
            releaseDate = releaseDate,
            genres = genres,
            platforms = platforms,
            coverUrl = coverUrl,
            screenshotUrls = screenshots,
            backdropUrl = backdropUrl
        )
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
