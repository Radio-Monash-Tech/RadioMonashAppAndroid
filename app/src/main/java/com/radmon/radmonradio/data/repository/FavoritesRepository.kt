/**
 * Repository for managing user favorite tracks.
 * Uses DataStore for persistent storage with JSON serialization.
 */
package com.radmon.radmonradio.data.repository
import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.radmon.radmonradio.data.model.FavoriteTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
@SuppressLint("StaticFieldLeak")
class FavoritesRepository private constructor(private val context: Context) {
    companion object {
        private const val DATASTORE_NAME = "favorites_store"
        private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)
        private val KEY_FAVORITES_JSON = stringPreferencesKey("favorites_json")
        @Volatile
        private var INSTANCE: FavoritesRepository? = null
        fun getInstance(context: Context): FavoritesRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FavoritesRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
    private val gson = Gson()
    private val listType = TypeToken.getParameterized(List::class.java, FavoriteTrack::class.java).type
    val favorites: Flow<List<FavoriteTrack>> = context.favoritesDataStore.data
        .map { prefs ->
            val json = prefs[KEY_FAVORITES_JSON]
            if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    gson.fromJson(json, listType) as? List<FavoriteTrack> ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("FavoritesRepository", "Failed to parse favorites JSON", e)
                    emptyList()
                }
            }
        }
    suspend fun toggleFavorite(track: FavoriteTrack) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES_JSON]
            val list: MutableList<FavoriteTrack> = if (current.isNullOrBlank()) {
                mutableListOf()
            } else {
                try {
                    (gson.fromJson(current, listType) as? List<FavoriteTrack>)?.toMutableList() ?: mutableListOf()
                } catch (e: Exception) {
                    android.util.Log.e("FavoritesRepository", "Failed to parse favorites for toggle", e)
                    mutableListOf()
                }
            }
            val idx = list.indexOfFirst { it.title.equals(track.title, true) && it.artist.equals(track.artist, true) }
            if (idx >= 0) list.removeAt(idx) else list.add(track)
            try {
                prefs[KEY_FAVORITES_JSON] = gson.toJson(list)
            } catch (e: Exception) {
                android.util.Log.e("FavoritesRepository", "Failed to save favorites", e)
            }
        }
    }
}
