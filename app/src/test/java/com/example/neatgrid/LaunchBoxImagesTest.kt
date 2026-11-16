package com.example.neatgrid

import com.example.neatgrid.data.CachedGameMetadata
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxImageUpgrade
import com.example.neatgrid.data.LaunchBoxImages
import com.example.neatgrid.data.LaunchBoxService
import com.example.neatgrid.data.MetadataCacheStatus
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchBoxImagesTest {

    private val payload = """
        [["ShallowReactive",1],
         {"imageFileName":2,"imageTypeName":3,"fullGameImageFileName":4},
         "0df27a4c.jpg","Box - Front","b06e0533.jpg",
         {"imageFileName":6,"imageTypeName":7,"fullGameImageFileName":8},
         "r2_3a620b60.png","Box - 3D","r2_fa1787b9.png",
         {"imageFileName":10,"fullGameImageFileName":99},
         "orphan.jpg"]
    """.trimIndent()

    private val names =
        mapOf("0df27a4c.jpg" to "b06e0533.jpg", "r2_3a620b60.png" to "r2_fa1787b9.png")

    @Test
    fun fullSizeNames_readsThumbnailToFullSizePairs() {
        assertEquals(names, LaunchBoxImages.fullSizeNames(JSONArray(payload)))
    }

    @Test
    fun fullSizeNames_readsPayloadScriptFromPage() {
        val html =
            "<html><body><script type=\"application/json\" id=\"__NUXT_DATA__\">$payload</script></body></html>"
        assertEquals(names, LaunchBoxImages.fullSizeNames(Jsoup.parse(html)))
    }

    @Test
    fun fullSizeNames_isEmptyForMissingOrBrokenPayload() {
        assertTrue(LaunchBoxImages.fullSizeNames(Jsoup.parse("<html></html>")).isEmpty())
        val broken = "<script id=\"__NUXT_DATA__\">not json</script>"
        assertTrue(LaunchBoxImages.fullSizeNames(Jsoup.parse(broken)).isEmpty())
    }

    @Test
    fun toFullSize_swapsOnlyKnownFileNamesAndKeepsHost() {
        assertEquals(
            "https://images.launchbox-app.com//b06e0533.jpg",
            LaunchBoxImages.toFullSize("https://images.launchbox-app.com//0df27a4c.jpg", names)
        )
        assertEquals(
            "https://gamesdb-images.launchbox.gg/r2_fa1787b9.png",
            LaunchBoxImages.toFullSize("https://gamesdb-images.launchbox.gg/r2_3a620b60.png", names)
        )
        assertEquals(
            "https://images.launchbox-app.com//other.jpg",
            LaunchBoxImages.toFullSize("https://images.launchbox-app.com//other.jpg", names)
        )
    }

    @Test
    fun isLaunchBoxImage_recognisesBothHosts() {
        assertTrue(LaunchBoxImages.isLaunchBoxImage("https://images.launchbox-app.com//a.jpg"))
        assertTrue(LaunchBoxImages.isLaunchBoxImage("https://gamesdb-images.launchbox.gg/r2_a.png"))
        assertFalse(LaunchBoxImages.isLaunchBoxImage("https://assets.iisu.network/a.png"))
        assertFalse(LaunchBoxImages.isLaunchBoxImage(null))
    }

    @Test
    fun parseArtwork_returnsFullSizeUrlsWithThumbnailPreviews() {
        val html = """
            <html><body>
              <h3>Box - Front</h3>
              <div><img src="https://images.launchbox-app.com//0df27a4c.jpg" /></div>
              <script type="application/json" id="__NUXT_DATA__">$payload</script>
            </body></html>
        """.trimIndent()

        val cover = LaunchBoxService().parseArtwork(Jsoup.parse(html)).covers.single()

        assertEquals("https://images.launchbox-app.com//b06e0533.jpg", cover.url)
        assertEquals("https://images.launchbox-app.com//0df27a4c.jpg", cover.previewUrl)
    }

    @Test
    fun parseGameDetails_usesFullSizeCoverButKeepsScreenshotThumbnails() {
        val html = """
            <html><body>
              <h1>Sea of Stars</h1>
              <h3>Box - Front</h3>
              <div><img src="https://images.launchbox-app.com//0df27a4c.jpg" /></div>
              <h3>Screenshot - Gameplay</h3>
              <div><img src="https://gamesdb-images.launchbox.gg/r2_3a620b60.png" /></div>
              <script type="application/json" id="__NUXT_DATA__">$payload</script>
            </body></html>
        """.trimIndent()

        val details = LaunchBoxService().parseGameDetails(
            Jsoup.parse(html),
            game(sourceId = "1-sea-of-stars")
        )

        assertEquals("https://images.launchbox-app.com//b06e0533.jpg", details.coverUrl)
        assertEquals(
            listOf("https://gamesdb-images.launchbox.gg/r2_3a620b60.png"),
            details.screenshotUrls
        )
    }

    @Test
    fun upgrade_skipsCachesAlreadyMarkedFullSize() = runBlocking {
        val service = FakeLaunchBox()
        val cached = CachedGameMetadata(
            game(cover = "https://images.launchbox-app.com//0df27a4c.jpg"),
            MetadataCacheStatus.CUSTOM
        )

        assertNull(LaunchBoxImageUpgrade(service).upgrade(cached))
        assertEquals(0, service.pageRequests)
    }

    @Test
    fun upgrade_marksGamesWithoutLaunchBoxImagesWithoutNetwork() = runBlocking {
        val service = FakeLaunchBox()
        val cached = CachedGameMetadata(
            game(cover = "https://assets.iisu.network/box.png"),
            MetadataCacheStatus.AUTOMATIC,
            false
        )

        val upgraded = LaunchBoxImageUpgrade(service).upgrade(cached)

        assertEquals(cached.copy(fullSizeImages = true), upgraded)
        assertEquals(0, service.pageRequests)
    }

    @Test
    fun upgrade_swapsThumbnailsForLaunchBoxSourcedGame() = runBlocking {
        val service = FakeLaunchBox()
        val cached = CachedGameMetadata(
            game(
                sourceId = "1-sea-of-stars",
                cover = "https://images.launchbox-app.com//0df27a4c.jpg"
            ),
            MetadataCacheStatus.AUTOMATIC,
            false
        )

        val upgraded = LaunchBoxImageUpgrade(service).upgrade(cached)!!

        assertEquals("https://images.launchbox-app.com//b06e0533.jpg", upgraded.metadata.coverUrl)
        assertTrue(upgraded.fullSizeImages)
        assertEquals(MetadataCacheStatus.AUTOMATIC, upgraded.status)
        assertEquals(listOf("1-sea-of-stars"), service.requestedPages)
    }

    @Test
    fun upgrade_findsLaunchBoxPageByTitleForOtherSources() = runBlocking {
        val service = FakeLaunchBox(
            searchResults = listOf(
                game(sourceId = "9-sea-of-stars-psp").copy(platforms = listOf("Sony PSP")),
                game(sourceId = "1-sea-of-stars").copy(platforms = listOf("Nintendo Switch"))
            )
        )
        val cached = CachedGameMetadata(
            game(sourceId = "iidb:1", cover = "https://gamesdb-images.launchbox.gg/r2_3a620b60.png")
                .copy(platforms = listOf("Nintendo Switch")),
            MetadataCacheStatus.CUSTOM,
            false
        )

        val upgraded = LaunchBoxImageUpgrade(service).upgrade(cached)!!

        assertEquals(
            "https://gamesdb-images.launchbox.gg/r2_fa1787b9.png",
            upgraded.metadata.coverUrl
        )
        assertEquals(listOf("1-sea-of-stars"), service.requestedPages)
    }

    private fun game(sourceId: String? = null, cover: String? = null) = GameMetadata(
        title = "Sea of Stars",
        summary = null,
        rating = null,
        releaseDate = null,
        genres = emptyList(),
        platforms = emptyList(),
        coverUrl = cover,
        screenshotUrls = emptyList(),
        sourceId = sourceId
    )

    private inner class FakeLaunchBox(
        private val searchResults: List<GameMetadata> = emptyList()
    ) : LaunchBoxService() {
        val requestedPages = mutableListOf<String>()
        val pageRequests get() = requestedPages.size

        override suspend fun searchGames(query: String): List<GameMetadata> = searchResults

        override suspend fun fetchFullSizeNames(detailsUrlSuffix: String): Map<String, String> {
            requestedPages += detailsUrlSuffix
            return names
        }
    }
}
