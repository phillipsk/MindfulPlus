package com.example.mindfulplus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF203F37)
val Cream = Color(0xFFF7F5ED)
val Lime = Color(0xFFDDE8AD)
val Muted = Color(0xFF69776C)
val Soft = Color(0xFFE8ECE2)

@Composable
fun MindfulTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, secondary = Forest,
            secondaryContainer = Lime, onSecondaryContainer = Forest, background = Cream, onBackground = Forest,
            surface = Cream, onSurface = Forest, surfaceVariant = Soft, onSurfaceVariant = Muted,
            outline = Color(0xFFCBD2C5)),
        typography = Typography(
            displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 42.sp, lineHeight = 47.sp),
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 40.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        ), content = content,
    )
}

/** Original vector artwork drawn by Compose; no downloaded image assets. */
@Composable
fun BotanicalArt(modifier: Modifier = Modifier, dark: Boolean = false) {
    Canvas(modifier.height(180.dp).fillMaxWidth()) {
        val center = Offset(size.width * .5f, size.height * .6f)
        drawCircle(if (dark) Color(0xFF365A4C) else Soft, size.height * .48f, center)
        drawCircle(if (dark) Lime else Color(0xFFE7DDAA), size.height * .12f, Offset(size.width * .7f, size.height * .24f))
        val leaf = Path().apply {
            moveTo(size.width * .5f, size.height * .95f)
            cubicTo(size.width * .12f, size.height * .63f, size.width * .4f, 0f, size.width * .55f, size.height * .14f)
            cubicTo(size.width * .81f, size.height * .32f, size.width * .7f, size.height * .7f, size.width * .5f, size.height * .95f)
        }
        drawPath(leaf, if (dark) Lime else Forest)
        drawLine(if (dark) Forest else Lime, Offset(size.width * .5f, size.height * .95f), Offset(size.width * .55f, size.height * .29f), 3.dp.toPx())
        drawLine(if (dark) Forest else Lime, Offset(size.width * .53f, size.height * .62f), Offset(size.width * .43f, size.height * .43f), 2.dp.toPx())
    }
}
