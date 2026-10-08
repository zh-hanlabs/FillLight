package com.glow.filllight

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** 胶囊滑条：粗圆轨道 + 可选手柄；按住时放大并通知景深焦点 */
@Composable
internal fun CapsuleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onPicking: ((Boolean) -> Unit)? = null,
    focusId: String? = null,
    pressScale: Float = 1.06f,
    showHandle: Boolean = true,
    fillBrush: Brush? = Brush.horizontalGradient(listOf(Color.White, Color.White)),
    trackContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    var widthPx by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }
    val focus = LocalPanelFocus.current
    val scale by animateFloatAsState(
        if (pressed) pressScale else 1f,
        GlassSpring,
        label = "csScale",
    )
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentPicking by rememberUpdatedState(onPicking)
    fun fractionAt(x: Float) = if (widthPx > 0f) (x / widthPx).coerceIn(0f, 1f) else 0f
    // 手柄半径 11dp：内缩 13dp 保证端点处手柄完全收进胶囊，不越界
    val inset = 13.dp
    Box(
        modifier
            .semantics {
                this.progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
            }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(focusId) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    if (focusId != null) focus.id = focusId
                    try {
                        while (awaitPointerEvent().changes.any { it.pressed }) {
                        }
                    } finally {
                        pressed = false
                        if (focusId != null && focus.id == focusId) focus.id = null
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { pos -> currentOnChange(fractionAt(pos.x)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        currentPicking?.invoke(true)
                        currentOnChange(fractionAt(pos.x))
                    },
                    onDragEnd = { currentPicking?.invoke(false) },
                    onDragCancel = { currentPicking?.invoke(false) },
                ) { change, dragAmount ->
                    change.consume()
                    if (widthPx > 0f) {
                        currentOnChange((currentValue + dragAmount / widthPx).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        // 底轨
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        // 整条自定义轨道（色温渐变）
        trackContent?.invoke(this)
        // 已填充部分
        if (fillBrush != null) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(fillBrush)
            )
        }
        // 手柄（无填充的渐变轨道需要它指示位置；白色填充轨道可省略）
        if (showHandle) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset(
                            (inset.toPx() + currentValue * (widthPx - 2 * inset.toPx()) - 11.dp.toPx())
                                .roundToInt(),
                            0,
                        )
                    }
                    .size(22.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color.Black.copy(alpha = 0.22f), CircleShape)
            )
        }
    }
}
