package com.example.pomostudy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.pomostudy.domain.model.ThemeMode

/** 单色（黑白几何）浅色主题，取自 logo 的墨黑 / 纸白。 */
private val LightColors = lightColorScheme(
    primary = MonoInk,
    onPrimary = MonoPaper,
    primaryContainer = MonoPaperDim,
    onPrimaryContainer = MonoInk,
    secondary = MonoGrey,
    onSecondary = MonoPaper,
    tertiary = Color(0xFF6E6E73),
    background = MonoPaper,
    onBackground = MonoInk,
    surface = MonoPaper,
    onSurface = MonoInk,
    surfaceVariant = Color(0xFFECECEF),
    onSurfaceVariant = MonoGrey,
    outline = MonoOutline,
    outlineVariant = Color(0xFFDADADF)
)

/** 单色（黑白几何）深色主题。 */
private val DarkColors = darkColorScheme(
    primary = MonoPaper,
    onPrimary = MonoInk,
    primaryContainer = MonoInkSoft,
    onPrimaryContainer = MonoPaper,
    secondary = MonoGreyLight,
    onSecondary = MonoInk,
    tertiary = Color(0xFFC8C8CC),
    background = MonoSurfaceDark,
    onBackground = MonoPaper,
    surface = MonoSurfaceDark,
    onSurface = MonoPaper,
    surfaceVariant = MonoInkSoft,
    onSurfaceVariant = MonoGreyLight,
    outline = Color(0xFF6E6E73),
    outlineVariant = Color(0xFF3A3A40)
)

/** 马卡龙色系（更鲜明的柔和配色）：薰衣草紫 + 薄荷绿 + 樱花粉，背景带淡紫调。 */
private val MacaronColors = lightColorScheme(
    primary = Color(0xFF8E6BD1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7D6FF),
    onPrimaryContainer = Color(0xFF33165E),
    secondary = Color(0xFF3FB38C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEFE2),
    onSecondaryContainer = Color(0xFF0E3B2D),
    tertiary = Color(0xFFEF6E97),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7E2),
    onTertiaryContainer = Color(0xFF4A0F26),
    background = Color(0xFFF4ECFB),
    onBackground = Color(0xFF2C2140),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF2C2140),
    surfaceVariant = Color(0xFFE9DCF7),
    onSurfaceVariant = Color(0xFF56466F),
    outline = Color(0xFFB49ED6),
    outlineVariant = Color(0xFFD9C9EE)
)

/**
 * 应用主题：默认使用与 logo 一致的黑白单色方案，可选马卡龙柔和配色；
 * [dynamicColor] 可开启 Material You 动态取色（默认关闭，保证品牌一致）。
 */
@Composable
fun PomoStudyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.MACARON -> false
    }

    val colorScheme = when {
        themeMode == ThemeMode.MACARON -> MacaronColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
