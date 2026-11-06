package com.huijui.nova.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 界面音效。
 *
 * 不引外部音频素材，也不增加安装包体积：启动时在 cache 目录里合成几个短促的
 * 正弦音（带指数衰减），再用 SoundPool 低延迟播放。
 * 好处是没有版权问题、可随时改参数；代价是音色朴素——这对「按钮点击反馈」够用。
 */
enum class Sfx(val freq: Float, val durationMs: Int, val volume: Float) {
    /** 导航 / 卡片点击 */
    Tap(760f, 55, 0.22f),

    /** 确认 / 进入 */
    Confirm(1180f, 90, 0.26f),

    /** 返回 / 关闭 */
    Back(480f, 70, 0.20f),

    /** 启动前的重音 */
    Launch(320f, 260, 0.30f),
}

@Stable
class SfxEngine internal constructor(context: Context) {

    private val appContext = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = mutableMapOf<Sfx, Int>()

    var enabled by mutableStateOf(true)

    init {
        Sfx.entries.forEach { sfx ->
            val file = toneFile(appContext, sfx)
            if (file.length() > 44) {
                runCatching { pool.load(file.absolutePath, 1) }.getOrNull()?.let { ids[sfx] = it }
            }
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val id = ids[sfx] ?: return
        runCatching { pool.play(id, sfx.volume, sfx.volume, 1, 0, 1f) }
    }

    fun release() {
        runCatching { pool.release() }
    }

    private companion object {
        /** 把 [Sfx] 合成为 16bit 单声道 WAV，缓存在磁盘上，只生成一次。 */
        fun toneFile(context: Context, sfx: Sfx): File {
            val dir = File(context.cacheDir, "nova_sfx")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${sfx.name.lowercase()}_${sfx.freq.toInt()}_${sfx.durationMs}.wav")
            if (file.exists() && file.length() > 44) return file
            runCatching { writeTone(file, sfx) }.onFailure { runCatching { file.delete() } }
            return file
        }

        fun writeTone(file: File, sfx: Sfx) {
            val sampleRate = 44_100
            val totalSamples = sampleRate * sfx.durationMs / 1000
            val bytes = ByteArray(44 + totalSamples * 2)

            fun putInt(offset: Int, value: Int) {
                bytes[offset] = (value and 0xFF).toByte()
                bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
                bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
                bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
            }

            fun putShort(offset: Int, value: Int) {
                bytes[offset] = (value and 0xFF).toByte()
                bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
            }

            // RIFF header
            bytes[0] = 'R'.code.toByte(); bytes[1] = 'I'.code.toByte()
            bytes[2] = 'F'.code.toByte(); bytes[3] = 'F'.code.toByte()
            putInt(4, 36 + totalSamples * 2)
            bytes[8] = 'W'.code.toByte(); bytes[9] = 'A'.code.toByte()
            bytes[10] = 'V'.code.toByte(); bytes[11] = 'E'.code.toByte()
            bytes[12] = 'f'.code.toByte(); bytes[13] = 'm'.code.toByte()
            bytes[14] = 't'.code.toByte(); bytes[15] = ' '.code.toByte()
            putInt(16, 16)
            putShort(20, 1) // PCM
            putShort(22, 1) // mono
            putInt(24, sampleRate)
            putInt(28, sampleRate * 2)
            putShort(32, 2)
            putShort(34, 16)
            bytes[36] = 'd'.code.toByte(); bytes[37] = 'a'.code.toByte()
            bytes[38] = 't'.code.toByte(); bytes[39] = 'a'.code.toByte()
            putInt(40, totalSamples * 2)

            for (i in 0 until totalSamples) {
                val t = i.toFloat() / sampleRate
                val envelope = exp(-4.5f * i / totalSamples)
                // 轻微的上滑音，避免纯正弦听起来像系统提示音
                val frequency = sfx.freq * (1f + 0.06f * i / totalSamples)
                val sample = sin(2.0 * PI * frequency * t) * envelope * sfx.volume
                putShort(44 + i * 2, (sample * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767))
            }

            FileOutputStream(file).use { it.write(bytes) }
        }
    }
}

/** 每次进入界面时创建一个引擎，离开时释放。 */
@Composable
fun rememberSfxEngine(): SfxEngine {
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = remember(context) { SfxEngine(context) }
    DisposableEffect(engine) { onDispose { engine.release() } }
    return engine
}
