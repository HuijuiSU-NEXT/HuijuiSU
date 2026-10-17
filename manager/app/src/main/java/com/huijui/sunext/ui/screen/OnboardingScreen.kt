package com.huijui.sunext.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.huijui.sunext.R

data class FeatureItem(val icon: String, val title: String, val desc: String)
data class OnboardingPageData(
    val title: String,
    val description: String,
    val isFirstPage: Boolean = false,
    val features: List<FeatureItem> = emptyList()
)

val pages = listOf(
    OnboardingPageData(
        "欢迎使用 HuiJuiSU NEXT",
        "基于全新架构，为你打造最纯净、最隐蔽的 Root 体验。",
        isFirstPage = true
    ),
    OnboardingPageData(
        "v4.4.0-Next 更新内容",
        "这次我们带来了脱胎换骨的改变：",
        features = listOf(
            FeatureItem("🎨", "全新 UI 与动效", "24dp 大圆角卡片、全局半透明毛玻璃与物理弹簧动效"),
            FeatureItem("🛡️", "防格机安全底座", "内置恶意模块扫描，拦截恶意脚本，保护设备安全"),
            FeatureItem("⚡", "底层驱动加固", "修复 JNI 链接错误，同步内核驱动 v4.1.0 状态"),
            FeatureItem("📦", "依赖库升级", "引入 Haze 极光模糊依赖与最新 M3 Expressive 体系")
        )
    ),
    OnboardingPageData(
        "做好准备了吗？",
        "HuiJuiSU 拥有极高的系统权限。在开始之前，请务必阅读以下免责声明。"
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var isAgreed by remember { mutableStateOf(false) }
    val cyanColor = Color(0xFF00E5FF)

    // OS4 风格：全屏深色液态渐变背景
    val bgBrush = Brush.verticalGradient(listOf(Color(0xFF0F0F13), Color(0xFF1A1A24), Color(0xFF0F0F13)))

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // 顶部的液态玻璃状进度指示器
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val width by animateFloatAsState(if (isSelected) 32f else 8f, spring(stiffness = 400f), label = "width")
                    Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(if (isSelected) cyanColor else Color.DarkGray).size(width = width.dp, height = 8.dp))
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                val page = pages[pageIndex]
                // 核心：计算当前页面的偏移量，实现视差
                val offset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

                Column(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationX = offset * 120.dp.toPx()
                        scaleX = 1f - (absOffset * 0.15f)
                        scaleY = 1f - (absOffset * 0.15f)
                        alpha = 1f - (absOffset * 0.5f)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (page.isFirstPage) {
                        Image(painter = painterResource(id = R.drawable.huijui_guide_miku), contentDescription = "Miku", modifier = Modifier.size(260.dp).graphicsLayer { translationY = offset * 50.dp.toPx() })
                        Spacer(modifier = Modifier.height(40.dp))
                    } else if (pageIndex == 1) {
                        // 更新内容展示区
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            page.features.forEach { feature ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A35)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(feature.icon, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(feature.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(feature.desc, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF2A1A1A)), contentAlignment = Alignment.Center) { Text("⚠️", fontSize = 64.sp) }
                        Spacer(modifier = Modifier.height(40.dp))
                    }

                    Text(text = page.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = page.description, color = Color.Gray, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            }

            if (pagerState.currentPage == pages.lastIndex) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A1A1A)).clickable { isAgreed = !isAgreed }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAgreed, onCheckedChange = { isAgreed = it }, colors = CheckboxDefaults.colors(checkedColor = cyanColor))
                    Text(text = "我已了解修改系统底层存在风险，可能导致数据丢失或设备损坏（变砖）。我自愿承担所有后果。", color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Button(
                onClick = { if (pagerState.currentPage == pages.lastIndex) { if (isAgreed) onFinish() } else { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } } },
                enabled = if (pagerState.currentPage == pages.lastIndex) isAgreed else true,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cyanColor, disabledContainerColor = Color.DarkGray)
            ) {
                Text(text = if (pagerState.currentPage == pages.lastIndex) "同意并开始" else "下一步", color = if (pagerState.currentPage == pages.lastIndex && !isAgreed) Color.Gray else Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
