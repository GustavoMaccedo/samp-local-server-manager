package com.samplocal.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun SampTheme(
    dark: Boolean = true,
    accentArgb: Int? = null,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(dark, accentArgb)
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = WizardOnNeon,
            background = palette.bg,
            onBackground = palette.textMain,
            surface = palette.surface,
            onSurface = palette.textMain,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.textDim,
            error = Red,
            onError = palette.textMain
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = WizardOnNeon,
            background = palette.bg,
            onBackground = palette.textMain,
            surface = palette.surface,
            onSurface = palette.textMain,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.textDim,
            error = Red,
            onError = palette.textMain
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
