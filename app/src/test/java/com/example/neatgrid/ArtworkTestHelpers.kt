package com.example.neatgrid

import com.example.neatgrid.data.ArtworkImage
import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.data.ArtworkOptions
import com.example.neatgrid.data.MetadataSource

fun images(vararg urls: String): List<ArtworkImage> = urls.map { ArtworkImage(it) }

fun ArtworkOptions.urlsOf(kind: ArtworkKind): List<Pair<MetadataSource, List<String>>> =
    of(kind).map { (source, images) -> source to images.map { it.url } }
