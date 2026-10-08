package com.glow.filllight

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightModelTest {

    @Test
    fun testKelvinToColor_rangeAndCharacteristics() {
        // 1850K 烛光：高红、中绿、低蓝（暖色）
        val candle = kelvinToColor(1850)
        assertTrue(candle.red > candle.green)
        assertTrue(candle.green > candle.blue)
        assertTrue(candle.red > 0.9f)
        assertTrue(candle.blue < 0.2f)

        // 6500K 冷白：近似日光平衡，红、绿、蓝均较高
        val daylight = kelvinToColor(6500)
        assertTrue(daylight.red > 0.8f)
        assertTrue(daylight.green > 0.8f)
        assertTrue(daylight.blue > 0.8f)

        // 9000K 高色温：蓝光通道接近饱和
        val highKelvin = kelvinToColor(9000)
        assertEquals(1.0f, highKelvin.blue, 0.01f)
    }

    @Test
    fun testKelvinToColor_extremeBoundaries() {
        // 小于 1000K 或大于 40000K 不应崩溃或产生 NaN，且各通道在 [0, 1]
        val low = kelvinToColor(500)
        val high = kelvinToColor(50000)

        assertTrue(low.red in 0f..1f)
        assertTrue(low.green in 0f..1f)
        assertTrue(low.blue in 0f..1f)

        assertTrue(high.red in 0f..1f)
        assertTrue(high.green in 0f..1f)
        assertTrue(high.blue in 0f..1f)
    }

    @Test
    fun testSosPattern_structureAndTimings() {
        // S (3 dots + 2 gaps + 1 letter gap) = 6
        // O (3 dashes + 2 gaps + 1 letter gap) = 6
        // S (3 dots + 2 gaps + 1 long gap) = 6
        // Total steps = 18
        assertEquals(18, sosPattern.size)

        // 第一步是短亮 (dot = 220ms)
        assertEquals(true to 220, sosPattern[0])
        // 第二步是间隔 (gap = 220ms)
        assertEquals(false to 220, sosPattern[1])
        // 最后一步是长间隔 (1800ms)
        assertEquals(false to 1800, sosPattern.last())
    }

    @Test
    fun testFormatRemaining() {
        assertEquals("0:00", formatRemaining(0L))
        assertEquals("0:00", formatRemaining(-500L))
        assertEquals("0:01", formatRemaining(500L))
        assertEquals("0:15", formatRemaining(15_000L))
        assertEquals("1:00", formatRemaining(60_000L))
        assertEquals("15:00", formatRemaining(15 * 60_000L))
        assertEquals("30:00", formatRemaining(30 * 60_000L))
        assertEquals("60:00", formatRemaining(60 * 60_000L))
    }
}
