package com.example.neatgrid

import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataAuthException
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.RawgService
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class RawgServiceTest {
    private lateinit var server: HttpServer
    private lateinit var service: RawgService
    private var handler: ((String, String) -> Pair<Int, String>)? = null

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val method = exchange.requestMethod
            val pathWithQuery = exchange.requestURI.rawPath +
                (exchange.requestURI.rawQuery?.let { "?$it" } ?: "")
            val (status, responseBody) = handler?.invoke(method, pathWithQuery) ?: (404 to "{}")
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        service = RawgService("http://127.0.0.1:${server.address.port}")
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun searchGames_parsesResultsAndScalesRating() = runBlocking {
        var capturedMethod: String? = null
        var capturedPath: String? = null
        handler = { method, path ->
            capturedMethod = method
            capturedPath = path
            if (method == "GET" && path.startsWith("/games?")) {
                200 to """
                    {
                      "results": [
                        {
                          "id": 3498,
                          "name": "Grand Theft Auto V",
                          "released": "2013-09-17",
                          "background_image": "https://media.rawg.io/media/games/gta5.jpg",
                          "rating": 4.47,
                          "genres": [
                            { "id": 4, "name": "Action" },
                            { "id": 3, "name": "Adventure" }
                          ],
                          "platforms": [
                            { "platform": { "id": 4, "name": "PC" } },
                            { "platform": { "id": 18, "name": "PlayStation 4" } }
                          ],
                          "short_screenshots": [
                            { "id": -1, "image": "https://media.rawg.io/media/games/gta5.jpg" },
                            { "id": 101, "image": "https://media.rawg.io/media/screenshots/gta5-1.jpg" }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }

        val results = service.searchGames("Grand Theft Auto V", " rawg-key ")
        assertEquals("GET", capturedMethod)
        assertTrue(capturedPath!!.startsWith("/games?"))
        assertTrue(capturedPath!!.contains("key=rawg-key"))
        assertTrue(capturedPath!!.contains("search=Grand+Theft+Auto+V"))
        assertTrue(capturedPath!!.contains("page_size=10"))

        assertEquals(1, results.size)
        val game = results[0]
        assertEquals("rawg:3498", game.sourceId)
        assertEquals("Grand Theft Auto V", game.title)
        assertEquals("2013-09-17", game.releaseDate)
        assertEquals("https://media.rawg.io/media/games/gta5.jpg", game.coverUrl)
        assertEquals(89.4, game.rating!!, 0.001)
        assertEquals(listOf("Action", "Adventure"), game.genres)
        assertEquals(listOf("PC", "PlayStation 4"), game.platforms)
        assertEquals(
            listOf(
                "https://media.rawg.io/media/games/gta5.jpg",
                "https://media.rawg.io/media/screenshots/gta5-1.jpg"
            ),
            game.screenshotUrls
        )
    }

    @Test
    fun fetchGameDetails_populatesSummaryAndAdditionalBackdrop() = runBlocking {
        val capturedPaths = mutableListOf<String>()
        handler = { method, path ->
            capturedPaths.add(path)
            if (method == "GET" && path == "/games/3498?key=rawg-key") {
                200 to """
                    {
                      "id": 3498,
                      "name": "Grand Theft Auto V",
                      "description_raw": "Los Santos is a sprawling sun-soaked metropolis.",
                      "released": "2013-09-17",
                      "background_image": "https://media.rawg.io/media/games/gta5.jpg",
                      "background_image_additional": "https://media.rawg.io/media/screenshots/gta5-bg.jpg",
                      "rating": 4.47,
                      "genres": [
                        { "id": 4, "name": "Action" }
                      ],
                      "platforms": [
                        { "platform": { "id": 4, "name": "PC" } }
                      ]
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }

        val existingScreenshots = listOf("https://media.rawg.io/media/screenshots/gta5-1.jpg")
        val basic = GameMetadata(
            title = "Grand Theft Auto V",
            summary = null,
            rating = 89.4,
            releaseDate = "2013-09-17",
            genres = listOf("Action"),
            platforms = listOf("PC"),
            coverUrl = "https://media.rawg.io/media/games/gta5.jpg",
            screenshotUrls = existingScreenshots,
            sourceId = "rawg:3498"
        )

        val fromPrefixed = service.fetchGameDetails("rawg:3498", basic, "rawg-key")
        assertEquals("rawg:3498", fromPrefixed.sourceId)
        assertEquals("Los Santos is a sprawling sun-soaked metropolis.", fromPrefixed.summary)
        assertEquals(
            "https://media.rawg.io/media/screenshots/gta5-bg.jpg",
            fromPrefixed.backdropUrl
        )
        assertEquals(existingScreenshots, fromPrefixed.screenshotUrls)

        val fromBareId = service.fetchGameDetails("3498", basic, "rawg-key")
        assertEquals("rawg:3498", fromBareId.sourceId)
        assertEquals(2, capturedPaths.size)
        assertTrue(capturedPaths.all { it == "/games/3498?key=rawg-key" })
    }

    @Test
    fun searchGames_throwsMetadataAuthExceptionOn401() = runBlocking {
        handler = { _, _ -> 401 to """{"error":"Invalid API key."}""" }

        val thrown = runCatching {
            service.searchGames("Grand Theft Auto V", "bad-key")
        }.exceptionOrNull()

        assertTrue(thrown is MetadataAuthException)
        assertEquals(MetadataSource.RAWG, (thrown as MetadataAuthException).source)
    }

    @Test
    fun fetchGameDetails_throwsMetadataAuthExceptionOn403() = runBlocking {
        handler = { _, _ -> 403 to """{"error":"Forbidden"}""" }

        val basic = GameMetadata(
            title = "Grand Theft Auto V",
            summary = null,
            rating = null,
            releaseDate = null,
            genres = emptyList(),
            platforms = emptyList(),
            coverUrl = null,
            screenshotUrls = emptyList(),
            sourceId = "rawg:3498"
        )

        val thrown = runCatching {
            service.fetchGameDetails("rawg:3498", basic, "bad-key")
        }.exceptionOrNull()

        assertTrue(thrown is MetadataAuthException)
        assertEquals(MetadataSource.RAWG, (thrown as MetadataAuthException).source)
    }

    @Test
    fun fetchArtwork_returnsBackgroundsAsBanners() = runBlocking {
        handler = { method, path ->
            if (method == "GET" && path.startsWith("/games/456?")) {
                200 to """
                    {
                      "id": 456,
                      "background_image": "https://media.rawg.io/media/games/bg1.jpg",
                      "background_image_additional": "https://rawg.example/bg2.jpg"
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }

        val artwork = service.fetchArtwork("rawg:456", "key")

        assertEquals(emptyList<String>(), artwork.covers.map { it.url })
        assertEquals(
            listOf("https://media.rawg.io/media/games/bg1.jpg", "https://rawg.example/bg2.jpg"),
            artwork.banners.map { it.url }
        )
        assertEquals(
            "https://media.rawg.io/media/resize/420/-/games/bg1.jpg",
            artwork.banners[0].previewUrl
        )
        assertEquals("https://rawg.example/bg2.jpg", artwork.banners[1].previewUrl)
        assertEquals(emptyList<String>(), artwork.icons.map { it.url })
    }
}
