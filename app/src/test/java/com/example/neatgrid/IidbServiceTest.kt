package com.example.neatgrid

import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.IidbService
import com.example.neatgrid.data.MetadataAuthException
import com.example.neatgrid.data.MetadataSource
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class IidbServiceTest {
    private lateinit var server: HttpServer
    private lateinit var service: IidbService
    private var handler: ((String, String, String?, String) -> Pair<Int, String>)? = null
    private val responseHeaders = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val method = exchange.requestMethod
            val pathWithQuery = exchange.requestURI.rawPath +
                (exchange.requestURI.rawQuery?.let { "?$it" } ?: "")
            val apiKeyHeader = exchange.requestHeaders.getFirst("X-API-Key")
            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val (status, responseBody) = handler?.invoke(method, pathWithQuery, apiKeyHeader, body)
                ?: (404 to "{}")
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            responseHeaders.forEach { (k, v) -> exchange.responseHeaders.set(k, v) }
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        service = IidbService("http://127.0.0.1:${server.address.port}")
    }

    @After
    fun tearDown() {
        responseHeaders.clear()
        server.stop(0)
    }

    @Test
    fun verifyKey_sendsApiKeyHeaderAndParsesTier() = runBlocking {
        var capturedHeader: String? = null
        handler = { method, path, apiKey, _ ->
            if (method == "GET" && path == "/me/access") {
                capturedHeader = apiKey
                if (apiKey == "valid-key") {
                    200 to """
                        {
                          "user_id": 7,
                          "username": "neatgrid",
                          "tier": { "code": "pro", "requests_per_second": 10 }
                        }
                    """.trimIndent()
                } else {
                    401 to """{"detail":"Invalid API key"}"""
                }
            } else {
                404 to "{}"
            }
        }

        val ok = service.verifyKey(" valid-key ")
        assertTrue(ok.isSuccess)
        assertEquals("valid-key", capturedHeader)
        val summary = ok.getOrThrow()
        assertTrue(summary.contains("pro"))
        assertTrue(summary.contains("10"))

        val failed = service.verifyKey("bad-key")
        assertTrue(failed.isFailure)
        val error = failed.exceptionOrNull()
        assertTrue(error is MetadataAuthException)
        assertEquals(MetadataSource.IIDB, (error as MetadataAuthException).source)
    }

    @Test
    fun searchGames_parsesParentSearchResponse() = runBlocking {
        var capturedPath: String? = null
        var capturedKey: String? = null
        handler = { method, path, apiKey, _ ->
            capturedPath = path
            capturedKey = apiKey
            if (method == "GET" && path.startsWith("/search/parents")) {
                200 to """
                    {
                      "results": [
                        {
                          "parent_id": 46185,
                          "name": "Ape Escape 3",
                          "primary_platform": "PlayStation 2",
                          "platform_list": "PlayStation 2, PlayStation 3",
                          "initial_release_year": 2005,
                          "boxart_url": "https://assets.iisu.network/boxart/46185.jpg"
                        }
                      ]
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }

        val results = service.searchGames("Ape Escape 3", "my-key")
        assertEquals("my-key", capturedKey)
        assertTrue(capturedPath!!.contains("q=Ape+Escape+3"))
        assertTrue(capturedPath!!.contains("limit=10"))
        assertEquals(1, results.size)
        val game = results[0]
        assertEquals("iidb:46185", game.sourceId)
        assertEquals("Ape Escape 3", game.title)
        assertEquals(listOf("PlayStation 2", "PlayStation 3"), game.platforms)
        assertEquals("2005", game.releaseDate)
        assertEquals("https://assets.iisu.network/boxart/46185.jpg", game.coverUrl)
    }

    @Test
    fun resolveBestMatch_matched_parsesArtworkAndValidatesPlatform() = runBlocking {
        var lastRequestBody: JSONObject? = null
        handler = { method, path, _, body ->
            if (method == "POST" && path == "/resolve") {
                lastRequestBody = JSONObject(body)
                200 to """
                    {
                      "match": {
                        "status": "matched",
                        "parent": {
                          "id": 46185,
                          "name": "Ape Escape 3",
                          "year": 2005,
                          "primary_platform": "PlayStation 2",
                          "platform_summary": "PlayStation 2"
                        }
                      },
                      "artwork": {
                        "boxart": [{ "url": "https://assets.iisu.network/boxart/46185.jpg" }],
                        "background": [{ "url": "https://assets.iisu.network/bg/46185.jpg" }],
                        "screenshot": [
                          { "url": "https://assets.iisu.network/ss/46185-1.jpg" },
                          { "url": "https://assets.iisu.network/ss/46185-2.jpg" }
                        ]
                      }
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }

        val matched = service.resolveBestMatch("Ape Escape 3", setOf("PlayStation 2"), "my-key")
        assertNotNull(matched)
        assertEquals("ps2", lastRequestBody?.optString("preferred_console"))
        assertEquals("iidb:46185", matched!!.sourceId)
        assertEquals("Ape Escape 3", matched.title)
        assertEquals("2005", matched.releaseDate)
        assertEquals("https://assets.iisu.network/boxart/46185.jpg", matched.coverUrl)
        assertEquals("https://assets.iisu.network/bg/46185.jpg", matched.backdropUrl)
        assertEquals(
            listOf(
                "https://assets.iisu.network/ss/46185-1.jpg",
                "https://assets.iisu.network/ss/46185-2.jpg"
            ),
            matched.screenshotUrls
        )

        val mismatched = service.resolveBestMatch("Ape Escape 3", setOf("Android"), "my-key")
        assertEquals("android", lastRequestBody?.optString("preferred_console"))
        assertNull(mismatched)
    }

    @Test
    fun resolveBestMatch_needsChoice_picksBestCandidateAndFetchesAssets() = runBlocking {
        val calledPaths = mutableListOf<String>()
        handler = { method, path, _, _ ->
            calledPaths.add("$method $path")
            when {
                method == "POST" && path == "/resolve" -> 200 to """
                    {
                      "match": { "status": "needs_choice", "parent": null },
                      "candidates": [
                        {
                          "id": 1001,
                          "name": "Ape Escape Academy",
                          "year": 2004,
                          "primary_platform": "PSP",
                          "platform_summary": "PSP"
                        },
                        {
                          "id": 46185,
                          "name": "Ape Escape 3",
                          "year": 2005,
                          "primary_platform": "PlayStation 2",
                          "platform_summary": "PlayStation 2"
                        }
                      ]
                    }
                """.trimIndent()

                method == "GET" && path.startsWith("/assets/parents/46185") -> 200 to """
                    {
                      "match": {
                        "status": "matched",
                        "parent": {
                          "id": 46185,
                          "name": "Ape Escape 3",
                          "year": 2005,
                          "primary_platform": "PlayStation 2",
                          "platform_summary": "PlayStation 2"
                        }
                      },
                      "artwork": {
                        "boxart": [{ "url": "https://assets.iisu.network/boxart/46185.jpg" }],
                        "background": [{ "url": "https://assets.iisu.network/bg/46185.jpg" }],
                        "screenshot": [{ "url": "https://assets.iisu.network/ss/46185.jpg" }]
                      }
                    }
                """.trimIndent()

                else -> 404 to "{}"
            }
        }

        val resolved = service.resolveBestMatch("Ape Escape 3", setOf("PlayStation 2"), "my-key")
        assertNotNull(resolved)
        assertEquals("iidb:46185", resolved!!.sourceId)
        assertEquals("https://assets.iisu.network/boxart/46185.jpg", resolved.coverUrl)
        assertEquals("https://assets.iisu.network/bg/46185.jpg", resolved.backdropUrl)
        assertEquals(listOf("https://assets.iisu.network/ss/46185.jpg"), resolved.screenshotUrls)
        assertTrue(calledPaths.any { it.startsWith("GET /assets/parents/46185") })
    }

    @Test
    fun fetchGameDetails_stripsIidbPrefixAndHandles429Retry() = runBlocking {
        val attempts = AtomicInteger(0)
        handler = { method, path, _, _ ->
            if (method == "GET" && path.startsWith("/assets/parents/46185")) {
                val count = attempts.incrementAndGet()
                if (count == 1) {
                    responseHeaders["Retry-After"] = "0"
                    429 to """{"detail":"Rate limit exceeded"}"""
                } else {
                    responseHeaders.clear()
                    200 to """
                        {
                          "match": {
                            "status": "matched",
                            "parent": {
                              "id": 46185,
                              "name": "Ape Escape 3",
                              "year": 2005,
                              "primary_platform": "PlayStation 2",
                              "platform_summary": "PlayStation 2"
                            }
                          },
                          "artwork": {
                            "boxart": [{ "url": "https://assets.iisu.network/boxart/46185.jpg" }],
                            "background": [{ "url": "https://assets.iisu.network/bg/46185.jpg" }],
                            "screenshot": [{ "url": "https://assets.iisu.network/ss/46185-1.jpg" }]
                          }
                        }
                    """.trimIndent()
                }
            } else {
                404 to "{}"
            }
        }

        val basic = GameMetadata(
            title = "Ape Escape 3",
            summary = null,
            rating = null,
            releaseDate = null,
            genres = emptyList(),
            platforms = emptyList(),
            coverUrl = null,
            screenshotUrls = emptyList(),
            sourceId = "iidb:46185"
        )

        val prefixed = service.fetchGameDetails("iidb:46185", basic, "my-key")
        assertEquals(2, attempts.get())
        assertEquals("iidb:46185", prefixed.sourceId)
        assertEquals("https://assets.iisu.network/boxart/46185.jpg", prefixed.coverUrl)
        assertEquals("https://assets.iisu.network/bg/46185.jpg", prefixed.backdropUrl)
        assertEquals(listOf("https://assets.iisu.network/ss/46185-1.jpg"), prefixed.screenshotUrls)

        val raw = service.fetchGameDetails("46185", basic, "my-key")
        assertEquals(3, attempts.get())
        assertEquals("iidb:46185", raw.sourceId)
    }

    @Test
    fun resolveBestMatch_notFoundAndToConsoleIdAndAliases() = runBlocking {
        assertEquals("switch", IidbService.toConsoleId(setOf("Nintendo Switch")))
        assertNull(IidbService.toConsoleId(setOf("GameCube", "Wii")))
        assertNull(IidbService.toConsoleId(emptySet()))
        assertTrue(com.example.neatgrid.data.MetadataMatcher.platformsMatch("Nintendo Switch", "Switch"))
        assertTrue(com.example.neatgrid.data.MetadataMatcher.platformsMatch("PlayStation 1", "PSX"))

        var requestJson: JSONObject? = null
        handler = { method, path, _, body ->
            if (method == "POST" && path == "/resolve") {
                requestJson = JSONObject(body)
                200 to """{ "match": { "status": "not_found", "parent": null } }"""
            } else {
                404 to "{}"
            }
        }

        val missing = service.resolveBestMatch("Unknown Game", setOf("GameCube", "Wii"), "my-key")
        assertNull(missing)
        assertEquals(false, requestJson?.has("preferred_console"))
    }
}
