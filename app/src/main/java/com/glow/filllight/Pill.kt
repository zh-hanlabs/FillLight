package com.glow.filllight

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 统一的预设胶囊：选中白底黑字；按住时弹性放大 */
@Composable
internal fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    swatch: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 1.06f else 1f,
        GlassSpring,
        label = "pillScale",
    )
    val bg by animateColorAsState(
        if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.06f),
        tween(180),
        label = "pillBg",
    )
    val fg by animateColorAsState(
        if (selected) PillDarkText else PanelDimText,
        tween(180),
        label = "pillFg",
    )
    Row(
        modifier
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(if (compact) 28.dp else 32.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                1.dp,
                if (selected) Color.Transparent else Color.White.copy(alpha = 0.10f),
                CircleShape,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    try {
                        while (awaitPointerEvent().changes.any { it.pressed }) {
                        }
                    } finally {
                        pressed = false
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    onClick()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            .padding(
                start = if (compact) 9.dp else 11.dp,
                end = if (compact) 10.dp else 13.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        swatch?.invoke()
        Text(label, color = fg, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ColorDot(color: Color) {
    Box(
        Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
    )
}

@Composable
internal fun SectionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

@Composable
internal fun SettingRow(
    label: String,
    valueText: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PanelDimText, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Spacer(Modifier.width(12.dp))
        content()
        Spacer(Modifier.width(12.dp))
        Text(
            valueText,
            style = NumericStyle,
            color = PanelText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp),
        )
    }
}
