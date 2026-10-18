package com.example.pomostudy.ui.theme

import androidx.compose.ui.graphics.Color

// 单色（黑白几何）品牌配色 —— 取自黑白几何时钟 logo
val MonoInk = Color(0xFF111318)        // 墨黑（主色）
val MonoInkSoft = Color(0xFF2A2A2F)
val MonoPaper = Color(0xFFFFFFFF)      // 纸白
val MonoPaperDim = Color(0xFFF3F3F5)
val MonoGrey = Color(0xFF4A4A4F)
val MonoGreyLight = Color(0xFFC8C8CC)
val MonoOutline = Color(0xFF9A9AA0)
val MonoSurfaceDark = Color(0xFF101012)

// 优先级颜色：高=红、中=黄、低=绿（功能性配色，保持可辨识）
val PriorityHighColor = Color(0xFFE53935)
val PriorityMediumColor = Color(0xFFF9A825)
val PriorityLowColor = Color(0xFF43A047)

/**
 * 标签可选颜色调色板（ARGB Long，便于存入数据库）。
 */
val TagColorPalette: List<Long> = listOf(
    0xFFEF5350, 0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2,
    0xFF5C6BC0, 0xFF42A5F5, 0xFF29B6F6, 0xFF26C6DA,
    0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFFFCA28,
    0xFFFFA726, 0xFFFF7043, 0xFF8D6E63, 0xFF78909C
)

/** 将数据库中的 ARGB Long 转为 Compose Color。 */
fun Long.toComposeColor(): Color = Color(this.toInt())
