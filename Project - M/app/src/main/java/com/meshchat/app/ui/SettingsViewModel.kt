package com.meshchat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshchat.app.core.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    fun updateRotationInterval(minutes: Int) {
        val clampedMinutes = minutes.coerceIn(
            Settings.MIN_ROTATION_INTERVAL,
            Settings.MAX_ROTATION_INTERVAL
        )
        _settings.value = _settings.value.copy(
            ephemeralIdRotationIntervalMinutes = clampedMinutes
        )
    }
    
    fun updateScanMode(scanMode: Settings.ScanMode) {
        _settings.value = _settings.value.copy(scanMode = scanMode)
    }
    
    fun updateRelayMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(relayMode = enabled)
    }
    
    fun updateAutoConnect(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoConnect = enabled)
    }
    
    fun updateLogLevel(logLevel: Settings.LogLevel) {
        _settings.value = _settings.value.copy(logLevel = logLevel)
    }
    
    fun updateMaxLogEntries(entries: Int) {
        val clampedEntries = entries.coerceIn(
            Settings.MIN_LOG_ENTRIES,
            Settings.MAX_LOG_ENTRIES
        )
        _settings.value = _settings.value.copy(maxLogEntries = clampedEntries)
    }
    
    fun updateNotifications(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableNotifications = enabled)
    }
    
    fun updateSound(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableSound = enabled)
    }
    
    fun updateVibration(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableVibration = enabled)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // TODO: Implement actual settings persistence
                // For now, just simulate saving
                kotlinx.coroutines.delay(500)
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    fun resetToDefaults() {
        _settings.value = Settings()
    }
}
