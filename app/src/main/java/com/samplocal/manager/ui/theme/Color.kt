package com.samplocal.manager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF22C55E)

@Immutable
data class AppPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val divider: Color,
    val textMain: Color,
    val textDim: Color,
    val accent: Color,
    val wizardBg: Color,
    val wizardSubtitle: Color,
    val wizardDesc: Color
)

private val DarkPaletteBase = AppPalette(
    bg = Color(0xFF0B0F14),
    surface = Color(0xFF121826),
    surface2 = Color(0xFF1A2233),
    border = Color(0xFF232E45),
    divider = Color(0xFF1E2637),
    textMain = Color(0xFFE8EDF5),
    textDim = Color(0xFF9AA7BD),
    accent = BrandGreen,
    wizardBg = Color(0xFF030504),
    wizardSubtitle = Color(0xFFB9C4D1),
    wizardDesc = Color(0xFF8E99A8)
)

private val LightPaletteBase = AppPalette(
    bg = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE9EDF4),
    border = Color(0xFFD5DCE8),
    divider = Color(0xFFE2E8F2),
    textMain = Color(0xFF101828),
    textDim = Color(0xFF5B6B82),
    accent = BrandGreen,
    wizardBg = Color(0xFF030504),
    wizardSubtitle = Color(0xFFB9C4D1),
    wizardDesc = Color(0xFF8E99A8)
)

fun paletteFor(dark: Boolean, accentArgb: Int?): AppPalette {
    val base = if (dark) DarkPaletteBase else LightPaletteBase
    if (accentArgb == null) return base
    return try {
        base.copy(accent = Color(accentArgb))
    } catch (_: Exception) {
        base
    }
}

val LocalPalette = staticCompositionLocalOf { DarkPaletteBase }

val Bg: Color @Composable get() = LocalPalette.current.bg
val Surface: Color @Composable get() = LocalPalette.current.surface
val Surface2: Color @Composable get() = LocalPalette.current.surface2
val Border: Color @Composable get() = LocalPalette.current.border
val TextMain: Color @Composable get() = LocalPalette.current.textMain
val TextDim: Color @Composable get() = LocalPalette.current.textDim

val Accent: Color @Composable get() = LocalPalette.current.accent
val Divider: Color @Composable get() = LocalPalette.current.divider
val WizardBg: Color @Composable get() = LocalPalette.current.wizardBg
val WizardSubtitle: Color @Composable get() = LocalPalette.current.wizardSubtitle
val WizardDesc: Color @Composable get() = LocalPalette.current.wizardDesc

val Green: Color @Composable get() = LocalPalette.current.accent
val Neon: Color @Composable get() = LocalPalette.current.accent

val Red = Color(0xFFEF4444)
val Yellow = Color(0xFFEAB308)
val Orange = Color(0xFFFB923C)
val WizardOnNeon = Color(0xFF04120A)
