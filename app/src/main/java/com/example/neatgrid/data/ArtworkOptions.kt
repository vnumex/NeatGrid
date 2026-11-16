package com.example.neatgrid.data

enum class ArtworkKind(val label: String) {
    COVER("Cover"),
    BANNER("Banner"),
    ICON("Icon")
}

data class ArtworkImage(val url: String, val previewUrl: String = url)

data class ArtworkUrls(
    val covers: List<ArtworkImage> = emptyList(),
    val banners: List<ArtworkImage> = emptyList(),
    val icons: List<ArtworkImage> = emptyList()
)

data class ArtworkOptions(val bySource: List<Pair<MetadataSource, ArtworkUrls>> = emptyList()) {

    fun of(kind: ArtworkKind): List<Pair<MetadataSource, List<ArtworkImage>>> {
        val seen = mutableSetOf<String>()
        return bySource.mapNotNull { (source, urls) ->
            val list = when (kind) {
                ArtworkKind.COVER -> urls.covers
                ArtworkKind.BANNER -> urls.banners
                ArtworkKind.ICON -> urls.icons
            }.filter { seen.add(it.url) }
            list.takeIf { it.isNotEmpty() }?.let { source to it }
        }
    }

    fun isEmpty(): Boolean = ArtworkKind.entries.all { of(it).isEmpty() }
}

fun GameMetadata.withArtwork(cover: String?, banner: String?, icon: String?): GameMetadata = copy(
    coverUrl = cover ?: coverUrl,
    backdropUrl = banner ?: backdropUrl,
    iconUrl = icon ?: iconUrl
)
