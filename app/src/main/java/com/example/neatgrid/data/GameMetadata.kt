package com.example.neatgrid.data

import org.json.JSONArray
import org.json.JSONObject

data class GameMetadata(
    val title: String,
    val summary: String?,
    val rating: Double?,
    val releaseDate: String?,
    val genres: List<String>,
    val platforms: List<String>,
    val coverUrl: String?,
    val screenshotUrls: List<String>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("title", title)
        json.put("summary", summary ?: JSONObject.NULL)
        json.put("rating", rating ?: JSONObject.NULL)
        json.put("releaseDate", releaseDate ?: JSONObject.NULL)
        json.put("genres", JSONArray(genres))
        json.put("platforms", JSONArray(platforms))
        json.put("coverUrl", coverUrl ?: JSONObject.NULL)
        json.put("screenshotUrls", JSONArray(screenshotUrls))
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): GameMetadata {
            val title = json.getString("title")
            val summary = if (json.isNull("summary")) null else json.getString("summary")
            val rating = if (json.isNull("rating")) null else json.getDouble("rating")
            val releaseDate = if (json.isNull("releaseDate")) null else json.getString("releaseDate")

            val genresList = mutableListOf<String>()
            val genresArr = json.getJSONArray("genres")
            for (i in 0 until genresArr.length()) {
                genresList.add(genresArr.getString(i))
            }

            val platformsList = mutableListOf<String>()
            val platformsArr = json.getJSONArray("platforms")
            for (i in 0 until platformsArr.length()) {
                platformsList.add(platformsArr.getString(i))
            }

            val coverUrl = if (json.isNull("coverUrl")) null else json.getString("coverUrl")

            val screenshotsList = mutableListOf<String>()
            val screenshotsArr = json.getJSONArray("screenshotUrls")
            for (i in 0 until screenshotsArr.length()) {
                screenshotsList.add(screenshotsArr.getString(i))
            }

            return GameMetadata(
                title = title,
                summary = summary,
                rating = rating,
                releaseDate = releaseDate,
                genres = genresList,
                platforms = platformsList,
                coverUrl = coverUrl,
                screenshotUrls = screenshotsList
            )
        }
    }
}
