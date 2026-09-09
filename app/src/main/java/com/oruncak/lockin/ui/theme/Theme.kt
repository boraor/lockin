package com.oruncak.lockin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette shared with the design prototype: amber for warnings/enforcement,
// blue for primary actions, green for streaks/success, red for restricted state.
val Amber = Color(0xFFE8A33D)
val AmberDim = Color(0xFF4A3A1D)
val Blue = Color(0xFF4E8DF0)
val BlueDim = Color(0xFF1C2A44)
val Green = Color(0xFF3FBF83)
val Red = Color(0xFFE5584F)

private val DarkColors = darkColorScheme(
    primary = Blue,
    secondary = Amber,
    tertiary = Green,
    error = Red,
    background = Color(0xFF0D1013),
    surface = Color(0xFF15181C),
    surfaceVariant = Color(0xFF1D2127),
    onBackground = Color(0xFFECEEF0),
    onSurface = Color(0xFFECEEF0)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F65CF),
    secondary = Color(0xFFB9740A),
    tertiary = Color(0xFF1F8A5A),
    error = Color(0xFFC23D35),
    background = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF6F7F9),
    onBackground = Color(0xFF171A1E),
    onSurface = Color(0xFF171A1E)
)

/** appearance: "system" | "light" | "dark" — matches the value stored in LockInSettings. */
@Composable
fun LockInTheme(appearance: String = "system", content: @Composable () -> Unit) {
    val useDark = when (appearance) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}
