package com.example.data.repository

import android.content.Context
import com.example.data.model.Channel
import com.example.data.parser.M3UParser
import com.example.utils.Constants
import com.example.utils.PreferenceManager
import com.example.utils.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelRepository(private val context: Context) {
    private val preferenceManager = PreferenceManager(context)

    suspend fun getChannels(forceRefresh: Boolean = false): List<Channel> = withContext(Dispatchers.IO) {
        if (!context.isNetworkAvailable()) {
            return@withContext preferenceManager.getChannels()
        }

        val cached = preferenceManager.getChannels()
        if (cached.isNotEmpty() && !forceRefresh) {
            return@withContext cached
        }

        val parsed = try {
            M3UParser.parse(Constants.M3U_URL)
        } catch (e: Exception) {
            emptyList()
        }

        if (parsed.isNotEmpty()) {
            preferenceManager.saveChannels(parsed)
            return@withContext parsed
        }

        return@withContext cached
    }
}
