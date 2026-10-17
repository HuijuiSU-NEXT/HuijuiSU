package com.huijui.sunext.huijui.feature

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.huijui.sunext.data.packageinfo.InstalledPackageRepository
import com.huijui.sunext.domain.model.InstalledPackageInfo
import com.huijui.sunext.huijui.data.HuijuiAppManagerRepository
import com.huijui.sunext.huijui.ui.HuijuiDialogHeader
import org.koin.compose.koinInject

private enum class AppManagerAction(val label: String) {
    FREEZE("冻结"),
    UNFREEZE("解冻"),
    FORCE_STOP("强停"),
    CLEAR("清数据"),
    UNINSTALL("卸载（当前用户）"),
}

@Composable
fun HuijuiAppManager(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val packageRepo: InstalledPackageRepository = koinInject()

    val allPackages by packageRepo.packages.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var expandedPkg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { packageRepo.refresh() }
    }

    val filtered = remember(allPackages, searchQuery, showSystemApps) {
        allPackages
            .filter { showSystemApps || !it.isSystem }
            .filter {
                searchQuery.isBlank() ||
                    it.appLabel.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    fun runAction(label: String, block: suspend () -> Result<Unit>) {
        if (running) return
        scope.launch {
            running = true
            log = "执行：$label ..."
            val r = withContext(Dispatchers.IO) { block() }
            log = if (r.isSuccess) "✓ $label 成功"
                  else "✗ $label 失败：${r.exceptionOrNull()?.message}"
            running = false
        }
    }

    Dialog(onDismissRequest = { if (!running) onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                HuijuiDialogHeader(
                    icon = Icons.TwoTone.Apps,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "应用管理器",
                    subtitle = "冻结 / 卸载 / 强停 已安装应用",
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索应用名或包名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showSystemApps,
                        onCheckedChange = { showSystemApps = it },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("显示系统应用", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${filtered.size} 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (log.isNotEmpty()) {
                    Text(
                        log,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            log.startsWith("✓") -> MaterialTheme.colorScheme.primary
                            log.startsWith("✗") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.packageName }) { pkg ->
                        AppManagerRow(
                            pkg = pkg,
                            expanded = expandedPkg == pkg.packageName,
                            onExpandChange = { expandedPkg = if (it) pkg.packageName else null },
                            onAction = { action ->
                                expandedPkg = null
                                runAction("${action.label} ${pkg.appLabel.ifBlank { pkg.packageName }}") {
                                    when (action) {
                                        AppManagerAction.FREEZE -> HuijuiAppManagerRepository.freeze(pkg.packageName)
                                        AppManagerAction.UNFREEZE -> HuijuiAppManagerRepository.unfreeze(pkg.packageName)
                                        AppManagerAction.FORCE_STOP -> HuijuiAppManagerRepository.forceStop(pkg.packageName)
                                        AppManagerAction.CLEAR -> HuijuiAppManagerRepository.clearCache(pkg.packageName)
                                        AppManagerAction.UNINSTALL -> HuijuiAppManagerRepository.uninstallForUser(pkg.packageName)
                                    }
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { if (!running) onDismiss() }) { Text("关闭") }
                }
            }
        }
    }
}

@Composable
private fun AppManagerRow(
    pkg: InstalledPackageInfo,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAction: (AppManagerAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onExpandChange(!expanded) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(pkg.packageName, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                pkg.appLabel.ifBlank { pkg.packageName },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pkg.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (pkg.isSystem) {
            Text(
                "系统",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Box {
            IconButton(onClick = { onExpandChange(!expanded) }) {
                Icon(Icons.TwoTone.MoreVert, contentDescription = "更多")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpandChange(false) }) {
                AppManagerAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = { onAction(action) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, size: Dp) {
    val appContext = LocalContext.current
    val bitmap = remember(packageName) {
        try {
            val drawable = appContext.packageManager.getApplicationIcon(packageName)
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (t: Throwable) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    } else {
        Icon(
            imageVector = Icons.TwoTone.Android,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}
