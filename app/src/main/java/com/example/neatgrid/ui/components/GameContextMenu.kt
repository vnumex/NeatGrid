package com.example.neatgrid.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GameContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Launch") },
            onClick = onLaunch
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = onDelete
        )
    }
}