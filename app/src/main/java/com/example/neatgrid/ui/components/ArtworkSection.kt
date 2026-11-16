package com.example.neatgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.neatgrid.data.ArtworkImage
import com.example.neatgrid.data.ArtworkKind
import com.example.neatgrid.data.MetadataSource

private val thumbnailShape = RoundedCornerShape(8.dp)

@Composable
fun ArtworkSection(
    kind: ArtworkKind,
    groups: List<Pair<MetadataSource, List<ArtworkImage>>>,
    selectedUrl: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = kind.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (groups.isEmpty()) {
            Text(
                text = "No ${kind.label.lowercase()} images found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        groups.forEach { (source, images) ->
            Text(
                text = source.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images, key = { it.url }) { image ->
                    ArtworkThumbnail(
                        previewUrl = image.previewUrl,
                        size = kind.thumbnailSize(),
                        selected = image.url == selectedUrl,
                        onClick = { onSelect(image.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkThumbnail(
    previewUrl: String,
    size: DpSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(size)
            .clip(thumbnailShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(3.dp, borderColor, thumbnailShape)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(2.dp)
            )
        }
    }
}

private fun ArtworkKind.thumbnailSize(): DpSize = when (this) {
    ArtworkKind.COVER -> DpSize(96.dp, 144.dp)
    ArtworkKind.BANNER -> DpSize(192.dp, 108.dp)
    ArtworkKind.ICON -> DpSize(96.dp, 96.dp)
}
