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

fun getSELinuxStatus(appContext: Context) = SuFile("/sys/fs/selinux/enforce").run {
    when {
        !exists() -> appContext.getString(R.string.selinux_status_disabled)

        !isFile -> appContext.getString(R.string.unknown)

        !canRead() -> appContext.getString(R.string.selinux_status_enforcing)

        else -> when (
            runCatching { newInputStream() }.getOrNull()?.bufferedReader()
                ?.use { it.runCatching { readLine() }.getOrNull()?.trim()?.toIntOrNull() }
        ) {
            1 -> appContext.getString(R.string.selinux_status_enforcing)
            0 -> appContext.getString(R.string.selinux_status_permissive)
            else -> appContext.getString(R.string.unknown)
        }
    }
}
