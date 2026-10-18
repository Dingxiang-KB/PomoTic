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

/**
 * 应用主题：默认使用与 logo 一致的黑白单色方案；
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
    }

    val colorScheme = when {
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
