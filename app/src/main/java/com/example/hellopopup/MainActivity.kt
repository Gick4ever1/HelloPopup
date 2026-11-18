package com.example.hellopopup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class MainActivity : Activity() {

    private var askedForPermission = false

    override fun onResume() {
        super.onResume()
        when {
            Settings.canDrawOverlays(this) -> {
                startService(Intent(this, PopupService::class.java))
                finish()
            }
            !askedForPermission -> {
                askedForPermission = true
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            else -> finish()
        }
    }
}
