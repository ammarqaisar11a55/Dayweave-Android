package io.github.ammarqaisar11a55.dayweave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.ammarqaisar11a55.dayweave.R
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The same tokens as the desktop app's docs/DESIGN.md, including its contrast decisions: filled orange
// uses dark ink, and small orange text uses a darker or lighter orange per theme.

val Accent = Color(0xFFE95420)
val AccentInk = Color(0xFF1F0A02)

@Immutable
data class DayweaveColors(
    val window: Color,
    val card: Color,
    val elevated: Color,
    val sunken: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val accentText: Color,
    val accentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val violet: Color,
    val threadIdle: Color,
    val threadGhost: Color,
    val threadWarp: Color,
    val heat: List<Color>,
    val palette: Map<String, Color>,
    val dark: Boolean,
)

private val Light = DayweaveColors(
    window = Color(0xFFF7F7F8), card = Color.White, elevated = Color.White, sunken = Color(0xFFF1F1F3),
    line = Color(0xFFE8E8EA), ink = Color(0xFF202124), ink2 = Color(0xFF6C6C72), ink3 = Color(0xFFA2A2A8),
    accentText = Color(0xFFC2410F), accentSoft = Color(0xFFFDEEE8), success = Color(0xFF1B7F54),
    warning = Color(0xFF8F6400), danger = Color(0xFFBF2F2B), info = Color(0xFF2267AE), violet = Color(0xFF6A49B8),
    threadIdle = Color(0xFFDCDCE0), threadGhost = Color(0xFFECECEE), threadWarp = Color(0xFFF4996F),
    heat = listOf(0xFFECECEE, 0xFFF9D2BF, 0xFFF4A57E, 0xFFEE7A48, 0xFFD9480F).map(::Color),
    palette = mapOf(
        "orange" to 0xFFC9470F, "red" to 0xFFC53A3A, "amber" to 0xFFA06C00, "green" to 0xFF1B7F54,
        "teal" to 0xFF0F7D80, "blue" to 0xFF2267AE, "violet" to 0xFF6A49B8, "pink" to 0xFFB83B7E, "slate" to 0xFF5A6472,
    ).mapValues { Color(it.value) },
    dark = false,
)

private val Dark = DayweaveColors(
    window = Color(0xFF171717), card = Color(0xFF252525), elevated = Color(0xFF2B2B2B), sunken = Color(0xFF1F1F1F),
    line = Color(0xFF383838), ink = Color(0xFFF5F5F5), ink2 = Color(0xFFA5A5A5), ink3 = Color(0xFF737373),
    accentText = Color(0xFFF4764A), accentSoft = Color(0xFF3A2519), success = Color(0xFF4CBB87),
    warning = Color(0xFFE5B23C), danger = Color(0xFFF0776F), info = Color(0xFF6FAEEB), violet = Color(0xFFB19AE8),
    threadIdle = Color(0xFF454545), threadGhost = Color(0xFF262626), threadWarp = Color(0xFFA8481F),
    heat = listOf(0xFF2B2B2B, 0xFF5A3020, 0xFF8F4322, 0xFFC9531F, 0xFFF4764A).map(::Color),
    palette = mapOf(
        "orange" to 0xFFF4764A, "red" to 0xFFF0776F, "amber" to 0xFFE5B23C, "green" to 0xFF4CBB87,
        "teal" to 0xFF4CC0C2, "blue" to 0xFF6FAEEB, "violet" to 0xFFB19AE8, "pink" to 0xFFEF8FC0, "slate" to 0xFF9EAABA,
    ).mapValues { Color(it.value) },
    dark = true,
)

val LocalDayweaveColors = staticCompositionLocalOf { Light }

object Dw {
    val colors: DayweaveColors @Composable get() = LocalDayweaveColors.current
}

fun DayweaveColors.named(name: String?): Color = palette[name] ?: palette.getValue("slate")

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Inter, as on the desktop, bundled so nothing is fetched. One variable file covers every weight, which
 * lets headings use the design system's 650 rather than the nearest static weight.
 */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(650), inter(700))

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.sp,
)

// The desktop's scale (docs/DESIGN.md), one step larger where touch reading distance calls for it.
// Material's letter spacing is tuned for Roboto and is dropped: Inter is spaced for screens already.
private fun typography() = Typography(
    displaySmall = style(34, 40, 650, -0.8),
    headlineLarge = style(32, 38, 650, -0.7),
    headlineMedium = style(28, 34, 650, -0.5),
    headlineSmall = style(24, 30, 650, -0.4),
    titleLarge = style(20, 26, 600, -0.2),
    titleMedium = style(17, 24, 600, -0.1),
    titleSmall = style(15, 21, 600),
    bodyLarge = style(16, 23, 400),
    bodyMedium = style(14, 20, 400),
    bodySmall = style(13, 18, 400),
    labelLarge = style(14, 20, 600),
    labelMedium = style(13, 18, 500),
    labelSmall = style(12, 16, 500),
)

val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun DayweaveTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val tokens = if (dark) Dark else Light
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = Accent,
        onPrimary = AccentInk,
        primaryContainer = tokens.accentSoft,
        onPrimaryContainer = tokens.accentText,
        secondary = tokens.accentText,
        secondaryContainer = tokens.accentSoft,
        onSecondaryContainer = tokens.accentText,
        background = tokens.window,
        onBackground = tokens.ink,
        surface = tokens.window,
        onSurface = tokens.ink,
        surfaceVariant = tokens.sunken,
        onSurfaceVariant = tokens.ink2,
        surfaceContainerLowest = tokens.card,
        surfaceContainerLow = tokens.card,
        surfaceContainer = tokens.card,
        surfaceContainerHigh = tokens.elevated,
        surfaceContainerHighest = tokens.sunken,
        outline = tokens.ink3,
        outlineVariant = tokens.line,
        error = tokens.danger,
    )
    CompositionLocalProvider(LocalDayweaveColors provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = typography(), shapes = AppShapes, content = content)
    }
}
