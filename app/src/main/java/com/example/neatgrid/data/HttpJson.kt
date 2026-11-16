package com.example.neatgrid.data

import java.net.HttpURLConnection
import java.net.URL

internal data class HttpResult(val code: Int, val body: String)

internal object HttpJson {
    private const val TIMEOUT_MS = 10_000

    fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResult {
        val connection = open(url, "GET", headers)
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResult(code, text)
        } finally {
            connection.disconnect()
        }
    }

    fun exists(url: String): Boolean {
        val connection = open(url, "HEAD", emptyMap())
        return try {
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, method: String, headers: Map<String, String>): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
    }
}
