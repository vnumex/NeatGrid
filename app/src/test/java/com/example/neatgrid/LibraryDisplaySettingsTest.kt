package com.example.neatgrid

import com.example.neatgrid.data.LibraryDensity
import com.example.neatgrid.data.LibraryTitleStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryDisplaySettingsTest {
    @Test
    fun density_readsStoredValue() {
        assertEquals(LibraryDensity.COMFORTABLE, LibraryDensity.fromPreference("COMFORTABLE", 8))
        assertEquals(LibraryDensity.COMPACT, LibraryDensity.fromPreference("COMPACT", 2))
    }

    @Test
    fun density_migratesLegacyColumnCount() {
        assertEquals(LibraryDensity.COMFORTABLE, LibraryDensity.fromPreference(null, 2))
        assertEquals(LibraryDensity.COMPACT, LibraryDensity.fromPreference(null, 3))
        assertEquals(LibraryDensity.COMPACT, LibraryDensity.fromPreference(null, 5))
    }

    @Test
    fun density_defaultsToCompact() {
        assertEquals(LibraryDensity.COMPACT, LibraryDensity.fromPreference(null))
        assertEquals(LibraryDensity.COMPACT, LibraryDensity.fromPreference("unknown"))
    }

    @Test
    fun density_columnsAreTwoAndThree() {
        assertEquals(2, LibraryDensity.COMFORTABLE.columns)
        assertEquals(3, LibraryDensity.COMPACT.columns)
    }

    @Test
    fun titleStyle_readsStoredValue() {
        LibraryTitleStyle.entries.forEach { style ->
            assertEquals(
                style,
                LibraryTitleStyle.fromPreference(style.name, legacyShowNames = false)
            )
        }
    }

    @Test
    fun titleStyle_migratesLegacyShowNames() {
        assertEquals(LibraryTitleStyle.HIDDEN, LibraryTitleStyle.fromPreference(null, false))
        assertEquals(LibraryTitleStyle.BELOW, LibraryTitleStyle.fromPreference(null, true))
    }

    @Test
    fun titleStyle_defaultsToBelow() {
        assertEquals(LibraryTitleStyle.BELOW, LibraryTitleStyle.fromPreference(null))
        assertEquals(LibraryTitleStyle.BELOW, LibraryTitleStyle.fromPreference("unknown"))
    }
}
