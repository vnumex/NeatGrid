package com.example.neatgrid.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.text.Collator

class AppsRepository(private val context: Context) {
    fun getLaunchableApps(): List<AppInfo> {
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val activities = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val collator = Collator.getInstance()

        return activities
            .map { resolveInfo ->
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
                val packageName = resolveInfo.activityInfo.packageName
                val icon = resolveInfo.loadIcon(pm)
                AppInfo(label = label, packageName = packageName, icon = icon)
            }
            .sortedWith(compareBy(collator) { it.label })
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

        var label = resolveInfo.loadLabel(pm).toString()
        if (label.startsWith("@")) {
            try {
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
        val icon = resolveInfo.loadIcon(pm)
        return AppInfo(label = label, packageName = packageName, icon = icon)
    }
}