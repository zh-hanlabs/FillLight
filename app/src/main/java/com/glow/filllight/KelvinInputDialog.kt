package com.glow.filllight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 色温手动输入：范围外自动钳制到 1500–9000K */
@Composable
internal fun KelvinInputDialog(
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
                Spacer(Modifier.height(4.dp))
                Text(
                    "提示：若显色偏暖，请检查系统是否开启了「护眼模式」",
                    color = PanelDimText.copy(alpha = 0.7f),
                    fontSize = 11.sp,
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
