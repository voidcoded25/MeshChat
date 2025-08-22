package com.meshchat.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshchat.app.core.ble.MeshForegroundService
import com.meshchat.app.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    
    private val _isMeshRunning = MutableStateFlow(false)
    val isMeshRunning: StateFlow<Boolean> = _isMeshRunning.asStateFlow()
    
    private val _currentEphemeralId = MutableStateFlow("")
    val currentEphemeralId: StateFlow<String> = _currentEphemeralId.asStateFlow()
    
    private var meshService: MeshForegroundService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Since MeshForegroundService doesn't use binding, we'll use a different approach
            // We'll communicate through the service directly
            Logger.i("Service connection established", "HomeViewModel")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            meshService = null
            isServiceBound = false
            Logger.i("Service disconnected", "HomeViewModel")
        }
    }
    
    fun startMesh(context: Context) {
        if (_isMeshRunning.value) {
            Logger.w("Mesh already running", "HomeViewModel")
            return
        }
        
        try {
            val intent = Intent(context, MeshForegroundService::class.java).apply {
                action = MeshForegroundService.ACTION_START
            }
            context.startForegroundService(intent)
            
            // Update UI state
            _isMeshRunning.value = true
            Logger.i("Started mesh network", "HomeViewModel")
            
        } catch (e: Exception) {
            Logger.e("Failed to start mesh", e, "HomeViewModel")
        }
    }
    
    fun stopMesh(context: Context) {
        if (!_isMeshRunning.value) return
        
        try {
            val intent = Intent(context, MeshForegroundService::class.java).apply {
                action = MeshForegroundService.ACTION_STOP
            }
            context.startForegroundService(intent)
            
            // Update UI state
            _isMeshRunning.value = false
            _currentEphemeralId.value = ""
            Logger.i("Stopped mesh network", "HomeViewModel")
            
        } catch (e: Exception) {
            Logger.e("Failed to stop mesh", e, "HomeViewModel")
        }
    }
    
    fun refreshEphemeralId(context: Context) {
        // For now, we'll simulate the ephemeral ID
        // In a real implementation, you'd get this from the service
        if (_isMeshRunning.value) {
            _currentEphemeralId.value = generateMockEphemeralId()
        }
    }
    
    private fun generateMockEphemeralId(): String {
        // Generate a mock 8-byte hex string for demonstration
        return (0 until 8).joinToString("") { "%02x".format((0..255).random()) }
    }
    
    override fun onCleared() {
        super.onCleared()
        meshService = null
        isServiceBound = false
    }
}
