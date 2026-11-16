package com.example.neatgrid

import com.example.neatgrid.data.LibraryCoverStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryCoverStyleTest {
    private val cover = "https://cdn.example/box.jpg"
    private val icon = "https://cdn.example/icon.png"

    @Test
    fun fromPreference_defaultsToBoxArt() {
        assertEquals(LibraryCoverStyle.BOX_ART, LibraryCoverStyle.fromPreference(null))
        assertEquals(LibraryCoverStyle.BOX_ART, LibraryCoverStyle.fromPreference("unknown"))
        assertEquals(LibraryCoverStyle.ICON, LibraryCoverStyle.fromPreference("ICON"))
    }

    @Test
    fun icons_requireIidb() {
        assertEquals(
            LibraryCoverStyle.ICON,
            LibraryCoverStyle.ICON.effective(iidbConfigured = true)
        )
        assertEquals(
            LibraryCoverStyle.BOX_ART,
            LibraryCoverStyle.ICON.effective(iidbConfigured = false)
        )
        assertEquals(
            LibraryCoverStyle.BOX_ART,
            LibraryCoverStyle.BOX_ART.effective(iidbConfigured = true)
        )
    }

    @Test
    fun boxArt_prefersCoverAndFallsBackToIcon() {
        assertEquals(cover, LibraryCoverStyle.BOX_ART.artworkUrl(cover, icon))
        assertEquals(icon, LibraryCoverStyle.BOX_ART.artworkUrl(null, icon))
    }

    @Test
    fun icon_prefersIconAndFallsBackToCover() {
        assertEquals(icon, LibraryCoverStyle.ICON.artworkUrl(cover, icon))
        assertEquals(cover, LibraryCoverStyle.ICON.artworkUrl(cover, null))
    }

    @Test
    fun noArtwork_returnsNull() {
        LibraryCoverStyle.entries.forEach { assertNull(it.artworkUrl(null, null)) }
    }

    @Test
    fun boxTilesAreTallerThanIconTiles() {
        assertEquals(2f / 3f, LibraryCoverStyle.BOX_ART.aspectRatio, 0.0001f)
        assertEquals(1f, LibraryCoverStyle.ICON.aspectRatio, 0.0001f)
    }
}
