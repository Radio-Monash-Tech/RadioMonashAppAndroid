/** A radio show (whatever day it's on) and one scheduled episode of it. */
package com.radmon.radmonradio.data.model

import android.icu.text.ListFormatter
import com.radmon.radmonradio.util.MelbourneTime
import java.time.Instant

data class ShowSeries(
    val id: String,
    val title: String,
    val hosts: List<String> = emptyList(),
    val summary: String? = null
) {
    /** "Alex, Sam, and Jo" — null when there are no hosts. */
    val hostLine: String?
        get() = if (hosts.isEmpty()) null else ListFormatter.getInstance().format(hosts)
}

data class ScheduledShow(
    val id: String,
    val series: ShowSeries,
    val start: Instant,
    val end: Instant
) {
    fun isOnAir(at: Instant = Instant.now()): Boolean = !start.isAfter(at) && at.isBefore(end)

    /** "09:00 – 10:00" */
    val timeRange: String
        get() = "${MelbourneTime.clock(start)} – ${MelbourneTime.clock(end)}"

    /** "Mon 3 Nov", in Melbourne time */
    val dayLabel: String
        get() = MelbourneTime.dayLabel(start)
}
