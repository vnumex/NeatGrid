package com.example.neatgrid.data

import android.graphics.drawable.Drawable

data class RomInfo(
    val label: String,
    val romFileSource: String,
    val machine: String,
    val icon: Drawable,
)