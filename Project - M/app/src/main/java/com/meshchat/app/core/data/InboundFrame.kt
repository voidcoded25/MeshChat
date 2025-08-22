package com.meshchat.app.core.data

/**
 * Represents an incoming data frame from a GATT connection.
 */
data class InboundFrame(
    val sourceAddress: String,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InboundFrame

        if (sourceAddress != other.sourceAddress) return false
        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceAddress.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
    
    /**
     * Get data as string (assuming UTF-8 encoding)
     */
    fun getDataAsString(): String {
        return try {
            String(data, Charsets.UTF_8)
        } catch (e: Exception) {
            "Invalid UTF-8 data"
        }
    }
    
    /**
     * Get data as hex string
     */
    fun getDataAsHex(): String {
        return data.joinToString("") { "%02x".format(it) }
    }
}
