package com.example.neatgrid.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.neatgrid.data.MetadataSource

@Composable
fun MetadataSourceRow(
    position: Int,
    source: MetadataSource,
    isLast: Boolean,
    enabled: Boolean,
    keyConfigured: Boolean,
    onMove: (delta: Int) -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    ListItem(
        modifier = Modifier.padding(start = 8.dp),
        headlineContent = {
            Text(
                text = "${position + 1}. ${source.displayName}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        },
        supportingContent = {
            Text(
                text = sourceStatus(source, enabled, keyConfigured),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = position > 0, onClick = { onMove(-1) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move ${source.displayName} up"
                    )
                }
                IconButton(enabled = !isLast, onClick = { onMove(1) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move ${source.displayName} down"
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
    )
}

private fun sourceStatus(source: MetadataSource, enabled: Boolean, keyConfigured: Boolean): String {
    if (!enabled) return "Off"
    val keyStatus = when {
        !source.requiresApiKey -> "No API key required"
        keyConfigured -> "API key configured"
        else -> "API key required"
    }
    return if (source.providesMetadata) keyStatus else "Artwork only · $keyStatus"
}
