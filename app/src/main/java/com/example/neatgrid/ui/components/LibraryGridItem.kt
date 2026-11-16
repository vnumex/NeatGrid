package com.example.neatgrid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.neatgrid.data.AppInfo
import com.example.neatgrid.data.LibraryCoverStyle
import com.example.neatgrid.data.LibraryDensity
import com.example.neatgrid.data.LibraryTitleStyle

private val titleScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
)
private val fetchingScrim = Color.Black.copy(alpha = 0.5f)
private val backdropDim = Color.Black.copy(alpha = 0.25f)
private const val BACKDROP_SAMPLE_PX = 32

@Composable
fun LibraryGridItem(
    app: AppInfo,
    isFetching: Boolean,
    density: LibraryDensity,
    titleStyle: LibraryTitleStyle,
    coverStyle: LibraryCoverStyle,
    coverShape: Shape,
    onClick: (String) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val titleTextStyle = when (density) {
        LibraryDensity.COMFORTABLE -> MaterialTheme.typography.bodyMedium
        LibraryDensity.COMPACT -> MaterialTheme.typography.bodySmall
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = coverShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(coverStyle.aspectRatio)
                .pointerInput(app.packageName, onClick, onLongPress) {
                    detectTapGestures(
                        onTap = { onClick(app.packageName) },
                        onLongPress = { onLongPress(app.packageName) }
                    )
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CoverArt(
                    app = app,
                    artworkUrl = coverStyle.artworkUrl(app.coverUrl, app.iconUrl),
                    iconSize = density.iconSize()
                )
                if (titleStyle == LibraryTitleStyle.OVERLAY) {
                    OverlayTitle(text = app.label, style = titleTextStyle)
                }
                if (isFetching) {
                    FetchingIndicator()
                }
            }
        }
        if (titleStyle == LibraryTitleStyle.BELOW) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                textAlign = TextAlign.Center,
                text = app.label,
                style = titleTextStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun CoverArt(app: AppInfo, artworkUrl: String?, iconSize: Dp) {
    if (artworkUrl != null) {
        BlurredBackdrop(url = artworkUrl)
        AsyncImage(
            model = artworkUrl,
            contentDescription = app.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun BlurredBackdrop(url: String) {
    val context = LocalContext.current
    val request = remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .size(BACKDROP_SAMPLE_PX)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(16.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backdropDim)
    )
}

@Composable
private fun BoxScope.OverlayTitle(text: String, style: TextStyle) {
    Text(
        text = text,
        style = style,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(titleScrim)
            .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun FetchingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fetchingScrim),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.5.dp
        )
    }
}

private fun LibraryDensity.iconSize(): Dp = when (this) {
    LibraryDensity.COMFORTABLE -> 72.dp
    LibraryDensity.COMPACT -> 64.dp
}
