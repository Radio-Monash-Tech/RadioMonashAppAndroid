package com.radmon.radmonradio.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for iTunes Search API.
 * Used to search for track information and artwork.
 */
interface ITunesApiService {
    /**
     * Searches iTunes catalog for matching tracks.
     * @param searchTerm Artist and song name to search for
     * @param entity Type of content to search (default: "song")
     * @param limit Maximum number of results (default: 5)
     * @return Search results containing track information
     */
    @GET("search")
    suspend fun searchTrack(
        @Query("term") searchTerm: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 5
    ): ITunesSearchResponse
}
