package com.gcg.wpchanger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_settings")

data class WallpaperSettings(
    val isActive: Boolean = false,
    val interval: TimerInterval = TimerInterval.HOURS_1,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    val lastChangedTimestamp: Long = 0L,
    val lastWallpaperId: String = "",
    val lastWallpaperName: String = "",
    val shuffleQueue: List<String> = emptyList(),
    val notifyOnChange: Boolean = false,
    val activeStack: String = "",
)

class WallpaperPreferences(private val context: Context) {

    private object PreferencesKeys {
        val IS_ACTIVE = booleanPreferencesKey("is_active")
        val INTERVAL = stringPreferencesKey("timer_interval")
        val TARGET = stringPreferencesKey("wallpaper_target")
        val LAST_CHANGED_TIMESTAMP = longPreferencesKey("last_changed_timestamp")
        val LAST_WALLPAPER_ID = stringPreferencesKey("last_wallpaper_id")
        val LAST_WALLPAPER_NAME = stringPreferencesKey("last_wallpaper_name")
        val SHUFFLE_QUEUE = stringPreferencesKey("shuffle_queue")
        val NOTIFY_ON_CHANGE = booleanPreferencesKey("notify_on_change")
        val ACTIVE_STACK = stringPreferencesKey("active_stack")
    }

    val settingsFlow: Flow<WallpaperSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            WallpaperSettings(
                isActive = preferences[PreferencesKeys.IS_ACTIVE] ?: false,
                interval = TimerInterval.fromName(preferences[PreferencesKeys.INTERVAL]),
                target = WallpaperTarget.fromName(preferences[PreferencesKeys.TARGET]),
                lastChangedTimestamp = preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] ?: 0L,
                lastWallpaperId = preferences[PreferencesKeys.LAST_WALLPAPER_ID] ?: "",
                lastWallpaperName = preferences[PreferencesKeys.LAST_WALLPAPER_NAME] ?: "",
                shuffleQueue = preferences[PreferencesKeys.SHUFFLE_QUEUE]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList(),
                notifyOnChange = preferences[PreferencesKeys.NOTIFY_ON_CHANGE] ?: false,
                activeStack = preferences[PreferencesKeys.ACTIVE_STACK] ?: "",
            )
        }

    suspend fun setActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ACTIVE] = active
        }
    }

    suspend fun setInterval(interval: TimerInterval) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERVAL] = interval.name
        }
    }

    suspend fun setTarget(target: WallpaperTarget) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET] = target.name
        }
    }

    suspend fun recordWallpaperChange(id: String, name: String, timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CHANGED_TIMESTAMP] = timestamp
            preferences[PreferencesKeys.LAST_WALLPAPER_ID] = id
            preferences[PreferencesKeys.LAST_WALLPAPER_NAME] = name
        }
    }

    suspend fun setShuffleQueue(queue: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHUFFLE_QUEUE] = queue.joinToString(",")
        }
    }

    suspend fun setNotifyOnChange(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFY_ON_CHANGE] = enabled
        }
    }

    suspend fun setActiveStack(stack: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_STACK] = stack
        }
    }

    suspend fun getSettings(): WallpaperSettings = settingsFlow.first()
    suspend fun getActive(): Boolean = getSettings().isActive
    suspend fun getInterval(): TimerInterval = getSettings().interval
    suspend fun getTarget(): WallpaperTarget = getSettings().target
    suspend fun getLastWallpaperId(): String = getSettings().lastWallpaperId
    suspend fun getShuffleQueue(): List<String> = getSettings().shuffleQueue
    suspend fun getNotifyOnChange(): Boolean = getSettings().notifyOnChange
    suspend fun getActiveStack(): String = getSettings().activeStack
}
