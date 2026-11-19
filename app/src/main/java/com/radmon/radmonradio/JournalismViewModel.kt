/** Backs the Journalism tab: the article feed, topic filter, pagination, and an offline copy to fall back on. */

package com.radmon.radmonradio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radmon.radmonradio.data.model.Article
import com.radmon.radmonradio.data.model.ArticleTopic
import com.radmon.radmonradio.data.repository.JournalismRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant

sealed class JournalismPhase {
    data object Idle : JournalismPhase()
    data object Loading : JournalismPhase()
    data object Loaded : JournalismPhase()
    data class Failed(val message: String) : JournalismPhase()
}

data class JournalismUiState(
    val phase: JournalismPhase = JournalismPhase.Idle,
    val articles: List<Article> = emptyList(),
    // nil means all topics
    val topic: ArticleTopic? = null,
    // set only while showing the saved copy because loading failed
    val offlineCopyAt: Instant? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false
)

class JournalismViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalismRepository.getInstance(application)

    private val _uiState = MutableStateFlow(JournalismUiState())
    val uiState: StateFlow<JournalismUiState> = _uiState.asStateFlow()

    // where the next page starts; null when there are no more
    private var nextOffset: Long? = null
    private var loadedAt: Instant? = null
    private val staleAfter: Duration = Duration.ofMinutes(15)

    init {
        // show the saved copy straight away; the first load replaces it
        viewModelScope.launch {
            showSavedCopy(_uiState.value.topic)
        }
    }

    private suspend fun showSavedCopy(topic: ArticleTopic?) {
        repository.cachedArticles(topic)?.let { (articles, _) ->
            _uiState.update { it.copy(articles = articles) }
        }
    }

    /** Switches topic and loads its first page. */
    fun selectTopic(topic: ArticleTopic?) {
        if (topic == _uiState.value.topic) return
        nextOffset = null
        _uiState.update { it.copy(topic = topic, offlineCopyAt = null, loadMoreFailed = false) }
        viewModelScope.launch { showSavedCopy(topic) }
        load()
    }

    /** Loads unless it was loaded in the last 15 minutes. */
    fun loadIfNeeded() {
        val last = loadedAt
        if (last != null && Duration.between(last, Instant.now()) < staleAfter) return
        if (_uiState.value.phase == JournalismPhase.Loading) return
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            val topic = _uiState.value.topic
            _uiState.update { it.copy(phase = JournalismPhase.Loading) }

            repository.articles(tag = topic?.siteTag)
                .onSuccess { page ->
                    // another topic was picked while this one loaded
                    if (topic != _uiState.value.topic) return@onSuccess
                    loadedAt = Instant.now()
                    nextOffset = page.nextOffset
                    repository.saveCache(topic, page.articles)
                    _uiState.update {
                        it.copy(articles = page.articles, phase = JournalismPhase.Loaded, offlineCopyAt = null, loadMoreFailed = false)
                    }
                }
                .onFailure { error ->
                    if (topic != _uiState.value.topic) return@onFailure
                    val current = _uiState.value
                    if (current.articles.isNotEmpty()) {
                        // keep showing the saved copy rather than an error
                        val cachedAt = repository.cachedArticles(topic)?.second ?: Instant.now()
                        _uiState.update { it.copy(phase = JournalismPhase.Loaded, offlineCopyAt = cachedAt) }
                    } else {
                        _uiState.update { it.copy(phase = JournalismPhase.Failed(errorMessage(error))) }
                    }
                }
        }
    }

    /** The next page, added to the end. */
    fun loadMore() {
        val offset = nextOffset ?: return
        if (_uiState.value.isLoadingMore || _uiState.value.phase != JournalismPhase.Loaded) return
        val topic = _uiState.value.topic

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.articles(tag = topic?.siteTag, offset = offset)
                .onSuccess { page ->
                    if (topic != _uiState.value.topic) return@onSuccess
                    nextOffset = page.nextOffset
                    // skip any already in the list
                    val known = _uiState.value.articles.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            articles = it.articles + page.articles.filter { article -> article.id !in known },
                            isLoadingMore = false,
                            loadMoreFailed = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
                }
        }
    }

    private fun errorMessage(error: Throwable): String = when (error) {
        is UnknownHostException -> "You're offline. Connect to the internet to see articles."
        is SocketTimeoutException -> "Connection timed out. Try again."
        is IOException -> "Couldn't load articles. Check your connection."
        else -> "Couldn't load articles."
    }
}
