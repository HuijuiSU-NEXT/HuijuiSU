package com.huijui.nova.miuix

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.ui.Text
import com.huijui.nova.webui.WebUi

/**
 * 模块 WebUI 查看器。
 *
 * 模块的 webroot 先被复制到应用缓存，再用 WebView 以 file:// 打开 ——
 * 这样既不用起本地服务器，也不受 /data/adb 的沙箱限制。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiuixWebUiScreen(
    moduleId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    var indexPath by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(moduleId) {
        val index = WebUi.prepare(context, moduleId)
        if (index == null) {
            error = "这个模块没有可用的 WebUI（缺少 webroot/index.html），或读取失败"
        } else {
            indexPath = index.absolutePath
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.18f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                fontSize = 26.sp,
                color = ink,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(moduleId, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1)
                Text("模块 WebUI", fontSize = 10.sp, color = dim)
            }
            Text(
                S("刷新"),
                fontSize = 12.sp,
                color = ink,
                modifier = Modifier.clickable {
                    val p = indexPath
                    if (p != null) {
                        // 重新加载由 WebView 自己处理
                        indexPath = null
                    }
                }.padding(6.dp),
            )
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在准备 WebUI…", fontSize = 12.sp, color = dim)
            }
            error.isNotBlank() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error, fontSize = 12.sp, color = dim)
            }
            else -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        indexPath?.let { loadUrl("file://$it") }
                    }
                },
                update = { view ->
                    indexPath?.let { path ->
                        val url = "file://$path"
                        if (view.url != url) view.loadUrl(url)
                    }
                },
            )
        }
    }
}
