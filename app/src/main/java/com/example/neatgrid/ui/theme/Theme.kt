package com.example.neatgrid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeutralPrimaryDark,
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF111111),
    secondary = NeutralSecondaryDark,
    onSecondary = Color(0xFF202020),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = NeutralTertiaryDark,
    onTertiary = Color(0xFF202020),
    tertiaryContainer = Color(0xFF4A4A4A),
    onTertiaryContainer = Color(0xFFF0F0F0),
    background = AppDarkBackground,
    onBackground = AppDarkOnSurface,
    surface = AppDarkSurface,
    onSurface = AppDarkOnSurface,
    surfaceBright = AppDarkSurface,
    surfaceDim = AppDarkSurface,
    surfaceContainerLowest = AppDarkSurface,
    surfaceContainerLow = AppDarkSurface,
    surfaceContainer = AppDarkSurface,
    surfaceContainerHigh = AppDarkSurface,
    surfaceContainerHighest = AppDarkSurface,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = AppDarkOnSurfaceVariant,
    surfaceTint = Color.Transparent,
    outline = Color(0xFF8F8F8F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = NeutralPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF111111),
    secondary = NeutralSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF181818),
    tertiary = NeutralTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDADADA),
    onTertiaryContainer = Color(0xFF202020),
    background = AppLightBackground,
    onBackground = AppLightOnSurface,
    surface = AppLightSurface,
    onSurface = AppLightOnSurface,
    surfaceBright = AppLightSurface,
    surfaceDim = AppLightSurface,
    surfaceContainerLowest = AppLightSurface,
    surfaceContainerLow = AppLightSurface,
    surfaceContainer = AppLightSurface,
    surfaceContainerHigh = AppLightSurface,
    surfaceContainerHighest = AppLightSurface,
    surfaceVariant = AppLightSurfaceVariant,
    onSurfaceVariant = AppLightOnSurfaceVariant,
    surfaceTint = Color.Transparent,
    outline = Color(0xFF747474),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private fun ColorScheme.withAmoledBlack() = copy(
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceDim = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledBlack,
    surfaceContainer = AmoledBlack,
    surfaceContainerHigh = AmoledBlack,
    surfaceContainerHighest = AmoledBlack,
    surfaceVariant = AmoledSurfaceVariant,
    surfaceTint = Color.Transparent
)

@Composable
fun NeatGridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = if (amoledBlack && darkTheme) {
        baseColorScheme.withAmoledBlack()
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
