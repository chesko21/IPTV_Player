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
import android.provider.Settings
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

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "in") ?: "in")
    val appLanguage: StateFlow<String> = _appLanguage

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Stream Pro User") ?: "Stream Pro User")
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "user@streampro.com") ?: "user@streampro.com")
    val userEmail: StateFlow<String> = _userEmail

    private val _profileImageUri = MutableStateFlow(prefs.getString("profile_image_uri", null))
    val profileImageUri: StateFlow<String?> = _profileImageUri

    private val _deviceId = MutableStateFlow(
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)?.uppercase() ?: "UNKNOWN"
    )
    val deviceId: StateFlow<String> = _deviceId

    private val _memberSince = MutableStateFlow(getSavedMemberSince())
    val memberSince: StateFlow<String> = _memberSince

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cache(okhttp3.Cache(
            directory = File(application.cacheDir, "http_cache"),
            maxSize = 50L * 1024L * 1024L // 50 MB
        ))
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val networkObserver = NetworkObserver(application)
    val networkStatus = networkObserver.networkStatus.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NetworkObserver.NetworkStatus.Available
    )

    private fun getSavedMemberSince(): String {
        return try {
            val installTime = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0).firstInstallTime
            val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(installTime))
        } catch (e: Exception) {
            "January 2026"
        }
    }

    companion object {
        private const val URL1 = "iuuqt;00sfcsboe/mz0VQQM3137"
        private const val URL2 = "iuuqt;00sbx/hjuivcvtfsdpoufou/dpn0njnjqjqj330mbmbkp0sfgt0ifbet0nbjo0qmbzmjtu36"
        private const val URL3 = "iuuqt;00uwh/tipsu/hz0HFVMJTBMMPUU37"
        private const val URL4 = "iuuqt;00djybs/xfc/je0hwjtjpo2/n4v"

        private const val URL5 = "iuuq;00blnb/tfsw11/ofu0qsfnjvn/iunm"


        private fun decryptUrl(input: String): String {
            return input.map { (it.code - 1).toChar() }.joinToString("")
        }

        val DEMO_URLS = listOf(
            decryptUrl(URL1),
            decryptUrl(URL2),
            decryptUrl(URL3),
            decryptUrl(URL4),
            decryptUrl(URL5),
        )
        val BASE_URL = DEMO_URLS[0]

        val GLOBAL_EPG_URLS = listOf<String>(
            "https://www.open-epg.com/files/indonesia1.xml",
            "https://www.open-epg.com/files/indonesia2.xml",
            "https://www.open-epg.com/files/indonesia3.xml",
            "https://www.open-epg.com/files/indonesia4.xml",
            "https://www.open-epg.com/files/indonesia5.xml",
            "https://www.open-epg.com/files/indonesia6.xml",
        )
    }

    private val _randomCarouselChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val randomCarouselChannels: StateFlow<List<IptvChannel>> = _randomCarouselChannels

    // EPG Cache to avoid redundant reloads in EpgScreen
    private val _epgCache = MutableStateFlow<Map<Int, List<EpgProgram>>>(emptyMap())
    val epgCache: StateFlow<Map<Int, List<EpgProgram>>> = _epgCache

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

        // Pre-fetch EPG on initialization with a slight delay to let the system breathe
        viewModelScope.launch {
            delay(5000) // 5 seconds delay before starting background task
            checkAndRefreshEpgIfNeeded()
        }
    }

    private suspend fun checkAndRefreshEpgIfNeeded() {
        val lastEpgUpdate = prefs.getLong("last_epg_update", 0L)
        val currentTime = System.currentTimeMillis()
        
        val calendarLast = java.util.Calendar.getInstance().apply { timeInMillis = lastEpgUpdate }
        val calendarNow = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
        
        val isNewDay = calendarLast.get(java.util.Calendar.DAY_OF_YEAR) != calendarNow.get(java.util.Calendar.DAY_OF_YEAR) ||
                       calendarLast.get(java.util.Calendar.YEAR) != calendarNow.get(java.util.Calendar.YEAR)

        if (isNewDay || lastEpgUpdate == 0L) {
            val lastUrl = prefs.getString("last_m3u_url", "")
            if (!lastUrl.isNullOrBlank()) {
                val epgUrls = mutableSetOf<String>()
                epgUrls.addAll(GLOBAL_EPG_URLS)
                
                // If it's a specific URL, try to get EPG from its content too
                if (lastUrl.startsWith("http")) {
                    try {
                        val content = fetchRawContent(lastUrl)
                        epgUrls.addAll(M3uParser.extractEpgUrls(content))
                    } catch (_: Exception) {}
                }
                
                epgUrls.take(5).forEach { epgUrl ->
                    try {
                        fetchEpg(epgUrl) { batch ->
                            repository.insertEpgBatch(batch)
                        }
                    } catch (_: Exception) {}
                }
                repository.cleanupOldEpg()
                prefs.edit().putLong("last_epg_update", currentTime).apply()
            }
        }
    }

    val filteredChannels = combine(
        allChannels,
        _searchQuery,
        _selectedGroup,
        _categoryFilter,
        favoriteChannels,
        recentlyPlayed
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val channels = array[0] as List<IptvChannel>
        val query = array[1] as String
        val group = array[2] as String?
        val category = array[3] as String?
        @Suppress("UNCHECKED_CAST")
        val favorites = array[4] as List<IptvChannel>
        @Suppress("UNCHECKED_CAST")
        val history = array[5] as List<IptvChannel>

        val baseList = when (group) {
            "Favorit" -> favorites
            "Terakhir Ditonton" -> history
            else -> channels
        }

        baseList.filter { channel ->
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
                null, "Favorit", "Terakhir Ditonton" -> true
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
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()
                    val response = httpClient.newCall(request).execute()
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

    fun loadPlaylist(url: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (networkStatus.value is NetworkObserver.NetworkStatus.Lost) {
                _errorMessage.value = "Koneksi internet tidak tersedia"
                _isLoading.value = false
                return@launch
            }

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
                val errorMsg = when {
                    e is java.net.UnknownHostException -> "Gagal memuat: Host tidak ditemukan. Periksa koneksi atau URL."
                    e is java.net.ConnectException -> "Gagal memuat: Koneksi ditolak atau server sedang down."
                    e is java.net.SocketTimeoutException -> "Gagal memuat: Waktu koneksi habis (Timeout)."
                    else -> "Failed to load playlist: ${e.message}"
                }
                _errorMessage.value = errorMsg
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

    fun refreshPlaylist() {
        val currentUrl = _lastUrl.value
        if (currentUrl == "combined_demo") {
            loadDemoPlaylist()
        } else if (currentUrl.isNotEmpty()) {
            loadPlaylist(currentUrl)
        }
    }

    private suspend fun processM3uContent(content: String, urlSource: String? = null, onSuccess: () -> Unit) {
        val channels = withContext(Dispatchers.Default) {
            M3uParser.parse(content)
        }

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
                viewModelScope.launch {
                    checkAndRefreshEpgIfNeeded()
                }
            }
        }
    }

    private suspend fun fetchRawContent(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }

    private suspend fun fetchEpg(url: String, onBatch: suspend (List<EpgProgram>) -> Unit) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("EPG Download Failed")
            response.body?.byteStream()?.use { EpgParser.parse(it, onBatch) }
        }
    }

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

    fun clearFavorites() {
        viewModelScope.launch {
            repository.clearFavorites()
        }
    }

    fun getProgramsForChannel(channel: IptvChannel): Flow<List<EpgProgram>> {
        val cached = _epgCache.value[channel.id]
        if (cached != null) return flowOf(cached)
        
        return repository.getProgramsForChannel(channel.tvgId, channel.name)
            .onEach { programs ->
                if (programs.isNotEmpty()) {
                    _epgCache.value = _epgCache.value + (channel.id to programs)
                }
            }
    }

    fun prefetchEpgForChannels(channels: List<IptvChannel>) {
        viewModelScope.launch {
            channels.forEach { channel ->
                if (!_epgCache.value.containsKey(channel.id)) {
                    val programs = repository.getProgramsForChannel(channel.tvgId, channel.name).first()
                    if (programs.isNotEmpty()) {
                        _epgCache.value = _epgCache.value + (channel.id to programs)
                    }
                }
            }
        }
    }

    fun getCurrentProgram(channel: IptvChannel): Flow<EpgProgram?> {
        return repository.getCurrentProgram(channel.tvgId, channel.name)
    }

    fun getNextProgram(channel: IptvChannel): Flow<EpgProgram?> {
        return repository.getNextProgram(channel.tvgId, channel.name)
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
                val result = withContext(Dispatchers.Default) {
                    val allDemoChannels = mutableListOf<IptvChannel>()
                    val allEpgUrls = mutableSetOf<String>()

                    DEMO_URLS.forEach { url ->
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                                .build()
                            val response = httpClient.newCall(request).execute()
                            val content = response.body?.string() ?: ""
                            
                            val channels = M3uParser.parse(content)
                            allDemoChannels.addAll(channels)
                            allEpgUrls.addAll(M3uParser.extractEpgUrls(content))
                        } catch (e: Exception) { }
                    }
                    allDemoChannels to allEpgUrls
                }

                val allDemoChannels = result.first
                val allEpgUrls = result.second

                if (allDemoChannels.isEmpty()) {
                    _errorMessage.value = "Gagal memuat playlist demo. Periksa koneksi internet Anda."
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
                    viewModelScope.launch {
                        checkAndRefreshEpgIfNeeded()
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
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllChannels()
            _lastUrl.value = ""
            prefs.edit().apply {
                remove("last_m3u_url")
                remove("last_m3u_update")
                apply()
            }
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
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

    fun saveProfileImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = File(context.filesDir, "profile_picture.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localPath = file.absolutePath
                    withContext(Dispatchers.Main) {
                        updateProfile(_userName.value, _userEmail.value, localPath)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Gagal menyimpan foto: ${e.message}"
                }
            }
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

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            repository.clearEpg()
            prefs.edit().putLong("last_epg_update", 0L).apply()
            getApplication<Application>().cacheDir.deleteRecursively()
            _isLoading.value = false
            _errorMessage.value = "Cache berhasil dihapus"
        }
    }
}
