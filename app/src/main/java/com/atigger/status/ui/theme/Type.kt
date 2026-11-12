package com.atigger.status.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun statusText(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displaySmall = statusText(36, 42, FontWeight.SemiBold),
    headlineLarge = statusText(30, 38, FontWeight.Bold),
    headlineMedium = statusText(26, 34, FontWeight.Bold),
    headlineSmall = statusText(22, 30, FontWeight.SemiBold),
    titleLarge = statusText(20, 28, FontWeight.SemiBold),
    titleMedium = statusText(16, 24, FontWeight.SemiBold),
    titleSmall = statusText(14, 22, FontWeight.Medium),
    bodyLarge = statusText(15, 24),
    bodyMedium = statusText(13, 21),
    bodySmall = statusText(12, 19),
    labelLarge = statusText(13, 20, FontWeight.SemiBold),
    labelMedium = statusText(11, 17, FontWeight.Medium),
    labelSmall = statusText(10, 16, FontWeight.Medium)
)
