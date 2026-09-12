package com.samplocal.manager.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.theme.Accent
import com.samplocal.manager.ui.theme.BrandGreen
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.theme.WizardOnNeon
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private fun rgbToHsv(c: Color): FloatArray {
    val out = FloatArray(3)
    AndroidColor.RGBToHSV(
        (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt(), out
    )
    return out
}

private fun hexOf(c: Color): String {
    val a = c.toArgb()
    return "#%02X%02X%02X".format((a shr 16) and 0xFF, (a shr 8) and 0xFF, a and 0xFF)
}

private fun colorOfHex(raw: String): Color? {
    val h = raw.trim().removePrefix("#")
    if (h.length != 6 || h.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
    return try {
        Color(0xFF000000.toInt() or h.toInt(16))
    } catch (_: Exception) { null }
}

@Composable
fun ColorPickerDialog(
    T: Strings,
    initial: Color?,
    onConfirm: (Color?) -> Unit,
    onDismiss: () -> Unit
) {
    val start = initial ?: BrandGreen
    var hsv by remember { mutableStateOf(rgbToHsv(start)) }
    var hex by remember { mutableStateOf(hexOf(start)) }
    var hexError by remember { mutableStateOf(false) }
    val current = remember(hsv) { Color.hsv(hsv[0], hsv[1], hsv[2]) }

    fun pick(offset: Offset, center: Offset, radius: Float) {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > radius) return
        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (deg < 0) deg += 360f
        val sat = (dist / radius).coerceIn(0f, 1f)
        hsv = floatArrayOf(deg, sat, hsv[2])
        hex = hexOf(Color.hsv(deg, sat, hsv[2]))
        hexError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text(T.accentTitle, color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val hueStops = remember {
                    List(13) { i -> Color.hsv(i * 30f, 1f, 1f) }
                }
                Box(
                    Modifier.size(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WheelCanvas(hsv = hsv, hueStops = hueStops, onPick = ::pick)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("V", color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = hsv[2],
                        onValueChange = {
                            hsv = floatArrayOf(hsv[0], hsv[1], it)
                            hex = hexOf(Color.hsv(hsv[0], hsv[1], it))
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Accent, activeTrackColor = Accent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(current)
                            .border(1.dp, TextDim, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { v ->
                            hex = v.uppercase()
                            val c = colorOfHex(v)
                            if (c == null) {
                                hexError = true
                            } else {
                                hexError = false
                                hsv = rgbToHsv(c)
                            }
                        },
                        label = { Text(T.accentHex) },
                        singleLine = true,
                        isError = hexError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (!hexError) onConfirm(current) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent, contentColor = WizardOnNeon
                ),
                shape = RoundedCornerShape(50)
            ) { Text(T.accentUse, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) { Text(T.accentReset) }
                TextButton(onClick = onDismiss) { Text(T.cancel) }
            }
        }
    )
}

@Composable
private fun WheelCanvas(
    hsv: FloatArray,
    hueStops: List<Color>,
    onPick: (Offset, Offset, Float) -> Unit
) {
    var centerPx by remember { mutableStateOf(Offset.Zero) }
    var radiusPx by remember { mutableStateOf(1f) }
    Canvas(
        Modifier
            .size(210.dp)
            .pointerInput(Unit) {
                detectTapGestures { off -> onPick(off, centerPx, radiusPx) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onPick(change.position, centerPx, radiusPx)
                }
            }
    ) {
        val r = size.minDimension / 2f
        centerPx = center
        radiusPx = r

        drawArc(
            brush = Brush.sweepGradient(hueStops),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = r * 0.52f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.White,
                0.45f to Color.White.copy(alpha = 0.55f),
                1f to Color.Transparent,
                center = center,
                radius = r
            )
        )

        val ang = Math.toRadians(hsv[0].toDouble())
        val rr = hsv[1] * r
        val knob = Offset(
            center.x + (cos(ang) * rr).toFloat(),
            center.y + (sin(ang) * rr).toFloat()
        )
        drawCircle(Color.White, radius = 9f, center = knob)
        drawCircle(Color.Black.copy(alpha = 0.6f), radius = 9f, center = knob, style = Stroke(width = 3f))
    }
}
