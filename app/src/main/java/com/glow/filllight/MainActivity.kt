package com.glow.filllight

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    /** 桌面快捷方式 / 外部 action 携带的启动指令，由界面消费后清空 */
    private var startupAction by mutableStateOf<String?>(null)

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
        startupAction = intent?.action?.takeIf { it.startsWith(ACTION_PREFIX) }
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
                FillLightScreen(
                    window = window,
                    startupAction = startupAction,
                    onStartupActionConsumed = { startupAction = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        startupAction = intent.action?.takeIf { it.startsWith(ACTION_PREFIX) }
    }

    companion object {
        const val ACTION_PREFIX = "com.glow.filllight.ACTION_"
        const val ACTION_SOS = "com.glow.filllight.ACTION_SOS"
        const val ACTION_NIGHT = "com.glow.filllight.ACTION_NIGHT"
    }
}
