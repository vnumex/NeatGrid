package com.example.neatgrid

import com.example.neatgrid.data.LibraryEntry
import com.example.neatgrid.data.LibrarySortMode
import com.example.neatgrid.data.arrangeLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Collator
import java.util.Locale

class LibraryArrangementTest {

    private data class Entry(
        override val packageName: String,
        override val label: String,
        override val platform: String? = null
    ) : LibraryEntry

    private val collator = Collator.getInstance(Locale.ENGLISH)

    private val zelda = Entry("rom:zelda", "Zelda", "Nintendo 64")
    private val apex = Entry("com.apex", "apex Legends", "Android")
    private val bully = Entry("com.bully", "Bully", "Android")
    private val gow = Entry("rom:gow", "God of War", "PlayStation 2")
    private val unknown = Entry("com.unknown", "Among Us")
    private val all = listOf(zelda, apex, bully, gow, unknown)

    @Test
    fun titleAscending_sortsAlphabeticallyIgnoringCase() {
        val sections = arrangeLibrary(all, emptySet(), LibrarySortMode.TITLE_ASCENDING, collator)

        assertEquals(listOf(unknown, apex, bully, gow, zelda), sections.visible)
    }

    @Test
    fun titleDescending_isReverseOfAscending() {
        val ascending = arrangeLibrary(all, emptySet(), LibrarySortMode.TITLE_ASCENDING, collator)
        val descending = arrangeLibrary(all, emptySet(), LibrarySortMode.TITLE_DESCENDING, collator)

        assertEquals(ascending.visible.reversed(), descending.visible)
    }

    @Test
    fun platform_groupsByPlatformThenTitleWithUnknownPlatformLast() {
        val sections = arrangeLibrary(all, emptySet(), LibrarySortMode.PLATFORM, collator)

        assertEquals(listOf(apex, bully, zelda, gow, unknown), sections.visible)
    }

    @Test
    fun hiddenGames_areRemovedFromVisibleAndSortedSeparately() {
        val sections = arrangeLibrary(
            all,
            setOf(zelda.packageName, apex.packageName),
            LibrarySortMode.TITLE_ASCENDING,
            collator
        )

        assertEquals(listOf(unknown, bully, gow), sections.visible)
        assertEquals(listOf(apex, zelda), sections.hidden)
    }

    @Test
    fun hiddenPackagesNotInLibrary_areIgnored() {
        val sections = arrangeLibrary(
            listOf(bully),
            setOf("com.removed"),
            LibrarySortMode.TITLE_ASCENDING,
            collator
        )

        assertEquals(listOf(bully), sections.visible)
        assertTrue(sections.hidden.isEmpty())
    }

    @Test
    fun emptyLibrary_producesEmptySections() {
        val sections = arrangeLibrary(
            emptyList<Entry>(),
            setOf("com.bully"),
            LibrarySortMode.PLATFORM,
            collator
        )

        assertTrue(sections.visible.isEmpty())
        assertTrue(sections.hidden.isEmpty())
    }
}
