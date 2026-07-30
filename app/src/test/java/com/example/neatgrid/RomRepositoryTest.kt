package com.example.neatgrid

import com.example.neatgrid.data.RomRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RomRepositoryTest {

    @Test
    fun testBuildPackageName_isCorrect() {
        val emulator = "org.ppsspp.ppsspp"
        val label = "God of War"
        val uri = "content://com.android.providers/123"
        val result = RomRepository.buildPackageName(emulator, label, uri)
        assertEquals("rom:org.ppsspp.ppsspp|God of War|content://com.android.providers/123", result)
    }

    @Test
    fun testIsRom_isCorrect() {
        assertTrue(RomRepository.isRom("rom:something"))
        assertFalse(RomRepository.isRom("com.example.game"))
    }

    @Test
    fun testParse_isCorrect() {
        val pkg = "rom:org.ppsspp.ppsspp|God of War|content://com.android.providers/123"
        val parsed = RomRepository.parse(pkg)
        assertNotNull(parsed)
        assertEquals("org.ppsspp.ppsspp", parsed!!.emulatorPackage)
        assertEquals("God of War", parsed.label)
        assertEquals("content://com.android.providers/123", parsed.uriString)
    }

    @Test
    fun testParse_withPipesInUri_joinsCorrectly() {
        val pkg = "rom:com.retroarch|Super Mario|content://some|path|with|pipes"
        val parsed = RomRepository.parse(pkg)
        assertNotNull(parsed)
        assertEquals("com.retroarch", parsed!!.emulatorPackage)
        assertEquals("Super Mario", parsed.label)
        assertEquals("content://some|path|with|pipes", parsed.uriString)
    }

    @Test
    fun testParse_invalidFormat_returnsNull() {
        assertNull(RomRepository.parse("com.example.game"))
        assertNull(RomRepository.parse("rom:short_string"))
    }

    @Test
    fun testSaveFileSuffix_matchesOnlyKnownSaveFormats() {
        assertTrue(RomRepository.isSaveFileSuffix("sav"))
        assertTrue(RomRepository.isSaveFileSuffix("state3"))
        assertTrue(RomRepository.isSaveFileSuffix("st2"))
        assertFalse(RomRepository.isSaveFileSuffix("iso"))
        assertFalse(RomRepository.isSaveFileSuffix("png"))
    }
}
