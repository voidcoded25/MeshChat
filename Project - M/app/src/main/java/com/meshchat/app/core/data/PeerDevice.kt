package com.meshchat.app.core.data

/**
 * Represents a discovered peer device in the mesh network.
 */
data class PeerDevice(
    val address: String,
    val addressHash: String,
    val ephemeralId: ByteArray,
    val lastSeen: Long = System.currentTimeMillis(),
    val rssi: Int,
    val isConnected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerDevice

        if (address != other.address) return false
        if (addressHash != other.addressHash) return false
        if (!ephemeralId.contentEquals(other.ephemeralId)) return false
        if (lastSeen != other.lastSeen) return false
        if (rssi != other.rssi) return false
        if (isConnected != other.isConnected) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + addressHash.hashCode()
        result = 31 * result + ephemeralId.contentHashCode()
        result = 31 * result + lastSeen.hashCode()
        result = 31 * result + rssi
        result = 31 * result + isConnected.hashCode()
        return result
    }
    
    /**
     * Get ephemeral ID as hex string
     */
    fun getEphemeralIdHex(): String {
        return ephemeralId.joinToString("") { "%02x".format(it) }
    }
}
