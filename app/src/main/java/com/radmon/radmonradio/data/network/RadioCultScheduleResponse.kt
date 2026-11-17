package com.radmon.radmonradio.data.network

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.radmon.radmonradio.data.model.ScheduledShow
import com.radmon.radmonradio.data.model.ShowSeries
import java.time.Instant

data class ScheduleResponse(
    val schedules: List<ScheduleEvent>?
)

/** One episode on the schedule. */
data class ScheduleEvent(
    val id: String,
    // the same for every episode of a recurring show
    val originalId: String?,
    val title: String,
    val startDateUtc: String,
    val endDateUtc: String,
    // RadioCult's rich-text (TipTap) description json; may be absent
    val description: JsonElement?,
    // only present for presenter-hosted shows; playlist shows have none
    val artists: List<ScheduleArtist>?
)

data class ScheduleArtist(
    val id: String,
    val name: String
)

/** Turns the raw json into the app's own show models, in start order. */
fun ScheduleResponse.toScheduledShows(): List<ScheduledShow> {
    val shows = schedules.orEmpty().mapNotNull { event ->
        val start = parseInstant(event.startDateUtc) ?: return@mapNotNull null
        val end = parseInstant(event.endDateUtc) ?: return@mapNotNull null
        if (end <= start) return@mapNotNull null

        val summary = event.description
            ?.let { plainText(it) }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val series = ShowSeries(
            id = event.originalId ?: event.id,
            title = event.title.trim(),
            hosts = event.artists.orEmpty().map { it.name },
            summary = summary
        )
        ScheduledShow(id = event.id, series = series, start = start, end = end)
    }
    return shows.sortedBy { it.start }
}

/** Radiocult's dates are ISO-8601, with or without fractional seconds — [Instant.parse] handles both. */
private fun parseInstant(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()

/**
 * Flattens RadioCult's TipTap rich-text json into plain text, joining paragraph-level
 * nodes with a blank line. Styling (bold/italic/links) isn't carried over.
 */
private fun plainText(node: JsonElement): String {
    if (!node.isJsonObject) return ""
    val obj = node.asJsonObject
    return when (obj.typeOrNull()) {
        "text" -> obj.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        "hardBreak" -> "\n"
        "doc", "blockquote", "bulletList", "orderedList", "listItem" -> joinChildren(obj, "\n\n")
        else -> joinChildren(obj, "")
    }
}

private fun joinChildren(obj: JsonObject, separator: String): String {
    val content = obj.get("content") as? JsonArray ?: return ""
    val parts = content.mapNotNull { child -> plainText(child).takeIf { it.isNotEmpty() } }
    return parts.joinToString(separator)
}

private fun JsonObject.typeOrNull(): String? = get("type")?.takeIf { it.isJsonPrimitive }?.asString
