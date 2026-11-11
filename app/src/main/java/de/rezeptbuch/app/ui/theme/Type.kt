package de.rezeptbuch.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.rezeptbuch.app.R

/** „Plus Jakarta Sans“ als variable Schrift (SIL Open Font License, siehe licenses/). */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val AppTypography = Typography(
    displaySmall = style(36, FontWeight.ExtraBold, 42, -0.5),
    headlineLarge = style(32, FontWeight.ExtraBold, 38, -0.5),
    headlineMedium = style(28, FontWeight.ExtraBold, 34, -0.3),
    headlineSmall = style(24, FontWeight.ExtraBold, 30, -0.2),
    titleLarge = style(21, FontWeight.ExtraBold, 27, -0.2),
    titleMedium = style(17, FontWeight.Bold, 23),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(13, FontWeight.Medium, 18),
    labelLarge = style(15, FontWeight.SemiBold, 20),
    labelMedium = style(13, FontWeight.SemiBold, 16),
    labelSmall = style(11, FontWeight.SemiBold, 14, 0.2),
)
