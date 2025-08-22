package com.meshchat.app.core.data

data class Settings(
    val ephemeralIdRotationIntervalMinutes: Int = 15,
    val scanMode: ScanMode = ScanMode.BALANCED,
    val relayMode: Boolean = true,
    val autoConnect: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val maxLogEntries: Int = 1000,
    val enableNotifications: Boolean = true,
    val enableSound: Boolean = false,
    val enableVibration: Boolean = true
) {
    enum class ScanMode(val displayName: String, val androidScanMode: Int) {
        LOW_LATENCY("Low Latency", android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY),
        BALANCED("Balanced", android.bluetooth.le.ScanSettings.SCAN_MODE_BALANCED),
        LOW_POWER("Low Power", android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER)
    }
    
    enum class LogLevel(val displayName: String, val priority: Int) {
        VERBOSE("Verbose", 0),
        DEBUG("Debug", 1),
        INFO("Info", 2),
        WARNING("Warning", 3),
        ERROR("Error", 4)
    }
    
    companion object {
        const val MIN_ROTATION_INTERVAL = 5
        const val MAX_ROTATION_INTERVAL = 60
        const val MIN_LOG_ENTRIES = 100
        const val MAX_LOG_ENTRIES = 10000
    }
}
