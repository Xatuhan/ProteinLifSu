package com.proteinlifsu.ui.theme

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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proteinlifsu.domain.Metric
import com.proteinlifsu.domain.ThemeMode

/** Sade, sıcak palet. Açık ve koyu modda aynı yapı, aynı sadelik. */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val danger: Color,
    val protein: Color,
    val fiber: Color,
    val water: Color,
    /** Halka izinin vurgu rengine göre saydamlığı. */
    val trackAlpha: Float,
) {
    val onAccent: Color get() = Color.White

    fun accent(metric: Metric): Color = when (metric) {
        Metric.PROTEIN -> protein
        Metric.FIBER -> fiber
        Metric.WATER -> water
    }
}

private val Light = AppColors(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF6F4F1),
    hairline = Color(0xFFECE8E3),
    textPrimary = Color(0xFF2B2724),
    textSecondary = Color(0xFF6F6862),
    textMuted = Color(0xFFA8A19A),
    danger = Color(0xFFE5484D),
    // Canlı ama beyazda okunaklı tonlar.
    protein = Color(0xFFFF8A00),
    fiber = Color(0xFF00C46A),
    water = Color(0xFF0095FF),
    trackAlpha = 0.14f,
)

private val Dark = AppColors(
    isDark = true,
    background = Color(0xFF151413),
    surface = Color(0xFF211F1D),
    hairline = Color(0xFF2E2B28),
    textPrimary = Color(0xFFF3F0EC),
    textSecondary = Color(0xFFB9B2AB),
    textMuted = Color(0xFF7D766F),
    danger = Color(0xFFFF6B6B),
    protein = Color(0xFFFF9F1A),
    fiber = Color(0xFF1EDC82),
    water = Color(0xFF33AAFF),
    trackAlpha = 0.2f,
)

val LocalAppColors = staticCompositionLocalOf { Light }

/** Kısa erişim: `Palette.protein` vb. (yalnız composable içinde). */
val Palette: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

/** Rakamlar: tabular (eş genişlikli) — değer değişirken rakamlar yerinde kalır. */
object NumStyle {
    val Large: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFeatureSettings = "tnum",
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = Palette.textPrimary,
        )
    val Small: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFeatureSettings = "tnum",
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Palette.textSecondary,
        )
    val Body: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFeatureSettings = "tnum",
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = Palette.textPrimary,
        )
}

/** Bölüm başlığı: normal yazımlı, sakin. */
val LabelStyle: TextStyle
    @Composable @ReadOnlyComposable get() = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Palette.textPrimary,
    )

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ProteinLifSuTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.protein, onPrimary = Color.White, secondary = c.fiber, tertiary = c.water,
            background = c.background, onBackground = c.textPrimary,
            surface = c.background, onSurface = c.textPrimary,
            surfaceVariant = c.surface, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.background, surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surface,
            outline = c.hairline, outlineVariant = c.hairline,
            inverseSurface = c.textPrimary, inverseOnSurface = c.background, inversePrimary = Color(0xFFB85F00),
            error = c.danger,
        )
    } else {
        lightColorScheme(
            primary = c.protein, onPrimary = Color.White, secondary = c.fiber, tertiary = c.water,
            background = c.background, onBackground = c.textPrimary,
            surface = c.background, onSurface = c.textPrimary,
            surfaceVariant = c.surface, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.background, surfaceContainerHigh = c.background, surfaceContainerHighest = c.surface,
            outline = c.hairline, outlineVariant = c.hairline,
            inverseSurface = c.textPrimary, inverseOnSurface = Color.White, inversePrimary = Color(0xFFFFB45C),
            error = c.danger,
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, shapes = shapes, typography = Typography(), content = content)
    }
}
