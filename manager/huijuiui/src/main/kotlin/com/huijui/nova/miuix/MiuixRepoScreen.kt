package com.huijui.nova.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.S
import com.huijui.nova.design.novaDim
import com.huijui.nova.design.novaInk
import com.huijui.nova.hide.HideBundle
import com.huijui.nova.repo.ModuleRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 在线模块仓库。
 *
 * 索引来自 HuijuiSU-NEXT/Huijui_Module_Repo。选中后下载 zip 到缓存，
 * 再交给 `ksud module install` 安装——和本地安装走同一条链路。
 */
@Composable
fun MiuixRepoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ink = novaInk()
    val dim = novaDim()
    val accent = Color(0xFF7C6BF0)

    var all by remember { mutableStateOf<List<ModuleRepo.Mod>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var installing by remember { mutableStateOf<String?>(null) }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val watchdog = launch {
            kotlinx.coroutines.delay(25_000)
            if (loading) {
                loading = false
                error = "网络超时：所有镜像都不可达（检查设备联网）"
            }
        }
        withContext(Dispatchers.IO) {
            runCatching { ModuleRepo.load() }
                .onSuccess { all = it; error = "" }
                .onFailure { error = "拉取仓库失败：${it.message}" }
        }
        watchdog.cancel()
        loading = false
    }

    val shown = remember(all, query) {
        if (query.isBlank()) all
        else all.filter {
            it.name.contains(query, true) || it.author.contains(query, true) ||
                it.description.contains(query, true)
        }
    }

    fun install(mod: ModuleRepo.Mod) {
        if (installing != null) return
        installing = mod.name
        log = listOf("$ 下载 ${mod.name}")
        MainScope().launch {
            val path = withContext(Dispatchers.IO) {
                runCatching { ModuleRepo.downloadZip(mod.zipUrl, context.cacheDir, mod.name) }.getOrNull()
            }
            if (path == null) {
                log = log + "下载失败"
                installing = null
                return@launch
            }
            log = log + "已下载，开始安装"
            HideBundle.run("ksud module install '$path'") { line -> log = (log + line).takeLast(40) }
            log = log + "安装结束，重启后生效"
            installing = null
        }
    }

    MiuixTheme(colors = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    fontSize = 26.sp,
                    color = ink,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(S("在线模块"), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        when {
                            loading -> "拉取索引中…"
                            error.isNotBlank() -> error
                            else -> "${all.size} 个模块 · ${ModuleRepo.lastMirror.ifBlank { "本地" }}"
                        },
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ink.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", fontSize = 12.sp, color = dim)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = ink),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text(S("搜索模块 / 作者"), fontSize = 12.sp, color = dim.copy(alpha = 0.6f))
                        inner()
                    },
                )
                if (query.isNotEmpty()) {
                    Text(
                        S("清除"),
                        fontSize = 10.sp,
                        color = dim,
                        modifier = Modifier.clickable { query = "" }.padding(4.dp),
                    )
                }
            }

            if (log.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp, max = 130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B2027))
                        .padding(10.dp),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        log.forEach {
                            Text(
                                it,
                                fontSize = 9.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (it.startsWith("$")) Color(0xFF7FD1C0) else Color(0xFFD6DEE8),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                shown.forEach { mod ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        corner = 16.dp,
                        padding = PaddingValues(14.dp),
                        onClick = { install(mod) },
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(mod.name.take(1).uppercase(), fontSize = 14.sp, color = accent, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mod.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink, maxLines = 1)
                                Text(
                                    "${mod.author} · ${mod.version} · ${mod.sizeKb} KB",
                                    fontSize = 9.sp,
                                    color = dim,
                                    maxLines = 1,
                                )
                                if (mod.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(mod.description, fontSize = 9.sp, color = dim, maxLines = 2, lineHeight = 13.sp)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    installing == mod.name -> "…"
                                    installing != null -> "等待"
                                    else -> "安装"
                                },
                                fontSize = 10.sp,
                                color = accent,
                            )
                        }
                    }
                }

                if (!loading && shown.isEmpty() && error.isBlank()) {
                    GlassCard(Modifier.fillMaxWidth(), corner = 16.dp, padding = PaddingValues(16.dp)) {
                        Text(S("没有匹配的模块"), fontSize = 11.sp, color = dim)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
