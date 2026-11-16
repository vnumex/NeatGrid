package com.example.neatgrid

import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.data.ArtworkOptions
import com.example.neatgrid.data.ArtworkUrls
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.withArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkOptionsTest {

    @Test
    fun of_groupsBySourceInOrderAndDropsEmptyGroups() {
        val options = ArtworkOptions(
            listOf(
                MetadataSource.IIDB to ArtworkUrls(covers = images("a"), icons = images("i")),
                MetadataSource.LAUNCHBOX to ArtworkUrls(covers = images("b", "c")),
                MetadataSource.RAWG to ArtworkUrls(banners = images("r"))
            )
        )

        assertEquals(
            listOf(
                MetadataSource.IIDB to listOf("a"),
                MetadataSource.LAUNCHBOX to listOf("b", "c")
            ),
            options.urlsOf(ArtworkKind.COVER)
        )
        assertEquals(listOf(MetadataSource.RAWG to listOf("r")), options.urlsOf(ArtworkKind.BANNER))
        assertEquals(listOf(MetadataSource.IIDB to listOf("i")), options.urlsOf(ArtworkKind.ICON))
    }

    @Test
    fun of_showsEachImageOnlyOnceKeepingTheFirstSource() {
        val options = ArtworkOptions(
            listOf(
                MetadataSource.IIDB to ArtworkUrls(covers = images("same")),
                MetadataSource.LAUNCHBOX to ArtworkUrls(covers = images("same", "other"))
            )
        )

        assertEquals(
            listOf(
                MetadataSource.IIDB to listOf("same"),
                MetadataSource.LAUNCHBOX to listOf("other")
            ),
            options.urlsOf(ArtworkKind.COVER)
        )
    }

    @Test
    fun isEmpty_trueOnlyWithoutAnyImages() {
        assertTrue(ArtworkOptions().isEmpty())
        assertTrue(ArtworkOptions(listOf(MetadataSource.RAWG to ArtworkUrls())).isEmpty())
        assertFalse(ArtworkOptions(listOf(MetadataSource.RAWG to ArtworkUrls(banners = images("r")))).isEmpty())
    }

    @Test
    fun withArtwork_replacesOnlyPickedImagesAndKeepsScreenshots() {
        val game = GameMetadata(
            title = "Sea of Stars",
            summary = "Turn-based RPG",
            rating = null,
            releaseDate = null,
            genres = emptyList(),
            platforms = listOf("Nintendo Switch"),
            coverUrl = "old-cover",
            screenshotUrls = listOf("shot1", "shot2"),
            sourceId = "1-sea-of-stars",
            backdropUrl = "old-banner",
            iconUrl = "old-icon"
        )

        val updated = game.withArtwork(cover = "new-cover", banner = null, icon = "new-icon")

        assertEquals(game.copy(coverUrl = "new-cover", iconUrl = "new-icon"), updated)
        assertEquals(listOf("shot1", "shot2"), updated.screenshotUrls)
    }
}
