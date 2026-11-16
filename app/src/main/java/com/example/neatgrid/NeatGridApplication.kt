package com.example.neatgrid

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class NeatGridApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .respectCacheHeaders(false)
            .build()
    }
}
