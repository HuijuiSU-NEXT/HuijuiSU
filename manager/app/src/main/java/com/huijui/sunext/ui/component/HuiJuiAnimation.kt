
package com.huijui.sunext.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 物理软弹点击效果
 * 用法：Modifier.bouncyClick { 你的点击逻辑 }
 */
@Composable
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按下时缩小到 95%，松开时使用弹簧动画弹回 100%
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncy_click"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // 去掉原本死板的水波纹，全靠缩放反馈
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * 赛博朋克风呼吸 Loading 动画
 * 用法：HuiJuiLoadingPulse()
 */
@Composable
fun HuiJuiLoadingPulse() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // 外圈青色呼吸
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .graphicsLayer(alpha = alpha)
                .background(Color(0xFF00E5FF), shape = androidx.compose.foundation.shape.CircleShape)
        )
        // 内圈纯色
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF121212), shape = androidx.compose.foundation.shape.CircleShape)
        )
    }
}
