package org.bakasu.bakasu.ui.screen.main

import android.annotation.SuppressLint
import android.app.Activity.CLIPBOARD_SERVICE
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Undo
import androidx.compose.material.icons.automirrored.twotone.Wysiwyg
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.Cloud
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Download
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.R
import org.bakasu.bakasu.data.settings.SettingsPlatformRepository
import org.bakasu.bakasu.domain.model.InstalledModule
import org.bakasu.bakasu.domain.model.MetaModuleStatus
import org.bakasu.bakasu.domain.usecase.EnqueueDownloadUseCase
import org.bakasu.bakasu.domain.usecase.ExtractModuleNameUseCase
import org.bakasu.bakasu.domain.usecase.FetchRemoteTextUseCase
import org.bakasu.bakasu.domain.usecase.IsModuleUriAccessibleUseCase
import org.bakasu.bakasu.domain.usecase.ObserveDownloadUseCase
import org.bakasu.bakasu.domain.usecase.TakeModuleUriPermissionUseCase
import org.bakasu.bakasu.ui.component.ConfirmResult
import org.bakasu.bakasu.ui.component.InstallConfirmationDialog
import org.bakasu.bakasu.ui.component.SearchAppBar
import org.bakasu.bakasu.ui.component.SwipeableSnackbarHost
import org.bakasu.bakasu.ui.component.WarningCard
import org.bakasu.bakasu.ui.component.ZipFileDetector
import org.bakasu.bakasu.ui.component.ZipFileInfo
import org.bakasu.bakasu.ui.component.ZipType
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.rememberLoadingDialog
import org.bakasu.bakasu.ui.component.rememberSearchAppBarScrollBehavior
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SettingsBaseWidget
import org.bakasu.bakasu.ui.component.settings.SettingsJumpPageWidget
import org.bakasu.bakasu.ui.component.settings.SettingsTextFieldWidget
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.navigation.Route
import org.bakasu.bakasu.ui.screen.LabelText
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.theme.renderBackgroundBlur
import org.bakasu.bakasu.ui.util.LocalPermissionRequestInterface
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.Shortcut
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.util.downloader.download
import org.bakasu.bakasu.ui.util.showReplacingSnackbar
import org.bakasu.bakasu.ui.viewmodel.HomeViewModel
import org.bakasu.bakasu.ui.viewmodel.ModuleUiAction
import org.bakasu.bakasu.ui.viewmodel.ModuleUiEvent
import org.bakasu.bakasu.ui.viewmodel.ModuleUiState
import org.bakasu.bakasu.ui.viewmodel.ModuleViewModel
import org.bakasu.bakasu.ui.webui.WebUIActivity
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.text.font.FontFamily
@Composable
fun ModuleItem(
    viewModel: ModuleViewModel,
    module: InstalledModule,
    moduleSizes: Map<String, String>,
    updateUrl: String,
    onUninstallClicked: (InstalledModule) -> Unit,
    onCheckChanged: suspend (Boolean) -> Boolean,
    onUpdate: (InstalledModule) -> Unit,
    onClick: (InstalledModule) -> Unit,
    onModuleAddShortcut: (InstalledModule, ShortcutType) -> Unit,
    showMoreModuleInfo: Boolean) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isEnabled by remember(module.dirId) { mutableStateOf(module.enabled) }
    var isChangingEnabled by remember(module.dirId) { mutableStateOf(false) }

    LaunchedEffect(module.enabled) {
        isEnabled = module.enabled
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .renderBackgroundBlur(),
        color =
            if (themeConfig.isEnableBlurExp) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceBright.copy(cardConfig.cardAlpha)
            },
        shape = RoundedCornerShape(16.dp)) {
        val textDecoration = if (!module.remove) null else TextDecoration.LineThrough
        val interactionSource = remember { MutableInteractionSource() }

        LaunchedEffect(module.dirId) {
            viewModel.dispatch(ModuleUiAction.LoadSize(module.dirId))
        }

        val sizeStr = moduleSizes[module.dirId]

        Column(
            modifier = Modifier
                .run {
                    if (module.hasActionScript || module.hasWebUi) {
                        combinedClickable(
                            onClick = {
                                if (module.hasWebUi) {
                                    onClick(module)
                                }
                            })
                    } else {
                        this
                    }
                }
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                val moduleVersion = stringResource(id = R.string.module_version)
                val moduleAuthor = stringResource(id = R.string.module_author)

                Column(
                    modifier = Modifier.fillMaxWidth(0.8f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = module.name,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            textDecoration = textDecoration,
                            modifier = Modifier.weight(1f, false))
                    }

                    Text(
                        text = "$moduleVersion: ${module.version}",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                        textDecoration = textDecoration)

                    Text(
                        text = "$moduleAuthor: ${module.author}",
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                        textDecoration = textDecoration)

                    // 显示更多模块信息时添加updateJson
                    if (showMoreModuleInfo && module.updateJson.isNotEmpty()) {
                        val updateJsonLabel = stringResource(R.string.module_update_json)
                        Text(
                            text = "$updateJsonLabel: ${module.updateJson}",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                            textDecoration = textDecoration,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        val clipData = ClipData.newPlainText(
                                            "Update JSON URL",
                                            module.updateJson)
                                        clipboardManager.setPrimaryClip(clipData)
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.module_update_json_copied),
                                            Toast.LENGTH_SHORT).show()
                                    }))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End) {
                    Switch(
                        enabled = !module.update && !isChangingEnabled,
                        checked = isEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                isChangingEnabled = true
                                try {
                                    if (onCheckChanged(enabled)) {
                                        isEnabled = enabled
                                    }
                                } finally {
                                    isChangingEnabled = false
                                }
                            }
                        },
                        interactionSource = if (!module.hasWebUi) interactionSource else null,
                        thumbContent = {
                            if (isEnabled) {
                                Icon(
                                    imageVector = Icons.TwoTone.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(SwitchDefaults.IconSize))
                            } else {
                                Icon(
                                    imageVector = Icons.TwoTone.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surfaceBright,
                                    modifier = Modifier.size(SwitchDefaults.IconSize))
                            }
                        })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = module.description,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                textDecoration = textDecoration)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                LabelText(
                    label = module.dirId,
                    containerColor = MaterialTheme.colorScheme.primary)
                if (module.metamodule) {
                    LabelText(
                        label = "META",
                        containerColor = MaterialTheme.colorScheme.tertiary)
                }
                LabelText(
                    label = sizeStr ?: "0 KB",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(thickness = Dp.Hairline)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (module.hasActionScript) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .defaultMinSize(minWidth = 52.dp, minHeight = 32.dp)
                            .clip(ButtonDefaults.filledTonalShape)
                            .combinedClickable(
                                onClick = {
                                    navigator.push(Route.ExecuteModuleAction(module.dirId, false))
                                    viewModel.dispatch(ModuleUiAction.MarkNeedRefresh)
                                },
                                onLongClick = {
                                    onModuleAddShortcut(module, ShortcutType.Action)
                                }),
                        color =
                            if (!module.remove && isEnabled) {
                                ButtonDefaults.filledTonalButtonColors().containerColor
                            } else {
                                ButtonDefaults.filledTonalButtonColors().disabledContainerColor
                            },
                        contentColor = if (!module.remove && isEnabled) {
                            ButtonDefaults.filledTonalButtonColors().contentColor
                        } else {
                            ButtonDefaults.filledTonalButtonColors().disabledContentColor
                        }) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(12.dp, 7.dp, 12.dp, 7.dp),
                            imageVector = Icons.TwoTone.PlayArrow,
                            contentDescription = null)
                    }
                }

                if (module.hasWebUi) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .defaultMinSize(minWidth = 52.dp, minHeight = 32.dp)
                            .clip(ButtonDefaults.filledTonalShape)
                            .combinedClickable(
                                onClick = { onClick(module) },
                                onLongClick = {
                                    onModuleAddShortcut(module, ShortcutType.WebUI)
                                }),
                        color =
                            if (!module.remove && isEnabled) {
                                ButtonDefaults.filledTonalButtonColors().containerColor
                            } else {
                                ButtonDefaults.filledTonalButtonColors().disabledContainerColor
                            },
                        contentColor = if (!module.remove && isEnabled) {
                            ButtonDefaults.filledTonalButtonColors().contentColor
                        } else {
                            ButtonDefaults.filledTonalButtonColors().disabledContentColor
                        }) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(12.dp, 7.dp, 12.dp, 7.dp),
                            imageVector = Icons.AutoMirrored.TwoTone.Wysiwyg,
                            contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.weight(1f, true))

                if (updateUrl.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .defaultMinSize(minWidth = 52.dp, minHeight = 32.dp)
                            .clip(ButtonDefaults.filledTonalShape)
                            .clickable {
                                onUpdate(module)
                            },
                        color =
                            if (!module.remove) {
                                ButtonDefaults.filledTonalButtonColors().containerColor
                            } else {
                                ButtonDefaults.filledTonalButtonColors().disabledContainerColor
                            },
                        contentColor = if (!module.remove) {
                            ButtonDefaults.filledTonalButtonColors().contentColor
                        } else {
                            ButtonDefaults.filledTonalButtonColors().disabledContentColor
                        }) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(12.dp, 7.dp, 12.dp, 7.dp),
                            imageVector = Icons.TwoTone.Download,
                            contentDescription = null)
                    }
                }

                Surface(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .defaultMinSize(minWidth = 52.dp, minHeight = 32.dp)
                        .clip(ButtonDefaults.filledTonalShape)
                        .clickable {
                            onUninstallClicked(module)
                        },
                    color =
                        if (!module.remove) {
                            ButtonDefaults.filledTonalButtonColors().containerColor
                        } else {
                            ButtonDefaults.filledTonalButtonColors().disabledContainerColor
                        },
                    contentColor = if (!module.remove) {
                        ButtonDefaults.filledTonalButtonColors().contentColor
                    } else {
                        ButtonDefaults.filledTonalButtonColors().disabledContentColor
                    }) {
                    if (!module.remove) {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(12.dp, 7.dp, 12.dp, 7.dp),
                            imageVector = Icons.TwoTone.Delete,
                            contentDescription = null)
                    } else {
                        Icon(
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(180f)
                                .padding(12.dp, 7.dp, 12.dp, 7.dp),
                            imageVector = Icons.TwoTone.Refresh,
                            contentDescription = null)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ModuleItemPreview() {
    val module = InstalledModule(
        id = "id",
        name = "name",
        version = "version",
        versionCode = 1,
        author = "author",
        description = "I am a test module and i do nothing but show a very long description",
        enabled = true,
        update = true,
        remove = false,
        updateJson = "",
        hasWebUi = true,
        hasActionScript = true,
        metamodule = true,
        actionIconPath = null,
        webUiIconPath = null,
        dirId = "dirId",
        moduleUpdate = null)
    ModuleItem(
        koinViewModel<ModuleViewModel>(),
        module,
        emptyMap(),
        "",
        {},
        { true },
        {},
        {},
        { _, _ -> },
        false)
}
