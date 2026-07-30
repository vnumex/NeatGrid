package com.example.neatgrid.data

enum class LibrarySortMode(val label: String) {
    TITLE_ASCENDING("Title A-Z"),
    TITLE_DESCENDING("Title Z-A"),
    PLATFORM("Platform");

    companion object {
        fun fromPreference(value: String?): LibrarySortMode {
            return entries.firstOrNull { it.name == value } ?: TITLE_ASCENDING
        }
    }
}
