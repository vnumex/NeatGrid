package com.example.neatgrid.data

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class)
class ImageCache(private val context: Context) {

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        context.imageLoader.diskCache?.size ?: 0L
    }

    suspend fun clear() {
        val imageLoader = context.imageLoader
        imageLoader.memoryCache?.clear()
        withContext(Dispatchers.IO) {
            imageLoader.diskCache?.clear()
        }
    }
}
