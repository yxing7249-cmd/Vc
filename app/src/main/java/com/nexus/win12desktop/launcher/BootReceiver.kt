package com.nexus.win12desktop.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexus.win12desktop.ui.DesktopActivity

/**
 * Starts the desktop after device boot if set as launcher.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            val launch = Intent(context, DesktopActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launch)
        }
    }
}
