package com.huijui.sunext.ui.util.downloader

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.huijui.sunext.R
import com.huijui.sunext.domain.model.DownloadStatus
import com.huijui.sunext.domain.model.ManagerUpdateInfo
import com.huijui.sunext.domain.usecase.EnqueueDownloadUseCase
import com.huijui.sunext.domain.usecase.EnqueueManagerUpdateUseCase
import com.huijui.sunext.domain.usecase.ObserveDownloadUseCase
import com.huijui.sunext.ui.activity.PermissionRequestInterface

/**
 * @author weishu
 * @date 2023/6/22.
 */
fun download(
    appContext: Context,
    permissionRequestInterface: PermissionRequestInterface,
    url: String,
    fileName: String,
    enqueueDownload: EnqueueDownloadUseCase,
    observeDownload: ObserveDownloadUseCase,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {},
    onProgress: (Int) -> Unit = {},
) {
    fun startDownloadFile(
        url: String,
        fileName: String,
        onDownloaded: (Uri) -> Unit,
        onDownloading: () -> Unit,
        onProgress: (Int) -> Unit,
    ) {
        onDownloading()

        val downloadId = enqueueDownload(url, fileName)

        CoroutineScope(Dispatchers.Main).launch {
            observeDownload(downloadId).collect { state ->
                state ?: return@collect
                onProgress(state.progress)
                if (state.status == DownloadStatus.COMPLETED ||
                    state.status == DownloadStatus.FAILED
                ) {
                    state.resultUri?.let { onDownloaded(it.toUri()) }
                    cancel()
                }
            }
        }
    }

    requestDownloadPermissions(context, permissionRequestInterface) {
        startDownloadFile(
            url = url,
            fileName = fileName,
            onDownloaded = onDownloaded,
            onDownloading = onDownloading,
            onProgress = onProgress,
        )
    }
}

fun downloadManagerUpdate(
    appContext: Context,
    permissionRequestInterface: PermissionRequestInterface,
    update: ManagerUpdateInfo,
    enqueueManagerUpdate: EnqueueManagerUpdateUseCase,
) {
    requestDownloadPermissions(context, permissionRequestInterface) {
        enqueueManagerUpdate(update)
    }
}

private fun requestDownloadPermissions(
    appContext: Context,
    permissionRequestInterface: PermissionRequestInterface,
    onGranted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // sdk 32+, require post_notifications permission
        permissionRequestInterface.requestPermission(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            callback = { success ->
                if (!success) {
                    Toast.makeText(
                        context,
                        appContext.getString(R.string.notification_permission_denied),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@requestPermission
                }

                onGranted()
            },
            requestDescription = appContext.getString(R.string.notification_permission_description),
        )
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) {
        // sdk 32, no need any permission
        onGranted()
    } else {
        // sdk 32-, require write external storage
        permissionRequestInterface.requestPermissions(
            permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
            callback = { result ->
                val success = result.all { it.value }
                if (!success) {
                    Toast.makeText(
                        context,
                        appContext.getString(R.string.storage_permission_denied),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@requestPermissions
                }
                onGranted()
            },
            requestDescription = mapOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE to appContext.getString(R.string.storage_permission_description),
            ),
        )
    }
}
