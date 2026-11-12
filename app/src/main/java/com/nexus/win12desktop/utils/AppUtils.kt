package com.nexus.win12desktop.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystemApp: Boolean
)

object AppUtils {

    fun getLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        
        return resolveInfos
            .mapNotNull { info ->
                try {
                    val appInfo = pm.getApplicationInfo(info.activityInfo.packageName, 0)
                    if (appInfo.packageName == context.packageName) return@mapNotNull null
                    
                    AppInfo(
                        packageName = appInfo.packageName,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
            .distinctBy { it.packageName }
    }
}
