/** Backs the Schedule tab: this week's shows, the selected day, follow state, and an offline copy to fall back on. */

package com.radmon.radmonradio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radmon.radmonradio.data.model.ScheduledShow
import com.radmon.radmonradio.data.repository.RadioRepository
import com.radmon.radmonradio.data.repository.ScheduleRepository
import com.radmon.radmonradio.reminders.ShowReminderScheduler
import com.radmon.radmonradio.util.MelbourneTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

sealed class SchedulePhase {
    data object Idle : SchedulePhase()
    data object Loading : SchedulePhase()
    data object Loaded : SchedulePhase()
    data class Failed(val message: String) : SchedulePhase()
}

data class ScheduleUiState(
    val phase: SchedulePhase = SchedulePhase.Idle,
    val shows: List<ScheduledShow> = emptyList(),
    // set only while showing the saved copy because loading failed
    val offlineCopyAt: Instant? = null,
    val selectedDay: LocalDate = MelbourneTime.today(),
    val followedIds: Set<String> = emptySet()
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RadioRepository()
    private val scheduleRepository = ScheduleRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // ticks once a minute so on-air highlighting advances without a manual refresh
    private val _now = MutableStateFlow(Instant.now())
    val now: StateFlow<Instant> = _now.asStateFlow()

    private var loadedAt: Instant? = null
    private val staleAfter: Duration = Duration.ofMinutes(15)

    val week: List<LocalDate> get() = MelbourneTime.weekDays()

    init {
        ShowReminderScheduler.ensureChannel(application)

        // show the saved copy straight away; the first load replaces it
        viewModelScope.launch {
            scheduleRepository.cachedShows()?.let { (shows, _) ->
                _uiState.update { it.copy(shows = shows) }
            }
        }

        // rebuild reminders whenever the followed set (or later, the schedule) changes
        viewModelScope.launch {
            scheduleRepository.followedShowIds.collect { ids ->
                _uiState.update { it.copy(followedIds = ids) }
                syncReminders()
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                _now.value = Instant.now()
            }
        }
    }

    fun showsOn(day: LocalDate): List<ScheduledShow> =
        _uiState.value.shows.filter { MelbourneTime.isSameDay(it.start, day) }

    fun occurrencesOf(seriesId: String): List<ScheduledShow> =
        _uiState.value.shows.filter { it.series.id == seriesId }

    fun onAirNow(at: Instant = _now.value): ScheduledShow? =
        _uiState.value.shows.firstOrNull { it.isOnAir(at) }

    fun selectDay(day: LocalDate) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun toggleFollow(seriesId: String) {
        viewModelScope.launch {
            scheduleRepository.toggleFollow(seriesId)
        }
    }

    /** Loads unless it was loaded in the last 15 minutes. */
    fun loadIfNeeded() {
        val last = loadedAt
        if (last != null && Duration.between(last, Instant.now()) < staleAfter) return
        if (_uiState.value.phase == SchedulePhase.Loading) return
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = SchedulePhase.Loading) }
            val week = week
            val start = MelbourneTime.startOfDay(week.first())
            val end = MelbourneTime.startOfDay(week.last().plusDays(1))

            repository.schedule(start, end)
                .onSuccess { shows ->
                    loadedAt = Instant.now()
                    scheduleRepository.saveCache(shows)
                    _uiState.update { it.copy(shows = shows, phase = SchedulePhase.Loaded, offlineCopyAt = null) }
                    syncReminders()
                }
                .onFailure { error ->
                    val current = _uiState.value
                    if (current.shows.isNotEmpty()) {
                        // keep showing the saved copy rather than an error
                        val cachedAt = scheduleRepository.cachedShows()?.second ?: Instant.now()
                        _uiState.update { it.copy(phase = SchedulePhase.Loaded, offlineCopyAt = cachedAt) }
                    } else {
                        _uiState.update { it.copy(phase = SchedulePhase.Failed(errorMessage(error))) }
                    }
                }
        }
    }

    private fun syncReminders() {
        val state = _uiState.value
        ShowReminderScheduler.sync(getApplication(), state.shows, state.followedIds)
    }

    private fun errorMessage(error: Throwable): String = when (error) {
        is UnknownHostException -> "You're offline. Connect to the internet to see this week's shows."
        is SocketTimeoutException -> "Connection timed out. Try again."
        is IOException -> "Couldn't load the schedule. Check your connection."
        else -> "Couldn't load the schedule."
    }
}
