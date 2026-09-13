package com.dialy.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Aesthetic Stationery Color Palette
object DiaryColors {
    val PaperBackground = Color(0xFFFBF9F5)
    val CardBackground = Color(0xFFFFFFFF)
    val SubtleCard = Color(0xFFF7F4EE)
    val RuledLine = Color(0xFFE8E2D6)
    val BorderSubtle = Color(0xFFE2DCCE)

    val TextPrimary = Color(0xFF2D2A26)
    val TextSecondary = Color(0xFF7A746B)
    val TextTertiary = Color(0xFFA59E93)

    val RoseAccent = Color(0xFFE5989B)
    val RoseSoft = Color(0xFFFDE8E9)
    val SageAccent = Color(0xFF8FA382)
    val SageSoft = Color(0xFFEAF0E7)
    val PeachAccent = Color(0xFFEAA672)
    val PeachSoft = Color(0xFFFDF0E5)
    val LavenderAccent = Color(0xFFB5A6C9)
    val LavenderSoft = Color(0xFFF3EEF8)
    val GoldAccent = Color(0xFFD4A373)
    val GoldSoft = Color(0xFFFAEDCD)
}

val DiaryTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = 0.5.sp,
        color = DiaryColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.3.sp,
        color = DiaryColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.5.sp,
        color = DiaryColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = DiaryColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        color = DiaryColors.TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = DiaryColors.TextSecondary
    )
)

private val LightColorScheme = lightColorScheme(
    primary = DiaryColors.GoldAccent,
    onPrimary = Color.White,
    primaryContainer = DiaryColors.GoldSoft,
    onPrimaryContainer = DiaryColors.TextPrimary,
    background = DiaryColors.PaperBackground,
    onBackground = DiaryColors.TextPrimary,
    surface = DiaryColors.CardBackground,
    onSurface = DiaryColors.TextPrimary,
    surfaceVariant = DiaryColors.SubtleCard,
    onSurfaceVariant = DiaryColors.TextSecondary,
    outline = DiaryColors.BorderSubtle
)

@Composable
fun DiaryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = DiaryTypography,
        content = content
    )
}
