package com.example.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

fun View.slideInLeft(duration: Long = 300) {
    this.visibility = View.VISIBLE
    this.animate()
        .translationX(0f)
        .setDuration(duration)
        .setListener(null)
        .start()
}

fun View.slideOutLeft(width: Float, duration: Long = 300, onEnd: () -> Unit = {}) {
    this.animate()
        .translationX(-width)
        .setDuration(duration)
        .withEndAction {
            this.visibility = View.GONE
            onEnd()
        }
        .start()
}
