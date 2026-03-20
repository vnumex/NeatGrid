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
                val label = resolveInfo.loadLabel(pm).toString()
                val packageName = resolveInfo.activityInfo.packageName
                val icon = resolveInfo.loadIcon(pm)
                AppInfo(label = label, packageName = packageName, icon = icon)
            }
            .sortedWith(compareBy(collator) { it.label })
    }
}