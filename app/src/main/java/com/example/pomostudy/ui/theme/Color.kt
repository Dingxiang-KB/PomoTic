package com.example.pomostudy.ui.theme

import androidx.compose.ui.graphics.Color

// 品牌色（浅色 / 深色）
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// 优先级颜色：高=红、中=黄、低=绿
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
