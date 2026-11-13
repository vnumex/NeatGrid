package com.example.neatgrid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ApiKeyDialog(
    title: String,
    currentKey: String,
    helperText: String,
    helperUrl: String,
    onVerify: ((String, (Result<String>) -> Unit) -> Unit)? = null,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<Result<String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        verifyResult = null
                    },
                    enabled = !isVerifying,
                    label = { Text(text = "API Key") },
                    singleLine = true,
                    visualTransformation = if (keyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (keyVisible) "Hide API key" else "Show API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri(helperUrl) }
                    }
                )

                if (onVerify != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            enabled = keyInput.isNotBlank() && !isVerifying,
                            onClick = {
                                isVerifying = true
                                verifyResult = null
                                onVerify(keyInput.trim()) { result ->
                                    isVerifying = false
                                    verifyResult = result
                                }
                            }
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = if (isVerifying) "Verifying..." else "Verify")
                        }
                    }

                    verifyResult?.let { result ->
                        val isSuccess = result.isSuccess
                        val message = if (isSuccess) {
                            result.getOrNull().orEmpty().ifBlank { "Verified" }
                        } else {
                            result.exceptionOrNull()?.message ?: "Verification failed"
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(keyInput.trim()) }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = keyInput.isNotEmpty() && !isVerifying,
                    onClick = {
                        keyInput = ""
                        verifyResult = null
                    }
                ) {
                    Text(text = "Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationCreditsDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )
    val linkListener = LinkInteractionListener { link ->
        if (link is LinkAnnotation.Url) {
            runCatching { uriHandler.openUri(link.url) }
        }
    }
    val iidbAttribution = buildAnnotatedString {
        append("Catalog data from ")
        withLink(LinkAnnotation.Url("https://iidb.iisu.network", linkStyles, linkListener)) {
            append("iiDB (iisu.network)")
        }
        append(", adapted from the ")
        withLink(LinkAnnotation.Url("https://github.com/libretro/libretro-database", linkStyles, linkListener)) {
            append("Libretro Database")
        }
        append(", ")
        withLink(LinkAnnotation.Url("https://creativecommons.org/licenses/by-sa/4.0/", linkStyles, linkListener)) {
            append("CC BY-SA 4.0")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = { Text(text = "Information & Credits") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { contentPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NeatGrid",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version $appVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Developer & Source Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Developer:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "vnumex",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    runCatching { uriHandler.openUri("https://github.com/vnumex") }
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Source Code:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "NeatGrid on GitHub",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    runCatching { uriHandler.openUri("https://github.com/vnumex/NeatGrid") }
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Metadata Acknowledgments & Licenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "iiDB (iiSU)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                runCatching { uriHandler.openUri("https://iidb.iisu.network") }
                            }
                        )

                        Text(
                            text = iidbAttribution,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "LaunchBox Games Database",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Fallback catalog and cover metadata provided by the LaunchBox Games Database.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "RAWG",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Optional user-keyed metadata and artwork source via the RAWG Video Games Database API.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Third-party artwork, soundbites, and trademarks remain the property of their respective owners.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
