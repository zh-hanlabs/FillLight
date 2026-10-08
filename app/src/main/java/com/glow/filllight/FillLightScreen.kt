package com.glow.filllight

import android.app.Activity
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FillLightScreen(
    window: Window,
    startupAction: String?,
    onStartupActionConsumed: () -> Unit,
    isPiPMode: Boolean = false,
    onEnterPiP: (() -> Unit)? = null,
) {
    val currentVersion = "1.0.4"
    var mode by rememberSaveable { mutableStateOf(LightMode.STEADY) }
    var brightness by rememberSaveable { mutableFloatStateOf(1f) }
    var useKelvin by rememberSaveable { mutableStateOf(true) }
    var kelvin by rememberSaveable { mutableIntStateOf(6500) }
    var customColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(Color(0xFFFFB7C5)) }
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

    // 全屏手势快速调节的 HUD 提示
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudVisible by remember { mutableStateOf(false) }
    var lastDragEndTime by remember { mutableLongStateOf(0L) }

    // 检查更新状态与弹窗
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
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

    // 静态颜色过渡动画（仅在非 FLOW 模式下驱动）
    val staticLightColor by animateColorAsState(
        targetValue = if (useKelvin) kelvinToColor(kelvin) else customColor,
        animationSpec = if (picking) snap() else tween(220),
        label = "lightColor",
    )

    // 拟真光效渐变画刷缓存，避免重组反复分配
    val realisticCenterGlow = remember {
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.20f),
            0.5f to Color.Transparent,
        )
    }
    val realisticEdgeFalloff = remember {
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.22f),
        )
    }

    BackHandler(enabled = panelVisible && !isPiPMode) { panelVisible = false }

    // 锁定提示自动消失
    LaunchedEffect(lockToast) {
        if (lockToast != null) {
            delay(2000)
            lockToast = null
        }
    }

    // 手势结束后延迟淡出 HUD
    LaunchedEffect(lastDragEndTime) {
        if (lastDragEndTime > 0L) {
            delay(900)
            hudVisible = false
        }
    }

    fun triggerCheckUpdate() {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            val result = UpdateChecker.checkUpdate(currentVersion)
            isCheckingUpdate = false
            result.onSuccess { info ->
                if (info.isNewVersion) {
                    updateInfo = info
                } else {
                    Toast.makeText(context, "当前已是最新版本 (v$currentVersion)", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "检查更新失败，请检查网络", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 全屏手势快速微调（纯灯面状态）
            .pointerInput(locked, panelVisible, isPiPMode) {
                if (!panelVisible && !locked && !isPiPMode) {
                    detectDragGestures(
                        onDragStart = {
                            hudVisible = true
                        },
                        onDragEnd = {
                            lastDragEndTime = System.currentTimeMillis()
                        },
                        onDragCancel = {
                            lastDragEndTime = System.currentTimeMillis()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        if (abs(dragAmount.y) > abs(dragAmount.x)) {
                            // 垂直滑动：调节亮度
                            val delta = -dragAmount.y / size.height
                            brightness = (brightness + delta).coerceIn(0.05f, 1.0f)
                            hudText = "☀️ ${(brightness * 100).roundToInt()}%"
                        } else if (useKelvin && mode != LightMode.FLOW) {
                            // 水平滑动：调节色温
                            val deltaK = (dragAmount.x / size.width) * 7500f
                            kelvin = (kelvin + deltaK.roundToInt()).coerceIn(1500, 9000)
                            hudText = "🌡️ $kelvin K"
                        }
                    }
                }
            }
            .pointerInput(locked, panelVisible, isPiPMode) {
                if (!isPiPMode) {
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
            }
    ) {
        // 灯面渲染：推迟到 drawBehind 与 graphicsLayer 读取高频状态，彻底杜绝重组发热
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (if (mode == LightMode.BREATHING) breathAlpha else if (lightOn) 1f else 0f) * timerAlpha
                }
                .drawBehind {
                    val c = if (mode == LightMode.FLOW) Color.hsv(flowHue, 0.85f, 1f) else staticLightColor
                    drawRect(c)
                    if (realistic) {
                        drawRect(realisticCenterGlow)
                        drawRect(realisticEdgeFalloff)
                    }
                }
        )

        // 仅在非 PiP 模式下渲染浮动提示与面板
        if (!isPiPMode) {
            // 全屏手势 HUD 读数胶囊
            LightHud(
                text = hudText,
                visible = hudVisible,
                modifier = Modifier.align(Alignment.Center),
            )

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

            // 面板收起后的短暂引导提示
            AnimatedVisibility(
                visible = !panelVisible && !locked && !hudVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(600)),
            ) {
                Text(
                    "轻触屏幕打开控制面板 · 上下滑调节亮度",
                    color = Color.Black.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                )
            }

            // 控制面板
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
                    customColor = customColor,
                    onCustomColor = { customColor = it },
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
                    onEnterPiP = onEnterPiP,
                    onLock = {
                        locked = true
                        panelVisible = false
                        lockToast = "已锁定 · 双击解锁"
                    },
                    onCheckUpdate = { triggerCheckUpdate() },
                )
            }
        }
    }

    if (updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            currentVersion = currentVersion,
            onDismiss = { updateInfo = null },
        )
    }
}
