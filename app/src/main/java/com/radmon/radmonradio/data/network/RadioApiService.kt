package com.radmon.radmonradio.data.network

import retrofit2.http.GET

/**
 * Retrofit service interface for Radio Monash API.
 * Provides access to live radio stream metadata.
 */
interface RadioApiService {

    /**
     * Fetches currently playing track information.
     * @return Metadata response containing current song, artist, and artwork
     */
    @GET("api/station/radio-monash/schedule/live")
    suspend fun getNowPlaying(): MetadataResponse
}