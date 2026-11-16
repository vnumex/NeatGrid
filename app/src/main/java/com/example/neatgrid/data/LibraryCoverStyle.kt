package com.example.neatgrid.data

enum class LibraryCoverStyle(val label: String, val aspectRatio: Float) {
    BOX_ART("Box Art", 2f / 3f),
    ICON("Icons", 1f);

    fun effective(iidbConfigured: Boolean): LibraryCoverStyle {
        return if (this == ICON && !iidbConfigured) BOX_ART else this
    }

    fun artworkUrl(coverUrl: String?, iconUrl: String?): String? = when (this) {
        BOX_ART -> coverUrl ?: iconUrl
        ICON -> iconUrl ?: coverUrl
    }

    companion object {
        fun fromPreference(value: String?): LibraryCoverStyle {
            return entries.firstOrNull { it.name == value } ?: BOX_ART
        }
    }
}
