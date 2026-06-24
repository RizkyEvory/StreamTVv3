package com.example.data.parser

import com.example.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object M3UParser {
    suspend fun parse(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 11; TV)")
                    .build()
                chain.proceed(request)
            }
            .build()

        val request = Request.Builder().url(url).build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            null
        }

        val body = response?.body?.string() ?: return@withContext emptyList()

        val channels = mutableListOf<Channel>()
        val lines = body.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF")) {
                val name = line.substringAfterLast(",").trim()
                val logo = extractAttr(line, "tvg-logo")
                val group = extractAttr(line, "group-title") ?: "Lainnya"
                val id = extractAttr(line, "tvg-id") ?: name
                
                // Find next line that contains the url and is not empty/metadata
                var streamUrl = ""
                var nextIndex = i + 1
                while (nextIndex < lines.size) {
                    val possibleUrl = lines[nextIndex].trim()
                    if (possibleUrl.isNotEmpty()) {
                        if (!possibleUrl.startsWith("#")) {
                            streamUrl = possibleUrl
                            break
                        } else {
                            // Another tag metadata before URL
                            if (possibleUrl.startsWith("#EXTINF")) {
                                break // Next channel started without URL for this one
                            }
                        }
                    }
                    nextIndex++
                }

                if (streamUrl.isNotEmpty()) {
                    channels.add(Channel(id, name, streamUrl, logo ?: "", group))
                    i = nextIndex + 1
                    continue
                }
            }
            i++
        }
        channels
    }

    private fun extractAttr(line: String, attr: String): String? {
        val regex = Regex("""$attr="([^"]*)"""")
        return regex.find(line)?.groupValues?.get(1)
    }
}
