package com.huijui.nova.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huijui.nova.design.LocalHuiColors
import com.huijui.nova.modules.NovaModules
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme


/**
 * 模块列表页。
 *
 * 反馈里说要给「模块功能」一个入口——这里就是。列表走 `su` 读
 * `/data/adb/modules`，开关直接建/删 `disable` 文件，和官方管理器语义一致。
 */
@Composable
fun MiuixModulesScreen(
    onOpenRepo: () -> Unit = {},onBack: () -> Unit = {}, embedded: Boolean = false) {
    val accent = LocalHuiColors.current.accent
    val ink = com.huijui.nova.design.novaInk()
    val dim = com.huijui.nova.design.novaDim()
    var modules by remember { mutableStateOf<List<NovaModules.Module>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    suspend fun refresh() {
        loading = true
        val list = NovaModules.list()
        modules = list
        error = if (list.isEmpty()) "没读到模块（可能没有 Root，或者一个都没装）" else null
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            installing = false
            return@rememberLauncherForActivityResult
        }
        installing = true
        kotlinx.coroutines.MainScope().launch {
            val target = java.io.File(context.cacheDir, "module_${System.currentTimeMillis()}.zip")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
            }
            com.huijui.nova.hide.HideBundle.run("ksud module install '${target.absolutePath}'")
            installing = false
            refresh()
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
                if (!embedded) Text(
                    "‹",
                    fontSize = 26.sp,
                    color = ink,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("模块", fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text(
                        if (loading) "读取中…" else "${modules.size} 个模块",
                        fontSize = 11.sp,
                        color = dim,
                    )
                }
                Text(
                    "在线",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { onOpenRepo() }.padding(8.dp),
                )
                Text(
                    if (installing) "安装中…" else "安装",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier
                        .clickable { if (!installing) pickZip.launch(arrayOf("application/zip")) }
                        .padding(8.dp),
                )
                Text(
                    "刷新",
                    fontSize = 12.sp,
                    color = accent,
                    modifier = Modifier.clickable { kotlinx.coroutines.MainScope().launch { refresh() } },
                )
            }
            Spacer(Modifier.height(12.dp))

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                modules.forEach { module ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        corner = 18.dp,
                        padding = PaddingValues(14.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { expanded = if (expanded == module.id) null else module.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (module.enabled) accent else dim.copy(alpha = 0.4f)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    module.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink,
                                    maxLines = 1,
                                )
                                Text(
                                    "${module.version}${if (module.author.isNotBlank()) " · ${module.author}" else ""}",
                                    fontSize = 10.sp,
                                    color = dim,
                                    maxLines = 1,
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        (if (module.enabled) accent else dim).copy(alpha = 0.14f),
                                    )
                                    .clickable {
                                        kotlinx.coroutines.MainScope().launch {
                                            NovaModules.setEnabled(module.id, !module.enabled)
                                            refresh()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (module.enabled) "已启用" else "已停用",
                                    fontSize = 11.sp,
                                    color = if (module.enabled) accent else dim,
                                )
                            }
                        }
                    }
                    // 展开：描述 + 卸载
                    if (expanded == module.id) {
                        Spacer(Modifier.height(8.dp))
                        if (module.description.isNotBlank()) {
                            Text(
                                module.description,
                                fontSize = 10.sp,
                                color = dim,
                                lineHeight = 14.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(dim.copy(alpha = 0.12f))
                                    .clickable {
                                        kotlinx.coroutines.MainScope().launch {
                                            NovaModules.setEnabled(module.id, !module.enabled)
                                            refresh()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (module.enabled) "停用" else "启用",
                                    fontSize = 10.sp,
                                    color = dim,
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFD9534F).copy(alpha = 0.12f))
                                    .clickable {
                                        kotlinx.coroutines.MainScope().launch {
                                            NovaModules.uninstall(module.id)
                                            refresh()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("卸载（重启后生效）", fontSize = 10.sp, color = Color(0xFFD9534F))
                            }
                        }
                    }
                }

                if (error != null) {
                    GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                        Text(error!!, fontSize = 12.sp, color = dim)
                    }
                }
                Spacer(Modifier.height(90.dp))
            }
        }
    }
}
