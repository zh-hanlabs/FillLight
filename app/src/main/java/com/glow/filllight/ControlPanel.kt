package com.glow.filllight

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ControlPanel(
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
    onEnterPiP: (() -> Unit)? = null,
    onLock: (() -> Unit)? = null,
    onCheckUpdate: (() -> Unit)? = null,
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
            // 顶部手柄与快捷操作
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                )
                Row(
                    Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onEnterPiP != null) {
                        Pill(
                            label = "画中画",
                            selected = false,
                            onClick = onEnterPiP,
                            compact = true,
                        )
                    }
                    if (onLock != null) {
                        Pill(
                            label = "锁定",
                            selected = false,
                            onClick = onLock,
                            compact = true,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

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

            // 选项栏：光色来源（色温 / 彩色）与 灯面风格（纯色 / 拟真）同排在同一行，大幅提升下边界留白
            Box(Modifier.fillMaxWidth().focusBlur("sources")) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (mode != LightMode.FLOW) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Spacer(Modifier.height(16.dp))

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
                                .size(140.dp)
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

            Spacer(Modifier.height(14.dp))
            Text(
                text = "FillLight v1.0.4 · 检查更新",
                color = PanelDimText.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .clickable { onCheckUpdate?.invoke() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
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
