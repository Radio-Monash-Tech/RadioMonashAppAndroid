/** The station's clock */
package com.radmon.radmonradio.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MelbourneTime {
    val zone: ZoneId = ZoneId.of("Australia/Melbourne")

    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).withZone(zone)
    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH).withZone(zone)
    private val weekdayAbbrevFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH).withZone(zone)
    private val dayNumberFormatter = DateTimeFormatter.ofPattern("d", Locale.ENGLISH).withZone(zone)

    fun today(): LocalDate = LocalDate.now(zone)

    /** The seven days of the week containing [referenceDate], Monday first. */
    fun weekDays(referenceDate: LocalDate = today()): List<LocalDate> {
        val monday = referenceDate.with(DayOfWeek.MONDAY)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun startOfDay(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant()

    fun isSameDay(instant: Instant, date: LocalDate): Boolean =
        instant.atZone(zone).toLocalDate() == date

    /** "09:00" */
    fun clock(instant: Instant): String = clockFormatter.format(instant)

    /** "Mon 3 Nov" */
    fun dayLabel(instant: Instant): String = dayLabelFormatter.format(instant)

    /** "MON", for a day chip */
    fun weekdayAbbrev(date: LocalDate): String = weekdayAbbrevFormatter.format(date).uppercase(Locale.ENGLISH)

    /** "3", for a day chip */
    fun dayNumber(date: LocalDate): String = dayNumberFormatter.format(date)
}
