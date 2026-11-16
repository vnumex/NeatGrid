package com.example.neatgrid.data

enum class LibraryTitleStyle(val label: String) {
    HIDDEN("None"),
    BELOW("Below"),
    OVERLAY("On Cover");

    companion object {
        fun fromPreference(value: String?, legacyShowNames: Boolean? = null): LibraryTitleStyle {
            entries.firstOrNull { it.name == value }?.let { return it }
            return if (legacyShowNames == false) HIDDEN else BELOW
        }
    }
}
