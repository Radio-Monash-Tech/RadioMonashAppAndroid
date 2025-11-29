package com.radmon.radmonradio.data.model

/**
 * Represents a track that has been marked as favorite by the user.
 *
 * @property title The song title
 * @property artist The artist name
 * @property artworkUrl Optional URL to the album artwork
 * @property likedAt Timestamp when the track was favorited (milliseconds since epoch)
 */
data class FavoriteTrack(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val likedAt: Long
)
