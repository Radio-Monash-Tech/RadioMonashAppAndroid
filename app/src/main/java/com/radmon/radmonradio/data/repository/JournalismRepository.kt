/** Fetches journalism articles from Squarespace and persists a per-topic offline copy. */
package com.radmon.radmonradio.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.data.model.ArticlePage
import com.radmon.radmonradio.data.model.ArticleTopic
import com.radmon.radmonradio.data.network.ApiClient
import com.radmon.radmonradio.data.network.toArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import java.time.Instant

@SuppressLint("StaticFieldLeak")
class JournalismRepository private constructor(private val context: Context) {
    private val service = ApiClient.squarespaceApiService

    private val instantAdapter = object : JsonSerializer<Instant>, JsonDeserializer<Instant> {
        override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext) =
            JsonPrimitive(src.toString())

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant =
            Instant.parse(json.asString)
    }
    private val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, instantAdapter)
        .create()
    private val articleListType = TypeToken.getParameterized(List::class.java, Article::class.java).type

    /**
     * One page, newest first; Squarespace does the tag filtering so paging still works.
     * @return Result containing the page of articles, or error
     */
    suspend fun articles(tag: String? = null, offset: Long? = null): Result<ArticlePage> = withContext(Dispatchers.IO) {
        runCatching {
            val response = service.articles(tag = tag, offset = offset)
            val articles = response.items.orEmpty().mapNotNull { it.toArticle() }
            // only an offset when squarespace says there's another page
            val nextOffset = if (response.pagination?.nextPage == true) response.pagination.nextPageOffset else null
            ArticlePage(articles, nextOffset)
        }.onFailure { exception -> logError(exception) }
    }

    /** The last successfully fetched first page for [topic] and when it was saved — null if nothing's cached yet. */
    suspend fun cachedArticles(topic: ArticleTopic?): Pair<List<Article>, Instant>? {
        val prefs = context.journalismDataStore.data.first()
        val json = prefs[cacheKey(topic)] ?: return null
        val savedAtMillis = prefs[cachedAtKey(topic)] ?: return null
        val articles = runCatching {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, articleListType) as? List<Article>
        }.getOrNull() ?: return null
        return articles to Instant.ofEpochMilli(savedAtMillis)
    }

    suspend fun saveCache(topic: ArticleTopic?, articles: List<Article>) {
        context.journalismDataStore.edit { prefs ->
            prefs[cacheKey(topic)] = gson.toJson(articles)
            prefs[cachedAtKey(topic)] = System.currentTimeMillis()
        }
    }

    // one cached page per topic: "articles_all", "articles_pop_culture" ...
    private fun topicSlug(topic: ArticleTopic?) = topic?.name?.lowercase() ?: "all"
    private fun cacheKey(topic: ArticleTopic?) = stringPreferencesKey("articles_${topicSlug(topic)}_json")
    private fun cachedAtKey(topic: ArticleTopic?) = longPreferencesKey("articles_${topicSlug(topic)}_at")

    private fun logError(exception: Throwable) {
        Log.e(TAG, "Squarespace request failed: ${exception.message}", exception)
    }

    companion object {
        private const val TAG = "JournalismRepository"
        private const val DATASTORE_NAME = "journalism_store"
        private val Context.journalismDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

        @Volatile
        private var INSTANCE: JournalismRepository? = null
        fun getInstance(context: Context): JournalismRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: JournalismRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
