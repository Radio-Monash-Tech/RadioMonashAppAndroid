/** Persists the schedule tab's offline copy and which shows the user follows. */
package com.radmon.radmonradio.data.repository

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.radmon.radmonradio.data.model.ScheduledShow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

@SuppressLint("StaticFieldLeak")
class ScheduleRepository private constructor(private val context: Context) {

    private val instantAdapter = object : JsonSerializer<Instant>, JsonDeserializer<Instant> {
        override fun serialize(src: Instant, typeOfSrc: java.lang.reflect.Type, context: com.google.gson.JsonSerializationContext) =
            JsonPrimitive(src.toString())

        override fun deserialize(json: com.google.gson.JsonElement, typeOfT: java.lang.reflect.Type, context: com.google.gson.JsonDeserializationContext): Instant =
            Instant.parse(json.asString)
    }
    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, instantAdapter)
        .create()
    private val showListType = TypeToken.getParameterized(List::class.java, ScheduledShow::class.java).type

    val followedShowIds: Flow<Set<String>> = context.scheduleDataStore.data
        .map { prefs -> prefs[KEY_FOLLOWED_IDS] ?: emptySet() }

    suspend fun toggleFollow(seriesId: String) {
        context.scheduleDataStore.edit { prefs ->
            val current = prefs[KEY_FOLLOWED_IDS] ?: emptySet()
            prefs[KEY_FOLLOWED_IDS] = if (seriesId in current) current - seriesId else current + seriesId
        }
    }

    /** The last successfully fetched schedule and when it was saved — null if nothing's cached yet. */
    suspend fun cachedShows(): Pair<List<ScheduledShow>, Instant>? {
        val prefs = context.scheduleDataStore.data.first()
        val json = prefs[KEY_CACHED_SHOWS_JSON] ?: return null
        val savedAtMillis = prefs[KEY_CACHED_AT] ?: return null
        val shows = runCatching {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, showListType) as? List<ScheduledShow>
        }.getOrNull() ?: return null
        return shows to Instant.ofEpochMilli(savedAtMillis)
    }

    suspend fun saveCache(shows: List<ScheduledShow>) {
        context.scheduleDataStore.edit { prefs ->
            prefs[KEY_CACHED_SHOWS_JSON] = gson.toJson(shows)
            prefs[KEY_CACHED_AT] = System.currentTimeMillis()
        }
    }

    companion object {
        private const val DATASTORE_NAME = "schedule_store"
        private val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)
        private val KEY_FOLLOWED_IDS = stringSetPreferencesKey("followed_show_ids")
        private val KEY_CACHED_SHOWS_JSON = stringPreferencesKey("cached_shows_json")
        private val KEY_CACHED_AT = longPreferencesKey("cached_at")

        @Volatile
        private var INSTANCE: ScheduleRepository? = null
        fun getInstance(context: Context): ScheduleRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScheduleRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
