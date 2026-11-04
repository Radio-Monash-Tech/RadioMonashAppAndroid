package com.radmon.radmonradio.data.repository
import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*

@SuppressLint("StaticFieldLeak")
class ListeningStatsRepository private constructor(private val context: Context) {
    companion object {
        private const val DATASTORE_NAME = "listening_stats"
        @Volatile
        private var INSTANCE: ListeningStatsRepository? = null
        private val Context.statsDataStore: DataStore<Preferences> by preferencesDataStore(
name = DATASTORE_NAME
        )
        private val KEY_TOTAL_LISTENING_TIME = longPreferencesKey("total_listening_time")
        private val KEY_SESSION_COUNT = longPreferencesKey("session_count")
        private val KEY_LAST_SESSION_START = longPreferencesKey("last_session_start")
        private const val TIME_SLOT_MORNING = "morning"
        private const val TIME_SLOT_AFTERNOON = "afternoon"
        private const val TIME_SLOT_EVENING = "evening"
        private const val TIME_SLOT_NIGHT = "night"
        fun getInstance(context: Context): ListeningStatsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ListeningStatsRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    suspend fun recordSessionStart(timestamp: Long = System.currentTimeMillis()) {
        context.statsDataStore.edit { preferences ->
            preferences[KEY_LAST_SESSION_START] = timestamp
        }
    }
    
    suspend fun recordSessionEnd(timestamp: Long = System.currentTimeMillis()) {
        context.statsDataStore.edit { preferences ->
            val sessionStart = preferences[KEY_LAST_SESSION_START] ?: timestamp
            val sessionDuration = timestamp - sessionStart
            if (sessionDuration > 0) {
                val currentTotal = preferences[KEY_TOTAL_LISTENING_TIME] ?: 0L
                preferences[KEY_TOTAL_LISTENING_TIME] = currentTotal + sessionDuration
                val currentCount = preferences[KEY_SESSION_COUNT] ?: 0L
                preferences[KEY_SESSION_COUNT] = currentCount + 1
                updateTimeOfDayStats(preferences, sessionStart, sessionDuration)
                updateDayOfWeekStats(preferences, sessionStart)
            }
        }
    }
    
    private fun updateTimeOfDayStats(
        preferences: MutablePreferences,
        sessionStart: Long,
        duration: Long
    ) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = sessionStart
        }
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val timeSlot = when (hourOfDay) {
            in 0..5 -> TIME_SLOT_NIGHT
            in 6..11 -> TIME_SLOT_MORNING
            in 12..17 -> TIME_SLOT_AFTERNOON
            else -> TIME_SLOT_EVENING
        }
        val key = longPreferencesKey("time_slot_$timeSlot")
        val currentTime = preferences[key] ?: 0L
        preferences[key] = currentTime + duration
    }
    
    private fun updateDayOfWeekStats(
        preferences: MutablePreferences,
        sessionStart: Long
    ) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = sessionStart
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val key = longPreferencesKey("day_count_$dayOfWeek")
        val currentCount = preferences[key] ?: 0L
        preferences[key] = currentCount + 1
    }
    
    fun getListeningStats(): Flow<ListeningStats> {
        return context.statsDataStore.data.map { preferences ->
            val totalTime = preferences[KEY_TOTAL_LISTENING_TIME] ?: 0L
            val sessionCount = preferences[KEY_SESSION_COUNT] ?: 0L
            val timeSlots: Map<String, Long> = mapOf(
                TIME_SLOT_MORNING to (preferences[longPreferencesKey("time_slot_$TIME_SLOT_MORNING")] ?: 0L),
                TIME_SLOT_AFTERNOON to (preferences[longPreferencesKey("time_slot_$TIME_SLOT_AFTERNOON")] ?: 0L),
                TIME_SLOT_EVENING to (preferences[longPreferencesKey("time_slot_$TIME_SLOT_EVENING")] ?: 0L),
                TIME_SLOT_NIGHT to (preferences[longPreferencesKey("time_slot_$TIME_SLOT_NIGHT")] ?: 0L)
            )
            val favoriteTimeSlot = timeSlots.maxByOrNull { it.value }?.key ?: TIME_SLOT_MORNING
            ListeningStats(
                totalListeningTimeMs = totalTime,
                sessionCount = sessionCount,
                averageSessionDurationMs = if (sessionCount > 0) totalTime / sessionCount else 0L,
                favoriteTimeOfDay = favoriteTimeSlot,
                timeSlotDistribution = timeSlots
            )
        }
    }
    
    suspend fun getCurrentStats(): ListeningStats {
        return getListeningStats().first()
    }
    
    suspend fun resetStats() {
        context.statsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class ListeningStats(
    val totalListeningTimeMs: Long,
    val sessionCount: Long,
    val averageSessionDurationMs: Long,
    val favoriteTimeOfDay: String,
    val timeSlotDistribution: Map<String, Long>
) {
    
    fun getFormattedTotalTime(): String {
        val hours = totalListeningTimeMs / (1000 * 60 * 60)
        val minutes = (totalListeningTimeMs / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
    
    fun getFormattedAverageSession(): String {
        val minutes = averageSessionDurationMs / (1000 * 60)
        return when {
            minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
    
    fun getFavoriteTimeDisplay(): String {
        return when (favoriteTimeOfDay) {
            "morning" -> "Morning (6AM-12PM)"
            "afternoon" -> "Afternoon (12PM-6PM)"
            "evening" -> "Evening (6PM-12AM)"
            "night" -> "Night (12AM-6AM)"
            else -> "Not enough data"
        }
    }
}
