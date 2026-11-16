package com.example.neatgrid

import com.example.neatgrid.data.EmulatorApps
import com.example.neatgrid.data.MetadataConfig
import com.example.neatgrid.data.MetadataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataConfigTest {

    private val allKeys = MetadataConfig(
        order = MetadataSource.DEFAULT_ORDER,
        iidbApiKey = "iidb",
        rawgApiKey = "rawg",
        steamGridDbApiKey = "sgdb"
    )

    @Test
    fun activeSources_keepsOrderAndNeedsKeys() {
        assertEquals(MetadataSource.DEFAULT_ORDER, allKeys.activeSources())
        assertEquals(
            listOf(MetadataSource.LAUNCHBOX, MetadataSource.STEAM),
            allKeys.copy(iidbApiKey = "", rawgApiKey = " ", steamGridDbApiKey = "").activeSources()
        )
    }

    @Test
    fun activeSources_skipsDisabledSources() {
        val config = allKeys.copy(
            disabledSources = setOf(
                MetadataSource.LAUNCHBOX,
                MetadataSource.STEAMGRIDDB
            )
        )

        assertEquals(
            listOf(MetadataSource.IIDB, MetadataSource.STEAM, MetadataSource.RAWG),
            config.activeSources()
        )
        assertFalse(config.isActive(MetadataSource.LAUNCHBOX))
        assertTrue(config.isActive(MetadataSource.STEAM))
    }

    @Test
    fun metadataSources_leaveOutArtworkOnlySources() {
        assertEquals(
            listOf(
                MetadataSource.IIDB,
                MetadataSource.LAUNCHBOX,
                MetadataSource.STEAM,
                MetadataSource.RAWG
            ),
            allKeys.metadataSources()
        )
    }

    @Test
    fun emulatorApps_recognisesEmulatorsAndLaunchers() {
        listOf(
            "org.ppsspp.ppsspp",
            "com.swordfish.lemuroid",
            "com.retroarch.aarch64",
            "dev.eden.eden_emulator",
            "org.azahar_emu.azahar",
            "com.iisulauncher",
            "com.magneticchen.daijishou",
            "org.es_de.frontend",
            "com.explusalpha.Snes9xPlus"
        ).forEach { assertTrue(it, EmulatorApps.isEmulatorOrFrontend(it)) }
    }

    @Test
    fun emulatorApps_leavesRegularGamesAlone() {
        listOf(
            "com.mojang.minecraftpe",
            "com.supercell.clashofclans",
            "com.innersloth.spacemafia",
            "com.playdigious.deadcells.mobile"
        ).forEach { assertFalse(it, EmulatorApps.isEmulatorOrFrontend(it)) }
    }
}
