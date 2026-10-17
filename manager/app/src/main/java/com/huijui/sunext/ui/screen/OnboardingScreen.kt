package com.huijui.sunext.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.huijui.sunext.R

data class OnboardingPageData(
    val title: String,
    val description: String,
    val isFirstPage: Boolean = false
)

val pages = listOf(
    OnboardingPageData(
        title = "欢迎使用 HuiJuiSU",
        description = "基于全新架构，为你打造最纯净、最隐蔽的 Root 体验。",
        isFirstPage = true
    ),
    OnboardingPageData(
        title = "极致隐藏 环境自检",
        description = "深度适配 SUSFS，一键检测路径与挂载，让检测软件无处遁形。",
    ),
    OnboardingPageData(
        title = "做好准备了吗？",
        description = "HuiJuiSU 拥有极高的系统权限。在开始之前，请务必阅读以下免责声明。",
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // 免责声明勾选状态
    var isAgreed by remember { mutableStateOf(false) }

    val darkBg = Color(0xFF121212)
    val cyanColor = Color(0xFF00E5FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部跳过按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage != pages.lastIndex) {
                TextButton(onClick = onFinish) {
                    Text("跳过", color = Color.Gray)
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            val scale by animateFloatAsState(
                targetValue = if (pagerState.currentPage == pageIndex) 1f else 0.8f,
                animationSpec = tween(durationMillis = 500),
                label = "image_scale"
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (page.isFirstPage) {
                    Image(
                        painter = painterResource(id = R.drawable.huijui_guide_miku),
                        contentDescription = "Miku Logo",
                        modifier = Modifier.size(240.dp).scale(scale)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                } else if (pageIndex == 1) {
                    // 第二页占位
                    Box(
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(32.dp)).background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("功能图标", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                } else {
                    // 最后一页占位
                    Box(
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF2A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 64.sp)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                Text(
                    text = page.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = page.description,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 底部：免责声明 + 按钮
        if (pagerState.currentPage == pages.lastIndex) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A1A1A))
                    .clickable { isAgreed = !isAgreed }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = { isAgreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = cyanColor)
                )
                Text(
                    text = "我已了解修改系统底层存在风险，可能导致数据丢失或设备损坏（变砖）。我自愿承担所有后果。",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 指示点
            Row {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) cyanColor else Color.DarkGray
                    val width = if (pagerState.currentPage == iteration) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(width = width, height = 8.dp)
                    )
                }
            }

            // 下一步 / 完成按钮
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        if (isAgreed) onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = if (pagerState.currentPage == pages.lastIndex) isAgreed else true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cyanColor,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.lastIndex) "同意并开始" else "下一步",
                    color = if (pagerState.currentPage == pages.lastIndex && !isAgreed) Color.Gray else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
