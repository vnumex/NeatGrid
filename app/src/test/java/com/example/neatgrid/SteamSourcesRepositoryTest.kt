package com.example.neatgrid

import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.data.ArtworkUrls
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxService
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataConfig
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.SteamGridDbService
import com.example.neatgrid.data.SteamService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SteamSourcesRepositoryTest {

    private lateinit var tempDir: File
    private val launchBox = FakeLaunchBox()
    private val steam = FakeSteam()
    private val steamGridDb = FakeSteamGridDb()
    private var config = MetadataConfig(
        order = listOf(MetadataSource.LAUNCHBOX, MetadataSource.STEAM, MetadataSource.STEAMGRIDDB),
        iidbApiKey = "",
        rawgApiKey = "",
        steamGridDbApiKey = "sgdb-key"
    )

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("steam-sources-test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun artwork_usesSteamGridDbSteamPathForSteamGames() = runBlocking {
        val options =
            repository().findArtworkOptions(game("DREDGE", "steam:1562430", listOf("Windows")))

        assertEquals(listOf("steam:1562430"), steamGridDb.steamRequests)
        assertTrue(steamGridDb.gameRequests.isEmpty())
        assertEquals(
            listOf(
                MetadataSource.STEAM to listOf("https://steam/dredge-600x900.jpg"),
                MetadataSource.STEAMGRIDDB to listOf("https://sgdb/steam-grid.png")
            ),
            options.urlsOf(ArtworkKind.COVER)
        )
        assertEquals(
            listOf(MetadataSource.STEAMGRIDDB to listOf("https://sgdb/icon.png")),
            options.urlsOf(ArtworkKind.ICON)
        )
    }

    @Test
    fun artwork_searchesSteamGridDbByTitleForOtherGames() = runBlocking {
        val options = repository().findArtworkOptions(
            game(
                "Sea of Stars",
                "1-sea-of-stars",
                listOf("Nintendo Switch")
            )
        )

        assertEquals(listOf("steamgriddb:5"), steamGridDb.gameRequests)
        assertEquals(
            listOf(MetadataSource.STEAMGRIDDB to listOf("https://sgdb/game-grid.png")),
            options.urlsOf(ArtworkKind.COVER)
        )
    }

    @Test
    fun artwork_skipsDisabledSources() = runBlocking {
        config = config.copy(disabledSources = setOf(MetadataSource.STEAMGRIDDB))

        repository().findArtworkOptions(game("DREDGE", "steam:1562430", listOf("Windows")))

        assertTrue(steamGridDb.steamRequests.isEmpty() && steamGridDb.gameRequests.isEmpty())
    }

    @Test
    fun lookup_skipsDisabledLaunchBoxAndMatchesOnSteam() = runBlocking {
        config = config.copy(disabledSources = setOf(MetadataSource.LAUNCHBOX))

        val cached = repository().lookup("DREDGE", setOf("PC"))

        assertEquals(0, launchBox.searches)
        assertEquals(MetadataCacheStatus.AUTOMATIC, cached.status)
        assertEquals("steam:1562430", cached.metadata.sourceId)
        assertEquals("Steam summary", cached.metadata.summary)
    }

    @Test
    fun lookup_backfillsSummaryFromSteam() = runBlocking {
        launchBox.results = listOf(game("DREDGE", "9-dredge", listOf("Windows")))

        val cached = repository().lookup("DREDGE", setOf("PC"))

        assertEquals("9-dredge", cached.metadata.sourceId)
        assertEquals("Steam summary", cached.metadata.summary)
    }

    private fun repository() = MetadataRepository(
        metadataDirectory = tempDir,
        service = launchBox,
        steamService = steam,
        steamGridDbService = steamGridDb
    ) { config }

    private fun game(title: String, sourceId: String?, platforms: List<String> = emptyList()) =
        GameMetadata(
            title = title,
            summary = null,
            rating = null,
            releaseDate = null,
            genres = emptyList(),
            platforms = platforms,
            coverUrl = null,
            screenshotUrls = emptyList(),
            sourceId = sourceId
        )

    private inner class FakeLaunchBox : LaunchBoxService() {
        var results: List<GameMetadata> = emptyList()
        var searches = 0

        override suspend fun searchGames(query: String): List<GameMetadata> {
            searches++
            return results
        }

        override suspend fun fetchGameDetails(
            detailsUrlSuffix: String,
            basicMetadata: GameMetadata
        ) = basicMetadata

        override suspend fun fetchArtwork(detailsUrlSuffix: String) = ArtworkUrls()
    }

    private inner class FakeSteam : SteamService("http://unused", "http://unused") {
        override suspend fun searchGames(query: String): List<GameMetadata> =
            if (query.equals("DREDGE", ignoreCase = true)) listOf(
                game(
                    "DREDGE",
                    "steam:1562430",
                    listOf("Windows")
                )
            ) else emptyList()

        override suspend fun fetchGameDetails(steamId: String, basicMetadata: GameMetadata) =
            basicMetadata.copy(summary = "Steam summary", sourceId = steamId)

        override suspend fun fetchArtwork(steamId: String) =
            ArtworkUrls(covers = images("https://steam/dredge-600x900.jpg"))
    }

    private inner class FakeSteamGridDb : SteamGridDbService("http://unused") {
        val steamRequests = mutableListOf<String>()
        val gameRequests = mutableListOf<String>()

        override suspend fun searchGames(query: String, apiKey: String): List<GameMetadata> =
            listOf(game("Sea of Stars", "steamgriddb:5"))

        override suspend fun fetchSteamArtwork(steamAppId: String, apiKey: String): ArtworkUrls {
            steamRequests += steamAppId
            return ArtworkUrls(
                covers = images("https://sgdb/steam-grid.png"),
                icons = images("https://sgdb/icon.png")
            )
        }

        override suspend fun fetchArtwork(gameId: String, apiKey: String): ArtworkUrls {
            gameRequests += gameId
            return ArtworkUrls(covers = images("https://sgdb/game-grid.png"))
        }
    }
}
