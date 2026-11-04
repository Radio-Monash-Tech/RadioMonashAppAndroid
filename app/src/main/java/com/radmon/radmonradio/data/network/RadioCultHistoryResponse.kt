package com.radmon.radmonradio.data.network

import com.radmon.radmonradio.data.model.PlayedTrack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HistoryResponse(
    val success: Boolean,
    val data: List<HistoryEntry>
)

/** One played song. */
data class HistoryEntry(
    val playoutStart: String,
    val title: String,
    val artist: String?,
    val artwork: ArtworkVariants?
)

private val radioCultDateFormats = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX"
).map { pattern -> SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }

private fun parseRadioCultDate(value: String): Date {
    for (format in radioCultDateFormats) {
        runCatching { format.parse(value) }.getOrNull()?.let { return it }
    }
    return Date()
}

/** Turned into the app's own played tracks. */
fun HistoryResponse.toPlayedTracks(): List<PlayedTrack> = data.map { entry ->
    PlayedTrack(
        id = entry.playoutStart,
        title = entry.title,
        artist = entry.artist ?: "Radio Monash",
        playedAt = parseRadioCultDate(entry.playoutStart),
        artworkUrl = entry.artwork?.best(listOf("256x256", "default", "original"))
    )
}
