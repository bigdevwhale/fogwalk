package app.fogwalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.data.ThemeMode

/** A storybook atlas: parchment and ink by day, a candle-lit map table by night, gilt and emerald throughout. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val gold: Color,
    val goldSoft: Color,
    val sage: Color,
    val sageSoft: Color,
    val sky: Color,
    val skySoft: Color,
    val violet: Color,
    val violetSoft: Color,
    val rose: Color,
    val roseSoft: Color,
    val flame: Color,
    val flameSoft: Color,
    val danger: Color,
    /** Gilt edging on cards and frames. */
    val gilt: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFEFE3C8),
    surface = Color(0xFFFBF4E2),
    surface2 = Color(0xFFF4EAD2),
    line = Color(0xFFDCC9A0),
    ink = Color(0xFF2B1D10),
    muted = Color(0xFF6E5A42),
    faint = Color(0xFFA48F6D),
    accent = Color(0xFF1E6B50),
    accentSoft = Color(0xFFD7E6D3),
    onAccent = Color(0xFFFBF4E2),
    gold = Color(0xFFAE7A1C),
    goldSoft = Color(0xFFF3E1B4),
    sage = Color(0xFF55813C),
    sageSoft = Color(0xFFE1EBCB),
    sky = Color(0xFF2F6596),
    skySoft = Color(0xFFDCE6EE),
    violet = Color(0xFF6E3F9E),
    violetSoft = Color(0xFFE9DDF0),
    rose = Color(0xFF9E3A55),
    roseSoft = Color(0xFFF2DCDD),
    flame = Color(0xFFB4441E),
    flameSoft = Color(0xFFF5DCC6),
    danger = Color(0xFFA8322A),
    gilt = Color(0xFFC9A24E),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF14100B),
    surface = Color(0xFF221A11),
    surface2 = Color(0xFF2A2116),
    line = Color(0xFF433522),
    ink = Color(0xFFF1E4C6),
    muted = Color(0xFFC2AE8A),
    faint = Color(0xFF85745A),
    accent = Color(0xFF5FC79C),
    accentSoft = Color(0xFF1A3A2C),
    onAccent = Color(0xFF0E1F17),
    gold = Color(0xFFE9B949),
    goldSoft = Color(0xFF3A2C11),
    sage = Color(0xFF9CC77A),
    sageSoft = Color(0xFF233019),
    sky = Color(0xFF86B6E6),
    skySoft = Color(0xFF1B2A3A),
    violet = Color(0xFFB896E8),
    violetSoft = Color(0xFF2E2240),
    rose = Color(0xFFE88AA0),
    roseSoft = Color(0xFF3A1E26),
    flame = Color(0xFFF08A4B),
    flameSoft = Color(0xFF3B2214),
    danger = Color(0xFFFF6B5E),
    gilt = Color(0xFFB08A3C),
    dark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Body text: a humanist sans with a calligraphic hand. */
val Sans = FontFamily(
    Font(R.font.sans_regular, FontWeight.Normal),
    Font(R.font.sans_medium, FontWeight.Medium),
    Font(R.font.sans_bold, FontWeight.SemiBold),
    Font(R.font.sans_bold, FontWeight.Bold),
)

/** Small capitals for titles, the way old atlases letter their cartouches. */
val Rune = FontFamily(
    Font(R.font.sc_medium, FontWeight.Medium),
    Font(R.font.sc_bold, FontWeight.Bold),
)

/** A book face for lore and names of things. */
val Book = FontFamily(Font(R.font.alegreya, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(22.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Rune, fontFeatureSettings = "lnum", fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Rune, fontFeatureSettings = "lnum", fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontFeatureSettings = "lnum", fontSize = 15.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Rune, fontFeatureSettings = "lnum", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, letterSpacing = 0.08.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontFeatureSettings = "lnum", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontFeatureSettings = "lnum", fontSize = 16.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontFeatureSettings = "lnum", fontSize = 14.sp, lineHeight = 19.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontFeatureSettings = "lnum", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Rune, fontFeatureSettings = "lnum", fontWeight = FontWeight.Bold, fontSize = 23.sp, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontFeatureSettings = "lnum", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val Quote: TextStyle @Composable get() = TextStyle(fontFamily = Book, fontSize = 18.sp, lineHeight = 26.sp, color = U.c.ink)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun FogwalkTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
