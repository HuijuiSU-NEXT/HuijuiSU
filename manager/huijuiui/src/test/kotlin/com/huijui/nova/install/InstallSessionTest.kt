package com.huijui.nova.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 安装流程的三条硬要求：阶段推进可观测、失败能定位到具体步骤、音量键提问能被识别并转成按钮。
 */
class InstallSessionTest {

    @Test
    fun `presets cover every mode and start pending`() {
        InstallMode.entries.forEach { mode ->
            val stages = InstallStages.of(mode)
            assertTrue("${mode.label} 至少 4 个阶段", stages.size >= 4)
            assertEquals(stages.size, stages.map { it.key }.toSet().size)
        }
    }

    @Test
    fun `progress only moves forward as stages complete`() {
        val session = InstallSessionState(InstallMode.Direct)
        assertEquals(0f, session.overallProgress, 0.001f)
        session.start()
        val afterStart = session.overallProgress
        assertTrue(afterStart > 0f)
        session.succeed(0)
        val afterFirst = session.overallProgress
        assertTrue(afterFirst > afterStart)
        assertTrue(afterFirst <= 1f)
    }

    @Test
    fun `failure stops the run and marks the running stage`() {
        val session = InstallSessionState(InstallMode.Direct)
        session.start()
        session.running(1, "解包中")
        session.fail("分区不可读", index = 1)

        assertTrue(session.finished)
        assertEquals("分区不可读", session.failure)
        assertEquals(StageStatus.Failed, session.stageStates[1].status)
        assertEquals("分区不可读", session.stageStates[1].detail)
    }

    @Test
    fun `cancel clears prompt and finishes`() {
        val session = InstallSessionState(InstallMode.Module)
        session.start()
        session.raisePrompt(InstallPrompt(1, "继续?", listOf("是", "否"), "音量上 = 是"))
        assertNotNull(session.prompt)

        session.cancel()
        assertTrue(session.cancelled)
        assertTrue(session.finished)
        assertNull(session.prompt)
    }

    @Test
    fun `answer clears prompt and records choice in logs`() {
        val session = InstallSessionState(InstallMode.Direct)
        session.raisePrompt(
            InstallPrompt(7, "写哪里?", listOf("init_boot", "boot"), "音量上 = init_boot"),
        )
        session.answerPrompt(0)

        assertNull(session.prompt)
        assertTrue(session.logs.any { it.contains("init_boot") })
    }

    @Test
    fun `logs are capped so long installs do not grow without bound`() {
        val session = InstallSessionState(InstallMode.Direct)
        repeat(500) { session.log("line $it") }
        assertEquals(200, session.logs.size)
        assertEquals("line 499", session.logs.last())
    }

    @Test
    fun `chinese volume key prompt is recognised`() {
        val line = "请按音量上键选择：是，音量下键：否"
        assertTrue(PromptParser.isPrompt(line))
        val prompt = PromptParser.parse(line, 1)
        assertNotNull(prompt)
        assertTrue(prompt!!.options.isNotEmpty())
        assertTrue(prompt.keyHint.contains("音量上"))
    }

    @Test
    fun `english volume key prompt is recognised`() {
        val line = "Volume UP: install to init_boot, Volume DOWN: cancel"
        assertTrue(PromptParser.isPrompt(line))
        val prompt = PromptParser.parse(line, 2)
        assertNotNull(prompt)
        assertTrue(prompt!!.keyHint.startsWith("Volume"))
    }

    @Test
    fun `ordinary log lines are not treated as prompts`() {
        assertFalse(PromptParser.isPrompt("mkdir -p /data/adb/modules/zygisk"))
        assertNull(PromptParser.parse("Compiling module: success", 1))
        assertNull(PromptParser.firstPrompt(listOf("sh: 1: ok", "done")))
    }

    @Test
    fun `prompt falls back to yes no when options cannot be parsed`() {
        val prompt = PromptParser.parse("音量键选择", 3)
        assertNotNull(prompt)
        assertEquals(listOf("是", "否"), prompt!!.options)
    }

    @Test
    fun `first prompt scans mixed output`() {
        val output = listOf(
            "checking environment",
            "Volume UP: continue, Volume DOWN: abort",
            "音量上键：确认",
        )
        val prompt = PromptParser.firstPrompt(output)
        assertNotNull(prompt)
        assertEquals(2, prompt!!.id)
    }
}
