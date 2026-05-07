package com.chesko.stream_pro.core.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.chesko.stream_pro.core.R
import com.chesko.stream_pro.core.data.local.AppDatabase
import com.chesko.stream_pro.core.data.local.ChannelRepository
import com.chesko.stream_pro.core.data.model.IptvChannel
import com.chesko.stream_pro.core.data.model.EpgProgram
import com.chesko.stream_pro.core.data.parser.M3uParser
import com.chesko.stream_pro.core.data.parser.EpgParser
import com.chesko.stream_pro.core.utils.LocaleHelper
import com.chesko.stream_pro.core.utils.NetworkObserver
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import com.chesko.stream_pro.core.utils.PlayerUtils
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.session.MediaSession
import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.cast.framework.CastContext
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.CastMediaControlIntent
import java.io.File
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@OptIn(UnstableApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ChannelRepository
    private val prefs = application.getSharedPreferences("iptv_player_prefs", Context.MODE_PRIVATE)

    val allChannels: StateFlow<List<IptvChannel>>
    val favoriteChannels: StateFlow<List<IptvChannel>>
    val recentlyPlayed: StateFlow<List<IptvChannel>>
    val allPlaylists: StateFlow<List<com.chesko.stream_pro.core.data.model.Playlist>>

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup

    private val _selectedPlaylistId = MutableStateFlow<Int?>(null)
    val selectedPlaylistId: StateFlow<Int?> = _selectedPlaylistId

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

    private val _audioBoost = MutableStateFlow(prefs.getBoolean("audio_boost", false))
    val audioBoost: StateFlow<Boolean> = _audioBoost

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode_permanent", true))
    val darkMode: StateFlow<Boolean> = _darkMode

    private val _accentColor = MutableStateFlow(prefs.getInt("accent_color", 0xFF2979FF.toInt()))
    val accentColor: StateFlow<Int> = _accentColor

    private val _backgroundType = MutableStateFlow(prefs.getString("background_type", "default") ?: "default")
    val backgroundType: StateFlow<String> = _backgroundType

    private val _backgroundColor = MutableStateFlow(prefs.getInt("background_color", 0xFF000000.toInt()))
    val backgroundColor: StateFlow<Int> = _backgroundColor

    private val _backgroundImageUri = MutableStateFlow(prefs.getString("background_image_uri", null))
    val backgroundImageUri: StateFlow<String?> = _backgroundImageUri

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage

    private val _userName = MutableStateFlow(prefs.getString("user_name", application.getString(R.string.default_user_name)) ?: application.getString(R.string.default_user_name))
    val userName: StateFlow<String> = _userName

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", application.getString(R.string.default_user_email)) ?: application.getString(R.string.default_user_email))
    val userEmail: StateFlow<String> = _userEmail

    private val _profileImageUri = MutableStateFlow(prefs.getString("profile_image_uri", null))
    val profileImageUri: StateFlow<String?> = _profileImageUri

    private val _deviceId = MutableStateFlow(
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)?.uppercase() ?: "UNKNOWN"
    )
    val deviceId: StateFlow<String> = _deviceId

    private val _memberSince = MutableStateFlow(getSavedMemberSince())
    val memberSince: StateFlow<String> = _memberSince

    // Cast Discovery Logic

    private val _castPlayer = MutableStateFlow<CastPlayer?>(null)
    val castPlayer: StateFlow<CastPlayer?> = _castPlayer

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting

    private val _lastCastPosition = MutableStateFlow(0L)
    val lastCastPosition: StateFlow<Long> = _lastCastPosition

    private var mediaSession: MediaSession? = null

    private val _availableRoutes = MutableStateFlow<List<MediaRouter.RouteInfo>>(emptyList())
    val availableRoutes: StateFlow<List<MediaRouter.RouteInfo>> = _availableRoutes

    private val mediaRouter = MediaRouter.getInstance(application)
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
        .build()

    private val routeCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes()
        }
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes()
        }
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            updateRoutes()
        }
    }

    private val sessionManagerListener = object : com.google.android.gms.cast.framework.SessionManagerListener<com.google.android.gms.cast.framework.CastSession> {
        override fun onSessionStarted(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {
            viewModelScope.launch(Dispatchers.Main) {
                _isCasting.value = true
                delay(600) 
                transferCurrentMediaToCast()
            }
        }
        override fun onSessionResumed(session: com.google.android.gms.cast.framework.CastSession, wasSuspended: Boolean) {
            viewModelScope.launch(Dispatchers.Main) {
                _isCasting.value = true
                delay(600)
                transferCurrentMediaToCast()
            }
        }
        override fun onSessionStarting(session: com.google.android.gms.cast.framework.CastSession) {
            Log.d("MainViewModel", "Cast session starting...")
        }
        override fun onSessionStartFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
            Log.e("MainViewModel", "Cast session start failed: $error")
            _isCasting.value = false
        }
        override fun onSessionEnding(session: com.google.android.gms.cast.framework.CastSession) {
            Log.d("MainViewModel", "Cast session ending...")
        }
        override fun onSessionEnded(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
            Log.d("MainViewModel", "Cast session ended")
            viewModelScope.launch(Dispatchers.Main) {
                _lastCastPosition.value = _castPlayer.value?.currentPosition ?: 0L
                _isCasting.value = false
            }
        }
        override fun onSessionResuming(session: com.google.android.gms.cast.framework.CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: com.google.android.gms.cast.framework.CastSession, error: Int) {
            _isCasting.value = false
        }
        override fun onSessionSuspended(session: com.google.android.gms.cast.framework.CastSession, reason: Int) {
            viewModelScope.launch(Dispatchers.Main) {
                _isCasting.value = false
            }
        }
    }

    fun transferCurrentMediaToCast(startPositionMs: Long = -1L) {
        val channel = _selectedChannel.value ?: return
        val player = _castPlayer.value ?: return
        
        if (!player.isCastSessionAvailable) {
            Log.w("MainViewModel", "Cast session not available, cannot transfer")
            return
        }

        // Check if already playing this item to avoid double-loading
        val currentMediaId = try { player.currentMediaItem?.mediaId } catch(_: Exception) { null }
        if (currentMediaId == channel.url && player.playbackState != Player.STATE_IDLE) {
            Log.d("MainViewModel", "Already playing ${channel.name} on Cast, skipping transfer")
            _isCasting.value = true
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                Log.d("MainViewModel", "Building MediaItem for Cast: ${channel.name}")
                val mediaItem = PlayerUtils.buildMediaItem(channel)
                
                player.setMediaItem(mediaItem, if (startPositionMs >= 0) startPositionMs else 0L)
                
                player.prepare()
                player.playWhenReady = true
                
                Log.d("MainViewModel", "Transferred ${channel.name} to Cast at position $startPositionMs")
                _isCasting.value = true
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error transferring media to cast: ${e.message}")
                _errorMessage.value = "Gagal mengirim ke TV: ${e.localizedMessage}"
            }
        }
    }

    private fun updateRoutes() {
        _availableRoutes.value = mediaRouter.routes.filter { it.matchesSelector(selector) && !it.isDefault }
    }

    fun retryDiscovery() {
        viewModelScope.launch {
            mediaRouter.removeCallback(routeCallback)
            delay(100)
            mediaRouter.addCallback(selector, routeCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
            updateRoutes()
        }
    }

    private fun initCastPlayer() {
        if (_castPlayer.value != null) return 

        viewModelScope.launch(Dispatchers.Main) {
            try {
                val castContext = CastContext.getSharedInstance(getApplication())
                val cp = CastPlayer(castContext, DefaultMediaItemConverter())
                _castPlayer.value = cp

                cp.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isCasting.value = cp.isCastSessionAvailable
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        _isCasting.value = cp.isCastSessionAvailable
                        if (state == Player.STATE_READY && cp.playWhenReady && !cp.isPlaying) {
                            cp.play()
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("MainViewModel", "Cast Player Error: ${error.message}")
                        if (_isCasting.value) {
                            // Attempt auto-recovery for cast if disconnected
                            viewModelScope.launch {
                                delay(2000)
                                if (_isCasting.value) transferCurrentMediaToCast()
                            }
                        }
                    }
                })
                
                cp.setSessionAvailabilityListener(object : androidx.media3.cast.SessionAvailabilityListener {
                    override fun onCastSessionAvailable() {
                        Log.d("MainViewModel", "Cast Session Available")
                        _isCasting.value = true
                        transferCurrentMediaToCast()
                    }
                    override fun onCastSessionUnavailable() {
                        Log.d("MainViewModel", "Cast Session Unavailable")
                        _isCasting.value = false
                    }
                })

                // Set up MediaSession for CastPlayer
                val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(getApplication<Application>().packageName)
                val pendingIntent = PendingIntent.getActivity(
                    getApplication(),
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                mediaSession = MediaSession.Builder(getApplication(), cp)
                    .setSessionActivity(pendingIntent)
                    .build()

                // Initial state check
                _isCasting.value = cp.isCastSessionAvailable
            } catch (e: Exception) {
                Log.e("MainViewModel", "Cast init failed: ${e.message}")
            }
        }
    }

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(okhttp3.Cache(
                    directory = File(getApplication<Application>().cacheDir, "http_cache"),
                    maxSize = 50L * 1024L * 1024L
                ))
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (_: Exception) {
            return OkHttpClient.Builder().build()
        }
    }

    private val httpClient = createUnsafeOkHttpClient()

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
            val sdf = java.text.SimpleDateFormat(getApplication<Application>().getString(R.string.member_since_format), java.util.Locale.getDefault())
            sdf.format(java.util.Date(installTime))
        } catch (e: Exception) {
            getApplication<Application>().getString(R.string.default_member_since_date)
        }
    }

    companion object {
        private const val CONFIG_URL = "iuuqt;00sbx/hjuivcvtfsdpoufou/dpn0diftlp320JQUW`Qmbzfs0sfgt0ifbet0nbtufs0efnp`dpogjh/ktpo" // https://raw.githubusercontent.com/chesko21/IPTV_Player/refs/heads/master/demo_config.json

        private fun decryptUrl(input: String): String {
            return input.map { (it.code - 1).toChar() }.joinToString("")
        }

        val DEMO_URLS = emptyList<String>()

        val GLOBAL_EPG_URLS = listOf(
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

    private val _dynamicDemoUrls = MutableStateFlow<List<String>>(emptyList())
    val dynamicDemoUrls: StateFlow<List<String>> = _dynamicDemoUrls

    init {
        val database = AppDatabase.getDatabase(application)
        val channelDao = database.channelDao()
        val epgDao = database.epgDao()
        val playlistDao = database.playlistDao()
        repository = ChannelRepository(channelDao, epgDao, playlistDao)

        _lastUrl.value = prefs.getString("last_m3u_url", "") ?: ""
        
        // RESTORE: Ambil ID playlist terakhir yang dipilih
        val savedPlaylistId = prefs.getInt("last_selected_playlist_id", -1)
        _selectedPlaylistId.value = if (savedPlaylistId != -1) savedPlaylistId else null

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

        allPlaylists = repository.allPlaylists.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            delay(5000)
            checkAndRefreshEpgIfNeeded()
        }

        initCastPlayer()
        mediaRouter.addCallback(selector, routeCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        try {
            CastContext.getSharedInstance(application).sessionManager.addSessionManagerListener(sessionManagerListener, com.google.android.gms.cast.framework.CastSession::class.java)
        } catch (e: Exception) {}

        fetchRemoteConfig()
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            try {
                val configRequest = Request.Builder()
                    .url(decryptUrl(CONFIG_URL))
                    .build()
                val configResponse = withContext(Dispatchers.IO) { httpClient.newCall(configRequest).execute() }
                if (configResponse.isSuccessful) {
                    val json = configResponse.body?.string() ?: ""
                    val urls = parseRemoteConfig(json)
                    if (!urls.isNullOrEmpty()) {
                        _dynamicDemoUrls.value = urls
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch remote config", e)
            }
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
            val lastUrlStored = prefs.getString("last_m3u_url", "")
            if (!lastUrlStored.isNullOrBlank()) {
                val epgUrls = mutableSetOf<String>()
                epgUrls.addAll(GLOBAL_EPG_URLS)
                
                if (lastUrlStored.startsWith("http")) {
                    try {
                        val content = fetchRawContent(lastUrlStored)
                        epgUrls.addAll(M3uParser.extractEpgUrls(content))
                    } catch (e: Exception) {}
                }
                
                epgUrls.take(5).forEach { epgUrl ->
                    try {
                        fetchEpg(epgUrl) { batch ->
                            repository.insertEpgBatch(batch)
                        }
                    } catch (e: Exception) {}
                }
                repository.cleanupOldEpg()
                prefs.edit {
                    putLong("last_epg_update", currentTime)
                }
            }
        }
    }

    val filteredChannels = combine(
        allChannels,
        _searchQuery,
        _selectedGroup,
        _selectedPlaylistId,
        favoriteChannels,
        recentlyPlayed,
        _categoryFilter
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val channels = array[0] as List<IptvChannel>
        val query = array[1] as String
        val group = array[2] as String?
        val playlistId = array[3] as Int?
        @Suppress("UNCHECKED_CAST")
        val favorites = array[4] as List<IptvChannel>
        @Suppress("UNCHECKED_CAST")
        val history = array[5] as List<IptvChannel>
        val category = array[6] as String?

        var baseList = when (group) {
            getApplication<Application>().getString(R.string.group_favorites) -> favorites
            getApplication<Application>().getString(R.string.group_recently_played) -> history
            else -> if (playlistId != null) channels.filter { it.playlistId == playlistId } else channels
        }

        // Apply Category Filter
        if (category != null) {
            baseList = baseList.filter { ch ->
                val g = ch.group?.lowercase() ?: ""
                when (category.lowercase()) {
                    "live" -> g.contains("live") || g.contains("tv") || g.contains("stream") || (!g.contains("movie") && !g.contains("series"))
                    "movies" -> g.contains("movie") || g.contains("film") || g.contains("cinema")
                    "sport" -> g.contains("sport") || g.contains("bola") || g.contains("arena")
                    else -> true
                }
            }
        }

        baseList.filter { channel ->
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                channel.name.contains(query, ignoreCase = true) ||
                        channel.group?.contains(query, ignoreCase = true) == true
            }

            val matchesGroup = when (group) {
                null, getApplication<Application>().getString(R.string.group_favorites), getApplication<Application>().getString(R.string.group_recently_played) -> true
                getApplication<Application>().getString(R.string.group_other) -> channel.group.isNullOrBlank()
                else -> channel.group == group
            }
            
            matchesSearch && matchesGroup
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val groups = combine(allChannels, _selectedPlaylistId) { channels, playlistId ->
        val filtered = if (playlistId != null) channels.filter { it.playlistId == playlistId } else channels
        val context = getApplication<Application>()
        val existingGroups = filtered.mapNotNull { it.group }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        
        val hasNoGroup = filtered.any { it.group.isNullOrBlank() }
        if (hasNoGroup) {
            existingGroups + context.getString(R.string.group_other)
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
                    _errorMessage.value = getApplication<Application>().getString(R.string.error_invalid_url)
                }
            } catch (e: Exception) {
                _connectionStatus.value = false
                _errorMessage.value = getApplication<Application>().getString(R.string.error_connection_failed, e.message ?: "")
            } finally {
                _isCheckingConnection.value = false
            }
        }
    }

    fun loadPlaylist(url: String, name: String? = null, onSuccess: () -> Unit = {}) {
        if (url == "combined_demo") {
            loadDemoPlaylist(onSuccess)
            return
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (networkStatus.value is NetworkObserver.NetworkStatus.Lost) {
                _errorMessage.value = context.getString(R.string.error_no_internet)
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null
            _lastUrl.value = url

            try {
                // Ensure Playlist exists in DB
                val playlistName = name ?: if (url.contains("indihome")) "Indihome" else "Server ${allPlaylists.value.size + 1}"
                var playlist = repository.getPlaylistByUrl(url)
                if (playlist == null) {
                    val id = repository.insertPlaylist(
                        com.chesko.stream_pro.core.data.model.Playlist(
                            name = playlistName,
                            url = url
                        )
                    )
                    playlist = repository.getPlaylistByUrl(url)
                }

                // AKTIFKAN FILTER: Set playlist yang baru dimuat sebagai playlist aktif
                val pId = playlist?.id
                _selectedPlaylistId.value = pId
                prefs.edit { putInt("last_selected_playlist_id", pId ?: -1) }
                
                _selectedGroup.value = null // Reset filter grup agar tidak bentrok

                val m3uContent = when {
                    url.startsWith("http") -> fetchRawContent(url)
                    url.startsWith("file://") -> {
                        val filePath = url.substring(7)
                        withContext(Dispatchers.IO) { File(filePath).readText() }
                    }
                    else -> url
                }
                processM3uContent(m3uContent, url, playlist?.id ?: 0, onSuccess)
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is java.net.UnknownHostException -> context.getString(R.string.error_host_not_found)
                    is java.net.ConnectException -> context.getString(R.string.error_connection_refused)
                    is java.net.SocketTimeoutException -> context.getString(R.string.error_socket_timeout)
                    else -> context.getString(R.string.error_load_playlist_failed, e.message ?: "")
                }
                _errorMessage.value = errorMsg
                _isLoading.value = false
            }
        }
    }

    fun loadPlaylistFromFile(content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val internalFile = File(context.filesDir, "last_local_playlist.m3u")
                withContext(Dispatchers.IO) {
                    internalFile.writeText(content)
                }
                processM3uContent(content, "file://${internalFile.absolutePath}", 0, onSuccess)
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.error_save_parse_failed, e.message ?: "")
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

    private suspend fun processM3uContent(content: String, urlSource: String? = null, playlistId: Int = 0, onSuccess: () -> Unit) {
        val channels = withContext(Dispatchers.Default) {
            M3uParser.parse(content)
        }

        if (channels.isEmpty()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.error_no_channels_found)
            _isLoading.value = false
        } else {
            val previousUrl = prefs.getString("last_m3u_url", "")
            if (urlSource != null && urlSource != previousUrl && previousUrl?.isNotBlank() == true) {
                // repository.clearEpg() // Don't clear EPG if we want multi-playlist
            }

            repository.syncChannels(channels, playlistId)
            _randomCarouselChannels.value = channels.shuffled().take(10)
            _errorMessage.value = null

            urlSource?.let {
                _lastUrl.value = it
                prefs.edit {
                    putString("last_m3u_url", it)
                    putLong("last_m3u_update", System.currentTimeMillis())
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

    fun toggleFavorite(channel: IptvChannel) {
        viewModelScope.launch {
            val targetUrl = channel.url.trim()
            val existingChannel = repository.getChannelByUrl(targetUrl)
            val currentlyFavorite = existingChannel?.isFavorite ?: false
            repository.updateFavoriteStatusByUrl(targetUrl, !currentlyFavorite)
        }
    }

    fun stopCasting(): Long {
        val lastPos = _castPlayer.value?.currentPosition ?: 0L
        _lastCastPosition.value = lastPos
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _castPlayer.value?.stop()
                _castPlayer.value?.clearMediaItems()
                val castContext = CastContext.getSharedInstance(getApplication())
                castContext.sessionManager.endCurrentSession(true)
                _isCasting.value = false
                Log.d("MainViewModel", "Cast screen disconnected manually")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error stopping cast: ${e.message}")
            }
        }
        return lastPos
    }

    fun markAsPlayed(channel: IptvChannel) {
        viewModelScope.launch {
            repository.markAsPlayed(channel)
        }
    }

    fun getProgramsForChannel(channel: IptvChannel): Flow<List<EpgProgram>> {
        return repository.getProgramsForChannel(channel.tvgId, channel.name)
    }

    fun prefetchEpgForChannels(channels: List<IptvChannel>) {
        viewModelScope.launch {
            channels.forEach { channel ->
                repository.getProgramsForChannel(channel.tvgId, channel.name).first()
            }
        }
    }

    fun getCurrentProgram(channel: IptvChannel): Flow<EpgProgram?> {
        return repository.getCurrentProgram(channel.tvgId, channel.name)
    }

    fun getNextProgram(channel: IptvChannel): Flow<EpgProgram?> {
        return repository.getNextProgram(channel.tvgId, channel.name)
    }

    fun refreshEpgForChannel(channel: IptvChannel) {
        viewModelScope.launch {
            // This triggers a re-query of the Flow in the UI by slightly poking the repository or just relying on Flow collection
            // In Room, since we use Flow, it should auto-update if the database changes.
            // If we want to force a refresh from network:
            checkAndRefreshEpgIfNeeded()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun setSelectedPlaylistId(id: Int?) {
        _selectedPlaylistId.value = id
        prefs.edit { putInt("last_selected_playlist_id", id ?: -1) }
        
        if (id != null) {
            viewModelScope.launch {
                val playlist = allPlaylists.value.find { it.id == id }
                val hasChannels = allChannels.value.any { it.playlistId == id }
                
                if (!hasChannels && playlist != null) {
                    loadPlaylist(playlist.url, playlist.name)
                }
            }
        }
    }

    fun deletePlaylist(playlist: com.chesko.stream_pro.core.data.model.Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    fun setSelectedChannel(channel: IptvChannel?) {
        _selectedChannel.value = channel
        if (channel != null) {
            // Otomatis deteksi dan set grup jika saat ini berada di mode "Semua Saluran"
            if (_selectedGroup.value == null) {
                if (!channel.group.isNullOrBlank()) {
                    _selectedGroup.value = channel.group
                } else {
                    _selectedGroup.value = getApplication<Application>().getString(R.string.group_other)
                }
            }
            
            if (_isCasting.value) {
                transferCurrentMediaToCast()
            }
        }
    }

    fun saveBackupToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                val xmlContent = exportChannelsToXml()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(xmlContent.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    _errorMessage.value = context.getString(R.string.success_backup)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = context.getString(R.string.error_backup_failed, e.message ?: "")
                }
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                _isLoading.value = true
                val inputStream = context.contentResolver.openInputStream(uri)
                val channels = parseBackupXml(inputStream)
                if (channels.isNotEmpty()) {
                    repository.syncChannels(channels)
                    _lastUrl.value = "backup_restore"
                    prefs.edit().apply {
                        putString("last_m3u_url", "backup_restore")
                        putLong("last_m3u_update", System.currentTimeMillis())
                        apply()
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                        _errorMessage.value = context.getString(R.string.success_restore)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = context.getString(R.string.error_invalid_backup_file)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = context.getString(R.string.error_restore_failed, e.message ?: "")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun parseBackupXml(inputStream: InputStream?): List<IptvChannel> {
        if (inputStream == null) return emptyList()
        val channels = mutableListOf<IptvChannel>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            
            var eventType = parser.eventType
            var currentChannel: MutableMap<String, String>? = null
            var currentTag: String? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "channel") {
                            currentChannel = mutableMapOf()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && currentChannel != null && currentTag != null) {
                            currentChannel[currentTag] = text
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "channel" && currentChannel != null) {
                            channels.add(
                                IptvChannel(
                                    name = currentChannel["name"] ?: "",
                                    url = currentChannel["url"] ?: "",
                                    logo = currentChannel["logo"],
                                    group = currentChannel["group"],
                                    tvgId = currentChannel["tvgId"],
                                    tvgName = currentChannel["tvgName"],
                                    channelNumber = currentChannel["channelNumber"],
                                    userAgent = currentChannel["userAgent"],
                                    referrer = currentChannel["referrer"],
                                    cookie = currentChannel["cookie"],
                                    drmConfig = currentChannel["drmConfig"],
                                    drmType = currentChannel["drmType"],
                                    drmKey = currentChannel["drmKey"],
                                    drmKeyId = currentChannel["drmKeyId"],
                                    drmLicenseUrl = currentChannel["drmLicenseUrl"],
                                    isFavorite = currentChannel["isFavorite"]?.toBoolean() ?: false,
                                    lastPlayed = currentChannel["lastPlayed"]?.toLongOrNull()
                                )
                            )
                            currentChannel = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.close()
        }
        return channels
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
            xmlBuilder.append("        <group>${escapeXml(channel.group ?: "")}</group>\n")
            xmlBuilder.append("        <tvgId>${escapeXml(channel.tvgId ?: "")}</tvgId>\n")
            xmlBuilder.append("        <tvgName>${escapeXml(channel.tvgName ?: "")}</tvgName>\n")
            xmlBuilder.append("        <channelNumber>${escapeXml(channel.channelNumber ?: "")}</channelNumber>\n")
            xmlBuilder.append("        <userAgent>${escapeXml(channel.userAgent ?: "")}</userAgent>\n")
            xmlBuilder.append("        <referrer>${escapeXml(channel.referrer ?: "")}</referrer>\n")
            xmlBuilder.append("        <cookie>${escapeXml(channel.cookie ?: "")}</cookie>\n")
            xmlBuilder.append("        <drmConfig>${escapeXml(channel.drmConfig ?: "")}</drmConfig>\n")
            xmlBuilder.append("        <drmType>${escapeXml(channel.drmType ?: "")}</drmType>\n")
            xmlBuilder.append("        <drmKey>${escapeXml(channel.drmKey ?: "")}</drmKey>\n")
            xmlBuilder.append("        <drmKeyId>${escapeXml(channel.drmKeyId ?: "")}</drmKeyId>\n")
            xmlBuilder.append("        <drmLicenseUrl>${escapeXml(channel.drmLicenseUrl ?: "")}</drmLicenseUrl>\n")
            xmlBuilder.append("        <isFavorite>${channel.isFavorite}</isFavorite>\n")
            xmlBuilder.append("        <lastPlayed>${channel.lastPlayed ?: ""}</lastPlayed>\n")
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
            val context = getApplication<Application>()
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = withContext(Dispatchers.Default) {
                    val allDemoChannels = mutableListOf<IptvChannel>()
                    val allEpgUrls = mutableSetOf<String>()

                    // 1. Try to fetch dynamic URLs from GitHub
                    val remoteUrls = try {
                        val configRequest = Request.Builder()
                            .url(decryptUrl(CONFIG_URL))
                            .build()
                        val configResponse = httpClient.newCall(configRequest).execute()
                        if (configResponse.isSuccessful) {
                            val json = configResponse.body?.string() ?: ""
                            parseRemoteConfig(json)
                        } else null
                    } catch (e: Exception) {
                        null
                    }

                    // 2. Use remote URLs if available, otherwise fallback to hardcoded
                    val urlsToLoad = if (!remoteUrls.isNullOrEmpty()) remoteUrls else DEMO_URLS

                    urlsToLoad.forEach { url ->
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
                    _errorMessage.value = context.getString(R.string.error_demo_load_failed)
                    _isLoading.value = false
                    return@launch
                }

                repository.syncChannels(allDemoChannels, 0)
                _randomCarouselChannels.value = allDemoChannels.shuffled().take(10)

                _lastUrl.value = "combined_demo"
                prefs.edit {
                    putString("last_m3u_url", "combined_demo")
                    putLong("last_m3u_update", System.currentTimeMillis())
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                    delay(1500)
                    _isLoading.value = false
                }

                val epgUrls = (allEpgUrls + GLOBAL_EPG_URLS).distinct()
                if (epgUrls.isNotEmpty()) {
                    viewModelScope.launch {
                        checkAndRefreshEpgIfNeeded()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = context.getString(R.string.error_demo_generic_failed, e.message ?: "")
                _isLoading.value = false
            }
        }
    }

    private fun parseRemoteConfig(json: String): List<String>? {
        return try {
            val urls = mutableListOf<String>()
            val regex = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
            regex.findAll(json).forEach { match ->
                urls.add(match.groupValues[1])
            }
            urls.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteCurrentPlaylist() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.clearAllChannels()
            repository.clearEpg()
            repository.clearRecentlyPlayed()
            _lastUrl.value = ""
            _selectedPlaylistId.value = null
            prefs.edit {
                remove("last_m3u_url")
                remove("last_m3u_update")
                remove("last_epg_update")
                remove("last_selected_playlist_id")
            }
            _isLoading.value = false
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

    fun updateProfile(name: String, email: String, imageUri: String? = null) {
        _userName.value = name
        _userEmail.value = email
        _profileImageUri.value = imageUri ?: _profileImageUri.value
        prefs.edit {
            putString("user_name", name)
            putString("user_email", email)
            putString("profile_image_uri", _profileImageUri.value)
        }
    }

    fun saveProfileImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
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
                    _errorMessage.value = context.getString(R.string.error_save_photo_failed, e.message ?: "")
                }
            }
        }
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setHwAcceleration(enabled: Boolean) {
        _hwAcceleration.value = enabled
        prefs.edit { putBoolean("hw_acceleration", enabled) }
    }

    fun setBufferSize(seconds: Int) {
        _bufferSize.value = seconds
        prefs.edit { putInt("buffer_size", seconds) }
    }

    fun setMaxVideoHeight(height: Int) {
        _maxVideoHeight.value = height
        prefs.edit { putInt("max_video_height", height) }
    }

    fun setAudioBoost(enabled: Boolean) {
        _audioBoost.value = enabled
        prefs.edit { putBoolean("audio_boost", enabled) }
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit { putBoolean("dark_mode_permanent", enabled) }
    }

    fun setAccentColor(color: Int) {
        _accentColor.value = color
        prefs.edit { putInt("accent_color", color) }
    }

    fun setBackgroundType(type: String) {
        _backgroundType.value = type
        prefs.edit { putString("background_type", type) }
    }

    fun setBackgroundColor(color: Int) {
        _backgroundColor.value = color
        prefs.edit { putInt("background_color", color) }
    }

    fun setBackgroundImageUri(uri: String?) {
        _backgroundImageUri.value = uri
        prefs.edit { putString("background_image_uri", uri) }
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit { putString("app_language", lang) }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRouter.removeCallback(routeCallback)
        try {
            CastContext.getSharedInstance(getApplication()).sessionManager.removeSessionManagerListener(sessionManagerListener, com.google.android.gms.cast.framework.CastSession::class.java)
        } catch (e: Exception) {}
        mediaSession?.release()
        _castPlayer.value?.release()
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val baseContext = getApplication<Application>()
            val lang = _appLanguage.value
            val context = LocaleHelper.applyLocale(baseContext, lang)
            
            _isLoading.value = true
            repository.clearEpg()
            prefs.edit { putLong("last_epg_update", 0L) }
            baseContext.cacheDir.deleteRecursively()
            _isLoading.value = false
            _errorMessage.value = context.getString(R.string.success_cache_cleared)
        }
    }
}
