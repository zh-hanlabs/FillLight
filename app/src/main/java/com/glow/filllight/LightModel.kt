package com.glow.filllight

import androidx.compose.ui.graphics.Color
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

enum class LightMode(val label: String) {
    STEADY("常亮"),
    STROBE("频闪"),
    BREATHING("呼吸"),
    FLOW("流彩"),
    SOS("SOS"),
}

data class KelvinPreset(val label: String, val kelvin: Int)

val kelvinPresets = listOf(
    KelvinPreset("烛光", 1850),
    KelvinPreset("暖白", 3200),
    KelvinPreset("自然白", 4500),
    KelvinPreset("冷白", 6500),
    KelvinPreset("正午", 9000),
)

data class AmbientPreset(val label: String, val color: Color)

val ambientPresets = listOf(
    AmbientPreset("樱花粉", Color(0xFFFFB7C5)),
    AmbientPreset("日落橙", Color(0xFFFF8A5C)),
    AmbientPreset("蜜桃", Color(0xFFFFCBA4)),
    AmbientPreset("薄荷绿", Color(0xFF9FE7C0)),
    AmbientPreset("海盐蓝", Color(0xFF8EC9F0)),
    AmbientPreset("薰衣草", Color(0xFFC5B3F0)),
    AmbientPreset("青柠", Color(0xFFD9F17E)),
    AmbientPreset("玫瑰金", Color(0xFFF4C2C2)),
)

/**
 * 色温(K) -> RGB，Tanner Helland 算法，适用 1500K~9000K。
 */
fun kelvinToColor(kelvin: Int): Color {
    val t = kelvin.coerceIn(1000, 40000) / 100.0
    val r = if (t <= 66) 255.0 else 329.698727446 * (t - 60).pow(-0.1332047592)
    val g = if (t <= 66) 99.4708025861 * ln(t) - 161.1195681661
    else 288.1221695283 * (t - 60).pow(-0.0755148492)
    val b = when {
        t >= 66 -> 255.0
        t <= 19 -> 0.0
        else -> 138.5177312231 * ln(t - 10) - 305.0447927307
    }
    fun ch(v: Double) = v.coerceIn(0.0, 255.0).roundToInt()
    return Color(ch(r), ch(g), ch(b))
}

/** SOS 摩尔斯灯光节奏：单位约 220ms，(亮?, 持续ms) */
val sosPattern: List<Pair<Boolean, Int>> = buildList {
    val dot = 220
    val dash = 660
    val symbolGap = 220
    val letterGap = 660
    fun on(ms: Int) = add(true to ms)
    fun off(ms: Int) = add(false to ms)
    repeat(2) { on(dot); off(symbolGap) }; on(dot); off(letterGap)      // S
    repeat(2) { on(dash); off(symbolGap) }; on(dash); off(letterGap)    // O
    repeat(2) { on(dot); off(symbolGap) }; on(dot); off(1800)           // S + 停顿
}

val ColorSaver: androidx.compose.runtime.saveable.Saver<Color, Long> = androidx.compose.runtime.saveable.Saver(
    save = { it.value.toLong() },
    restore = { Color(it.toULong()) }
)

fun formatRemaining(ms: Long): String {
    val total = ((ms + 999) / 1000).toInt().coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%d:%02d", total / 60, total % 60)
}

