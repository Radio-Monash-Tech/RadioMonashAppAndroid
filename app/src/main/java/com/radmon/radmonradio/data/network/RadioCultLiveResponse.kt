package com.radmon.radmonradio.data.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.radmon.radmonradio.data.model.LiveShow
import com.radmon.radmonradio.data.model.NowPlaying
import java.lang.reflect.Type
import java.util.Date

data class ScheduleLiveResponse(
    val success: Boolean,
    val result: LiveResult
)

data class LiveResult(
    val status: String,
    val metadata: LiveMetadata?,
    // only set when a show is scheduled; otherwise RadioCult sends a plain string
    val show: LiveShowEvent?
)

/** RadioCult's show, cut down to what the player needs. */
data class LiveShowEvent(
    val title: String,
    val artistIds: List<String>?,
    val media: Media?
) {
    data class Media(val type: String?)
}

/** The song playing. */
data class LiveMetadata(
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Double?,
    val playoutStartUnixTimestamp: Double?,
    val artwork: ArtworkVariants?,
    // only set when the automated playlist plays a library song
    val trackId: String?
)

private const val STATUS_SCHEDULE = "schedule"

/** `content` is only a [LiveShowEvent] object when status is "schedule"; otherwise it's a plain string. */
class LiveResultDeserializer : JsonDeserializer<LiveResult> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LiveResult {
        val obj = json.asJsonObject
        val status = obj.get("status")?.asString ?: "offAir"
        val metadata = obj.get("metadata")
            ?.takeIf { it.isJsonObject }
            ?.let { context.deserialize<LiveMetadata>(it, LiveMetadata::class.java) }
        val show = if (status == STATUS_SCHEDULE) {
            obj.get("content")
                ?.takeIf { it.isJsonObject }
                ?.let { context.deserialize<LiveShowEvent>(it, LiveShowEvent::class.java) }
        } else {
            null
        }
        return LiveResult(status, metadata, show)
    }
}

/** The song and show, in the app's own types. */
fun ScheduleLiveResponse.toNowPlaying(): Pair<NowPlaying, LiveShow?> {
    val metadata = result.metadata
    // the show, only once a presenter is actually on; playlist shows count as music
    val onAirShow = result.show?.takeIf { show ->
        val mediaType = show.media?.type
        if (mediaType == "playlist") return@takeIf false
        // a live slot still playing library songs means the presenter hasn't connected yet
        val awaitingPresenter = mediaType == "live" && metadata?.trackId != null
        !awaitingPresenter
    }
    val liveShow = onAirShow?.let { LiveShow(name = it.title, host = null, endsAt = null) }
    // during a live show the "song" is nothing or the show's name, so keep only the show
    if (metadata == null || (liveShow != null && metadata.artist == null)) {
        return NowPlaying.offAir to liveShow
    }
    val startedAt = metadata.playoutStartUnixTimestamp?.let { Date((it * 1000).toLong()) }
    val track = NowPlaying(
        title = metadata.title,
        artist = metadata.artist ?: "Radio Monash",
        artworkUrl = metadata.artwork?.best(),
        startedAt = startedAt,
        duration = metadata.duration
    )
    return track to liveShow
}
