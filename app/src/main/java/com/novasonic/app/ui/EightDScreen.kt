package com.novasonic.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novasonic.app.EightD
import com.novasonic.app.EightDState
import com.novasonic.app.MusicViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EightDScreen(state: EightDState, vm: MusicViewModel, onClose: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween((state.periodSeconds * 1000f).toInt(), easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "phase"
    )
    val twoPi = (2.0 * PI).toFloat()

    val subtitle = when {
        !state.enabled -> "Turn it on to hear your music all around you"
        state.mode == EightD.MODE_ORBIT -> "Your music circles around your head"
        state.mode == EightD.MODE_SWEEP -> "Your music sweeps across the stage in front of you"
        else -> "A wider, roomier sound stage"
    }
    val selected = if (!state.enabled) -1 else state.mode

    Column(
        Modifier.fillMaxSize().background(Nova.Bg).systemBarsPadding()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClose)
            Spacer(Modifier.width(12.dp))
            Text("8D Audio", color = Nova.TextHi, fontSize = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Switch(
                checked = state.enabled,
                onCheckedChange = { vm.setEightDEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Nova.Accent,
                    checkedBorderColor = Nova.Accent,
                    uncheckedThumbColor = Nova.TextLo,
                    uncheckedTrackColor = Nova.Surface,
                    uncheckedBorderColor = Nova.Border
                )
            )
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = center
                    val radius = size.minDimension / 2 - 16.dp.toPx()
                    val ring = if (state.enabled) Nova.Accent.copy(alpha = 0.35f) else Nova.Border
                    drawCircle(ring, radius, c, style = Stroke(1.5.dp.toPx()))
                    drawCircle(ring.copy(alpha = ring.alpha * 0.6f), radius * 0.68f, c, style = Stroke(1.dp.toPx()))

                    fun dot(angle: Float) {
                        val p = Offset(c.x + radius * sin(angle), c.y - radius * cos(angle))
                        drawCircle(Nova.Accent.copy(alpha = 0.25f), 15.dp.toPx(), p)
                        drawCircle(Nova.Accent, 7.dp.toPx(), p)
                    }
                    if (state.enabled) {
                        when (state.mode) {
                            EightD.MODE_ORBIT -> dot(twoPi * phase)
                            EightD.MODE_SWEEP -> dot(1.3f * sin(twoPi * phase))
                            else -> {
                                dot(-1.05f)
                                dot(1.05f)
                            }
                        }
                    }
                }
                Icon(
                    Icons.Rounded.Headphones, null,
                    tint = if (state.enabled) Nova.Accent else Nova.TextLo,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Surround Yourself", color = Nova.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Nova.TextLo, fontSize = 14.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Off" to -1, "8D" to EightD.MODE_ORBIT, "3D" to EightD.MODE_SWEEP, "Spatial" to EightD.MODE_SPATIAL)
                    .forEach { (label, m) ->
                        Chip(label, selected == m) {
                            if (m == -1) vm.setEightDEnabled(false) else vm.setEightDMode(m)
                        }
                    }
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Intensity", color = Nova.TextHi, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("${(state.intensity * 100).roundToInt()}%", color = Nova.Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Slider(
                value = state.intensity,
                onValueChange = { vm.setEightDIntensity(it) },
                colors = SliderDefaults.colors(
                    thumbColor = Nova.Accent,
                    activeTrackColor = Nova.Accent,
                    inactiveTrackColor = Nova.Border
                )
            )

            if (state.mode != EightD.MODE_SPATIAL) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed", color = Nova.TextHi, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            state.speed < 0.34f -> "Slow"
                            state.speed < 0.67f -> "Medium"
                            else -> "Fast"
                        },
                        color = Nova.Accent, fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                }
                Slider(
                    value = state.speed,
                    onValueChange = { vm.setEightDSpeed(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = Nova.Accent,
                        activeTrackColor = Nova.Accent,
                        inactiveTrackColor = Nova.Border
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Best experienced with headphones", color = Nova.TextLo, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
    }
}
