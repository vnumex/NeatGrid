package com.example.neatgrid.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import java.text.Collator

class AppsRepository(private val context: Context) {
    fun getLaunchableApps(): List<AppInfo> {
        return getLaunchableActivities()
            .map { resolveInfo -> resolveInfo.toAppInfo() }
            .sortedByLabel()
    }

    fun getInstalledGames(): List<AppInfo> {
        return getLaunchableActivities()
            .filter { resolveInfo ->
                val applicationInfo = resolveInfo.activityInfo.applicationInfo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    @Suppress("DEPRECATION")
                    applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0
                }
            }
            .map { resolveInfo -> resolveInfo.toAppInfo() }
            .distinctBy { it.packageName }
            .sortedByLabel()
    }

    private fun getLaunchableActivities(): List<ResolveInfo> {
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }

    private fun ResolveInfo.toAppInfo(): AppInfo {
        val pm = context.packageManager
        val packageName = activityInfo.packageName
        val icon = loadIcon(pm)
        return AppInfo(label = loadCleanLabel(this), packageName = packageName, icon = icon)
    }

    fun getAppInfo(packageName: String): AppInfo? {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return null
        val resolveInfo = if (Build.VERSION.SDK_INT >= 33) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, 0)
        } ?: return null

        val icon = resolveInfo.loadIcon(pm)
        return AppInfo(label = loadCleanLabel(resolveInfo), packageName = packageName, icon = icon)
    }

    private fun loadCleanLabel(resolveInfo: ResolveInfo): String {
        val pm = context.packageManager
        var label = resolveInfo.loadLabel(pm).toString()
        if (label.startsWith("@")) {
            try {
                val packageName = resolveInfo.activityInfo.packageName
                val resources = pm.getResourcesForApplication(packageName)
                val resName = label.substringAfter("@")
                val resId = resources.getIdentifier(resName, null, packageName)
                if (resId != 0) {
                    label = resources.getString(resId)
                } else {
                    val labelRes = resolveInfo.activityInfo.labelRes.takeIf { it != 0 }
                        ?: resolveInfo.activityInfo.applicationInfo.labelRes
                    if (labelRes != 0) {
                        label = resources.getString(labelRes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return label
    }

    private fun List<AppInfo>.sortedByLabel(): List<AppInfo> {
        val collator = Collator.getInstance()
        return sortedWith(compareBy(collator) { it.label })
    }
}
