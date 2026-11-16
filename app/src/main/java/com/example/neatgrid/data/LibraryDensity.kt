package com.example.neatgrid.data

enum class LibraryDensity(val label: String, val columns: Int) {
    COMFORTABLE("Comfortable", 2),
    COMPACT("Compact", 3);

    companion object {
        fun fromPreference(value: String?, legacyColumns: Int? = null): LibraryDensity {
            entries.firstOrNull { it.name == value }?.let { return it }
            return if (legacyColumns != null && legacyColumns <= COMFORTABLE.columns) COMFORTABLE else COMPACT
        }
    }
}
