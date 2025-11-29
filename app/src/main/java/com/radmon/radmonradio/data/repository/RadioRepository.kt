package com.radmon.radmonradio.data.repository

import android.util.Log
import com.radmon.radmonradio.data.network.ApiClient
import com.radmon.radmonradio.data.network.MetadataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repository for fetching radio station metadata.
 * Handles API calls and error handling for live stream information.
 */
class RadioRepository {
    private val apiService = ApiClient.apiService

    /**
     * Fetches the currently playing track information.
     * @return Result containing metadata response or error
     */
    suspend fun getCurrentTrack(): Result<MetadataResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getNowPlaying()
            validateResponse(response)
            response
        }.onFailure { exception ->
            logError(exception)
        }
    }

    /**
     * Validates API response contains required metadata.
     * @throws IllegalArgumentException if metadata is null
     */
    private fun validateResponse(response: MetadataResponse) {
        requireNotNull(response.result?.metadata) {
            "API response validation failed: metadata is null"
        }
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
            "Metadata retrieval failed: ${exception.message}",
            exception
        )
    }

    companion object {
        private const val TAG = "RadioRepository"
    }
}
