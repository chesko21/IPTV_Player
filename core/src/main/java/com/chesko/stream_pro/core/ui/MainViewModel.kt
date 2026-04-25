package com.chesko.stream_pro.core.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.chesko.stream_pro.core.data.local.AppDatabase
import com.chesko.stream_pro.core.data.local.ChannelRepository
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.data.model.EpgProgram
import com.chesko.stream_pro.core.data.parser.M3uParser
import com.chesko.stream_pro.core.data.parser.EpgParser
import com.chesko.stream_pro.core.utils.NetworkObserver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ChannelRepository
    private val prefs = application.getSharedPreferences("iptv_player_prefs", Context.MODE_PRIVATE)

    val allChannels: StateFlow<List<IptvChannel>>
    val favoriteChannels: StateFlow<List<IptvChannel>>
    val recentlyPlayed: StateFlow<List<IptvChannel>>

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup

    private val _categoryFilter = MutableStateFlow<String?>(null) 
    val categoryFilter: StateFlow<String?> = _categoryFilter

    private val _selectedChannel = MutableStateFlow<IptvChannel?>(null)
    val selectedChannel: StateFlow<IptvChannel?> = _selectedChannel

    private val _lastUrl = MutableStateFlow("")
    val lastUrl: StateFlow<String> = _lastUrl

    private val _hwAcceleration = MutableStateFlow(prefs.getBoolean("hw_acceleration", true))
    val hwAcceleration: StateFlow<Boolean> = _hwAcceleration

    private val _bufferSize = MutableStateFlow(prefs.getInt("buffer_size", 15))
    val bufferSize: StateFlow<Int> = _bufferSize

    private val _autoQuality = MutableStateFlow(prefs.getBoolean("auto_quality", true))
    val autoQuality: StateFlow<Boolean> = _autoQuality

    private val _maxVideoHeight = MutableStateFlow(prefs.getInt("max_video_height", 0)) // 0 means Auto
    val maxVideoHeight: StateFlow<Int> = _maxVideoHeight

    private val _playerEngine = MutableStateFlow(prefs.getString("player_engine", "EXO") ?: "EXO")
    val playerEngine: StateFlow<String> = _playerEngine

    private val _audioBoost = MutableStateFlow(prefs.getBoolean("audio_boost", false))
    val audioBoost: StateFlow<Boolean> = _audioBoost

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode_permanent", true))
    val darkMode: StateFlow<Boolean> = _darkMode

    private val _accentColor = MutableStateFlow(prefs.getInt("accent_color", 0xFF2979FF.toInt()))
    val accentColor: StateFlow<Int> = _accentColor

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Stream Pro User") ?: "Stream Pro User")
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "user@streampro.com") ?: "user@streampro.com")
    val userEmail: StateFlow<String> = _userEmail

    private val _profileImageUri = MutableStateFlow(prefs.getString("profile_image_uri", null))
    val profileImageUri: StateFlow<String?> = _profileImageUri

    private val _deviceId = MutableStateFlow(android.os.Build.ID)
    val deviceId: StateFlow<String> = _deviceId

    private val _memberSince = MutableStateFlow(getSavedMemberSince())
    val memberSince: StateFlow<String> = _memberSince

    private val networkObserver = NetworkObserver(application)
    val networkStatus = networkObserver.networkStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NetworkObserver.NetworkStatus.Available
    )

    private fun getSavedMemberSince(): String {
        val saved = prefs.getString("member_since", null)
        if (saved != null) return saved

        return try {
            val installTime = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0).firstInstallTime
            val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            val formattedDate = sdf.format(java.util.Date(installTime))
            
            prefs.edit().putString("member_since", formattedDate).apply()
            formattedDate
        } catch (e: Exception) {
            "januari 2026"
        }
    }

    companion object {
        private const val URL1 = "iuuqt;00sfcsboe/mz0VQQM3137"
        private const val URL2 = "iuuqt;00sbx/hjuivcvtfsdpoufou/dpn0njnjqjqj330mbmbkp0sfgt0ifbet0nbjo0qmbzmjtu36"
        private const val URL3 = "iuuqt;00fobl/nbmjoh/qm"
        private const val URL4 = "iuuqt;00qbtufcjo/dpn0sbx04I1CgOrv"

        private fun decryptUrl(input: String): String {
            return input.map { (it.code - 1).toChar() }.joinToString("")
        }

        val DEMO_URLS = listOf(
            decryptUrl(URL1),
            decryptUrl(URL2),
            decryptUrl(URL3),
            decryptUrl(URL4)
        )
        val BASE_URL = DEMO_URLS[0]

        val GLOBAL_EPG_URLS = listOf<String>(
            "https://epgshare01.online/epgshare01/epg_ripper_ID1.xml.gz",
            "https://epgshare01.online/epgshare01/epg_ripper_ALL_SOURCES1.xml.gz",
            "https://warningfm.github.io/x1/epg/guide.xml.gz",
            "https://raw.githubusercontent.com/apistech/project/refs/heads/main/ApisTECH.xml",
            "https://www.open-epg.com/files/indonesia1.xml",
            "https://www.open-epg.com/files/indonesia2.xml",
            "https://www.open-epg.com/files/indonesia3.xml",
            "https://www.open-epg.com/files/indonesia4.xml",
            "https://www.open-epg.com/files/indonesia5.xml",
            "https://www.open-epg.com/files/indonesia6.xml",
            "https://raw.githubusercontent.com/AqFad2811/epg/refs/heads/main/indonesia.xml",
            "https://tinyurl.com/DrewLive002-epg"
        )
    }

    private val _randomCarouselChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val randomCarouselChannels: StateFlow<List<IptvChannel>> = _randomCarouselChannels

    init {
        val database = AppDatabase.getDatabase(application)
        val channelDao = database.channelDao()
        val epgDao = database.epgDao()
        repository = ChannelRepository(channelDao, epgDao)

        _lastUrl.value = prefs?.getString("last_m3u_url", "") ?: ""

        checkAndClearCache()

        allChannels = repository.allChannels.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        favoriteChannels = repository.favoriteChannels.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        recentlyPlayed = repository.recentlyPlayed.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    val filteredChannels = combine(
        allChannels,
        _searchQuery,
        _selectedGroup,
        _categoryFilter
    ) { channels, query, group, category ->
        channels.filter { channel ->
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                channel.name.contains(query, ignoreCase = true) ||
                        channel.group?.contains(query, ignoreCase = true) == true
            }
            
            val matchesCategory = when (category) {
                "movies" -> channel.group?.contains("MOVIE", ignoreCase = true) == true || 
                           channel.group?.contains("FILM", ignoreCase = true) == true ||
                           channel.group?.contains("VOD", ignoreCase = true) == true ||
                           channel.group?.contains("SERIES", ignoreCase = true) == true || 
                           channel.group?.contains("TV SHOW", ignoreCase = true) == true ||
                           channel.group?.contains("SEASON", ignoreCase = true) == true
                "sport" -> channel.group?.contains("SPORT", ignoreCase = true) == true || 
                           channel.group?.contains("BOLA", ignoreCase = true) == true ||
                           channel.group?.contains("BEIN", ignoreCase = true) == true ||
                           channel.group?.contains("ESPN", ignoreCase = true) == true ||
                           channel.group?.contains("LIGA", ignoreCase = true) == true ||
                           channel.group?.contains("FOOTBALL", ignoreCase = true) == true ||
                           channel.group?.contains("BASKETBALL", ignoreCase = true) == true ||
                           channel.group?.contains("HOCKEY", ignoreCase = true) == true ||
                           channel.group?.contains("TENNIS", ignoreCase = true) == true ||
                           channel.group?.contains("VOLLEY", ignoreCase = true) == true ||
                           channel.group?.contains("BADMINTON", ignoreCase = true) == true ||
                           channel.group?.contains("ATHLETICS", ignoreCase = true) == true ||
                           channel.group?.contains("EVENT", ignoreCase = true) == true ||
                           channel.group?.contains("SOCCER", ignoreCase = true) == true
                "live" -> !(channel.group?.contains("MOVIE", ignoreCase = true) == true || 
                           channel.group?.contains("SERIES", ignoreCase = true) == true ||
                           channel.group?.contains("VOD", ignoreCase = true) == true ||
                           channel.group?.contains("FILM", ignoreCase = true) == true ||
                           channel.group?.contains("SPORT", ignoreCase = true) == true)
                else -> true
            }

            val matchesGroup = when (group) {
                null -> true
                "Other" -> channel.group.isNullOrBlank()
                else -> channel.group == group
            }
            
            matchesSearch && matchesGroup && matchesCategory
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val groups = allChannels.map { channels ->
        val existingGroups = channels.mapNotNull { it.group }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        
        val hasNoGroup = channels.any { it.group.isNullOrBlank() }
        if (hasNoGroup) {
            existingGroups + "Other"
        } else {
            existingGroups
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _isCheckingConnection = MutableStateFlow(false)
    val isCheckingConnection: StateFlow<Boolean> = _isCheckingConnection

    private val _connectionStatus = MutableStateFlow<Boolean?>(null)
    val connectionStatus: StateFlow<Boolean?> = _connectionStatus

    fun checkConnection(url: String) {
        viewModelScope.launch {
            _isCheckingConnection.value = true
            _connectionStatus.value = null
            try {
                val isValid = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    response.isSuccessful
                }
                _connectionStatus.value = isValid
                if (!isValid) {
                    _errorMessage.value = "Invalid M3U URL. Please check the URL and try again."
                }
            } catch (e: Exception) {
                _connectionStatus.value = false
                _errorMessage.value = "Connection failed: ${e.message}"
            } finally {
                _isCheckingConnection.value = false
            }
        }
    }

    fun refreshPlaylist() {
        loadPlaylist(_lastUrl.value) {
            _errorMessage.value = "Playlist berhasil diperbarui"
        }
    }

    fun loadPlaylist(url: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _lastUrl.value = url

            try {
                val m3uContent = when {
                    url.startsWith("http") -> fetchRawContent(url)
                    url.startsWith("file://") -> {
                        val filePath = url.substring(7)
                        withContext(Dispatchers.IO) { File(filePath).readText() }
                    }
                    else -> url
                }
                processM3uContent(m3uContent, url, onSuccess)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load playlist: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadPlaylistFromFile(content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val internalFile = File(getApplication<Application>().filesDir, "last_local_playlist.m3u")
                withContext(Dispatchers.IO) {
                    internalFile.writeText(content)
                }
                processM3uContent(content, "file://${internalFile.absolutePath}", onSuccess)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save/parse file: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun processM3uContent(content: String, urlSource: String? = null, onSuccess: () -> Unit) {
        val channels = M3uParser.parse(content)

        if (channels.isEmpty()) {
            _errorMessage.value = "No channels found in the playlist."
            _isLoading.value = false
        } else {
            // Reset cache if source is changing
            val previousUrl = prefs.getString("last_m3u_url", "")
            if (urlSource != null && urlSource != previousUrl && previousUrl?.isNotBlank() == true) {
                repository.clearEpg()
                repository.clearRecentlyPlayed()
            }

            repository.syncChannels(channels)
            _randomCarouselChannels.value = channels.shuffled().take(10)
            _errorMessage.value = null

            urlSource?.let {
                _lastUrl.value = it
                prefs?.edit()?.apply {
                    putString("last_m3u_url", it)
                    putLong("last_m3u_update", System.currentTimeMillis())
                    apply()
                }
            }

            withContext(Dispatchers.Main) {
                onSuccess()
                delay(1500)
                _isLoading.value = false
            }

            val epgUrls = (M3uParser.extractEpgUrls(content) + GLOBAL_EPG_URLS).distinct()
            if (epgUrls.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        // Ambil EPG dari playlist (jika ada) + Global EPG
                        // Batasi jumlah sumber agar tidak terlalu berat (max 5)
                        epgUrls.take(5).forEach { epgUrl ->
                            try {
                                fetchEpg(epgUrl) { batch ->
                                    repository.insertEpgBatch(batch)
                                }
                            } catch (e: Exception) {
                                // Silently skip failed EPG sources
                            }
                        }
                        repository.cleanupOldEpg()
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    private suspend fun fetchRawContent(url: String): String {
        return withContext(Dispatchers.IO) {
            val client = createHttpClient()
            val request = Request.Builder().url(url).header("User-Agent", "IPTVPlayer/2.0").build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }

    private suspend fun fetchEpg(url: String, onBatch: suspend (List<EpgProgram>) -> Unit) {
        withContext(Dispatchers.IO) {
            val client = createHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("EPG Download Failed")
            response.body?.byteStream()?.use { EpgParser.parse(it, onBatch) }
        }
    }

    private fun createHttpClient() = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private suspend fun fetchM3u(url: String): List<IptvChannel> {
        val content = fetchRawContent(url)
        return M3uParser.parse(content)
    }

    fun toggleFavorite(channel: IptvChannel) {
        viewModelScope.launch {
            val targetUrl = channel.url.trim()
            // Cek langsung ke database apakah channel ini saat ini favorit
            val existingChannel = repository.getChannelByUrl(targetUrl)
            val currentlyFavorite = existingChannel?.isFavorite ?: false
            
            // Toggle statusnya
            repository.updateFavoriteStatusByUrl(targetUrl, !currentlyFavorite)
        }
    }

    fun removeFromFavorites(url: String) {
        viewModelScope.launch {
            repository.updateFavoriteStatusByUrl(url, false)
        }
    }

    fun markAsPlayed(channel: IptvChannel) {
        viewModelScope.launch {
            repository.markAsPlayed(channel)
        }
    }

    fun clearRecentlyPlayed() {
        viewModelScope.launch {
            repository.clearRecentlyPlayed()
        }
    }

    fun getProgramsForChannel(tvgId: String?): Flow<List<EpgProgram>> {
        if (tvgId.isNullOrBlank()) return flowOf(emptyList())
        return repository.getProgramsForChannel(tvgId)
    }

    fun getCurrentProgram(tvgId: String?): Flow<EpgProgram?> {
        if (tvgId.isNullOrBlank()) return flowOf(null)
        return repository.getCurrentProgram(tvgId)
    }

    fun getNextProgram(tvgId: String?): Flow<EpgProgram?> {
        if (tvgId.isNullOrBlank()) return flowOf(null)
        return repository.getNextProgram(tvgId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        _selectedGroup.value = null
    }

    fun setSelectedChannel(channel: IptvChannel?) {
        _selectedChannel.value = channel
    }

    fun saveBackupToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val xmlContent = exportChannelsToXml()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(xmlContent.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Berhasil mencadangkan data"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Gagal mencadangkan: ${e.message}"
                }
            }
        }
    }

    fun saveBackupXml(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val xmlContent = exportChannelsToXml()
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val fileName = "iptv_backup_$timestamp.xml"

                val backupDir = getApplication<Application>().getExternalFilesDir("Backups")
                if (backupDir?.exists() == false) backupDir.mkdirs()
                
                val file = File(backupDir, fileName)
                file.writeText(xmlContent)
                
                withContext(Dispatchers.Main) {
                    onComplete(file.absolutePath)
                    _errorMessage.value = "Backup success: $fileName"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Backup failed: ${e.message}"
                }
            }
        }
    }

    fun exportChannelsToXml(): String {
        val channels = allChannels.value
        val xmlBuilder = StringBuilder()
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xmlBuilder.append("<playlist>\n")
        
        channels.forEach { channel ->
            xmlBuilder.append("    <channel>\n")
            xmlBuilder.append("        <name>${escapeXml(channel.name)}</name>\n")
            xmlBuilder.append("        <url>${escapeXml(channel.url)}</url>\n")
            xmlBuilder.append("        <logo>${escapeXml(channel.logo ?: "")}</logo>\n")
            xmlBuilder.append("        <group>${escapeXml(channel.group ?: "Uncategorized")}</group>\n")
            xmlBuilder.append("        <tvgId>${escapeXml(channel.tvgId ?: "")}</tvgId>\n")
            xmlBuilder.append("        <userAgent>${escapeXml(channel.userAgent ?: "")}</userAgent>\n")
            xmlBuilder.append("        <drmConfig>${escapeXml(channel.drmConfig ?: "")}</drmConfig>\n")
            xmlBuilder.append("    </channel>\n")
        }
        
        xmlBuilder.append("</playlist>")
        return xmlBuilder.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun loadDemoPlaylist(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val allDemoChannels = mutableListOf<IptvChannel>()
                val allEpgUrls = mutableSetOf<String>()

                DEMO_URLS.forEach { url ->
                    try {
                        val content = fetchRawContent(url)
                        val channels = M3uParser.parse(content)
                        allDemoChannels.addAll(channels)
                        allEpgUrls.addAll(M3uParser.extractEpgUrls(content))
                    } catch (e: Exception) {
                        // Skip failed source in demo
                    }
                }

                if (allDemoChannels.isEmpty()) {
                    _errorMessage.value = "Gagal memuat playlist demo"
                    _isLoading.value = false
                    return@launch
                }

                if (allDemoChannels.isNotEmpty()) {
                    repository.syncChannels(allDemoChannels)
                    _randomCarouselChannels.value = allDemoChannels.shuffled().take(10)
                }

                _lastUrl.value = "combined_demo"
                prefs.edit().apply {
                    putString("last_m3u_url", "combined_demo")
                    putLong("last_m3u_update", System.currentTimeMillis())
                    apply()
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                    delay(1500)
                    _isLoading.value = false
                }

                // Gunakan EPG dari playlist + GLOBAL_EPG_URLS
                val epgUrls = (allEpgUrls + GLOBAL_EPG_URLS).distinct()
                if (epgUrls.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        epgUrls.take(5).forEach { epgUrl ->
                            try {
                                fetchEpg(epgUrl) { batch ->
                                    repository.insertEpgBatch(batch)
                                }
                            } catch (e: Exception) {}
                        }
                        repository.cleanupOldEpg()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat demo: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun checkAndClearCache() {
        val lastUpdate = prefs.getLong("last_m3u_update", 0L)
        val lastUrlStored = prefs.getString("last_m3u_url", "")
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000

        if (currentTime - lastUpdate > twentyFourHoursInMillis) {
            if (lastUrlStored == "combined_demo") {
                loadDemoPlaylist()
            } else if (lastUrlStored?.startsWith("http") == true) {
                loadPlaylist(lastUrlStored)
            }
        }
    }

    fun deleteCurrentPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.clearAllChannels()
            repository.clearEpg()
            _lastUrl.value = ""
            prefs.edit().apply {
                remove("last_m3u_url")
                remove("last_m3u_update")
                apply()
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(name: String, email: String, imageUri: String? = null) {
        _userName.value = name
        _userEmail.value = email
        _profileImageUri.value = imageUri ?: _profileImageUri.value
        prefs.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("profile_image_uri", _profileImageUri.value)
            apply()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setHwAcceleration(enabled: Boolean) {
        _hwAcceleration.value = enabled
        prefs.edit().putBoolean("hw_acceleration", enabled).apply()
    }

    fun setBufferSize(seconds: Int) {
        _bufferSize.value = seconds
        prefs.edit().putInt("buffer_size", seconds).apply()
    }

    fun setAutoQuality(enabled: Boolean) {
        _autoQuality.value = enabled
        prefs.edit().putBoolean("auto_quality", enabled).apply()
    }

    fun setMaxVideoHeight(height: Int) {
        _maxVideoHeight.value = height
        prefs.edit().putInt("max_video_height", height).apply()
    }

    fun setPlayerEngine(engine: String) {
        _playerEngine.value = engine
        prefs.edit().putString("player_engine", engine).apply()
    }

    fun setAudioBoost(enabled: Boolean) {
        _audioBoost.value = enabled
        prefs.edit().putBoolean("audio_boost", enabled).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit().putBoolean("dark_mode_permanent", enabled).apply()
    }

    fun setAccentColor(color: Int) {
        _accentColor.value = color
        prefs.edit().putInt("accent_color", color).apply()
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            repository.clearEpg()
            getApplication<Application>().cacheDir.deleteRecursively()
            _isLoading.value = false
            _errorMessage.value = "Cache berhasil dihapus"
        }
    }
}
