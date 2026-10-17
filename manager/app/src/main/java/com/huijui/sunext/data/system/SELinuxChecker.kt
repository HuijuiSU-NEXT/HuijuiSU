package com.huijui.sunext.data.system

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.huijui.sunext.R

fun isSELinuxPermissive(): Boolean {
    val shell = Shell.Builder.create().build("sh")
    val stdoutList = ArrayList<String>()
    val result = shell.use {
        it.newJob().add("getenforce").to(stdoutList).exec()
    }
    return result.isSuccess && stdoutList.joinToString("").trim() == "Permissive"
}

fun getSELinuxStatus(ctx: Context) = SuFile("/sys/fs/selinux/enforce").run {
    when {
        !exists() -> ctx.getString(R.string.selinux_status_disabled)

        !isFile -> ctx.getString(R.string.unknown)

        !canRead() -> ctx.getString(R.string.selinux_status_enforcing)

        else -> when (
            runCatching { newInputStream() }.getOrNull()?.bufferedReader()
                ?.use { it.runCatching { readLine() }.getOrNull()?.trim()?.toIntOrNull() }
        ) {
            1 -> ctx.getString(R.string.selinux_status_enforcing)
            0 -> ctx.getString(R.string.selinux_status_permissive)
            else -> ctx.getString(R.string.unknown)
        }
    }
}
