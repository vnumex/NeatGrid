package com.example.neatgrid.data

class LaunchBoxImageUpgrade(private val service: LaunchBoxService) {

    suspend fun upgrade(cached: CachedGameMetadata): CachedGameMetadata? {
        if (cached.fullSizeImages) return null
        val metadata = cached.metadata
        val hasLaunchBoxImages = listOf(metadata.coverUrl, metadata.backdropUrl, metadata.iconUrl)
            .any(LaunchBoxImages::isLaunchBoxImage)
        if (!hasLaunchBoxImages) return cached.copy(fullSizeImages = true)

        val pageId = launchBoxPageId(metadata) ?: return cached.copy(fullSizeImages = true)
        val fullSizeNames = service.fetchFullSizeNames(pageId)
        fun fullSize(url: String?) = url?.let { LaunchBoxImages.toFullSize(it, fullSizeNames) }
        return cached.copy(
            metadata = metadata.copy(
                coverUrl = fullSize(metadata.coverUrl),
                backdropUrl = fullSize(metadata.backdropUrl),
                iconUrl = fullSize(metadata.iconUrl)
            ),
            fullSizeImages = true
        )
    }

    private suspend fun launchBoxPageId(metadata: GameMetadata): String? {
        val sourceId = metadata.sourceId?.takeIf { it.isNotBlank() }
        if (sourceId != null && MetadataSource.fromSourceId(sourceId) == MetadataSource.LAUNCHBOX) {
            return sourceId
        }
        val query = MetadataMatcher.cleanQuery(metadata.title)
        if (query.isBlank()) return null
        return MetadataMatcher.bestMatch(
            query,
            service.searchGames(query),
            metadata.platforms.toSet()
        )?.sourceId
    }
}
