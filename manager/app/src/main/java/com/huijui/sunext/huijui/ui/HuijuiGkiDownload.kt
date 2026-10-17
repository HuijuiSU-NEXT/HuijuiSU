package com.huijui.sunext.huijui.ui

import android.content.Intent
import android.net.Uri
import android.system.Os
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.huijui.sunext.parseKmi

/**
 * HuiJuiSU NEXT GKI 内核下载引导页
 *
 * GKI 设备（内核 5.10+）无法 built-in，这里引导用户下载社区预编译 GKI 内核。
 */
@Composable
fun HuijuiGkiDownload(onDismiss: () -> Unit) {
    val appContext = LocalContext.current
    var currentKmi by remember { mutableStateOf("") }
    var kernelRelease by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        kernelRelease = Os.uname().release
        currentKmi = parseKmi(kernelRelease)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                HuijuiDialogHeader(
                    icon = Icons.TwoTone.Cloud,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "GKI 内核下载",
                    subtitle = "内置模式（built-in）专用",
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "当前设备信息",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "内核版本：$kernelRelease",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "KMI：${if (currentKmi.isBlank()) "未识别" else currentKmi}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "说明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "GKI 设备（内核 5.10+）官方不支持 built-in 模式。\n\n" +
                        "如果需要 built-in，可以下载社区预编译的 GKI 内核刷入。\n\n" +
                        "推荐项目：WildKernels/GKI_KernelSU_SUSFS\n" +
                        "该项目提供 android12-5.10 到 android16-6.12 的预编译内核，已集成 KernelSU + SUSFS。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://github.com/WildKernels/GKI_KernelSU_SUSFS/releases")
                            }
                            appContext.startActivity(intent)
                        },
                    ) {
                        Text("去下载")
                    }
                }
            }
        }
    }
}
