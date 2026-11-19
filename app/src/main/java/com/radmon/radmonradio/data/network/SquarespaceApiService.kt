package com.radmon.radmonradio.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface SquarespaceApiService {

    /** One page of journalism articles, newest first; Squarespace does the tag filtering so paging still works. */
    @GET("journalism-all")
    suspend fun articles(
        @Query("format") format: String = "json",
        @Query("tag") tag: String? = null,
        @Query("offset") offset: Long? = null
    ): SquarespaceCollectionResponse
}
