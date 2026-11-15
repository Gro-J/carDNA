package com.example.cardna.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarDnaColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF101418),
    surface = Color(0xFF1A1F24),
    onBackground = Color(0xFFE6EAEE),
    onSurface = Color(0xFFE6EAEE)
)

/** 차량 가로 화면에 맞춘 고정 다크 테마. 동적 컬러/라이트 테마는 쓰지 않는다. */
@Composable
fun CarDnaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CarDnaColorScheme,
        content = content
    )
}
