/**
 * Main ViewModel for radio playback and track information.
 * Manages radio state, metadata fetching, favorites, and song finding functionality.
 */
package com.radmon.radmonradio
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.radmon.radmonradio.data.repository.RadioRepository
import com.radmon.radmonradio.data.repository.FavoritesRepository
import com.radmon.radmonradio.data.repository.SongFinderRepository
import com.radmon.radmonradio.data.model.FavoriteTrack
import com.radmon.radmonradio.data.model.NowPlaying
import com.radmon.radmonradio.data.model.PlayedTrack
import com.radmon.radmonradio.data.network.ScheduleLiveResponse
import com.radmon.radmonradio.data.network.SongData
import com.radmon.radmonradio.data.network.toNowPlaying
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

sealed class LoadingState {
    
    data object Idle : LoadingState()
    
    data object Loading : LoadingState()
    
    data object Success : LoadingState()
    
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : LoadingState()
}

enum class Connectivity {
    GOOD,
    WEAK,
    OFFLINE
}

data class RadioUiState(
    val songTitle: String = DEFAULT_TITLE,
    val artistName: String = DEFAULT_ARTIST,
    val artworkUrl: String = DEFAULT_ARTWORK_URL,
    val loadingState: LoadingState = LoadingState.Loading,
    val connectivity: Connectivity = Connectivity.GOOD,
    val isRefreshing: Boolean = false,
    val consecutiveErrors: Int = 0
) {
    companion object {
        private const val DEFAULT_TITLE = "Radio Monash"
        private const val DEFAULT_ARTIST = "Live Stream"
        private const val DEFAULT_ARTWORK_URL =
            "https://d4mt18vwj73wk.cloudfront.net/stationImage/2025-04-22T05:02:43Z-1024x1024.jpeg"
    }
}

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RadioRepository()
    private val favoritesRepository = FavoritesRepository.getInstance(application)
    private val songFinderRepository = SongFinderRepository()
    private val _uiState = MutableStateFlow(RadioUiState())
    private val _songFinderState = MutableStateFlow<SongFinderState>(SongFinderState.Idle)
    val songFinderState: StateFlow<SongFinderState> = _songFinderState.asStateFlow()
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Idle)
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()
    
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null
    private var retryCount = 0
    init {
        initializeMetadataPolling()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchMetadata()
            delay(REFRESH_DISPLAY_DURATION_MS)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    fun retry() {
        resetErrorState()
        if (pollingJob?.isActive != true) {
            initializeMetadataPolling()
        }
    }
    
    private fun initializeMetadataPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                val nextPollDelay = fetchMetadata()
                delay(ensureMinimumDelay(nextPollDelay))
            }
        }
    }
    
    private suspend fun fetchMetadata(): Long {
        var nextPollDelay = DEFAULT_POLL_INTERVAL_MS
        repository.liveStatus()
            .onSuccess { response ->
nextPollDelay = handleSuccessfulResponse(response)
            }
            .onFailure { error ->
                handleErrorResponse(error)
nextPollDelay = calculateBackoffDelay()
            }
        return nextPollDelay
    }

    private fun handleSuccessfulResponse(response: ScheduleLiveResponse): Long {
        val (nowPlaying, liveShow) = response.toNowPlaying()
        updateStateWithNowPlaying(nowPlaying, liveShow)
        return calculateNextFetchDelay(nowPlaying)
    }

    private fun updateStateWithNowPlaying(nowPlaying: NowPlaying, liveShow: com.radmon.radmonradio.data.model.LiveShow?) {
        _uiState.update { currentState ->
            currentState.copy(
                songTitle = liveShow?.name ?: nowPlaying.title,
                artistName = if (liveShow != null) "Live Now" else nowPlaying.artist,
                artworkUrl = nowPlaying.artworkUrl ?: currentState.artworkUrl,
                loadingState = LoadingState.Success,
                connectivity = Connectivity.GOOD,
                consecutiveErrors = 0
            )
        }
        resetErrorState()
    }

    private fun calculateNextFetchDelay(nowPlaying: NowPlaying): Long {
        val duration = nowPlaying.duration
        val startTimeSeconds = nowPlaying.startedAt?.let { it.time / MILLISECONDS_PER_SECOND }
        return if (duration != null && startTimeSeconds != null) {
            if (duration <= 0 || startTimeSeconds <= 0) {
                return DEFAULT_POLL_INTERVAL_MS
            }
            val currentTimeSeconds = System.currentTimeMillis() / MILLISECONDS_PER_SECOND
            val timeDiff = kotlin.math.abs(currentTimeSeconds - startTimeSeconds)
            if (timeDiff > 86400) {
                return DEFAULT_POLL_INTERVAL_MS
            }
            val elapsedSeconds = currentTimeSeconds - startTimeSeconds
            val remainingSeconds = duration - elapsedSeconds
            if (remainingSeconds <= 0) {
                return POST_TRACK_FETCH_DELAY_MS
            }
            val calculatedDelay = (remainingSeconds.toLong() * MILLISECONDS_PER_SECOND) + POST_TRACK_FETCH_DELAY_MS
            calculatedDelay.coerceIn(MINIMUM_POLL_INTERVAL_MS, 600_000L)
        } else {
            DEFAULT_POLL_INTERVAL_MS
        }
    }

    private fun handleErrorResponse(error: Throwable) {
        Log.e(TAG, "Metadata fetch error", error)
        updateErrorState(
            message = determineErrorMessage(error),
            recoverable = repository.isRecoverableError(error)
        )
    }
    
    private fun calculateBackoffDelay(): Long {
        return if (retryCount < MAX_RETRY_ATTEMPTS) {
            (BASE_RETRY_DELAY_MS * (retryCount + 1)).also {
                retryCount++
            }
        } else {
            MAX_BACKOFF_DELAY_MS
        }
    }
    
    private fun updateErrorState(message: String, recoverable: Boolean) {
        _uiState.update { currentState ->
            val newErrorCount = currentState.consecutiveErrors + 1
            currentState.copy(
                loadingState = LoadingState.Error(message, recoverable),
                connectivity = assessConnectivity(newErrorCount),
                consecutiveErrors = newErrorCount
            )
        }
    }

    private fun assessConnectivity(errorCount: Int): Connectivity {
        return when {
            errorCount <= 1 -> Connectivity.GOOD
            errorCount == 2 -> Connectivity.WEAK
            else -> Connectivity.OFFLINE
        }
    }
    
    private fun determineErrorMessage(error: Throwable): String {
        return when (error) {
            is java.net.UnknownHostException -> ERROR_MESSAGE_NO_CONNECTION
            is java.net.SocketTimeoutException -> ERROR_MESSAGE_TIMEOUT
            is java.io.IOException -> ERROR_MESSAGE_NETWORK_ERROR
            else -> ERROR_MESSAGE_GENERIC
        }
    }
    
    private fun resetErrorState() {
        retryCount = 0
    }
    
    private fun ensureMinimumDelay(delay: Long): Long {
        return max(MINIMUM_POLL_INTERVAL_MS, delay)
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
    val favorites = favoritesRepository.favorites
    fun isCurrentFavoriteFlow(): kotlinx.coroutines.flow.Flow<Boolean> =
        kotlinx.coroutines.flow.combine(favorites, uiState) { favs, state ->
            favs.any { it.title.equals(state.songTitle, true) && it.artist.equals(state.artistName, true) }
        }
    fun toggleCurrentFavorite() {
        val state = _uiState.value
        if (state.songTitle.isBlank() || state.artistName.isBlank()) return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(
                FavoriteTrack(
                    title = state.songTitle,
                    artist = state.artistName,
                    artworkUrl = state.artworkUrl.takeIf { it.isNotBlank() },
                    likedAt = System.currentTimeMillis()
                )
            )
        }
    }
    fun toggleFavorite(track: FavoriteTrack) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track)
        }
    }
    fun findCurrentSongPlatforms() {
        val currentState = _uiState.value
        if (currentState.songTitle.isEmpty() || currentState.artistName.isEmpty()) {
            _songFinderState.value = SongFinderState.Error("No song information available")
            return
        }
        viewModelScope.launch {
            _songFinderState.value = SongFinderState.Loading
            val result = songFinderRepository.findSongLinks(
                songName = currentState.songTitle,
                artistName = currentState.artistName
            )
            _songFinderState.value = result.fold(
                onSuccess = { songData -> SongFinderState.Success(songData) },
                onFailure = { error ->
                    SongFinderState.Error(error.message ?: "Failed to find platforms")
                }
            )
        }
    }
    fun dismissSongFinder() {
        _songFinderState.value = SongFinderState.Idle
    }
    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading
            _historyState.value = repository.history().fold(
                onSuccess = { tracks -> HistoryState.Success(tracks) },
                onFailure = { error -> HistoryState.Error(error.message ?: "Failed to load recently played") }
            )
        }
    }
    fun dismissHistory() {
        _historyState.value = HistoryState.Idle
    }
    companion object {
        private const val TAG = "RadioViewModel"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 5000L
        private const val DEFAULT_POLL_INTERVAL_MS = 10000L
        private const val MINIMUM_POLL_INTERVAL_MS = 5000L
        private const val MAX_BACKOFF_DELAY_MS = 15000L
        private const val POST_TRACK_FETCH_DELAY_MS = 2000L
        private const val REFRESH_DISPLAY_DURATION_MS = 500L
        private const val MILLISECONDS_PER_SECOND = 1000L
        private const val ERROR_MESSAGE_NO_CONNECTION = "No internet connection"
        private const val ERROR_MESSAGE_TIMEOUT = "Connection timeout. Please try again"
        private const val ERROR_MESSAGE_NETWORK_ERROR = "Network error. Check your connection"
        private const val ERROR_MESSAGE_GENERIC = "Unable to load track info. Tap to retry"
    }
}

sealed class SongFinderState {
    data object Idle : SongFinderState()
    data object Loading : SongFinderState()
    data class Success(val songData: SongData) : SongFinderState()
    data class Error(val message: String) : SongFinderState()
}

sealed class HistoryState {
    data object Idle : HistoryState()
    data object Loading : HistoryState()
    data class Success(val tracks: List<PlayedTrack>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}
