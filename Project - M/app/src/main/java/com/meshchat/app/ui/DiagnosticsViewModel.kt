package com.meshchat.app.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshchat.app.core.Logger
import com.meshchat.app.core.data.MeshStats
import com.meshchat.app.core.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DiagnosticsViewModel : ViewModel() {
    private val _stats = MutableStateFlow(MeshStats())
    val stats: StateFlow<MeshStats> = _stats.asStateFlow()
    
    private val _logs = MutableStateFlow<List<Logger.LogEntry>>(emptyList())
    val logs: StateFlow<List<Logger.LogEntry>> = _logs.asStateFlow()
    
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()
    
    private val startTime = System.currentTimeMillis()
    
    init {
        refreshStats()
        refreshLogs()
    }
    
    fun refreshStats() {
        viewModelScope.launch {
            val currentStats = _stats.value.copy(
                uptime = System.currentTimeMillis() - startTime,
                seenSetSize = Logger.getLogCount()
            )
            _stats.value = currentStats
        }
    }
    
    fun refreshLogs() {
        _logs.value = Logger.getLogs()
    }
    
    fun clearLogs() {
        Logger.clearLogs()
        refreshLogs()
    }
    
    fun exportLogs(directory: File) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "meshchat_logs_$timestamp.txt"
                val file = File(directory, filename)
                
                FileWriter(file).use { writer ->
                    writer.write("MeshChat Logs Export\n")
                    writer.write("Generated: ${Date()}\n")
                    writer.write("=" * 50 + "\n\n")
                    
                    // Write stats
                    writer.write("MESH STATISTICS\n")
                    writer.write("-" * 20 + "\n")
                    writer.write("Current Ephemeral ID: ${stats.value.currentEphemeralId}\n")
                    writer.write("MTU Size: ${stats.value.mtuSize}\n")
                    writer.write("Active Connections: ${stats.value.activeConnections}\n")
                    writer.write("Total Relayed Messages: ${stats.value.totalRelayedMessages}\n")
                    writer.write("Seen Set Size: ${stats.value.seenSetSize}\n")
                    writer.write("Battery Impact: ${stats.value.batteryImpact}\n")
                    writer.write("Uptime: ${stats.value.getFormattedUptime()}\n")
                    writer.write("Last Message: ${stats.value.getFormattedLastMessageTime()}\n")
                    writer.write("Total Bytes: ${stats.value.getFormattedBytesTransferred()}\n")
                    writer.write("Chunks: ${stats.value.chunkCount}\n")
                    writer.write("Reassembly Success: ${stats.value.reassemblySuccessCount}\n")
                    writer.write("Deduplication Hits: ${stats.value.dedupHits}\n\n")
                    
                    // Write logs
                    writer.write("LOG ENTRIES\n")
                    writer.write("-" * 20 + "\n")
                    writer.write(Logger.getLogsAsText())
                }
                
                _exportPath.value = file.absolutePath
            } catch (e: Exception) {
                Logger.e("Failed to export logs", "DiagnosticsViewModel", e)
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun updateStats(
        ephemeralId: String? = null,
        mtuSize: Int? = null,
        activeConnections: Int? = null,
        totalRelayedMessages: Long? = null,
        totalBytesTransferred: Long? = null,
        chunkCount: Long? = null,
        reassemblySuccessCount: Long? = null,
        dedupHits: Long? = null,
        lastMessageTime: Long? = null
    ) {
        val currentStats = _stats.value
        _stats.value = currentStats.copy(
            currentEphemeralId = ephemeralId ?: currentStats.currentEphemeralId,
            mtuSize = mtuSize ?: currentStats.mtuSize,
            activeConnections = activeConnections ?: currentStats.activeConnections,
            totalRelayedMessages = totalRelayedMessages ?: currentStats.totalRelayedMessages,
            totalBytesTransferred = totalBytesTransferred ?: currentStats.totalBytesTransferred,
            chunkCount = chunkCount ?: currentStats.chunkCount,
            reassemblySuccessCount = reassemblySuccessCount ?: currentStats.reassemblySuccessCount,
            dedupHits = dedupHits ?: currentStats.dedupHits,
            lastMessageTime = lastMessageTime ?: currentStats.lastMessageTime
        )
    }
    
    fun calculateBatteryImpact(): MeshStats.BatteryImpact {
        val stats = _stats.value
        val bytesPerMinute = if (stats.uptime > 0) {
            stats.totalBytesTransferred * 60000 / stats.uptime
        } else 0
        
        return when {
            bytesPerMinute < 1024 -> MeshStats.BatteryImpact.LOW
            bytesPerMinute < 10240 -> MeshStats.BatteryImpact.MEDIUM
            bytesPerMinute < 102400 -> MeshStats.BatteryImpact.HIGH
            else -> MeshStats.BatteryImpact.CRITICAL
        }
    }
}
