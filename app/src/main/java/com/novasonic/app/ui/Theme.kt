package com.novasonic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object Nova {
    val Bg = Color(0xFF060A13)
    val Surface = Color(0xFF0F1624)
    val SurfaceHi = Color(0xFF172134)
    val Border = Color(0xFF1B2539)
    val Accent = Color(0xFF2F8CFF)
    val AccentSoft = Color(0x262F8CFF)
    val TextHi = Color(0xFFF1F5FF)
    val TextLo = Color(0xFF8592AB)
    val Danger = Color(0xFFFF5C7A)
}

@Composable
fun NovaSonicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Nova.Accent,
            background = Nova.Bg,
            surface = Nova.Surface,
            onSurface = Nova.TextHi,
            onBackground = Nova.TextHi
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides Nova.TextHi, content = content)
    }
}

/** Flat card look: dark surface with a hairline border. */
fun Modifier.panel(shape: Shape = RoundedCornerShape(16.dp), highlight: Boolean = false): Modifier = this
    .clip(shape)
    .background(if (highlight) Nova.AccentSoft else Nova.Surface)
    .border(1.dp, if (highlight) Nova.Accent.copy(alpha = 0.6f) else Nova.Border, shape)
