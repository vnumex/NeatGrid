package com.example.neatgrid.data

import org.json.JSONArray
import org.jsoup.nodes.Document

object LaunchBoxImages {

    fun fullSizeNames(doc: Document): Map<String, String> {
        val payload = doc.selectFirst("script#__NUXT_DATA__")?.data() ?: return emptyMap()
        return runCatching { fullSizeNames(JSONArray(payload)) }.getOrDefault(emptyMap())
    }

    fun fullSizeNames(payload: JSONArray): Map<String, String> {
        val names = mutableMapOf<String, String>()
        for (i in 0 until payload.length()) {
            val entry = payload.optJSONObject(i) ?: continue
            if (!entry.has("imageFileName") || !entry.has("fullGameImageFileName")) continue
            val thumbnail = payload.stringAt(entry.optInt("imageFileName", -1)) ?: continue
            val fullSize = payload.stringAt(entry.optInt("fullGameImageFileName", -1)) ?: continue
            if (thumbnail != fullSize) names[thumbnail] = fullSize
        }
        return names
    }

    fun toFullSize(url: String, fullSizeNames: Map<String, String>): String {
        val fullName = fullSizeNames[url.substringAfterLast('/')] ?: return url
        return url.substringBeforeLast('/') + "/" + fullName
    }

    fun isLaunchBoxImage(url: String?): Boolean {
        if (url == null) return false
        return url.contains("launchbox-app.com") || url.contains("launchbox.gg")
    }

    private fun JSONArray.stringAt(index: Int): String? {
        if (index !in 0 until length()) return null
        return (opt(index) as? String)?.takeIf { it.isNotBlank() }
    }
}
