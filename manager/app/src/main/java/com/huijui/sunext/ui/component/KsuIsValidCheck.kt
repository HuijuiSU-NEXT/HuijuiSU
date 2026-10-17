package com.huijui.sunext.ui.component

import androidx.compose.runtime.Composable
import com.huijui.sunext.domain.model.KernelStatus

@Composable
inline fun KsuIsValid(
    status: KernelStatus,
    content: @Composable () -> Unit,
) {
    if (status.isFullFeatured) {
        content()
    }
}
