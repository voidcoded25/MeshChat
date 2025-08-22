package com.meshchat.app.core.ble

import java.util.UUID

object BleConstants {
    /**
     * Mesh service UUID for MeshChat
     * Using a custom 128-bit UUID in the Bluetooth SIG reserved range
     */
    val SERVICE_UUID: UUID = UUID.fromString("0000a0a0-0000-1000-8000-00805f9b34fb")
    
    /**
     * INBOX characteristic UUID for data transfer
     * Using a custom 128-bit UUID in the Bluetooth SIG reserved range
     */
    val INBOX_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000a0a1-0000-1000-8000-00805f9b34fb")
    
    /**
     * Ephemeral ID rotation interval in milliseconds (15 minutes)
     */
    const val EPHEMERAL_ID_ROTATION_INTERVAL = 15 * 60 * 1000L
    
    /**
     * Ephemeral ID size in bytes
     */
    const val EPHEMERAL_ID_SIZE = 8
    
    /**
     * Maximum MTU size for optimal data transfer
     */
    const val MAX_MTU_SIZE = 247
}
