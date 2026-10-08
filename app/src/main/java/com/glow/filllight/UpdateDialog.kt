package com.glow.filllight

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 检查更新弹窗：显示最新版本号、更新日志及下载跳转 */
@Composable
internal fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    currentVersion: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(24.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "🎉 发现新版本 ${updateInfo.tagName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PanelText,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "当前版本: v$currentVersion",
                    fontSize = 12.sp,
                    color = PanelDimText,
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "更新说明：",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PanelText,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = updateInfo.changelog,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = PanelDimText,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetUrl = updateInfo.downloadUrl ?: updateInfo.releaseUrl
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("立即下载", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说", color = PanelDimText)
            }
        },
        containerColor = Color(0xFF1E1E24),
        shape = shape,
    )
}
