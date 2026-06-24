package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Channel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ucih4_tv_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Channel::class.java)
    private val adapter = moshi.adapter<List<Channel>>(listType)

    fun saveChannels(channels: List<Channel>) {
        try {
            val json = adapter.toJson(channels)
            prefs.edit().putString("cached_channels", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getChannels(): List<Channel> {
        val json = prefs.getString("cached_channels", null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
