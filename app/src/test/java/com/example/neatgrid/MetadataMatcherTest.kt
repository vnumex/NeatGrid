package com.example.neatgrid

import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataMatcher
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataMatcherTest {
    @Test
    fun cleanQueryRemovesFileTags() {
        assertEquals(
            "Sonic Advance",
            MetadataMatcher.cleanQuery("Sonic Advance (USA) [Rev 1]")
        )
    }

    @Test
    fun bestMatchPrefersNormalizedExactTitle() {
        val results = listOf(
            metadata("Sonic Adventure"),
            metadata("Sonic Advance"),
            metadata("Advance Wars")
        )

        assertEquals("Sonic Advance", MetadataMatcher.bestMatch("sonic advance", results)?.title)
    }

    @Test
    fun bestMatchRejectsUnrelatedFirstResult() {
        val results = listOf(
            metadata("Call of Duty"),
            metadata("FIFA 24")
        )

        assertNull(MetadataMatcher.bestMatch("Minecraft", results))
    }

    @Test
    fun bestMatchRejectsExactTitleOnWrongPlatform() {
        val results = listOf(
            metadata("Chrome", "Windows")
        )

        assertNull(MetadataMatcher.bestMatch("Chrome", results, setOf("Android")))
    }

    @Test
    fun bestMatchAcceptsPlatformAlias() {
        val results = listOf(
            metadata("Sonic Advance", "Nintendo Game Boy Advance")
        )

        assertEquals(
            "Sonic Advance",
            MetadataMatcher.bestMatch("Sonic Advance", results, setOf("Game Boy Advance"))?.title
        )
    }

    @Test
    fun legacySourceIdMovesOutOfSummary() {
        val json = JSONObject()
            .put("title", "Sonic Advance")
            .put("summary", "launchbox:2224-sonic-advance")
            .put("rating", JSONObject.NULL)
            .put("releaseDate", JSONObject.NULL)
            .put("genres", JSONArray())
            .put("platforms", JSONArray())
            .put("coverUrl", JSONObject.NULL)
            .put("screenshotUrls", JSONArray())

        val metadata = GameMetadata.fromJson(json)

        assertNull(metadata.summary)
        assertEquals("2224-sonic-advance", metadata.sourceId)
    }

    private fun metadata(title: String, platform: String? = null) = GameMetadata(
        title = title,
        summary = null,
        rating = null,
        releaseDate = null,
        genres = emptyList(),
        platforms = listOfNotNull(platform),
        coverUrl = null,
        screenshotUrls = emptyList()
    )
}
