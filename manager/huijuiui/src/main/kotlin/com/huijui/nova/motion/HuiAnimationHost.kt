package com.huijui.nova.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 全局「动画开关」。
 *
 * Compose 的无限动画（极光漂移、液态高光、呼吸光）默认会一直跑，即使界面已经
 * 退到后台或者在息屏——这是纯粹的耗电。这里用宿主生命周期把它们硬停掉：
 * 只有 RESUMED 时才认为动画「活跃」。
 *
 * 系统设置里的「动画时长缩放 = 0」不需要额外处理：Compose 的动画会读
 * `MotionDurationScale`，缩放到 0 时动画会直接跳到终态。
 */
val LocalHuiAnimationsActive = compositionLocalOf { true }

@Composable
fun HuiAnimationHost(content: @Composable () -> Unit) {
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            val next = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (next != active) active = next
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalHuiAnimationsActive provides active, content = content)
}
