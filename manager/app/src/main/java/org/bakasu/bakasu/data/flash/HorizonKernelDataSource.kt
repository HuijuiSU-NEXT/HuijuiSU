package org.bakasu.bakasu.data.flash

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.bakasu.bakasu.R
import org.bakasu.bakasu.data.shell.KsuCliRepository
import org.bakasu.bakasu.domain.model.FlashProgress

/**
 * @author ShirkNeko
 * @date 2025/5/31.
 */
class HorizonKernelState {
    private val _state = MutableStateFlow(FlashProgress())
    private val fullLogs = ConcurrentLinkedQueue<String>()
    val state: StateFlow<FlashProgress> = _state.asStateFlow()

    fun updateProgress(progress: Float) {
        _state.update { it.copy(progress = progress) }
    }

    fun updateStep(step: String) {
        _state.update { it.copy(currentStep = step) }
    }

    fun addLog(log: String) {
        fullLogs.add(log)
        _state.update {
            it.copy(logs = it.logs + log)
        }
    }

    fun addConsoleLog(log: String) {
        fullLogs.add(log)
    }

    fun getFullLog(): String = fullLogs.joinToString("\n")

    fun setError(error: String) {
        _state.update { it.copy(isFlashing = false, error = error) }
    }

    fun startFlashing() {
        fullLogs.clear()
        _state.update {
            it.copy(
                isFlashing = true,
                isCompleted = false,
                progress = 0f,
                currentStep = "",
                logs = emptyList(),
                error = "",
            )
        }
    }

    fun completeFlashing() {
        _state.update {
            it.copy(
                isFlashing = false,
                isCompleted = true,
                progress = 1f,
            )
        }
    }

    fun reset() {
        fullLogs.clear()
        _state.value = FlashProgress()
    }
}

class HorizonKernelWorker(
    private val context: Context,
    private val state: HorizonKernelState,
    private val ksuCliRepository: KsuCliRepository,
    private val slot: String? = null,
    private val skipKsud: Boolean = false,
) : Thread() {
    var uri: Uri? = null

    override fun run() {
        state.startFlashing()
        state.updateStep(context.getString(R.string.horizon_preparing))

        val zipFile = File(context.cacheDir, "anykernel3.zip")
        try {
            if (!ksuCliRepository.rootAvailable()) {
                state.setError(context.getString(R.string.root_required))
                return
            }

            state.updateStep(context.getString(R.string.horizon_copying_files))
            state.updateProgress(0.2f)
            copyToCache(zipFile)

            state.updateStep(context.getString(R.string.horizon_flashing))
            state.updateProgress(0.7f)
            val succeeded = ksuCliRepository.flashAnyKernel(
                zipFile = zipFile,
                slot = slot,
                onStdout = ::handleOutput,
                onStderr = ::handleConsoleOutput,
            )
            if (!succeeded) {
                state.setError(context.getString(R.string.flash_failed_message))
                return
            }

            if (!skipKsud) {
                runCatching { ksuCliRepository.install() }.onFailure { error ->
                    // Log removed for stealth
                }
            }
            state.updateStep(context.getString(R.string.horizon_flash_complete_status))
            state.completeFlashing()
        } catch (error: Exception) {
            state.setError(
                error.message ?: context.getString(R.string.horizon_unknown_error),
            )
        } finally {
            if (zipFile.exists()) {
                zipFile.delete()
            }
        }
    }

    private fun copyToCache(zipFile: File) {
        zipFile.delete()
        val source = uri
            ?: throw IOException(context.getString(R.string.horizon_copy_failed))
        // 修复：高版本 content:// 权限过期会抛 SecurityException，增加严格捕获
        val input = try {
            context.contentResolver.openInputStream(source)
        } catch (e: SecurityException) {
            throw IOException("文件读取权限已过期，请重新选择文件", e)
        } catch (e: Exception) {
            throw IOException("无法读取文件流，请确认文件存在", e)
        } ?: throw IOException(context.getString(R.string.horizon_copy_failed))
        
        input.use {
            zipFile.outputStream().use { output ->
                it.buffered().copyTo(output.buffered())
            }
        }
        if (!zipFile.isFile) {
            throw IOException(context.getString(R.string.horizon_copy_failed))
        }
    }

    private fun handleOutput(line: String) {
        // Log removed for stealth
        state.addLog(line)

        when {
            line.contains("extracting", ignoreCase = true) -> {
                state.updateProgress(0.75f)
            }

            line.contains("installing", ignoreCase = true) -> {
                state.updateProgress(0.85f)
            }

            line.contains("complete", ignoreCase = true) -> {
                state.updateProgress(0.95f)
            }
        }
    }

    private fun handleConsoleOutput(line: String) {
        // Log removed for stealth
        state.addConsoleLog(line)
    }

    private companion object {
        const val TAG = "HorizonKernelWorker"
    }
}
