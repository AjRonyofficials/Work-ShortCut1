package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

data class AppInfoItem(
    val appName: String,
    val packageName: String,
    val isSelected: Boolean = false,
    val isLiteStorageApp: Boolean = false
)

object AppManagerHelper {

    fun isLiteOrModdedApp(packageName: String, appName: String): Boolean {
        val p = packageName.lowercase()
        val a = appName.lowercase()
        return p.contains("lite") ||
                p.contains("facebook") ||
                p.contains("fblite") ||
                p.contains("katana") ||
                a.contains("lite") ||
                a.contains("facebook")
    }

    fun getInstalledLauncherApps(context: Context): List<AppInfoItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        return resolveInfos.mapNotNull { info ->
            try {
                val pkgName = info.activityInfo.packageName
                // exclude self
                if (pkgName == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm).toString()
                AppInfoItem(
                    appName = label,
                    packageName = pkgName,
                    isLiteStorageApp = isLiteOrModdedApp(pkgName, label)
                )
            } catch (_: Exception) {
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Triggers zero-touch automated clearing of data and cache via AccessibilityService
     */
    fun openAppDetailsForClearData(
        context: Context,
        packageName: String,
        appName: String = "App",
        isLiteStorageMode: Boolean = false
    ) {
        val effectiveIsLite = isLiteStorageMode || isLiteOrModdedApp(packageName, appName)
        com.example.service.AutoCleanAccessibilityService.startAutoClean(
            context = context,
            packageName = packageName,
            appName = appName,
            isLiteMode = effectiveIsLite
        )
    }

    /**
     * Clears local application cache files in the background
     */
    fun clearSelfCache(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
            true
        } catch (_: Exception) {
            false
        }
    }
}
