package com.example.neatgrid

import com.example.neatgrid.data.CachedGameMetadata
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.LaunchBoxService
import com.example.neatgrid.data.LibraryMetadataBackfill
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataConfig
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.MetadataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LibraryMetadataBackfillTest {

    private lateinit var tempDir: File
    private lateinit var launchBox: FakeLaunchBox
    private lateinit var repository: MetadataRepository
    private val updated = mutableListOf<Pair<String, GameMetadata>>()

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("metadata-backfill-test").toFile()
        launchBox = FakeLaunchBox(
            mapOf(
                "1-sea-of-stars" to game(
                    "Sea of Stars",
                    "1-sea-of-stars"
                ).copy(iconUrl = "https://lb.example/sos.png"),
                "2-no-square" to game("No Square", "2-no-square")
            )
        )
        repository = MetadataRepository(metadataDirectory = tempDir, service = launchBox) {
            MetadataConfig(listOf(MetadataSource.LAUNCHBOX), iidbApiKey = "", rawgApiKey = "")
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun iconBackfill_savesFoundIconsAndKeepsStatus() = runBlocking {
        save("com.sos", game("Sea of Stars", "1-sea-of-stars"))
        save("com.nosquare", game("No Square", "2-no-square"))
        val backfill = iconBackfill(this).apply { enabled = true }

        backfill.start { listOf("com.sos", "com.nosquare") }?.join()

        assertEquals(listOf("com.sos"), updated.map { it.first })
        assertEquals("https://lb.example/sos.png", updated.single().second.iconUrl)
        assertEquals(
            "https://lb.example/sos.png",
            repository.readCached("com.sos")?.metadata?.iconUrl
        )
        assertEquals(MetadataCacheStatus.CUSTOM, repository.readCached("com.sos")?.status)
        assertNull(repository.readCached("com.nosquare")?.metadata?.iconUrl)
    }

    @Test
    fun start_triesEachGameOnlyOncePerSession() = runBlocking {
        save("com.nosquare", game("No Square", "2-no-square"))
        val backfill = iconBackfill(this).apply { enabled = true }

        backfill.start { listOf("com.nosquare") }?.join()
        backfill.start { listOf("com.nosquare") }?.join()

        assertEquals(1, launchBox.detailRequests)
    }

    @Test
    fun start_doesNothingWhileDisabled() = runBlocking {
        save("com.sos", game("Sea of Stars", "1-sea-of-stars"))
        val backfill = iconBackfill(this)

        assertNull(backfill.start { listOf("com.sos") })
        assertEquals(0, launchBox.detailRequests)
    }

    @Test
    fun start_skipsGamesWithoutCachedMetadata() = runBlocking {
        val backfill = iconBackfill(this).apply { enabled = true }

        backfill.start { listOf("com.unknown") }?.join()

        assertEquals(0, launchBox.detailRequests)
        assertEquals(emptyList<Pair<String, GameMetadata>>(), updated)
    }

    @Test
    fun start_doesNotSaveWhenNothingChanges() = runBlocking {
        save("com.sos", game("Sea of Stars", "1-sea-of-stars"))
        val before = repository.readCached("com.sos")
        val backfill = LibraryMetadataBackfill(
            this,
            repository,
            improve = { it },
            onUpdated = { p, m -> updated += p to m })
            .apply { enabled = true }

        backfill.start { listOf("com.sos") }?.join()

        assertEquals(emptyList<Pair<String, GameMetadata>>(), updated)
        assertEquals(before, repository.readCached("com.sos"))
    }

    private fun iconBackfill(scope: CoroutineScope) = LibraryMetadataBackfill(
        scope = scope,
        metadataRepository = repository,
        improve = { cached ->
            repository.findIcon(cached.metadata)
                ?.let { cached.copy(metadata = cached.metadata.copy(iconUrl = it)) }
        },
        onUpdated = { packageName, metadata -> updated += packageName to metadata }
    )

    private suspend fun save(packageName: String, metadata: GameMetadata) {
        repository.save(packageName, CachedGameMetadata(metadata, MetadataCacheStatus.CUSTOM))
    }

    private fun game(title: String, sourceId: String) = GameMetadata(
        title = title,
        summary = null,
        rating = null,
        releaseDate = null,
        genres = emptyList(),
        platforms = emptyList(),
        coverUrl = null,
        screenshotUrls = emptyList(),
        sourceId = sourceId
    )

    private class FakeLaunchBox(private val details: Map<String, GameMetadata>) :
        LaunchBoxService() {
        var detailRequests = 0
            private set

        override suspend fun searchGames(query: String): List<GameMetadata> = emptyList()

        override suspend fun fetchGameDetails(
            detailsUrlSuffix: String,
            basicMetadata: GameMetadata
        ): GameMetadata {
            detailRequests++
            return details[detailsUrlSuffix] ?: basicMetadata
        }
    }
}
