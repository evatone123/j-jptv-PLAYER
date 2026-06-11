package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.EpgProgramEntity
import com.example.data.model.PlaylistEntity
import com.example.data.repository.IptvRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = IptvRepository(db.iptvDao())

    // --- Exposed Lists ---
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<ChannelEntity>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels: StateFlow<List<ChannelEntity>> = repository.getRecentChannels(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<String>> = repository.channelGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    // --- Loading State ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _activeChannel = MutableStateFlow<ChannelEntity?>(null)
    val activeChannel: StateFlow<ChannelEntity?> = _activeChannel.asStateFlow()

    private val _refreshStatusMessage = MutableStateFlow<String?>(null)
    val refreshStatusMessage: StateFlow<String?> = _refreshStatusMessage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    // --- Master Channels Core Filter ---
    val filteredChannels: StateFlow<List<ChannelEntity>> = combine(
        repository.allChannels,
        _searchQuery,
        _selectedGroup,
        _selectedPlaylistId
    ) { all, query, group, playlistId ->
        var list = all
        if (playlistId != null) {
            list = list.filter { it.playlistId == playlistId }
        }
        if (group != null) {
            list = list.filter { it.groupTitle == group }
        }
        if (query.isNotBlank()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Multi-Screen viewing states ---
    // Allow up to 4 concurrent players
    private val _multiScreenStreams = MutableStateFlow<List<ChannelEntity?>>(listOf(null, null, null, null))
    val multiScreenStreams: StateFlow<List<ChannelEntity?>> = _multiScreenStreams.asStateFlow()

    private val _multiScreenLayout = MutableStateFlow(2) // 1 = Single, 2 = Split (2 streams), 4 = Grid (4 streams)
    val multiScreenLayout: StateFlow<Int> = _multiScreenLayout.asStateFlow()

    private val _focusedAudioIndex = MutableStateFlow(0) // Index of stream whose audio is active
    val focusedAudioIndex: StateFlow<Int> = _focusedAudioIndex.asStateFlow()

    init {
        // Automatically preheat with default embedded playlist if none exists
        viewModelScope.launch {
            // Keep a tiny hold to let the DB stream initialize
            val currentPlaylists = repository.playlists.first()
            if (currentPlaylists.isEmpty()) {
                val dId = repository.addPlaylist(
                    name = "IPTV GitHub Index",
                    url = "https://iptv-org.github.io/iptv/index.m3u"
                )
                refreshPlaylistData(dId, "https://iptv-org.github.io/iptv/index.m3u")
            } else {
                // Pre-select the first playlist if available
                _selectedPlaylistId.value = currentPlaylists.first().id
            }
            
            // Auto-select first channel from database as active
            repository.allChannels.first().firstOrNull()?.let { firstChan ->
                _activeChannel.value = firstChan
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun selectPlaylist(playlistId: Long?) {
        _selectedPlaylistId.value = playlistId
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun fetchUpcomingSchedule(channelTvgId: String): Flow<List<EpgProgramEntity>> {
        val tvgIdToQuery = if (channelTvgId.isBlank()) "Unknown" else channelTvgId
        return repository.getUpcomingPrograms(tvgIdToQuery, System.currentTimeMillis())
    }

    fun addNewCustomPlaylist(name: String, url: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshStatusMessage.value = "Adding playlists..."
            try {
                val newId = repository.addPlaylist(name, url)
                refreshPlaylistData(newId, url)
            } catch (e: Exception) {
                _refreshStatusMessage.value = "Failed: ${e.localizedMessage}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (_selectedPlaylistId.value == playlist.id) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun refreshPlaylistData(playlistId: Long, url: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshStatusMessage.value = "Downloading channels..."
            val result = repository.refreshPlaylist(playlistId, url)
            if (result.isSuccess) {
                val size = result.getOrNull() ?: 0
                _refreshStatusMessage.value = "Successfully parsed $size channels!"
                _selectedPlaylistId.value = playlistId
                
                // Auto-select first channel space if nothing is active
                if (_activeChannel.value == null) {
                    repository.getChannelsForPlaylist(playlistId).first().firstOrNull()?.let { firstChan ->
                        _activeChannel.value = firstChan
                    }
                }
            } else {
                _refreshStatusMessage.value = "Failed: ${result.exceptionOrNull()?.message}"
            }
            _isRefreshing.value = false
        }
    }

    fun downloadAndParseEpg(epgUrl: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshStatusMessage.value = "Downloading XMLTV EPG data..."
            try {
                val result = repository.refreshXmltvEpg(epgUrl)
                if (result.isSuccess) {
                    val size = result.getOrNull() ?: 0
                    _refreshStatusMessage.value = "Successfully parsed $size EPG programs!"
                } else {
                    _refreshStatusMessage.value = "Failed to load XMLTV EPG: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _refreshStatusMessage.value = "Failed to parse XMLTV: ${e.localizedMessage}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearAllEpg() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshStatusMessage.value = "Clearing EPG database..."
            try {
                repository.clearAllEpgPrograms()
                _refreshStatusMessage.value = "EPG database cleared. Showing empty guide."
            } catch (e: Exception) {
                _refreshStatusMessage.value = "Failed to clear: ${e.localizedMessage}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectChannel(channel: ChannelEntity?) {
        _activeChannel.value = channel
        channel?.let {
            viewModelScope.launch {
                repository.markChannelAsWatched(it.id)
            }
        }
    }

    // --- Multi-Screen actions ---
    fun selectLayout(grids: Int) {
        if (grids in listOf(1, 2, 4)) {
            _multiScreenLayout.value = grids
        }
    }

    fun setStreamInGrid(index: Int, channel: ChannelEntity?) {
        if (index in 0..3) {
            val newList = _multiScreenStreams.value.toMutableList()
            newList[index] = channel
            _multiScreenStreams.value = newList
            if (channel != null) {
                viewModelScope.launch {
                    repository.markChannelAsWatched(channel.id)
                }
                // Select audio automatically if we populate the focused index
                if (_multiScreenStreams.value[_focusedAudioIndex.value] == null) {
                    _focusedAudioIndex.value = index
                }
            }
        }
    }

    fun setFocusedAudio(index: Int) {
        if (index in 0..3) {
            _focusedAudioIndex.value = index
        }
    }

    fun clearGridSlot(index: Int) {
        if (index in 0..3) {
            val newList = _multiScreenStreams.value.toMutableList()
            newList[index] = null
            _multiScreenStreams.value = newList
        }
    }
}
