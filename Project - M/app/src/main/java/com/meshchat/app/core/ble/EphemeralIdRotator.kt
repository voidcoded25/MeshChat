package com.meshchat.app.core.ble

import com.meshchat.app.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom

/**
 * Generates and rotates ephemeral IDs for mesh networking.
 * Creates a new 8-byte ID every 15 minutes using a seeded PRNG.
 */
class EphemeralIdRotator {
    
    private val secureRandom = SecureRandom()
    private val _currentId = MutableStateFlow<ByteArray>(ByteArray(0))
    val currentId: StateFlow<ByteArray> = _currentId.asStateFlow()
    
    private var rotationJob: kotlinx.coroutines.Job? = null
    private var isRunning = false
    
    init {
        // Generate initial ID
        generateNewId()
    }
    
    /**
     * Start the ID rotation process
     */
    fun startRotation(scope: CoroutineScope) {
        if (isRunning) {
            Logger.w("EphemeralIdRotator already running", "EphemeralIdRotator")
            return
        }
        
        isRunning = true
        rotationJob = scope.launch(Dispatchers.Default) {
            while (isRunning) {
                delay(BleConstants.EPHEMERAL_ID_ROTATION_INTERVAL)
                if (isRunning) {
                    generateNewId()
                }
            }
        }
        
        Logger.i("Started ephemeral ID rotation", "EphemeralIdRotator")
    }
    
    /**
     * Stop the ID rotation process
     */
    fun stopRotation() {
        isRunning = false
        rotationJob?.cancel()
        rotationJob = null
        Logger.i("Stopped ephemeral ID rotation", "EphemeralIdRotator")
    }
    
    /**
     * Generate a new ephemeral ID
     */
    private fun generateNewId() {
        val newId = ByteArray(BleConstants.EPHEMERAL_ID_SIZE)
        secureRandom.nextBytes(newId)
        _currentId.value = newId
        
        Logger.i("Generated new ephemeral ID: ${newId.joinToString("") { "%02x".format(it) }}", "EphemeralIdRotator")
    }
    
    /**
     * Get current ID as hex string
     */
    fun getCurrentIdHex(): String {
        return _currentId.value.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Force generation of a new ID immediately
     */
    fun forceRotate() {
        generateNewId()
    }
}
