package com.meshchat.app.core.data

data class MeshStats(
    val currentEphemeralId: String = "",
    val mtuSize: Int = 0,
    val activeConnections: Int = 0,
    val totalRelayedMessages: Long = 0,
    val seenSetSize: Int = 0,
    val batteryImpact: BatteryImpact = BatteryImpact.LOW,
    val uptime: Long = 0,
    val lastMessageTime: Long = 0,
    val totalBytesTransferred: Long = 0,
    val chunkCount: Long = 0,
    val reassemblySuccessCount: Long = 0,
    val dedupHits: Long = 0
) {
    enum class BatteryImpact {
        LOW,        // Minimal battery usage
        MEDIUM,     // Moderate battery usage
        HIGH,       // High battery usage
        CRITICAL    // Very high battery usage
    }
    
    fun getFormattedUptime(): String {
        val hours = uptime / 3600000
        val minutes = (uptime % 3600000) / 60000
        return "${hours}h ${minutes}m"
    }
    
    fun getFormattedLastMessageTime(): String {
        if (lastMessageTime == 0L) return "Never"
        val now = System.currentTimeMillis()
        val diff = now - lastMessageTime
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
    
    fun getFormattedBytesTransferred(): String {
        return when {
            totalBytesTransferred < 1024 -> "${totalBytesTransferred} B"
            totalBytesTransferred < 1024 * 1024 -> "${totalBytesTransferred / 1024} KB"
            else -> "${totalBytesTransferred / (1024 * 1024)} MB"
        }
    }
}
