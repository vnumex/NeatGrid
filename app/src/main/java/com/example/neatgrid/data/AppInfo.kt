package com.example.neatgrid.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val coverUrl: String? = null,
    val platform: String? = null
)
