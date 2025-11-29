package com.radmon.radmonradio.data.network

import com.google.gson.annotations.SerializedName

/**
 * API response containing current radio stream metadata.
 */
data class MetadataResponse(
    val result: Result?
)

/**
 * Wrapper for metadata result.
 */
data class Result(
    val metadata: Metadata?
)

/**
 * Track metadata from the radio API.
 *
 * @property title Song title
 * @property artist Artist name
 * @property artwork Album artwork URLs
 * @property duration Track duration in seconds
 * @property startTimestamp Unix timestamp when track started playing
 */
data class Metadata(
    val title: String?,
    val artist: String?,
    val artwork: Artwork?,
    val duration: Double?,
    @SerializedName("playoutStartUnixTimestamp")
    val startTimestamp: Long?
)

/**
 * Album artwork URLs.
 */
data class Artwork(
    val default: String?
)