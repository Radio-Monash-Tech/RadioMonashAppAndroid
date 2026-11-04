/**
 * Core playback models, named to match the iOS app's RadioCult client models.
 */
package com.radmon.radmonradio.data.model

import java.util.Date

data class NowPlaying(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val startedAt: Date?,
    val duration: Double?
) {
    companion object {
        val offAir = NowPlaying(
            title = "Radio Monash",
            artist = "Default playlist",
            artworkUrl = null,
            startedAt = null,
            duration = null
        )
    }
}

data class LiveShow(
    val name: String,
    val host: String?,
    val endsAt: Date?
)

data class PlayedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val playedAt: Date,
    val artworkUrl: String?
)
