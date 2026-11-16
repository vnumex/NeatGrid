package com.example.neatgrid

import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.data.ArtworkUrls
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.IidbService
import com.example.neatgrid.data.LaunchBoxService
import com.example.neatgrid.data.MetadataAuthException
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataConfig
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.RawgService
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class MetadataRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var iidbServer: HttpServer
    private lateinit var rawgServer: HttpServer
    private lateinit var iidbService: IidbService
    private lateinit var rawgService: RawgService

    private var iidbHandler: ((String, String) -> Pair<Int, String>)? = null
    private var rawgHandler: ((String, String) -> Pair<Int, String>)? = null
    private val iidbRequests = AtomicInteger(0)
    private val rawgRequests = AtomicInteger(0)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("metadata-repo-test").toFile()

        iidbServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        iidbServer.createContext("/") { exchange ->
            iidbRequests.incrementAndGet()
            val method = exchange.requestMethod
            val pathWithQuery = exchange.requestURI.rawPath +
                (exchange.requestURI.rawQuery?.let { "?$it" } ?: "")
            val (status, responseBody) = iidbHandler?.invoke(method, pathWithQuery) ?: (404 to "{}")
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        iidbServer.start()
        iidbService = IidbService("http://127.0.0.1:${iidbServer.address.port}")

        rawgServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        rawgServer.createContext("/") { exchange ->
            rawgRequests.incrementAndGet()
            val method = exchange.requestMethod
            val pathWithQuery = exchange.requestURI.rawPath +
                (exchange.requestURI.rawQuery?.let { "?$it" } ?: "")
            val (status, responseBody) = rawgHandler?.invoke(method, pathWithQuery) ?: (404 to "{}")
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        rawgServer.start()
        rawgService = RawgService("http://127.0.0.1:${rawgServer.address.port}")
    }

    @After
    fun tearDown() {
        iidbServer.stop(0)
        rawgServer.stop(0)
        tempDir.deleteRecursively()
    }

    @Test
    fun lookup_skipsUnkeyedIidbAndFallsBackToLaunchBox() = runBlocking {
        val fakeLaunchBox = FakeLaunchBoxService(
            searchResults = listOf(
                sampleGame(
                    title = "Ape Escape 3",
                    platforms = listOf("Sony PlayStation 2"),
                    coverUrl = "https://lb.example/ape3.jpg",
                    sourceId = "123-ape-escape-3"
                )
            ),
            detailsById = mapOf(
                "123-ape-escape-3" to sampleGame(
                    title = "Ape Escape 3",
                    summary = "Catch monkeys",
                    rating = 88.0,
                    releaseDate = "2006-01-17",
                    genres = listOf("Platformer"),
                    platforms = listOf("Sony PlayStation 2"),
                    coverUrl = "https://lb.example/ape3-full.jpg",
                    sourceId = "123-ape-escape-3"
                )
            )
        )
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX, MetadataSource.RAWG),
                iidbApiKey = "",
                rawgApiKey = ""
            )
        }

        val cached = repository.lookup("Ape Escape 3", setOf("PS2"))

        assertEquals(0, iidbRequests.get())
        assertEquals(0, rawgRequests.get())
        assertEquals(MetadataCacheStatus.AUTOMATIC, cached.status)
        assertEquals("123-ape-escape-3", cached.metadata.sourceId)
        assertEquals("Catch monkeys", cached.metadata.summary)
    }

    @Test
    fun lookup_iidbMatchBackfillsSummaryFromNextSource() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "POST" && path == "/resolve") {
                200 to """
                    {
                      "match": {
                        "status": "matched",
                        "parent": {
                          "id": 99,
                          "name": "Ape Escape 3",
                          "year": 2005,
                          "primary_platform": "PlayStation 2"
                        }
                      },
                      "artwork": {
                        "boxart": [{"url": "https://cdn.example/box.png"}],
                        "background": [{"url": "https://cdn.example/bg.png"}],
                        "screenshot": [{"url": "https://cdn.example/s1.png"}]
                      }
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            searchResults = listOf(
                sampleGame(
                    title = "Ape Escape 3",
                    platforms = listOf("Sony PlayStation 2"),
                    coverUrl = "https://lb.example/box.jpg",
                    sourceId = "123-ape-escape-3"
                )
            ),
            detailsById = mapOf(
                "123-ape-escape-3" to sampleGame(
                    title = "Ape Escape 3",
                    summary = "Catch monkeys",
                    rating = 88.0,
                    releaseDate = "2006-01-17",
                    genres = listOf("Platformer"),
                    platforms = listOf("Sony PlayStation 2"),
                    coverUrl = "https://lb.example/box-full.jpg",
                    backdropUrl = "https://lb.example/bg.jpg",
                    screenshotUrls = listOf("https://lb.example/shot.jpg"),
                    sourceId = "123-ape-escape-3"
                )
            )
        )
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX),
                iidbApiKey = "iidb-secret",
                rawgApiKey = ""
            )
        }

        val cached = repository.lookup("Ape Escape 3", setOf("PS2"))

        assertEquals(MetadataCacheStatus.AUTOMATIC, cached.status)
        assertEquals("iidb:99", cached.metadata.sourceId)
        assertEquals("Ape Escape 3", cached.metadata.title)
        assertEquals("https://cdn.example/box.png", cached.metadata.coverUrl)
        assertEquals("https://cdn.example/bg.png", cached.metadata.backdropUrl)
        assertEquals(listOf("https://cdn.example/s1.png"), cached.metadata.screenshotUrls)
        assertEquals("2005", cached.metadata.releaseDate)
        assertEquals("Catch monkeys", cached.metadata.summary)
        assertEquals(listOf("Platformer"), cached.metadata.genres)
        assertEquals(88.0, cached.metadata.rating!!, 0.01)
    }

    @Test
    fun lookup_iidbMatchBackfillsSummaryUsingMatchedGamePlatforms() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "POST" && path == "/resolve") {
                200 to """
                    {
                      "match": {
                        "status": "matched",
                        "parent": {
                          "id": 171196,
                          "name": "DREDGE",
                          "year": 2023,
                          "primary_platform": "Nintendo Switch",
                          "platform_summary": ["Nintendo Switch", "Android", "PC", "PlayStation 5"]
                        }
                      },
                      "artwork": { "boxart": [{"url": "https://cdn.example/dredge.png"}] }
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            searchResults = listOf(
                sampleGame(title = "Dredge", platforms = listOf("Windows"), sourceId = "1-dredge"),
                sampleGame(
                    title = "Dredge",
                    platforms = listOf("Sony Playstation 5"),
                    sourceId = "2-dredge"
                )
            ),
            detailsById = mapOf(
                "1-dredge" to sampleGame(
                    title = "Dredge",
                    summary = "A fishing adventure with a sinister undercurrent.",
                    genres = listOf("Adventure"),
                    platforms = listOf("Windows"),
                    sourceId = "1-dredge"
                )
            )
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "iidb-secret")

        val cached = repository.lookup("DREDGE", setOf("Android"))

        assertEquals("iidb:171196", cached.metadata.sourceId)
        assertEquals("A fishing adventure with a sinister undercurrent.", cached.metadata.summary)
        assertEquals(listOf("Adventure"), cached.metadata.genres)
        assertEquals("https://cdn.example/dredge.png", cached.metadata.coverUrl)
    }

    @Test
    fun lookup_rethrowsAuthErrorWhenNoFallbackMatches() = runBlocking {
        iidbHandler = { _, _ -> 401 to """{"error":"Invalid API key"}""" }
        val fakeLaunchBox = FakeLaunchBoxService(searchResults = emptyList())
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX),
                iidbApiKey = "bad-key",
                rawgApiKey = ""
            )
        }

        try {
            repository.lookup("Ape Escape 3", setOf("PS2"))
            fail("Expected MetadataAuthException")
        } catch (e: MetadataAuthException) {
            assertEquals(MetadataSource.IIDB, e.source)
        }
    }

    @Test
    fun search_returnsPartialResultsAndWarningWhenOneSourceFailsAuth() = runBlocking {
        iidbHandler = { _, _ -> 401 to """{"error":"Unauthorized"}""" }
        val launchBoxItem = sampleGame(
            title = "Ape Escape 3",
            platforms = listOf("Sony PlayStation 2"),
            coverUrl = "https://lb.example/ape3.jpg",
            sourceId = "123-ape-escape-3"
        )
        val fakeLaunchBox = FakeLaunchBoxService(searchResults = listOf(launchBoxItem))
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX),
                iidbApiKey = "bad-key",
                rawgApiKey = ""
            )
        }

        val searchResult = repository.search("Ape Escape 3")

        assertEquals(listOf(launchBoxItem), searchResult.results)
        assertTrue(searchResult.warnings.isNotEmpty())
        assertTrue(searchResult.warnings.any { it.contains("iiDB") })
    }

    @Test
    fun search_throwsIOExceptionWhenAllActiveSourcesFailWithIOException() = runBlocking {
        iidbHandler = { _, _ -> 500 to """{"error":"Server error"}""" }
        val fakeLaunchBox = FakeLaunchBoxService(
            searchError = IOException("LaunchBox network error")
        )
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX),
                iidbApiKey = "iidb-secret",
                rawgApiKey = ""
            )
        }

        try {
            repository.search("Ape Escape 3")
            fail("Expected IOException")
        } catch (_: IOException) {
        }
    }

    @Test
    fun search_throwsAuthErrorWhenAuthFailsAndRemainingActiveSourcesFail() = runBlocking {
        iidbHandler = { _, _ -> 401 to """{"error":"Unauthorized"}""" }
        val fakeLaunchBox = FakeLaunchBoxService(
            searchError = IOException("LaunchBox network error")
        )
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX),
                iidbApiKey = "bad-key",
                rawgApiKey = ""
            )
        }

        try {
            repository.search("Ape Escape 3")
            fail("Expected MetadataAuthException")
        } catch (e: MetadataAuthException) {
            assertEquals(MetadataSource.IIDB, e.source)
        }
    }

    @Test
    fun resolve_routesLegacyUnprefixedIdToLaunchBoxAndIidbIdToIidbWithSummaryBackfill() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "GET" && path.startsWith("/assets/parents/99")) {
                200 to """
                    {
                      "match": {
                        "parent": {
                          "id": 99,
                          "name": "Ape Escape 3",
                          "year": 2005,
                          "primary_platform": "PlayStation 2"
                        }
                      },
                      "artwork": {
                        "boxart": [{"url": "https://cdn.example/box.png"}],
                        "background": [{"url": "https://cdn.example/bg.png"}],
                        "screenshot": [{"url": "https://cdn.example/s1.png"}]
                      }
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }
        rawgHandler = { method, path ->
            if (method == "GET" && path.startsWith("/games/456?")) {
                200 to """
                    {
                      "id": 456,
                      "name": "Ape Escape 3",
                      "description_raw": "RAWG monkey adventure",
                      "rating": 4.2,
                      "released": "2006-01-17",
                      "genres": [{"name": "Platformer"}],
                      "platforms": [{"platform": {"name": "PlayStation 2"}}],
                      "background_image": "https://rawg.example/cover.jpg",
                      "background_image_additional": "https://rawg.example/bg.jpg"
                    }
                """.trimIndent()
            } else {
                404 to "{}"
            }
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            searchResults = listOf(
                sampleGame(
                    title = "Ape Escape 3",
                    platforms = listOf("Sony PlayStation 2"),
                    sourceId = "123-ape-escape-3"
                )
            ),
            detailsById = mapOf(
                "123-ape-escape-3" to sampleGame(
                    title = "Ape Escape 3",
                    summary = "Catch monkeys",
                    rating = 86.0,
                    releaseDate = "2006-01-17",
                    genres = listOf("Platformer"),
                    platforms = listOf("Sony PlayStation 2"),
                    coverUrl = "https://lb.example/cover.jpg",
                    sourceId = "123-ape-escape-3"
                )
            )
        )
        val repository = MetadataRepository(
            metadataDirectory = tempDir,
            service = fakeLaunchBox,
            iidbService = iidbService,
            rawgService = rawgService
        ) {
            MetadataConfig(
                order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX, MetadataSource.RAWG),
                iidbApiKey = "iidb-secret",
                rawgApiKey = "rawg-secret"
            )
        }

        val resolvedLaunchBox = repository.resolve(
            sampleGame(
                title = "Ape Escape 3",
                platforms = listOf("Sony PlayStation 2"),
                sourceId = "123-ape-escape-3"
            )
        )
        assertEquals("123-ape-escape-3", resolvedLaunchBox.sourceId)
        assertEquals("Catch monkeys", resolvedLaunchBox.summary)
        assertEquals("https://lb.example/cover.jpg", resolvedLaunchBox.coverUrl)
        assertEquals(0, iidbRequests.get())
        assertEquals(0, rawgRequests.get())

        val resolvedIidb = repository.resolve(
            sampleGame(
                title = "Ape Escape 3",
                platforms = listOf("PlayStation 2"),
                sourceId = "iidb:99"
            )
        )
        assertEquals("iidb:99", resolvedIidb.sourceId)
        assertEquals("https://cdn.example/box.png", resolvedIidb.coverUrl)
        assertEquals("https://cdn.example/bg.png", resolvedIidb.backdropUrl)
        assertEquals("Catch monkeys", resolvedIidb.summary)
        assertEquals(listOf("Platformer"), resolvedIidb.genres)

        val resolvedRawg = repository.resolve(
            sampleGame(
                title = "Ape Escape 3",
                platforms = listOf("PlayStation 2"),
                sourceId = "rawg:456"
            )
        )
        assertEquals("rawg:456", resolvedRawg.sourceId)
        assertEquals("RAWG monkey adventure", resolvedRawg.summary)
        assertEquals("https://rawg.example/cover.jpg", resolvedRawg.coverUrl)
        assertEquals("https://rawg.example/bg.jpg", resolvedRawg.backdropUrl)
    }

    @Test
    fun findIcon_returnsExistingIconWithoutNetwork() = runBlocking {
        val repository = repositoryWith(FakeLaunchBoxService(), iidbKey = "iidb-secret")

        val icon = repository.findIcon(
            sampleGame(
                title = "Ape Escape 3",
                iconUrl = "https://cdn.example/known.png"
            )
        )

        assertEquals("https://cdn.example/known.png", icon)
        assertEquals(0, iidbRequests.get())
    }

    @Test
    fun findIcon_fetchesIidbAssetsForIidbSourcedGame() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "GET" && path.startsWith("/assets/parents/99")) {
                200 to iidbParentJson(icon = "https://cdn.example/icon.png")
            } else {
                404 to "{}"
            }
        }
        val repository = repositoryWith(FakeLaunchBoxService(), iidbKey = "iidb-secret")

        val icon = repository.findIcon(sampleGame(title = "Ape Escape 3", sourceId = "iidb:99"))

        assertEquals("https://cdn.example/icon.png", icon)
    }

    @Test
    fun findIcon_resolvesOnIidbByTitleForOtherSources() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "POST" && path == "/resolve") {
                200 to iidbParentJson(icon = "https://cdn.example/icon.png", status = "matched")
            } else {
                404 to "{}"
            }
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            detailsById = mapOf(
                "123-ape-escape-3" to sampleGame(
                    title = "Ape Escape 3",
                    iconUrl = "https://lb.example/square.png"
                )
            )
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "iidb-secret")

        val icon = repository.findIcon(
            sampleGame(
                title = "Ape Escape 3 (USA)",
                platforms = listOf("PlayStation 2"),
                sourceId = "123-ape-escape-3"
            )
        )

        assertEquals("https://cdn.example/icon.png", icon)
        assertEquals(0, fakeLaunchBox.detailRequests)
    }

    @Test
    fun findIcon_fallsBackToLaunchBoxSquareWhenIidbHasNoIcon() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "POST" && path == "/resolve") {
                200 to iidbParentJson(icon = null, status = "matched")
            } else {
                404 to "{}"
            }
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            detailsById = mapOf(
                "123-ape-escape-3" to sampleGame(
                    title = "Ape Escape 3",
                    iconUrl = "https://lb.example/square.png"
                )
            )
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "iidb-secret")

        val icon =
            repository.findIcon(sampleGame(title = "Ape Escape 3", sourceId = "123-ape-escape-3"))

        assertEquals("https://lb.example/square.png", icon)
    }

    @Test
    fun findIcon_withoutIidbKeyUsesLaunchBoxOnly() = runBlocking {
        val fakeLaunchBox = FakeLaunchBoxService(
            detailsById = mapOf(
                "123-ape-escape-3" to sampleGame(
                    title = "Ape Escape 3",
                    iconUrl = "https://lb.example/square.png"
                )
            )
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "")

        val icon =
            repository.findIcon(sampleGame(title = "Ape Escape 3", sourceId = "123-ape-escape-3"))

        assertEquals("https://lb.example/square.png", icon)
        assertEquals(0, iidbRequests.get())
    }

    @Test
    fun findIcon_returnsNullForRawgGameWithoutIidb() = runBlocking {
        val repository = repositoryWith(FakeLaunchBoxService(), iidbKey = "")

        assertNull(repository.findIcon(sampleGame(title = "Ape Escape 3", sourceId = "rawg:456")))
        assertEquals(0, rawgRequests.get())
    }

    @Test
    fun findArtworkOptions_usesOwnIdAndSearchesOtherSources() = runBlocking {
        val iidbPaths = mutableListOf<String>()
        iidbHandler = { method, path ->
            iidbPaths += "$method $path"
            when {
                method == "POST" && path == "/resolve" -> 200 to iidbParentJson(icon = null)
                method == "GET" && path.startsWith("/assets/parents/99") -> 200 to """
                    {
                      "artwork": {
                        "boxart": [{"url": "https://cdn.example/iidb-box.png"}],
                        "icon": [{"url": "https://cdn.example/iidb-icon.png"}],
                        "banner": [{"url": "https://cdn.example/iidb-banner.png"}]
                      }
                    }
                """.trimIndent()

                else -> 404 to "{}"
            }
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            artworkById = mapOf(
                "123-ape-escape-3" to ArtworkUrls(
                    covers = images(
                        "https://lb.example/front.jpg",
                        "https://cdn.example/iidb-box.png"
                    ),
                    banners = images("https://lb.example/banner.jpg")
                )
            )
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "iidb-secret")

        val options = repository.findArtworkOptions(
            sampleGame(
                title = "Ape Escape 3",
                platforms = listOf("PlayStation 2"),
                sourceId = "123-ape-escape-3"
            )
        )

        assertTrue(iidbPaths.any { it == "POST /resolve" })
        assertEquals(
            listOf(
                MetadataSource.LAUNCHBOX to listOf(
                    "https://lb.example/front.jpg",
                    "https://cdn.example/iidb-box.png"
                )
            ),
            options.urlsOf(ArtworkKind.COVER)
        )
        assertEquals(
            listOf(
                MetadataSource.IIDB to listOf("https://cdn.example/iidb-banner.png"),
                MetadataSource.LAUNCHBOX to listOf("https://lb.example/banner.jpg")
            ),
            options.urlsOf(ArtworkKind.BANNER)
        )
        assertEquals(
            listOf(
                MetadataSource.IIDB to listOf(
                    "https://cdn.example/iidb-icon.png",
                    "https://cdn.example/iidb-box.png"
                )
            ),
            options.urlsOf(ArtworkKind.ICON)
        )
        assertEquals(0, rawgRequests.get())
    }

    @Test
    fun findArtworkOptions_skipsFailingSource() = runBlocking {
        iidbHandler = { _, _ -> 500 to "{}" }
        val fakeLaunchBox = FakeLaunchBoxService(
            artworkById = mapOf("123-ape-escape-3" to ArtworkUrls(covers = images("https://lb.example/front.jpg")))
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "iidb-secret")

        val options = repository.findArtworkOptions(
            sampleGame(
                title = "Ape Escape 3",
                sourceId = "123-ape-escape-3"
            )
        )

        assertEquals(
            listOf(MetadataSource.LAUNCHBOX to listOf("https://lb.example/front.jpg")),
            options.urlsOf(ArtworkKind.COVER)
        )
    }

    @Test
    fun findArtworkOptions_searchesLaunchBoxWhenGameCameFromIidb() = runBlocking {
        iidbHandler = { method, path ->
            if (method == "GET" && path.startsWith("/assets/parents/99")) 200 to "{\"artwork\": {}}" else 404 to "{}"
        }
        val fakeLaunchBox = FakeLaunchBoxService(
            searchResults = listOf(
                sampleGame(
                    title = "Ape Escape 3",
                    platforms = listOf("Sony PlayStation 2"),
                    sourceId = "123-ape-escape-3"
                ),
                sampleGame(
                    title = "Ape Escape 3",
                    platforms = listOf("Sony PSP"),
                    sourceId = "999-wrong-platform"
                )
            ),
            artworkById = mapOf(
                "123-ape-escape-3" to ArtworkUrls(covers = images("https://lb.example/ps2.jpg")),
                "999-wrong-platform" to ArtworkUrls(covers = images("https://lb.example/psp.jpg"))
            )
        )
        val repository = repositoryWith(fakeLaunchBox, iidbKey = "iidb-secret")

        val options = repository.findArtworkOptions(
            sampleGame(
                title = "Ape Escape 3",
                platforms = listOf("PlayStation 2"),
                sourceId = "iidb:99"
            )
        )

        assertEquals(
            listOf(MetadataSource.LAUNCHBOX to listOf("https://lb.example/ps2.jpg")),
            options.urlsOf(ArtworkKind.COVER)
        )
    }

    private fun repositoryWith(launchBox: LaunchBoxService, iidbKey: String) = MetadataRepository(
        metadataDirectory = tempDir,
        service = launchBox,
        iidbService = iidbService,
        rawgService = rawgService
    ) {
        MetadataConfig(
            order = listOf(MetadataSource.IIDB, MetadataSource.LAUNCHBOX, MetadataSource.RAWG),
            iidbApiKey = iidbKey,
            rawgApiKey = ""
        )
    }

    private fun iidbParentJson(icon: String?, status: String = "matched"): String {
        val iconArray = icon?.let { "[{\"url\": \"$it\"}]" } ?: "[]"
        return """
            {
              "match": {
                "status": "$status",
                "parent": { "id": 99, "name": "Ape Escape 3", "year": 2005, "primary_platform": "PlayStation 2" }
              },
              "artwork": {
                "boxart": [{"url": "https://cdn.example/box.png"}],
                "icon": $iconArray
              }
            }
        """.trimIndent()
    }

    private fun sampleGame(
        title: String,
        summary: String? = null,
        rating: Double? = null,
        releaseDate: String? = null,
        genres: List<String> = emptyList(),
        platforms: List<String> = emptyList(),
        coverUrl: String? = null,
        screenshotUrls: List<String> = emptyList(),
        sourceId: String? = null,
        backdropUrl: String? = null,
        iconUrl: String? = null
    ) = GameMetadata(
        title = title,
        summary = summary,
        rating = rating,
        releaseDate = releaseDate,
        genres = genres,
        platforms = platforms,
        coverUrl = coverUrl,
        screenshotUrls = screenshotUrls,
        sourceId = sourceId,
        backdropUrl = backdropUrl,
        iconUrl = iconUrl
    )

    private class FakeLaunchBoxService(
        private val searchResults: List<GameMetadata> = emptyList(),
        private val detailsById: Map<String, GameMetadata> = emptyMap(),
        private val searchError: Exception? = null,
        private val artworkById: Map<String, ArtworkUrls> = emptyMap()
    ) : LaunchBoxService() {
        var detailRequests = 0
            private set

        override suspend fun searchGames(query: String): List<GameMetadata> {
            searchError?.let { throw it }
            return searchResults
        }
        override suspend fun fetchGameDetails(
            detailsUrlSuffix: String,
            basicMetadata: GameMetadata
        ): GameMetadata {
            detailRequests++
            return detailsById[detailsUrlSuffix] ?: basicMetadata
        }

        override suspend fun fetchArtwork(detailsUrlSuffix: String): ArtworkUrls =
            artworkById[detailsUrlSuffix] ?: ArtworkUrls()
    }
}
