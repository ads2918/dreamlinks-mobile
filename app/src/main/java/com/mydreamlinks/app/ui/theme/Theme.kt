package com.mydreamlinks.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mydreamlinks.app.R

/* The site's poster palette (dreamlinks.css :root). */
object Ink {
    val paper = Color(0xFFCFD6FF)
    val card = Color(0xFFFFFFFF)
    val ink = Color(0xFF26134A)
    val muted = Color(0xFF5D4F86)
    val pink = Color(0xFFFF4F9A)
    val orange = Color(0xFFFF8A3D)
    val yellow = Color(0xFFFFD93D)
    val teal = Color(0xFF18B7A6)
    val violet = Color(0xFF7A5CFF)
    val night = Color(0xFF2B1656)
    val danger = Color(0xFFD6245F)
}

val Display = FontFamily(Font(R.font.shrikhand))
val Mono = FontFamily(Font(R.font.dm_mono_medium, FontWeight.Medium))

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun sans(weight: Int) = Font(
    R.font.instrument_sans,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)
val Sans = FontFamily(sans(400), sans(500), sans(600), sans(700))

private val type = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontSize = 34.sp, lineHeight = 38.sp, color = Ink.ink),
    headlineMedium = TextStyle(fontFamily = Display, fontSize = 26.sp, lineHeight = 30.sp, color = Ink.ink),
    headlineSmall = TextStyle(fontFamily = Display, fontSize = 21.sp, lineHeight = 26.sp, color = Ink.ink),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = Ink.muted),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.6.sp),
)

@Composable
fun DreamLinksTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink.pink, onPrimary = Ink.card,
            secondary = Ink.yellow, onSecondary = Ink.ink,
            tertiary = Ink.teal, onTertiary = Ink.card,
            background = Ink.paper, onBackground = Ink.ink,
            surface = Ink.card, onSurface = Ink.ink,
            surfaceVariant = Ink.paper, onSurfaceVariant = Ink.muted,
            outline = Ink.ink, error = Ink.danger,
        ),
        typography = type,
        content = content,
    )
}
