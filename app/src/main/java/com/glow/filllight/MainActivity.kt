package com.glow.filllight

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 让灯面铺满摄像头挖孔区域
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.also {
                it.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val window: Window = window
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    surface = Color(0xFF121216),
                    onSurface = Color(0xFFEDEDF2),
                    secondaryContainer = Color(0xFF33333E),
                    onSecondaryContainer = Color(0xFFEDEDF2),
                )
            ) {
                FillLightScreen(window)
            }
        }
    }
}
