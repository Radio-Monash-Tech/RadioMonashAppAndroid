package com.radmon.radmonradio.data.network

import com.google.gson.annotations.SerializedName

/**
 * Response from iTunes Search API.
 */
data class ITunesSearchResponse(
    @SerializedName("resultCount")
    val resultCount: Int,
    @SerializedName("results")
    val results: List<ITunesTrack>
)

/**
 * Individual track result from iTunes search.
 *
 * @property trackViewUrl URL to view track on iTunes
 * @property trackName Name of the track
 * @property artistName Name of the artist
 * @property artworkUrl100 URL to 100x100 album artwork
 */
data class ITunesTrack(
    @SerializedName("trackViewUrl")
    val trackViewUrl: String?,
    @SerializedName("trackName")
    val trackName: String?,
    @SerializedName("artistName")
    val artistName: String?,
    @SerializedName("artworkUrl100")
    val artworkUrl100: String?
)
