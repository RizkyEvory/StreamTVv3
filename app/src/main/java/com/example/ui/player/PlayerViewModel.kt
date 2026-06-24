package com.example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChannelRepository(application)

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    
    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())
    val filteredChannels: StateFlow<List<Channel>> = _filteredChannels.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val channels = repository.getChannels(forceRefresh)
                if (channels.isEmpty()) {
                    _errorMessage.value = "Tidak ada channel ditemukan. Periksa koneksi internet."
                } else {
                    _allChannels.value = channels
                    
                    // Extract unique categories and sort
                    val uniqueCategories = listOf("Semua") + channels.map { it.group }.distinct().sorted()
                    _categories.value = uniqueCategories
                    
                    filterChannels()

                    if (_activeChannel.value == null && channels.isNotEmpty()) {
                        _activeChannel.value = channels.first()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        filterChannels()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterChannels()
    }

    fun setActiveChannel(channel: Channel?) {
        _activeChannel.value = channel
    }

    fun tryNextChannel(): Channel? {
        val currentChannels = _filteredChannels.value
        val active = _activeChannel.value ?: return null
        val currentIndex = currentChannels.indexOfFirst { it.id == active.id }
        if (currentIndex != -1 && currentIndex < currentChannels.size - 1) {
            val nextChannel = currentChannels[currentIndex + 1]
            _activeChannel.value = nextChannel
            return nextChannel
        }
        return null
    }

    private fun filterChannels() {
        val query = _searchQuery.value.trim().lowercase()
        val category = _selectedCategory.value

        var result = _allChannels.value

        if (category != "Semua") {
            result = result.filter { it.group == category }
        }

        if (query.isNotEmpty()) {
            result = result.filter { it.name.lowercase().contains(query) }
        }

        _filteredChannels.value = result
    }
}
