package com.glow.filllight

import android.app.Activity
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private val PanelText = Color(0xFFECECF4)
private val PanelDimText = Color(0xFF8E8E9C)
private val PanelBorder = Color.White.copy(alpha = 0.08f)
private val PillDarkText = Color(0xFF121216)

/** 等宽数字：数值跳动时不抖动 */
private val NumericStyle = TextStyle(fontFeatureSettings = "tnum")

private val PanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

private val kelvinBrush = Brush.horizontalGradient((1500..9000 step 500).map { kelvinToColor(it) })

/** 玻璃动效的弹簧参数：轻微过冲，有弹性 */
private val GlassSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 380f)

/** 面板景深焦点：非空表示某控件正被按住，其余区域虚化压暗 */
private class PanelFocusState {
    var id by mutableStateOf<String?>(null)
}

private val LocalPanelFocus = compositionLocalOf { PanelFocusState() }

private fun formatRemaining(ms: Long): String {
    val total = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

@Composable
fun FillLightScreen(
    window: Window,
    startupAction: String?,
    onStartupActionConsumed: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(LightMode.STEADY) }
    var brightness by rememberSaveable { mutableFloatStateOf(1f) }
    var useKelvin by rememberSaveable { mutableStateOf(true) }
    var kelvin by rememberSaveable { mutableIntStateOf(6500) }
    var customColor by rememberSaveable { mutableLongStateOf(0xFFFFB7C5) }
    var strobeHz by rememberSaveable { mutableFloatStateOf(2f) }
    var panelVisible by rememberSaveable { mutableStateOf(true) }
    // 灯面渲染风格：拟真=中心亮四角暗的光衰减，纯色=平涂满屏
    var realistic by rememberSaveable { mutableStateOf(true) }
    // 双击灯面锁定：锁定时单击不再呼出面板，防误触
    var locked by rememberSaveable { mutableStateOf(false) }
    var lockToast by remember { mutableStateOf<String?>(null) }
    // 定时关灯：0=未设定；timerEndAt 为结束时刻（epoch ms），到点渐隐后退出
    var timerMinutes by rememberSaveable { mutableIntStateOf(0) }
    var timerEndAt by rememberSaveable { mutableLongStateOf(0L) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var timerAlpha by remember { mutableFloatStateOf(1f) }
    // 手指正按在取色轮/色温条上：灯光即时跟随，不做过渡动画
    var picking by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    // 流彩模式的色相循环
    val flowHue by rememberInfiniteTransition(label = "flow").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flowHue",
    )
    // 呼吸灯的明暗起伏
    val breathAlpha by rememberInfiniteTransition(label = "breathing").animateFloat(
        initialValue = 0.10f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathAlpha",
    )

    val baseColor = when {
        mode == LightMode.FLOW -> Color.hsv(flowHue, 0.85f, 1f)
        useKelvin -> kelvinToColor(kelvin)
        else -> Color(customColor)
    }

    // 沉浸式全屏：隐藏状态栏和导航栏，从屏幕边缘轻滑可临时唤出
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }

    // 窗口亮度跟随滑条（App 内覆盖，不影响系统设置）
    LaunchedEffect(brightness) {
        window.attributes = window.attributes.also { it.screenBrightness = brightness }
    }

    // 频闪 / SOS 的亮灭节奏
    var lightOn by remember { mutableStateOf(true) }
    LaunchedEffect(mode, strobeHz) {
        when (mode) {
            LightMode.STEADY, LightMode.BREATHING, LightMode.FLOW -> lightOn = true
            LightMode.STROBE -> {
                var on = true
                while (true) {
                    lightOn = on
                    delay((500f / strobeHz).roundToInt().toLong().coerceAtLeast(10L))
                    on = !on
                }
            }
            LightMode.SOS -> {
                while (true) {
                    for ((on, ms) in sosPattern) {
                        lightOn = on
                        delay(ms.toLong())
                    }
                }
            }
        }
    }

    // 定时关灯倒计时：每 0.5s 刷新剩余，到点渐隐 2.5s 后退出应用
    LaunchedEffect(timerEndAt) {
        if (timerEndAt == 0L) {
            remainingMs = 0L
            timerAlpha = 1f
            return@LaunchedEffect
        }
        timerAlpha = 1f
        while (true) {
            val remain = timerEndAt - System.currentTimeMillis()
            if (remain <= 0L) break
            remainingMs = remain
            delay(500)
        }
        remainingMs = 0L
        val fade = Animatable(1f)
        fade.animateTo(0f, tween(2500))
        (context as? Activity)?.finish()
    }
    val remainingText = if (timerEndAt != 0L) formatRemaining(remainingMs) else "--"

    // 桌面快捷方式直达
    LaunchedEffect(startupAction) {
        when (startupAction) {
            MainActivity.ACTION_SOS -> {
                mode = LightMode.SOS
                panelVisible = false
            }
            MainActivity.ACTION_NIGHT -> {
                mode = LightMode.STEADY
                useKelvin = true
                kelvin = 1850
                brightness = 0.35f
                panelVisible = false
            }
        }
        if (startupAction != null) onStartupActionConsumed()
    }

    val lightAlpha =
        (if (mode == LightMode.BREATHING) breathAlpha else if (lightOn) 1f else 0f) * timerAlpha

    val lightColor by animateColorAsState(
        targetValue = baseColor,
        animationSpec = if (picking || mode == LightMode.FLOW) snap() else tween(220),
        label = "lightColor",
    )

    BackHandler(enabled = panelVisible) { panelVisible = false }

    // 锁定提示自动消失
    LaunchedEffect(lockToast) {
        if (lockToast != null) {
            delay(2000)
            lockToast = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (locked) {
                            lockToast = "已锁定 · 双击解锁"
                        } else {
                            panelVisible = !panelVisible
                        }
                    },
                    onDoubleTap = {
                        locked = !locked
                        lockToast = if (locked) "已锁定 · 双击解锁" else "已解锁"
                        if (locked) panelVisible = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                )
            }
    ) {
        // 灯面：拟真模式下中心亮、四角稍暗，模拟真实灯面的光衰减
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = lightAlpha }
                .background(lightColor)
        ) {
            if (realistic) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                0f to Color.White.copy(alpha = 0.20f),
                                0.5f to Color.Transparent,
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                0.55f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.22f),
                            )
                        )
                )
            }
        }

        // 锁定/解锁提示
        AnimatedVisibility(
            visible = lockToast != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
        ) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    lockToast.orEmpty(),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                )
            }
        }

        // 面板收起后的短暂提示
        AnimatedVisibility(
            visible = !panelVisible && !locked,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(600)),
        ) {
            Text(
                "轻触屏幕打开控制面板",
                color = Color.Black.copy(alpha = 0.4f),
                fontSize = 13.sp,
            )
        }

        AnimatedVisibility(
            visible = panelVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(200)),
            exit = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            ControlPanel(
                mode = mode,
                onMode = { mode = it },
                brightness = brightness,
                onBrightness = { brightness = it },
                useKelvin = useKelvin,
                onUseKelvin = { useKelvin = it },
                kelvin = kelvin,
                onKelvin = { kelvin = it },
                customColor = Color(customColor),
                onCustomColor = { customColor = it.toArgb().toLong() and 0xFFFFFFFFL },
                strobeHz = strobeHz,
                onStrobeHz = { strobeHz = it },
                realistic = realistic,
                onRealistic = { realistic = it },
                timerMinutes = timerMinutes,
                remainingText = remainingText,
                onTimer = { minutes ->
                    timerMinutes = minutes
                    timerEndAt = if (minutes == 0) 0L else System.currentTimeMillis() + minutes * 60_000L
                    if (minutes != 0) remainingMs = minutes * 60_000L
                },
                onPicking = { picking = it },
            )
        }
    }
}

@Composable
private fun ControlPanel(
    mode: LightMode,
    onMode: (LightMode) -> Unit,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    useKelvin: Boolean,
    onUseKelvin: (Boolean) -> Unit,
    kelvin: Int,
    onKelvin: (Int) -> Unit,
    customColor: Color,
    onCustomColor: (Color) -> Unit,
    strobeHz: Float,
    onStrobeHz: (Float) -> Unit,
    realistic: Boolean,
    onRealistic: (Boolean) -> Unit,
    timerMinutes: Int,
    remainingText: String,
    onTimer: (Int) -> Unit,
    onPicking: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val toastContext = LocalContext.current
    val focusState = remember { PanelFocusState() }
    var showKelvinEditor by rememberSaveable { mutableStateOf(false) }
    CompositionLocalProvider(LocalPanelFocus provides focusState) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(PanelShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xF3141418), Color(0xF70A0A0E)))
                )
                .border(1.dp, PanelBorder, PanelShape)
                .verticalScroll(rememberScrollState())
                // 吞掉面板区域的点按，避免误触收起
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 18.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
            )
            Spacer(Modifier.height(16.dp))

            // 模式切换：白色滑块在胶囊轨道里滑动
            Box(Modifier.fillMaxWidth().focusBlur("mode")) {
                ModePills(
                    modes = LightMode.entries,
                    selected = mode,
                    onSelect = onMode,
                )
            }
            Spacer(Modifier.height(14.dp))
            SectionDivider()
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth().focusBlur("brightness")) {
                SettingRow(label = "亮度", valueText = "${(brightness * 100).roundToInt()}%") {
                    CapsuleSlider(
                        value = (brightness - 0.05f) / 0.95f,
                        onValueChange = { onBrightness(0.05f + it * 0.95f) },
                        focusId = "brightness",
                        showHandle = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (mode == LightMode.STROBE) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().focusBlur("strobe")) {
                    SettingRow(
                        label = "频率",
                        valueText = "${String.format(Locale.US, "%.1f", strobeHz)} Hz",
                    ) {
                        CapsuleSlider(
                            value = (strobeHz - 0.5f) / 9.5f,
                            onValueChange = { onStrobeHz(0.5f + it * 9.5f) },
                            focusId = "strobe",
                            showHandle = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // 定时关灯
            Box(Modifier.fillMaxWidth().focusBlur("timer")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("定时", color = PanelDimText, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Row(
                        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pill("关", timerMinutes == 0, { onTimer(0) }, compact = true)
                        Pill("15 分", timerMinutes == 15, { onTimer(15) }, compact = true)
                        Pill("30 分", timerMinutes == 30, { onTimer(30) }, compact = true)
                        Pill("60 分", timerMinutes == 60, { onTimer(60) }, compact = true)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        remainingText,
                        style = NumericStyle,
                        color = if (timerMinutes > 0) PanelText else PanelDimText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionDivider()
            Spacer(Modifier.height(12.dp))

            if (mode != LightMode.FLOW) {
                Box(Modifier.fillMaxWidth().focusBlur("sources")) {
                    Column {
                        // 光色来源：色温 / 彩色
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pill(
                                label = "色温",
                                selected = useKelvin,
                                onClick = { onUseKelvin(true) },
                                swatch = {
                                    Box(
                                        Modifier
                                            .size(width = 14.dp, height = 8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFFFF9A3C),
                                                        Color(0xFFF4F4F6),
                                                        Color(0xFFBFD9FF),
                                                    )
                                                )
                                            )
                                    )
                                },
                            )
                            Pill(
                                label = "彩色",
                                selected = !useKelvin,
                                onClick = { onUseKelvin(false) },
                                swatch = {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.sweepGradient(
                                                    listOf(
                                                        Color.Red, Color.Yellow, Color.Green,
                                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                                                    )
                                                )
                                            )
                                    )
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 灯面风格：纯色 / 拟真（流彩模式下依然生效）
            Box(Modifier.fillMaxWidth().focusBlur("sources")) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill(
                            label = "纯色",
                            selected = !realistic,
                            onClick = { onRealistic(false) },
                            swatch = {
                                Box(
                                    Modifier
                                        .size(width = 14.dp, height = 8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8E8EC))
                                        .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                                )
                            },
                        )
                        Pill(
                            label = "拟真",
                            selected = realistic,
                            onClick = { onRealistic(true) },
                            swatch = {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(Color.White, Color(0xFF545460))
                                            )
                                        )
                                )
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            if (mode == LightMode.FLOW) {
                Text(
                    "色彩随时间流动，可切换灯面风格",
                    color = PanelDimText.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                )
            } else if (useKelvin) {
                Box(Modifier.fillMaxWidth().focusBlur("kelvin")) {
                    Column {
                        Row(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                                .pointerInput(Unit) { detectTapGestures { showKelvinEditor = true } }
                                .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                "$kelvin",
                                style = NumericStyle,
                                color = PanelText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                " K",
                                color = PanelDimText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 3.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        CapsuleSlider(
                            value = (kelvin - 1500f) / 7500f,
                            onValueChange = { onKelvin(1500 + (it * 7500).roundToInt()) },
                            onPicking = onPicking,
                            focusId = "kelvin",
                            pressScale = 1.04f,
                            fillBrush = null,
                            trackContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(kelvinBrush)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(36.dp),
                        ) {
                            items(kelvinPresets) { preset ->
                                Pill(
                                    label = preset.label,
                                    selected = useKelvin && kelvin == preset.kelvin,
                                    onClick = { onKelvin(preset.kelvin) },
                                    swatch = { ColorDot(kelvinToColor(preset.kelvin)) },
                                )
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().focusBlur("wheel")) {
                    Column {
                        ColorWheel(
                            current = customColor,
                            onPick = onCustomColor,
                            onPicking = onPicking,
                            onFocus = { f -> focusState.id = if (f) "wheel" else null },
                            modifier = Modifier
                                .size(200.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(8.dp))
                        val argb = customColor.toArgb()
                        val hexText = String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
                        Text(
                            hexText,
                            style = NumericStyle,
                            color = PanelDimText,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .clickable {
                                    clipboard.setText(AnnotatedString(hexText))
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    Toast.makeText(toastContext, "已复制 $hexText", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(36.dp),
                        ) {
                            items(ambientPresets) { preset ->
                                Pill(
                                    label = preset.label,
                                    selected = customColor == preset.color,
                                    onClick = { onCustomColor(preset.color) },
                                    swatch = { ColorDot(preset.color) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKelvinEditor) {
        KelvinInputDialog(
            current = kelvin,
            onConfirm = {
                onKelvin(it)
                showKelvinEditor = false
            },
            onDismiss = { showKelvinEditor = false },
        )
    }
}

/** 色温手动输入：范围外自动钳制到 1500–9000K */
@Composable
private fun KelvinInputDialog(
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current.toString()) }
    val submit = {
        val v = text.toIntOrNull()
        if (v != null) onConfirm(v.coerceIn(1500, 9000)) else onDismiss()
        Unit
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("输入色温") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    suffix = { Text("K", color = PanelDimText) },
                    singleLine = true,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "范围 1500 – 9000 K，超出将自动取边界值",
                    color = PanelDimText,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 模式选择：胶囊轨道 + 白色滑块平滑滑过 */
@Composable
private fun ModePills(
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

/** 胶囊滑条：粗圆轨道 + 可选手柄；按住时放大并通知景深焦点 */
@Composable
private fun CapsuleSlider(
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
    var widthPx by remember { mutableStateOf(0f) }
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

/** 统一的预设胶囊：选中白底黑字；按住时弹性放大 */
@Composable
private fun Pill(
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

/** 景深：某个控件被按住时，其余区块轻微虚化并压暗 */
@Composable
private fun Modifier.focusBlur(id: String): Modifier {
    val focus = LocalPanelFocus.current
    val active = focus.id != null && focus.id != id
    val radius by animateFloatAsState(if (active) 4f else 0f, tween(220), label = "blur$id")
    val dim by animateFloatAsState(if (active) 0.65f else 1f, tween(220), label = "dim$id")
    return this
        .graphicsLayer { alpha = dim }
        .blur(radius.dp)
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
    )
}

@Composable
private fun SectionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

@Composable
private fun SettingRow(
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
