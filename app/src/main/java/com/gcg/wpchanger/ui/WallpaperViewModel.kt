package com.gcg.wpchanger.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcg.wpchanger.data.ImportSummary
import com.gcg.wpchanger.data.TimerInterval
import com.gcg.wpchanger.data.WallpaperItem
import com.gcg.wpchanger.data.WallpaperManagerHelper
import com.gcg.wpchanger.data.WallpaperPreferences
import com.gcg.wpchanger.data.WallpaperRepository
import com.gcg.wpchanger.data.WallpaperTarget
import com.gcg.wpchanger.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WallpaperUiState(
    val isActive: Boolean = false,
    val interval: TimerInterval = TimerInterval.HOURS_1,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val wallpapers: List<WallpaperItem> = emptyList(),
    val lastChangedTimestamp: Long = 0L,
    val lastWallpaperId: String = "",
    val lastWallpaperName: String = "",
    val isLoading: Boolean = false,
    val userMessage: String? = null,
)

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = WallpaperPreferences(application)
    private val repository = WallpaperRepository(application)

    private val isLoadingFlow = MutableStateFlow(false)
    private val userMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WallpaperUiState> = combine(
        preferences.settingsFlow,
        repository.wallpapers,
        isLoadingFlow,
        userMessageFlow,
    ) { settings, wallpapers, isLoading, message ->
        WallpaperUiState(
            isActive = settings.isActive,
            interval = settings.interval,
            target = settings.target,
            wallpapers = wallpapers,
            lastChangedTimestamp = settings.lastChangedTimestamp,
            lastWallpaperId = settings.lastWallpaperId,
            lastWallpaperName = settings.lastWallpaperName,
            isLoading = isLoading,
            userMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WallpaperUiState(),
    )

    init {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun toggleActive(enabled: Boolean) {
        viewModelScope.launch {
            val wallpapers = repository.wallpapers.value
            if (enabled && wallpapers.isEmpty()) {
                userMessageFlow.value = "Please select some wallpapers before enabling auto-rotation."
                return@launch
            }

            preferences.setActive(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                val interval = preferences.getInterval()
                WorkManagerScheduler.schedulePeriodic(context, interval)
                userMessageFlow.value = "Auto-rotation enabled (${interval.label})"
            } else {
                WorkManagerScheduler.cancelPeriodic(context)
                userMessageFlow.value = "Auto-rotation paused"
            }
        }
    }

    fun setInterval(interval: TimerInterval) {
        viewModelScope.launch {
            preferences.setInterval(interval)
            if (preferences.getActive()) {
                WorkManagerScheduler.schedulePeriodic(getApplication(), interval)
                userMessageFlow.value = "Timer updated to ${interval.label}"
            }
        }
    }

    fun setTarget(target: WallpaperTarget) {
        viewModelScope.launch {
            preferences.setTarget(target)
            userMessageFlow.value = "Target updated to ${target.label}"
        }
    }

    fun importWallpapers(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isLoadingFlow.value = true
            val summary = repository.importFromUris(uris)
            isLoadingFlow.value = false
            userMessageFlow.value = buildImportMessage(summary)
        }
    }

    fun importFolder(treeUri: Uri) {
        viewModelScope.launch {
            isLoadingFlow.value = true
            val summary = repository.importFromFolder(treeUri)
            isLoadingFlow.value = false
            userMessageFlow.value = buildImportMessage(summary, emptyMessage = "No images found in the selected folder")
        }
    }

    private fun buildImportMessage(summary: ImportSummary, emptyMessage: String = "Could not import selected images"): String {
        if (summary.total == 0) return emptyMessage
        val parts = mutableListOf<String>()
        if (summary.saved > 0) parts += "Added ${summary.saved} wallpaper${if (summary.saved != 1) "s" else ""}"
        if (summary.duplicates > 0) parts += "${summary.duplicates} duplicate${if (summary.duplicates != 1) "s" else ""} skipped"
        if (summary.unsupported > 0) parts += "${summary.unsupported} unsupported file${if (summary.unsupported != 1) "s" else ""} skipped"
        if (summary.failed > 0) parts += "${summary.failed} failed"
        return if (parts.isEmpty()) emptyMessage else parts.joinToString(" • ")
    }

    fun deleteWallpaper(id: String) {
        viewModelScope.launch {
            val success = repository.deleteWallpaper(id)
            if (success) {
                userMessageFlow.value = "Wallpaper removed"
            }
        }
    }

    fun clearAllWallpapers() {
        viewModelScope.launch {
            val count = repository.clearAllWallpapers()
            if (preferences.getActive()) {
                toggleActive(false)
            }
            userMessageFlow.value = "Removed all $count wallpapers"
        }
    }

    fun changeWallpaperNow() {
        viewModelScope.launch {
            val wallpapers = repository.wallpapers.value
            if (wallpapers.isEmpty()) {
                userMessageFlow.value = "No wallpapers in pool to apply"
                return@launch
            }

            isLoadingFlow.value = true
            val lastId = preferences.getLastWallpaperId()
            val queue = preferences.getShuffleQueue()
            val pick = repository.pickNextRandomWallpaper(lastId, queue)
            val next = pick.item

            if (next == null) {
                isLoadingFlow.value = false
                userMessageFlow.value = "Could not select a wallpaper"
                return@launch
            }

            val target = preferences.getTarget()
            val result = WallpaperManagerHelper.applyWallpaper(
                context = getApplication(),
                wallpaperFile = next.file,
                target = target,
            )

            isLoadingFlow.value = false
            if (result.isSuccess) {
                preferences.recordWallpaperChange(next.id, next.name)
                preferences.setShuffleQueue(pick.queue)
                userMessageFlow.value = "Applied: ${next.name}"
            } else {
                userMessageFlow.value = "Failed to set wallpaper: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    fun clearMessage() {
        userMessageFlow.value = null
    }
}
