package com.radmon.radmonradio.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.radmon.radmonradio.data.model.ScheduledShow
import java.time.Duration
import java.time.Instant

object ShowReminderScheduler {
    const val CHANNEL_ID = "show_reminders"
    private const val WORK_TAG = "show-reminder"
    private val leadTime: Duration = Duration.ofMinutes(10)

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Show reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "A reminder 10 minutes before an episode of a show you follow"
        }
        manager.createNotificationChannel(channel)
    }

    fun sync(context: Context, shows: List<ScheduledShow>, followedIds: Set<String>) {
        val workManager = WorkManager.getInstance(context)
        // remove every old reminder first, then re-add what's still due
        workManager.cancelAllWorkByTag(WORK_TAG)

        val now = Instant.now()
        val upcoming = shows.filter { show ->
            followedIds.contains(show.series.id) && show.start.minus(leadTime).isAfter(now)
        }

        for (show in upcoming) {
            val fireAt = show.start.minus(leadTime)
            val delay = Duration.between(now, fireAt).let { if (it.isNegative) Duration.ZERO else it }
            // "with Alex · 09:00 – 10:00"
            val body = buildList {
                show.series.hostLine?.let { add("with $it") }
                add(show.timeRange)
            }.joinToString(" · ")

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay)
                .addTag(WORK_TAG)
                .setInputData(
                    Data.Builder()
                        .putString(ReminderWorker.KEY_SHOW_ID, show.id)
                        .putString(ReminderWorker.KEY_TITLE, "${show.series.title} starts in 10 minutes")
                        .putString(ReminderWorker.KEY_BODY, body)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork("$WORK_TAG-${show.id}", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
