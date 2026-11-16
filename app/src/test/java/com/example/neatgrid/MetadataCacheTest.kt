package com.example.neatgrid

import com.example.neatgrid.data.CachedGameMetadata
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataCacheStatus
import com.example.neatgrid.data.MetadataConfig
import com.example.neatgrid.data.MetadataRepository
import com.example.neatgrid.data.MetadataSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MetadataCacheTest {

    private lateinit var tempDir: File
    private lateinit var repository: MetadataRepository

    private val sample = GameMetadata(
        title = "Ape Escape 3",
        summary = "Catch monkeys",
        rating = 88.0,
        releaseDate = "2006-01-17",
        genres = listOf("Platformer", "Action"),
        platforms = listOf("PlayStation 2"),
        coverUrl = "https://cdn.example/box.png",
        screenshotUrls = listOf("https://cdn.example/s1.png"),
        sourceId = "iidb:99",
        backdropUrl = "https://cdn.example/bg.png",
        iconUrl = "https://cdn.example/icon.png"
    )

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("metadata-cache-test").toFile()
        repository = MetadataRepository(metadataDirectory = File(tempDir, "metadata")) {
            MetadataConfig(MetadataSource.DEFAULT_ORDER, iidbApiKey = "", rawgApiKey = "")
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun readCached_returnsNullWhenNothingSaved() {
        assertNull(repository.readCached("com.example.game"))
    }

    @Test
    fun save_thenReadCached_roundTripsMetadataAndStatus() = runBlocking {
        repository.save("com.example.game", CachedGameMetadata(sample, MetadataCacheStatus.CUSTOM))

        val cached = repository.readCached("com.example.game")

        assertEquals(CachedGameMetadata(sample, MetadataCacheStatus.CUSTOM), cached)
    }

    @Test
    fun save_overwritesPreviousEntry() = runBlocking {
        repository.save(
            "com.example.game",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )
        val edited = sample.copy(title = "Ape Escape 3 (Edited)")
        repository.save("com.example.game", CachedGameMetadata(edited, MetadataCacheStatus.CUSTOM))

        assertEquals(
            CachedGameMetadata(edited, MetadataCacheStatus.CUSTOM),
            repository.readCached("com.example.game")
        )
    }

    @Test
    fun entriesForDifferentGamesStaySeparate() = runBlocking {
        val romPackage = "rom:org.ppsspp.ppsspp|God of War|content://x"
        val other = sample.copy(title = "God of War", sourceId = null)
        repository.save(
            "com.example.game",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )
        repository.save(romPackage, CachedGameMetadata(other, MetadataCacheStatus.NOT_FOUND))

        assertEquals(sample, repository.readCached("com.example.game")?.metadata)
        assertEquals(
            CachedGameMetadata(other, MetadataCacheStatus.NOT_FOUND),
            repository.readCached(romPackage)
        )
    }

    @Test
    fun delete_removesOnlyThatEntry() = runBlocking {
        repository.save(
            "com.example.game",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )
        repository.save(
            "com.example.other",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )

        repository.delete("com.example.game")

        assertNull(repository.readCached("com.example.game"))
        assertEquals(sample, repository.readCached("com.example.other")?.metadata)
    }

    @Test
    fun readCached_returnsNullForCorruptedFile() = runBlocking {
        repository.save(
            "com.example.game",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )
        singleCacheFile().writeText("{not json")

        assertNull(repository.readCached("com.example.game"))
    }

    @Test
    fun readCached_readsLegacyFileWithoutWrapperAsAutomatic() = runBlocking {
        repository.save("com.example.game", CachedGameMetadata(sample, MetadataCacheStatus.CUSTOM))
        singleCacheFile().writeText(sample.toJson().toString())

        assertEquals(
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC, fullSizeImages = false),
            repository.readCached("com.example.game")
        )
    }

    @Test
    fun readCached_fallsBackToAutomaticForUnknownStatus() = runBlocking {
        repository.save("com.example.game", CachedGameMetadata(sample, MetadataCacheStatus.CUSTOM))
        val file = singleCacheFile()
        file.writeText(file.readText().replace("\"CUSTOM\"", "\"SOMETHING_NEW\""))

        assertEquals(
            MetadataCacheStatus.AUTOMATIC,
            repository.readCached("com.example.game")?.status
        )
    }

    @Test
    fun fullSizeImagesFlag_roundTripsAndIsFalseForOlderFiles() = runBlocking {
        repository.save(
            "com.example.old",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC, fullSizeImages = false)
        )
        repository.save(
            "com.example.new",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )

        assertEquals(false, repository.readCached("com.example.old")?.fullSizeImages)
        assertEquals(true, repository.readCached("com.example.new")?.fullSizeImages)

        repository.delete("com.example.new")
        repository.delete("com.example.old")
        repository.save(
            "com.example.game",
            CachedGameMetadata(sample, MetadataCacheStatus.AUTOMATIC)
        )
        val file = singleCacheFile()
        file.writeText(file.readText().replace("\"fullSizeImages\":true,", ""))
        assertEquals(false, repository.readCached("com.example.game")?.fullSizeImages)
    }

    private fun singleCacheFile(): File {
        val files = File(tempDir, "metadata").listFiles().orEmpty()
        assertEquals(1, files.size)
        return files.single()
    }
}
