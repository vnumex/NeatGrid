package com.example.neatgrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.neatgrid.data.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContextMenu(
    game: AppInfo,
    roundedCovers: Boolean,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onDetails: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
) {
    val coverShape = if (roundedCovers) RoundedCornerShape(6.dp) else RectangleShape
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = game.coverUrl ?: game.icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(coverShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = game.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                game.platform?.let { platform ->
                    Text(
                        text = platform,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider()
        GameActionRow("Launch", Icons.Default.PlayArrow, onLaunch)
        GameActionRow("View Details", Icons.Default.Info, onDetails)
        GameActionRow("Hide from Library", Icons.Default.VisibilityOff, onHide)
        GameActionRow(
            label = "Remove from Library",
            icon = Icons.Default.DeleteOutline,
            onClick = onDelete,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun GameActionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(text = label, color = color) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
        }
    )
}
