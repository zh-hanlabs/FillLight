package com.glow.filllight

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

internal val PanelText = Color(0xFFECECF4)
internal val PanelDimText = Color(0xFF8E8E9C)
internal val PanelBorder = Color.White.copy(alpha = 0.08f)
internal val PillDarkText = Color(0xFF121216)

/** 等宽数字：数值跳动时不抖动 */
internal val NumericStyle = TextStyle(fontFeatureSettings = "tnum")

internal val PanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

internal val kelvinBrush = Brush.horizontalGradient((1500..9000 step 500).map { kelvinToColor(it) })

/** 玻璃动效的弹簧参数：轻微过冲，有弹性 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 380f)

/** 面板景深焦点：非空表示某控件正被按住，其余区域虚化压暗 */
internal class PanelFocusState {
    var id by mutableStateOf<String?>(null)
}

internal val LocalPanelFocus = compositionLocalOf { PanelFocusState() }

/** 景深：某个控件被按住时，其余区块轻微虚化并压暗 */
@Composable
internal fun Modifier.focusBlur(id: String): Modifier {
    val focus = LocalPanelFocus.current
    val active = focus.id != null && focus.id != id
    val radius by animateFloatAsState(if (active) 4f else 0f, tween(220), label = "blur$id")
    val dim by animateFloatAsState(if (active) 0.65f else 1f, tween(220), label = "dim$id")
    return this
        .graphicsLayer { alpha = dim }
        .then(
            if (radius > 0.05f) {
                Modifier.blur(radius.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            }
        )
}
