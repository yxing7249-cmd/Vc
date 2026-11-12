package com.nexus.win12desktop.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystemApp: Boolean
)

object AppUtils {

    fun getLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val packages = LinkedHashMap<String, AppInfo>()

        fun collect(category: String) {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(category)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            try {
                pm.queryIntentActivities(intent, flags).forEach { info ->
                    try {
                        val pkg = info.activityInfo.packageName
                        if (pkg == context.packageName) return@forEach
                        if (packages.containsKey(pkg)) return@forEach
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        packages[pkg] = AppInfo(
                            packageName = pkg,
                            label = pm.getApplicationLabel(appInfo).toString(),
                            icon = pm.getApplicationIcon(appInfo),
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }

        collect(Intent.CATEGORY_LEANBACK_LAUNCHER)
        collect(Intent.CATEGORY_LAUNCHER)

        return packages.values.sortedBy { it.label.lowercase() }
    }
}
