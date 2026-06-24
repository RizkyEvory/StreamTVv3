package com.example.ui.player

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.R
import com.example.data.model.Channel
import com.example.databinding.ActivityPlayerBinding
import com.example.ui.channel.CategoryAdapter
import com.example.ui.channel.ChannelAdapter
import com.example.utils.isNetworkAvailable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()

    private var player: ExoPlayer? = null
    private var isPanelOpen = false

    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var retryCount = 0
    private val maxRetries = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Channels RecyclerView
        channelAdapter = ChannelAdapter(emptyList()) { channel ->
            viewModel.setActiveChannel(channel)
            closePanel()
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = channelAdapter

        // Categories RecyclerView
        categoryAdapter = CategoryAdapter(emptyList()) { category ->
            viewModel.selectCategory(category)
        }
        binding.rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = categoryAdapter

        // Search Bar filter
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Retry Button
        binding.btnRetry.setOnClickListener {
            viewModel.loadChannels(forceRefresh = true)
        }

        // Initially position panel offscreen
        binding.sidePanel.post {
            val panelWidth = resources.getDimension(R.dimen.panel_width)
            binding.sidePanel.translationX = -panelWidth
            binding.sidePanel.visibility = View.GONE
        }

        // Tap/click player view to toggle panel
        binding.playerView.setOnClickListener {
            if (isPanelOpen) {
                closePanel()
            } else {
                openPanel()
            }
        }
    }

    private fun setupObservers() {
        // 1. Is Loading
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                showLoading(isLoading)
            }
        }

        // 2. Error Message
        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { error ->
                if (error != null) {
                    showError(error)
                } else {
                    hideError()
                }
            }
        }

        // 3. Channels list
        lifecycleScope.launch {
            viewModel.filteredChannels.collectLatest { channels ->
                channelAdapter.updateData(channels)
                if (channels.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
        }

        // 4. Categories list
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                categoryAdapter.updateData(categories)
            }
        }

        // 5. Selected Category
        lifecycleScope.launch {
            viewModel.selectedCategory.collectLatest { category ->
                categoryAdapter.setSelectedCategory(category)
            }
        }

        // 6. Active Channel
        lifecycleScope.launch {
            viewModel.activeChannel.collectLatest { channel ->
                if (channel != null) {
                    playChannel(channel)
                }
            }
        }
    }

    private fun playChannel(channel: Channel) {
        retryCount = 0
        hideError()
        showLoading(true)

        // Update Overlay info
        binding.activeChannelName.text = channel.name
        if (channel.logoUrl.isNotEmpty()) {
            binding.activeChannelLogo.visibility = View.VISIBLE
            Glide.with(this)
                .load(channel.logoUrl)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(binding.activeChannelLogo)
        } else {
            binding.activeChannelLogo.setImageResource(R.mipmap.ic_launcher_round)
            binding.activeChannelLogo.visibility = View.VISIBLE
        }

        channelAdapter.setActiveChannel(channel.id)

        // Create Custom OkHttp DataSource with custom headers (User-Agent, Referer)
        val httpDataSourceFactory = OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        ).setDefaultRequestProperties(mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 11; TV) AppleWebKit/537.36",
            "Referer" to channel.streamUrl
        ))

        player?.release()
        val playerContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            createAttributionContext("default")
        } else {
            this
        }
        player = ExoPlayer.Builder(playerContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()
            .also { exo ->
                binding.playerView.player = exo
                exo.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                exo.prepare()
                exo.playWhenReady = true
                
                exo.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        handlePlayerError(error, channel)
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> showLoading(true)
                            Player.STATE_READY -> {
                                showLoading(false)
                                hideError()
                            }
                            Player.STATE_ENDED -> tryNextChannel()
                        }
                    }
                })
            }
    }

    private fun handlePlayerError(error: PlaybackException, channel: Channel) {
        error.printStackTrace()
        if (retryCount < maxRetries) {
            retryCount++
            binding.activeChannelName.text = "Error, mengulang kembali ($retryCount/$maxRetries)..."
            binding.playerView.postDelayed({
                playChannel(channel)
            }, 2000)
        } else {
            showError("Gagal memutar ${channel.name} setelah $maxRetries kali percobaan.")
            // Wait 3 seconds and automatically skip to next channel
            binding.playerView.postDelayed({
                tryNextChannel()
            }, 3000)
        }
    }

    private fun tryNextChannel() {
        val nextChannel = viewModel.tryNextChannel()
        if (nextChannel != null) {
            viewModel.setActiveChannel(nextChannel)
        } else {
            showError("Mencapai akhir playlist channel.")
        }
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true
        binding.sidePanel.visibility = View.VISIBLE
        binding.sidePanel.animate()
            .translationX(0f)
            .setDuration(300)
            .withEndAction {
                binding.rvChannels.requestFocus()
            }
            .start()
    }

    private fun closePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false
        val panelWidth = resources.getDimension(R.dimen.panel_width)
        binding.sidePanel.animate()
            .translationX(-panelWidth)
            .setDuration(300)
            .withEndAction {
                binding.sidePanel.visibility = View.GONE
                binding.playerView.requestFocus()
            }
            .start()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorContainer.visibility = View.VISIBLE
        showLoading(false)
    }

    private fun hideError() {
        binding.errorContainer.visibility = View.GONE
    }

    // Android TV Key Navigation Configuration
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!isPanelOpen) {
                    openPanel()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isPanelOpen) {
                    closePanel()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isPanelOpen) {
                    closePanel()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // Picture-in-Picture (PiP) support for Android 8.0+
    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.activeChannelOverlay.visibility = View.GONE
            binding.sidePanel.visibility = View.GONE
        } else {
            binding.activeChannelOverlay.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
