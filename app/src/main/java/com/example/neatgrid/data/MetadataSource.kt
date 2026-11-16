package com.example.neatgrid.data

import java.io.IOException

enum class MetadataSource(
    val id: String,
    val displayName: String,
    val requiresApiKey: Boolean,
    val providesMetadata: Boolean = true
) {
    IIDB("iidb", "iiDB", true),
    LAUNCHBOX("launchbox", "LaunchBox", false),
    STEAM("steam", "Steam", false),
    RAWG("rawg", "RAWG", true),
    STEAMGRIDDB("steamgriddb", "SteamGridDB", true, providesMetadata = false);

    companion object {
        val DEFAULT_ORDER: List<MetadataSource> = listOf(IIDB, LAUNCHBOX, STEAM, RAWG, STEAMGRIDDB)

        fun parseIds(serialized: Set<String>): Set<MetadataSource> =
            entries.filter { it.id in serialized }.toSet()

        fun parseOrder(serialized: String?): List<MetadataSource> {
            if (serialized.isNullOrBlank()) return DEFAULT_ORDER
            val parsed = serialized
                .split(',')
                .mapNotNull { token ->
                    val normalized = token.trim().lowercase()
                    entries.firstOrNull { it.id == normalized }
                }
                .distinct()
            return (parsed + DEFAULT_ORDER).distinct()
        }

        fun serializeOrder(order: List<MetadataSource>): String {
            return (order.distinct() + DEFAULT_ORDER).distinct().joinToString(",") { it.id }
        }

        fun fromSourceId(sourceId: String?): MetadataSource {
            if (sourceId.isNullOrBlank()) return LAUNCHBOX
            return entries.firstOrNull { sourceId.startsWith("${it.id}:") } ?: LAUNCHBOX
        }
    }
}

fun GameMetadata.sourceLabel(): String = MetadataSource.fromSourceId(sourceId).displayName

class MetadataAuthException(
    val source: MetadataSource,
    message: String
) : IOException(message)
