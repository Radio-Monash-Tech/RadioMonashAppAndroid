package com.radmon.radmonradio.data.repository

import android.util.Log
import com.radmon.radmonradio.data.model.PlayedTrack
import com.radmon.radmonradio.data.network.ApiClient
import com.radmon.radmonradio.data.network.ScheduleLiveResponse
import com.radmon.radmonradio.data.network.toPlayedTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repository for fetching radio station metadata and play history.
 * Handles API calls and error handling for RadioCult live stream information.
 */
class RadioRepository {
    private val service = ApiClient.radioCultService

    /**
     * Fetches what's on air right now.
     * @return Result containing the live-status response or error
     */
    suspend fun liveStatus(): Result<ScheduleLiveResponse> = withContext(Dispatchers.IO) {
        runCatching { service.liveStatus() }
            .onFailure { exception -> logError(exception) }
    }

    /**
     * Fetches recently played songs.
     * @return Result containing the played tracks or error
     */
    suspend fun history(limit: Int = 20): Result<List<PlayedTrack>> = withContext(Dispatchers.IO) {
        runCatching { service.history(limit).toPlayedTracks() }
            .onFailure { exception -> logError(exception) }
    }

    /**
     * Determines if an error is recoverable (network-related).
     * @param throwable The error to check
     * @return true if error is recoverable and should trigger retry
     */
    fun isRecoverableError(throwable: Throwable): Boolean {
        return when (throwable) {
            is UnknownHostException,
            is SocketTimeoutException,
            is IOException -> true
            else -> false
        }
    }

    private fun logError(exception: Throwable) {
        Log.e(
            TAG,
            "RadioCult request failed: ${exception.message}",
            exception
        )
    }

    companion object {
        private const val TAG = "RadioRepository"
    }
}
