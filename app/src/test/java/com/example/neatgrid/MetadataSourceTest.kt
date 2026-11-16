package com.example.neatgrid

import com.example.neatgrid.data.ApiKeyCipher
import com.example.neatgrid.data.GameMetadata
import com.example.neatgrid.data.MetadataSource
import com.example.neatgrid.data.sourceLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataSourceTest {
    @Test
    fun parseOrder_defaultsAndRecoversMissingSources() {
        assertEquals(MetadataSource.DEFAULT_ORDER, MetadataSource.parseOrder(null))
        assertEquals(MetadataSource.DEFAULT_ORDER, MetadataSource.parseOrder(""))
        assertEquals(
            listOf(
                MetadataSource.RAWG,
                MetadataSource.IIDB,
                MetadataSource.LAUNCHBOX,
                MetadataSource.STEAM,
                MetadataSource.STEAMGRIDDB
            ),
            MetadataSource.parseOrder("rawg,unknown,rawg")
        )
    }

    @Test
    fun parseOrder_appendsNewSourcesAfterSavedOrder() {
        assertEquals(
            listOf(
                MetadataSource.LAUNCHBOX,
                MetadataSource.IIDB,
                MetadataSource.RAWG,
                MetadataSource.STEAM,
                MetadataSource.STEAMGRIDDB
            ),
            MetadataSource.parseOrder("launchbox,iidb,rawg")
        )
    }

    @Test
    fun parseIds_readsDisabledSources() {
        assertEquals(
            setOf(MetadataSource.STEAM, MetadataSource.RAWG),
            MetadataSource.parseIds(setOf("steam", "rawg", "unknown"))
        )
    }

    @Test
    fun serializeOrder_roundTripsThroughParseOrder() {
        val customOrder = listOf(
            MetadataSource.STEAMGRIDDB,
            MetadataSource.LAUNCHBOX,
            MetadataSource.STEAM,
            MetadataSource.RAWG,
            MetadataSource.IIDB
        )
        assertEquals(
            "steamgriddb,launchbox,steam,rawg,iidb",
            MetadataSource.serializeOrder(customOrder)
        )
        assertEquals(customOrder, MetadataSource.parseOrder(MetadataSource.serializeOrder(customOrder)))
    }

    @Test
    fun fromSourceId_mapsPrefixesAndLegacyLaunchBox() {
        assertEquals(MetadataSource.IIDB, MetadataSource.fromSourceId("iidb:46185"))
        assertEquals(MetadataSource.STEAM, MetadataSource.fromSourceId("steam:1562430"))
        assertEquals(MetadataSource.STEAMGRIDDB, MetadataSource.fromSourceId("steamgriddb:5"))
        assertEquals(MetadataSource.RAWG, MetadataSource.fromSourceId("rawg:3498"))
        assertEquals(MetadataSource.LAUNCHBOX, MetadataSource.fromSourceId("2224-sonic-advance"))
        assertEquals(MetadataSource.LAUNCHBOX, MetadataSource.fromSourceId("launchbox:2224-sonic-advance"))
        assertEquals(MetadataSource.LAUNCHBOX, MetadataSource.fromSourceId(null))
    }

    @Test
    fun sourceLabel_matchesDisplayName() {
        val iidbGame = GameMetadata("Ape Escape 3", null, null, null, emptyList(), emptyList(), null, emptyList(), sourceId = "iidb:46185")
        val legacyGame = GameMetadata("Sonic Advance", null, null, null, emptyList(), emptyList(), null, emptyList(), sourceId = "2224-sonic-advance")
        assertEquals("iiDB", iidbGame.sourceLabel())
        assertEquals("LaunchBox", legacyGame.sourceLabel())
    }

    @Test
    fun apiKeyCipher_roundTripsAndPreservesLegacyPlaintext() {
        val encrypted = ApiKeyCipher.encrypt("secret-key")
        assertEquals("secret-key", ApiKeyCipher.decrypt(encrypted))
        assertEquals("legacy-plaintext-key", ApiKeyCipher.decrypt("legacy-plaintext-key"))
        assertEquals("", ApiKeyCipher.encrypt(""))
        assertEquals("", ApiKeyCipher.encrypt("   "))
        assertEquals("", ApiKeyCipher.decrypt(""))
        assertEquals("", ApiKeyCipher.decrypt("   "))
    }
}
