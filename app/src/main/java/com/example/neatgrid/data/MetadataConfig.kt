package com.example.neatgrid.data

data class MetadataConfig(
    val order: List<MetadataSource>,
    val iidbApiKey: String,
    val rawgApiKey: String,
    val steamGridDbApiKey: String = "",
    val disabledSources: Set<MetadataSource> = emptySet()
) {
    fun activeSources(): List<MetadataSource> =
        order.distinct().filter { it !in disabledSources && hasKeyFor(it) }

    fun metadataSources(): List<MetadataSource> = activeSources().filter { it.providesMetadata }

    fun isActive(source: MetadataSource): Boolean = source in activeSources()

    private fun hasKeyFor(source: MetadataSource): Boolean = when (source) {
        MetadataSource.IIDB -> iidbApiKey.isNotBlank()
        MetadataSource.RAWG -> rawgApiKey.isNotBlank()
        MetadataSource.STEAMGRIDDB -> steamGridDbApiKey.isNotBlank()
        MetadataSource.LAUNCHBOX, MetadataSource.STEAM -> true
    }
}
