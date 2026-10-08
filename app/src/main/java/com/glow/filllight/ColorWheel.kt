package com.glow.filllight

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** HSV 取色轮：中心为白，边缘为全饱和色相，点按/拖动取色。 */
@Composable
fun ColorWheel(
    current: Color,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
    onPicking: (Boolean) -> Unit = {},
    onFocus: ((Boolean) -> Unit)? = null,
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 1.045f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "wheelScale",
    )
    val hueColors = remember {
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    }

    fun pick(pos: Offset) {
        if (boxSize == IntSize.Zero) return
        val cx = boxSize.width / 2f
        val cy = boxSize.height / 2f
        val maxR = minOf(cx, cy)
        val dx = (pos.x - cx).toDouble()
        val dy = (pos.y - cy).toDouble()
        val dist = hypot(dx, dy).coerceAtMost(maxR.toDouble()).toFloat()
        var hue = (atan2(dy, dx) * 180.0 / PI).toFloat()
        if (hue < 0) hue += 360f
        onPick(Color.hsv(hue, dist / maxR, 1f))
    }

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(6.dp, CircleShape, clip = false)
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onFocus?.invoke(true)
                        pick(it)
                        if (tryAwaitRelease()) {
                            pressed = false
                            onFocus?.invoke(false)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        pressed = true
                        onPicking(true)
                        onFocus?.invoke(true)
                    },
                    onDragEnd = {
                        pressed = false
                        onPicking(false)
                        onFocus?.invoke(false)
                    },
                    onDragCancel = {
                        pressed = false
                        onPicking(false)
                        onFocus?.invoke(false)
                    },
                ) { change, _ ->
                    change.consume()
                    pick(change.position)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Brush.sweepGradient(hueColors, center = c), radius = r, center = c)
            drawCircle(
                Brush.radialGradient(listOf(Color.White, Color(0x00FFFFFF)), center = c, radius = r),
                radius = r,
                center = c,
            )
            drawCircle(
                Color.Black.copy(alpha = 0.15f),
                radius = r,
                center = c,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        if (boxSize != IntSize.Zero) {
            val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(current.toArgb(), it) }
            val r = (minOf(boxSize.width, boxSize.height) / 2f) * hsv[1]
            val rad = hsv[0].toDouble() * PI / 180.0
            val kx = boxSize.width / 2f + (r * cos(rad)).toFloat()
            val ky = boxSize.height / 2f + (r * sin(rad)).toFloat()
            Box(
                Modifier
                    .offset { IntOffset((kx - 13.dp.toPx()).roundToInt(), (ky - 13.dp.toPx()).roundToInt()) }
                    .size(26.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(current)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}
