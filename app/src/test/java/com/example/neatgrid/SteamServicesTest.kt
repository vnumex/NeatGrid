package com.example.neatgrid

import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataAuthException
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.SteamGridDbService
import com.example.neatgrid.data.SteamService
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class SteamServicesTest {

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private val requests = mutableListOf<String>()
    private val authHeaders = mutableListOf<String?>()
    private var handler: (method: String, path: String) -> Pair<Int, String> = { _, _ -> 404 to "" }

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val path =
                exchange.requestURI.rawPath + (exchange.requestURI.rawQuery?.let { "?$it" } ?: "")
            requests += "${exchange.requestMethod} $path"
            authHeaders += exchange.requestHeaders.getFirst("Authorization")
            val (status, body) = handler(exchange.requestMethod, path)
            if (exchange.requestMethod == "HEAD") {
                exchange.sendResponseHeaders(status, -1)
            } else {
                val bytes = body.toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun steamSearch_mapsStoreResults() = runBlocking {
        handler = { _, path ->
            if (path.startsWith("/api/storesearch/?term=DREDGE")) {
                200 to """{"items":[{"id":1562430,"name":"DREDGE","platforms":{"windows":true,"mac":true,"linux":false}}]}"""
            } else {
                404 to ""
            }
        }

        val result = SteamService(baseUrl, baseUrl).searchGames("DREDGE").single()

        assertEquals("DREDGE", result.title)
        assertEquals("steam:1562430", result.sourceId)
        assertEquals(listOf("Windows", "Mac"), result.platforms)
    }

    @Test
    fun steamSearch_dropsSoundtracksArtbooksAndAddOns() = runBlocking {
        handler = { _, _ ->
            200 to """{"items":[
              {"id":1,"name":"DREDGE"},
              {"id":2,"name":"DREDGE - The Iron Rig"},
              {"id":3,"name":"Gold Dredge Simulator"},
              {"id":4,"name":"DREDGE - Original Soundtrack"},
              {"id":5,"name":"DREDGE - Digital Artbook"},
              {"id":6,"name":"DAVE THE DIVER - DREDGE Content Pack"}
            ]}"""
        }

        val titles = SteamService(baseUrl, baseUrl).searchGames("DREDGE").map { it.title }

        assertEquals(listOf("DREDGE", "Gold Dredge Simulator"), titles)
    }

    @Test
    fun steamDetails_readsStoreDataAndLibraryArtwork() = runBlocking {
        handler = { method, path ->
            when {
                path.startsWith("/api/appdetails?appids=1562430") -> 200 to """
                    {"1562430":{"success":true,"data":{
                      "name":"DREDGE",
                      "short_description":"A fishing adventure &amp; <b>more</b>",
                      "metacritic":{"score":81},
                      "release_date":{"date":"30 Mar, 2023"},
                      "genres":[{"description":"Adventure"},{"description":"Indie"}],
                      "platforms":{"windows":true,"mac":false,"linux":false},
                      "header_image":"$baseUrl/header.jpg",
                      "screenshots":[{"path_thumbnail":"$baseUrl/s1.jpg","path_full":"$baseUrl/s1-full.jpg"}]
                    }}}
                """.trimIndent()

                method == "HEAD" && path.endsWith("/library_600x900_2x.jpg") -> 200 to ""
                method == "HEAD" && path.endsWith("/library_hero.jpg") -> 200 to ""
                else -> 404 to ""
            }
        }

        val details = SteamService(baseUrl, baseUrl).fetchGameDetails("steam:1562430", basicGame())

        assertEquals("A fishing adventure & more", details.summary)
        assertEquals(81.0, details.rating!!, 0.01)
        assertEquals("30 Mar, 2023", details.releaseDate)
        assertEquals(listOf("Adventure", "Indie"), details.genres)
        assertEquals(listOf("Windows"), details.platforms)
        assertEquals("$baseUrl/steam/apps/1562430/library_600x900_2x.jpg", details.coverUrl)
        assertEquals("$baseUrl/steam/apps/1562430/library_hero.jpg", details.backdropUrl)
        assertEquals(listOf("$baseUrl/s1.jpg"), details.screenshotUrls)
        assertEquals("steam:1562430", details.sourceId)
    }

    @Test
    fun steamDetails_fallsBackToHeaderWhenNoLibraryCover() = runBlocking {
        handler = { _, path ->
            if (path.startsWith("/api/appdetails")) {
                200 to """{"10":{"success":true,"data":{"name":"Old Game","header_image":"$baseUrl/header.jpg"}}}"""
            } else {
                404 to ""
            }
        }

        val details = SteamService(baseUrl, baseUrl).fetchGameDetails("10", basicGame())

        assertEquals("$baseUrl/header.jpg", details.coverUrl)
        assertEquals(null, details.backdropUrl)
    }

    @Test
    fun steamArtwork_usesSmallCoverAsPreview() = runBlocking {
        handler = { method, path ->
            if (method == "HEAD" && path.endsWith("library_600x900_2x.jpg")) 200 to "" else 404 to ""
        }

        val artwork = SteamService(baseUrl, baseUrl).fetchArtwork("steam:7")

        assertEquals("$baseUrl/steam/apps/7/library_600x900_2x.jpg", artwork.covers.single().url)
        assertEquals(
            "$baseUrl/steam/apps/7/library_600x900.jpg",
            artwork.covers.single().previewUrl
        )
        assertTrue(artwork.banners.isEmpty())
    }

    @Test
    fun steamGridDb_sendsKeyAndGroupsArtwork() = runBlocking {
        handler = { _, path ->
            when {
                path.startsWith("/grids/game/42?") -> 200 to """{"success":true,"data":[{"url":"https://sgdb/g1.png","thumb":"https://sgdb/g1-t.png"}]}"""
                path.startsWith("/heroes/game/42?") -> 200 to """{"success":true,"data":[{"url":"https://sgdb/h1.png"}]}"""
                path.startsWith("/icons/game/42?") -> 404 to """{"success":false}"""
                else -> 404 to ""
            }
        }

        val artwork = SteamGridDbService(baseUrl).fetchArtwork("steamgriddb:42", "secret")

        assertEquals("https://sgdb/g1.png", artwork.covers.single().url)
        assertEquals("https://sgdb/g1-t.png", artwork.covers.single().previewUrl)
        assertEquals("https://sgdb/h1.png", artwork.banners.single().previewUrl)
        assertTrue(artwork.icons.isEmpty())
        assertTrue(authHeaders.all { it == "Bearer secret" })
        assertTrue(requests.any { it.contains("dimensions=600x900") })
    }

    @Test
    fun steamGridDb_usesSteamPathForSteamGames() = runBlocking {
        handler = { _, _ -> 200 to """{"success":true,"data":[]}""" }

        SteamGridDbService(baseUrl).fetchSteamArtwork("steam:1562430", "secret")

        assertTrue(requests.all { it.contains("/steam/1562430") })
    }

    @Test
    fun steamGridDb_searchAndAuthFailure() = runBlocking {
        handler = { _, path ->
            if (path.startsWith("/search/autocomplete/Sea%20of%20Stars")) {
                200 to """{"success":true,"data":[{"id":5,"name":"Sea of Stars"}]}"""
            } else {
                401 to """{"success":false}"""
            }
        }
        val service = SteamGridDbService(baseUrl)

        assertEquals(
            "steamgriddb:5",
            service.searchGames("Sea of Stars", "secret").single().sourceId
        )
        try {
            service.fetchArtwork("5", "bad")
            fail("Expected MetadataAuthException")
        } catch (e: MetadataAuthException) {
            assertEquals(MetadataSource.STEAMGRIDDB, e.source)
        }
        assertTrue(service.verifyKey("bad").isFailure)
        assertTrue(service.verifyKey("").isFailure)
    }

    private fun basicGame() = GameMetadata(
        title = "DREDGE",
        summary = null,
        rating = null,
        releaseDate = null,
        genres = emptyList(),
        platforms = emptyList(),
        coverUrl = null,
        screenshotUrls = emptyList()
    )
}
