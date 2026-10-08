package com.glow.filllight

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 模式选择：胶囊轨道 + 白色滑块平滑滑过 */
@Composable
internal fun ModePills(
    modes: List<LightMode>,
    selected: LightMode,
    onSelect: (LightMode) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
    ) {
        val segW = maxWidth / modes.size
        val pad = 4.dp
        val pos by animateFloatAsState(
            targetValue = modes.indexOf(selected).toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "modePos",
        )
        Box(
            Modifier
                .offset(x = pad + segW * pos)
                .width(segW - pad * 2)
                .fillMaxHeight()
                .padding(vertical = pad)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
        )
        Row(Modifier.fillMaxSize()) {
            modes.forEach { m ->
                val isSel = m == selected
                val textColor by animateColorAsState(
                    if (isSel) PillDarkText else PanelDimText,
                    tween(180),
                    label = "modeText",
                )
                var pressed by remember { mutableStateOf(false) }
                val segScale by animateFloatAsState(
                    if (pressed) 1.04f else 1f,
                    GlassSpring,
                    label = "segScale",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            this.role = Role.Tab
                            this.selected = isSel
                            this.contentDescription = "模式: ${m.label}"
                        }
                        .graphicsLayer { scaleX = segScale; scaleY = segScale }
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
                        .pointerInput(m) {
                            detectTapGestures {
                                onSelect(m)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        m.label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
