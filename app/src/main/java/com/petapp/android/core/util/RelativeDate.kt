package com.petapp.android.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The app's one standard way to show a date relative to today -- "Hoy", "Ayer",
 * "Mañana", "Hace N días/meses/años", "Dentro de N días/meses/años" -- instead of each
 * screen picking its own absolute "d MMM yyyy" format ad hoc.
 */
fun relativeDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "Hoy"
        days == -1L -> "Ayer"
        days == 1L -> "Mañana"
        days < 0 -> {
            val period = Period.between(date, today)
            when {
                period.years >= 1 -> "Hace ${period.years} ${plural(period.years, "año", "años")}"
                period.months >= 1 -> "Hace ${period.months} ${plural(period.months, "mes", "meses")}"
                else -> "Hace ${-days} días"
            }
        }
        else -> {
            val period = Period.between(today, date)
            when {
                period.years >= 1 -> "Dentro de ${period.years} ${plural(period.years, "año", "años")}"
                period.months >= 1 -> "Dentro de ${period.months} ${plural(period.months, "mes", "meses")}"
                else -> "Dentro de $days días"
            }
        }
    }
}

/**
 * Accepts either a plain "yyyy-MM-dd" date or a full "yyyy-MM-ddTHH:mm:ss..." datetime
 * (only the date portion matters here), falling back to the raw string if unparseable.
 */
fun relativeDateLabel(iso: String): String =
    runCatching { relativeDateLabel(LocalDate.parse(iso.take(10))) }.getOrDefault(iso)

/**
 * Same as [relativeDateLabel] plus a trailing ", HH:mm" -- for a genuine zoned/UTC
 * instant (DRF's default DateTimeField, e.g. PetEvent.event_date, Document.created_at),
 * converted to the device's local zone before formatting. For Reminder.due_date, use
 * [relativeReminderDateTimeLabel] instead -- it deliberately skips zone conversion.
 */
fun relativeDateTimeLabel(iso: String): String = runCatching {
    val dateTime = Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDateTime()
    dateTimeLabel(dateTime)
}.getOrDefault(iso)

/**
 * For Reminder.due_date specifically: this client always sends a naive
 * "yyyy-MM-ddTHH:mm:ss" local string with no zone suffix (see
 * AnadirRecordatorioScreen.kt), but rows created before that migration may come back
 * with a "Z"/offset suffix from the DB cast -- strip anything past the first 19 chars
 * so both shapes parse the same way, deliberately with no zone conversion.
 */
fun relativeReminderDateTimeLabel(iso: String): String = runCatching {
    dateTimeLabel(LocalDateTime.parse(iso.take(19)))
}.getOrDefault(iso)

private fun dateTimeLabel(dateTime: LocalDateTime): String =
    "${relativeDateLabel(dateTime.toLocalDate())}, ${"%02d:%02d".format(dateTime.hour, dateTime.minute)}"

private fun plural(count: Int, singular: String, plural: String) = if (count == 1) singular else plural
