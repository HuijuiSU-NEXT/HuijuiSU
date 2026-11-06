package com.huijui.nova.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huijui.nova.design.LocalHuiColors

/**
 * Nova 自带图标集。不引 material-icons，路径全部是 24×24 顶点手写的，
 * 所以图标风格跟新界面的圆角/线宽语言一致，也不会把 M2 图标包带进来。
 */
@Immutable
class HuiGlyph(
    val name: String,
    val paths: List<String>,
    val stroked: Boolean = true,
    val strokeWidth: Float = 1.7f,
)

private fun glyph(name: String, vararg paths: String, stroked: Boolean = true, strokeWidth: Float = 1.7f) =
    HuiGlyph(name = name, paths = paths.toList(), stroked = stroked, strokeWidth = strokeWidth)

object HuiIcons {
    val Back = glyph("back", "M15 4.5 L7.5 12 L15 19.5")
    val ChevronRight = glyph("chevronRight", "M9.5 5 L16.5 12 L9.5 19")
    val ChevronDown = glyph("chevronDown", "M5 9.5 L12 16.5 L19 9.5")
    val Close = glyph("close", "M6 6 L18 18", "M18 6 L6 18")
    val Check = glyph("check", "M4.5 12.5 L9.5 17.5 L19.5 6.5")
    val Plus = glyph("plus", "M12 5 V19", "M5 12 H19")
    val Minus = glyph("minus", "M5 12 H19")
    val Power = glyph("power", "M12 3 V11", "M6.6 6.4 A7.6 7.6 0 1 0 17.4 6.4")
    val Home = glyph("home", "M4 11 L12 4 L20 11 V20 H14 V14 H10 V20 H4 Z")
    val Shield = glyph("shield", "M12 3 L20 6.5 V12 C20 17 16.2 20.2 12 21.5 C7.8 20.2 4 17 4 12 V6.5 Z")
    val Sparkle = glyph(
        "sparkle",
        "M12 3 L13.6 8.6 L19 10.2 L13.6 11.8 L12 17.4 L10.4 11.8 L5 10.2 L10.4 8.6 Z",
        stroked = false,
    )
    val Star = glyph(
        "star",
        "M12 3 L14.6 9 L21 9.6 L16.2 13.9 L17.6 20.2 L12 16.8 L6.4 20.2 L7.8 13.9 L3 9.6 L9.4 9 Z",
        stroked = false,
    )
    val Module = glyph(
        "module",
        "M12 3 L20.5 7.5 V16.5 L12 21 L3.5 16.5 V7.5 Z",
        "M12 12 L20.5 7.5",
        "M12 12 V21",
        "M12 12 L3.5 7.5",
    )
    val Layers = glyph(
        "layers",
        "M12 3 L21 8 L12 13 L3 8 Z",
        "M4 12 L12 17 L20 12",
        "M4 16 L12 21 L20 16",
    )
    val Settings = glyph(
        "settings",
        "M12 8.4 A3.6 3.6 0 1 0 12 15.6 A3.6 3.6 0 1 0 12 8.4",
        "M12 2.8 V5.4",
        "M12 18.6 V21.2",
        "M2.8 12 H5.4",
        "M18.6 12 H21.2",
        "M5.5 5.5 L7.3 7.3",
        "M16.7 16.7 L18.5 18.5",
        "M18.5 5.5 L16.7 7.3",
        "M7.3 16.7 L5.5 18.5",
    )
    val Terminal = glyph(
        "terminal",
        "M3.5 5 H20.5 V19 H3.5 Z",
        "M7 9.5 L10 12 L7 14.5",
        "M12.5 15 H16.5",
    )
    val Download = glyph("download", "M12 3.5 V14.5", "M7.5 10 L12 14.5 L16.5 10", "M4.5 20 H19.5")
    val Upload = glyph("upload", "M12 20.5 V9.5", "M7.5 14 L12 9.5 L16.5 14", "M4.5 4.5 H19.5")
    val Refresh = glyph("refresh", "M20 12 A8 8 0 1 1 16.6 5.6", "M20.4 3.6 V9 H15")
    val Folder = glyph(
        "folder",
        "M3 7.5 A2 2 0 0 1 5 5.5 H9.2 L11.2 7.5 H19 A2 2 0 0 1 21 9.5 V18 A2 2 0 0 1 19 20 H5 A2 2 0 0 1 3 18 Z",
    )
    val Search = glyph("search", "M10.5 4 A6.5 6.5 0 1 0 10.5 17 A6.5 6.5 0 1 0 10.5 4", "M15.5 15.5 L20.5 20.5")
    val Bell = glyph(
        "bell",
        "M18 9.5 A6 6 0 0 0 6 9.5 C6 15 4 16.5 4 16.5 H20 C20 16.5 18 15 18 9.5",
        "M10 20 A2.2 2.2 0 0 0 14 20",
    )
    val Warning = glyph("warning", "M12 3.5 L21.5 20 H2.5 Z", "M12 9 V14", "M12 17 V17.6")
    val Info = glyph("info", "M12 3.5 A8.5 8.5 0 1 0 12 20.5 A8.5 8.5 0 1 0 12 3.5", "M12 11 V16.5", "M12 7.6 V8.2")
    val Play = glyph("play", "M8 4.5 L19.5 12 L8 19.5 Z", stroked = false)
    val Pause = glyph("pause", "M7 4.5 H10 V19.5 H7 Z", "M14 4.5 H17 V19.5 H14 Z", stroked = false)
    val Zap = glyph("zap", "M13.5 2.5 L5 13.5 H11 L10.5 21.5 L19 10.5 H13 Z")
    val Cpu = glyph(
        "cpu",
        "M6.5 6.5 H17.5 V17.5 H6.5 Z",
        "M9.8 9.8 H14.2 V14.2 H9.8 Z",
        "M9.5 3 V6.5", "M14.5 3 V6.5", "M9.5 17.5 V21", "M14.5 17.5 V21",
        "M3 9.5 H6.5", "M3 14.5 H6.5", "M17.5 9.5 H21", "M17.5 14.5 H21",
    )
    val Trash = glyph("trash", "M4 6.5 H20", "M9.5 6.5 V4 H14.5 V6.5", "M6.2 6.5 L7.2 20.5 H16.8 L17.8 6.5")
    val Moon = glyph("moon", "M20.5 14.2 A8.8 8.8 0 1 1 9.8 3.5 A7.2 7.2 0 0 0 20.5 14.2 Z")
    val Sun = glyph(
        "sun",
        "M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8",
        "M12 2.5 V5", "M12 19 V21.5",
        "M2.5 12 H5", "M19 12 H21.5",
        "M5.2 5.2 L7 7", "M17 17 L18.8 18.8",
        "M18.8 5.2 L17 7", "M7 17 L5.2 18.8",
    )
    val Palette = glyph(
        "palette",
        "M12 3.5 A8.5 8.5 0 1 0 12 20.5 C14 20.5 14.4 19.2 13.4 18 C12.4 16.8 12.9 15 14.6 15 H17 C19.2 15 20.5 13.4 20.5 11.4 C20.5 6.9 16.7 3.5 12 3.5 Z",
        stroked = true,
    )
    val Rocket = glyph(
        "rocket",
        "M12 2.8 C15.4 5 16.8 8.6 16.8 12.4 L14 15.2 H10 L7.2 12.4 C7.2 8.6 8.6 5 12 2.8 Z",
        "M10 15.2 L8.6 20.4 L12 18.6 L15.4 20.4 L14 15.2",
    )
    val Gauge = glyph("gauge", "M4 17 A8.5 8.5 0 1 1 20 17", "M12 17 L16 10.5")
    val Lock = glyph(
        "lock",
        "M6 10.5 H18 V20.5 H6 Z",
        "M8.6 10.5 V7.6 A3.4 3.4 0 0 1 15.4 7.6 V10.5",
    )
    val Eye = glyph("eye", "M2.5 12 C5.5 7 8.7 5 12 5 C15.3 5 18.5 7 21.5 12 C18.5 17 15.3 19 12 19 C8.7 19 5.5 17 2.5 12 Z", "M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9")

    val SoundOn = glyph("soundOn", "M4 9.5 H7.2 L12 5.5 V18.5 L7.2 14.5 H4 Z", "M15.6 8.8 A4.6 4.6 0 0 1 15.6 15.2")
    val MuteOff = glyph("muteOff", "M4 9.5 H7.2 L12 5.5 V18.5 L7.2 14.5 H4 Z", "M15.8 9.2 L20.2 14.8", "M20.2 9.2 L15.8 14.8")
    val Grid = glyph(
        "grid",
        "M3.5 3.5 H10.5 V10.5 H3.5 Z",
        "M13.5 3.5 H20.5 V10.5 H13.5 Z",
        "M3.5 13.5 H10.5 V20.5 H3.5 Z",
        "M13.5 13.5 H20.5 V20.5 H13.5 Z",
    )
}

@Composable
fun HuiIcon(
    glyph: HuiGlyph,
    modifier: Modifier = Modifier,
    tint: Color = LocalHuiColors.current.text,
    size: Dp = 20.dp,
    strokeWidth: Float = glyph.strokeWidth,
) {
    val paths = remember(glyph) {
        glyph.paths.map { data ->
            runCatching { PathParser().parsePathString(data).toPath() }.getOrElse { Path() }
        }
    }
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 24f
        scale(unit, unit, pivot = Offset.Zero) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = tint,
                    style = if (glyph.stroked) {
                        Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    } else {
                        Fill
                    },
                )
            }
        }
    }
}
