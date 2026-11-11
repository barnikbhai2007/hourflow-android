package dev.hourflow.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.hourflow.app.R
import dev.hourflow.app.data.Category
import dev.hourflow.app.data.ThemeMode
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ShapeDefaults
import androidx.compose.foundation.isSystemInDarkTheme

val LocalHourFlowDark = staticCompositionLocalOf { false }

val HeroInk = Color(0xFF0D1420)
val HeroInkLight = Color(0xFF1B2A3E)
val Coral = Color(0xFFFF7A59)
val Cream = Color(0xFFF7F3EC)

private val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal)
)

private val HourFlowTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 45.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF121A28),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E9F1),
    onPrimaryContainer = Color(0xFF121A28),
    secondary = Color(0xFFE4573D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE5DE),
    onSecondaryContainer = Color(0xFF6B1B0A),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEDF0F4),
    onSurfaceVariant = Color(0xFF5A6474),
    outline = Color(0xFFD5DCE6),
    outlineVariant = Color(0xFFE6EAF0),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE4E0),
    onErrorContainer = Color(0xFF7A1A12)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF2F5FA),
    onPrimary = Color(0xFF0B111C),
    primaryContainer = Color(0xFF1A2535),
    onPrimaryContainer = Color(0xFFDCE4F0),
    secondary = Color(0xFFFF8A6A),
    onSecondary = Color(0xFF2A0C05),
    secondaryContainer = Color(0xFF3B1A12),
    onSecondaryContainer = Color(0xFFFFD8CD),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2A),
    background = Color(0xFF0A0F16),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF111823),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF192331),
    onSurfaceVariant = Color(0xFFA6B2C3),
    outline = Color(0xFF2B3747),
    outlineVariant = Color(0xFF1F2A38),
    error = Color(0xFFFF9B90),
    onError = Color(0xFF3A0A06),
    errorContainer = Color(0xFF4B1510),
    onErrorContainer = Color(0xFFFFD8D3)
)

private val HourFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HourFlowTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dark) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalHourFlowDark provides dark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HourFlowTypography,
            shapes = HourFlowShapes,
            content = content
        )
    }
}

@Composable
fun categoryAccent(category: Category): Color {
    val dark = LocalHourFlowDark.current
    return if (dark) {
        when (category) {
            Category.PHYSICS -> Color(0xFF6BA6FF)
            Category.CHEMISTRY -> Color(0xFF2DD4BF)
            Category.BIOLOGY -> Color(0xFF4ADE80)
            Category.REVISION -> Color(0xFFB39BFF)
            Category.MOCK -> Color(0xFFFBBF24)
            Category.CLASS -> Color(0xFF9AA8FF)
            Category.HEALTH -> Color(0xFFFF8A80)
            Category.BREAK -> Color(0xFF9AA9BC)
            Category.SLEEP -> Color(0xFFA5B4FC)
            Category.PERSONAL -> Color(0xFFF472B6)
        }
    } else {
        when (category) {
            Category.PHYSICS -> Color(0xFF2563EB)
            Category.CHEMISTRY -> Color(0xFF0F8F83)
            Category.BIOLOGY -> Color(0xFF15803D)
            Category.REVISION -> Color(0xFF6D28D9)
            Category.MOCK -> Color(0xFFB45309)
            Category.CLASS -> Color(0xFF4338CA)
            Category.HEALTH -> Color(0xFFB91C1C)
            Category.BREAK -> Color(0xFF5B6778)
            Category.SLEEP -> Color(0xFF4F46E5)
            Category.PERSONAL -> Color(0xFFBE185D)
        }
    }
}

@Composable
fun categoryContainer(category: Category): Color {
    val dark = LocalHourFlowDark.current
    return categoryAccent(category).copy(alpha = if (dark) 0.18f else 0.10f)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
