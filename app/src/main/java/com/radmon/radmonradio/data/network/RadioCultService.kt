/**
 * Retrofit service for RadioCult, the station's radio platform.
 * Named and shaped to match the iOS app's RadioCultClient.
 */
package com.radmon.radmonradio.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface RadioCultService {

    /** What's on air right now. */
    @GET("api/station/radio-monash/schedule/live")
    suspend fun liveStatus(): ScheduleLiveResponse

    /** Recently played songs. */
    @GET("api/station/radio-monash/streaming/history/latest-results")
    suspend fun history(@Query("limit") limit: Int): HistoryResponse
}
