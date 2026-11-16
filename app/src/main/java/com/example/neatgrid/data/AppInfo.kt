package com.example.neatgrid.data

import android.graphics.drawable.Drawable

data class AppInfo(
    override val label: String,
    override val packageName: String,
    val icon: Drawable,
    val coverUrl: String? = null,
    override val platform: String? = null,
    val iconUrl: String? = null
) : LibraryEntry
