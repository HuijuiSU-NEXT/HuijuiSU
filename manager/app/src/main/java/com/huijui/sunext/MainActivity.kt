package com.huijui.sunext

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * 唯一入口。
 *
 * 经典界面已移除，这里只负责把 Nova 拉起来，并兼容旧的 Intent 入口
 * （桌面图标、zip 打开、分享）。
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, com.huijui.sunext.nova.NovaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }
}
