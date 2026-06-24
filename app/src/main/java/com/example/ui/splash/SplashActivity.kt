package com.example.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.ChannelRepository
import com.example.databinding.ActivitySplashBinding
import com.example.ui.player.PlayerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var repository: ChannelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ChannelRepository(this)

        startAnimations()
        fetchChannelsAndNavigate()
    }

    private fun startAnimations() {
        // Fade pulse animation for logo container
        val animation = AlphaAnimation(0.3f, 1.0f).apply {
            duration = 800
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = AlphaAnimation.INFINITE
        }
        binding.logoCard.startAnimation(animation)
    }

    private fun fetchChannelsAndNavigate() {
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            // Fetch parsed channels in the background (updates preference cache)
            val channels = try {
                repository.getChannels(forceRefresh = true)
            } catch (e: Exception) {
                emptyList()
            }

            // Keep splash on screen for at least 2.5 seconds to feel professional
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingTime = 2500 - elapsedTime
            if (remainingTime > 0) {
                delay(remainingTime)
            }

            if (channels.isEmpty()) {
                Toast.makeText(
                    this@SplashActivity,
                    "Koneksi lambat. Menggunakan data offline jika ada.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Transition to Player
            val intent = Intent(this@SplashActivity, PlayerActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
