package com.radmon.radmonradio.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Song.link API.
 * Provides multi-platform music streaming links from a single source URL.
 */
interface SongLinkApiService {
    /**
     * Fetches platform-specific links for a track.
     * @param trackUrl iTunes or other platform URL to look up
     * @param userCountry Country code for region-specific links (default: "US")
     * @return Platform links for Spotify, Apple Music, YouTube Music, etc.
     */
    @GET("v1-alpha.1/links")
    suspend fun getPlatformLinks(
        @Query("url") trackUrl: String,
        @Query("userCountry") userCountry: String = "US"
    ): SongLinkResponse
}
