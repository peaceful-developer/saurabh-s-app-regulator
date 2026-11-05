package com.saurabh.sar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.saurabh.sar.R

/** Colour tokens from docs/DESIGN.md (Figma "SAR Tokens", mode Dark). Dark only. */
object SarColors {
    val BgBase = Color(0xFF05070F)
    val BgSurface = Color(0xFF0B1020)
    val BgRaised = Color(0xFF131B36)
    val LineSubtle = Color(0xFF1F2B52)
    val Cyan = Color(0xFF22E5FF)
    val Violet = Color(0xFF8B5CF6)
    val Danger = Color(0xFFFF3D7F)
    val Warn = Color(0xFFFFB020)
    val Ok = Color(0xFF2BFFA3)
    val TextPrimary = Color(0xFFE8EEFF)
    val TextSecondary = Color(0xFF8A97C2)
    val OnAccent = Color(0xFF04121A)
}

/** Bundled fonts (no downloadable fonts, no network). Both SIL Open Font License. */
val Orbitron = FontFamily(Font(R.font.orbitron_bold, FontWeight.Bold))
val Exo2 = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_semibold, FontWeight.SemiBold),
)

/** Type roles from DESIGN.md → Typography. */
object SarType {
    val Logo = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.12.em, color = SarColors.Cyan)
    val ScreenTitle = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.08.em, color = SarColors.TextPrimary)
    val Hero = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.04.em, color = SarColors.TextPrimary)
    val BigNumber = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = SarColors.TextPrimary)
    val Huge = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 42.sp, color = SarColors.TextPrimary)
    val Section = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.14.em, color = SarColors.TextSecondary)
    val Button = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.08.em)
    val Chip = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.10.em)
    val CardTitle = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.04.em, color = SarColors.TextPrimary)
    val Body = TextStyle(fontFamily = Exo2, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = SarColors.TextPrimary)
    val BodySecondary = TextStyle(fontFamily = Exo2, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = SarColors.TextSecondary)
    val Meta = TextStyle(fontFamily = Exo2, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = SarColors.TextSecondary)
    val RowTitle = TextStyle(fontFamily = Exo2, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SarColors.TextPrimary)
    val BlockMessage = TextStyle(fontFamily = Exo2, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, color = SarColors.TextPrimary)
}

object SarShapes {
    val Button = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(18.dp)
    val CardLarge = RoundedCornerShape(24.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Input = RoundedCornerShape(14.dp)
    val Key = RoundedCornerShape(16.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

private val scheme: ColorScheme = darkColorScheme(
    primary = SarColors.Cyan,
    onPrimary = SarColors.OnAccent,
    secondary = SarColors.Violet,
    background = SarColors.BgBase,
    onBackground = SarColors.TextPrimary,
    surface = SarColors.BgSurface,
    onSurface = SarColors.TextPrimary,
    surfaceVariant = SarColors.BgRaised,
    onSurfaceVariant = SarColors.TextSecondary,
    outline = SarColors.LineSubtle,
    error = SarColors.Danger,
)

@Composable
fun SarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(bodyMedium = SarType.Body, bodyLarge = SarType.Body, labelLarge = SarType.Button),
        content = content,
    )
}
