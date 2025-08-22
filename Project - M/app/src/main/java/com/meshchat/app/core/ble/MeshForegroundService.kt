package com.meshchat.app.core.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meshchat.app.MainActivity
import com.meshchat.app.R
import com.meshchat.app.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service for managing the mesh network operations.
 * Handles ephemeral ID rotation, BLE advertising, and scanning.
 */
class MeshForegroundService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "MeshForegroundService"
        private const val CHANNEL_NAME = "Mesh Network Service"
        
        const val ACTION_START = "com.meshchat.app.START_MESH"
        const val ACTION_STOP = "com.meshchat.app.STOP_MESH"
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var ephemeralIdRotator: EphemeralIdRotator
    private lateinit var meshBleManager: MeshBleManager
    private lateinit var gattServerManager: GattServerManager
    
    private var isMeshRunning = false
    
    override fun onCreate() {
        super.onCreate()
        Logger.i("MeshForegroundService created", "MeshForegroundService")
        
        createNotificationChannel()
        
        ephemeralIdRotator = EphemeralIdRotator()
        meshBleManager = MeshBleManager(this)
        gattServerManager = GattServerManager(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("MeshForegroundService started", "MeshForegroundService")
        
        when (intent?.action) {
            ACTION_START -> startMesh()
            ACTION_STOP -> stopMesh()
            else -> startMesh() // Default action
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Logger.i("MeshForegroundService destroyed", "MeshForegroundService")
        
        stopMesh()
        serviceScope.cancel()
    }
    
    private fun startMesh() {
        if (isMeshRunning) {
            Logger.w("Mesh already running", "MeshForegroundService")
            return
        }
        
        try {
            // Start ephemeral ID rotation
            ephemeralIdRotator.startRotation(serviceScope)
            
            // Start advertising with current ephemeral ID
            val currentId = ephemeralIdRotator.currentId.value
            if (currentId.isNotEmpty()) {
                meshBleManager.startAdvertising(currentId)
            }
            
            // Start GATT server for incoming connections
            gattServerManager.startServer()
            
            // Start scanning for peers
            meshBleManager.startScanning()
            
            // Monitor ephemeral ID changes and update advertising
            serviceScope.launch {
                ephemeralIdRotator.currentId.collectLatest { newId ->
                    if (isMeshRunning && newId.isNotEmpty()) {
                        meshBleManager.stopAdvertising()
                        meshBleManager.startAdvertising(newId)
                        Logger.i("Updated advertising with new ephemeral ID", "MeshForegroundService")
                    }
                }
            }
            
            isMeshRunning = true
            Logger.i("Started mesh network", "MeshForegroundService")
            
        } catch (e: Exception) {
            Logger.e("Failed to start mesh", e, "MeshForegroundService")
        }
    }
    
    private fun stopMesh() {
        if (!isMeshRunning) return
        
        try {
            // Stop ephemeral ID rotation
            ephemeralIdRotator.stopRotation()
            
            // Stop BLE operations
            meshBleManager.stopAdvertising()
            meshBleManager.stopScanning()
            
            // Stop GATT server
            gattServerManager.stopServer()
            
            isMeshRunning = false
            Logger.i("Stopped mesh network", "MeshForegroundService")
            
        } catch (e: Exception) {
            Logger.e("Failed to stop mesh", e, "MeshForegroundService")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.meshchat_service_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val startIntent = Intent(this, MeshForegroundService::class.java).apply {
            action = ACTION_START
        }
        val stopIntent = Intent(this, MeshForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        
        val startPendingIntent = PendingIntent.getService(
            this, 0, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 2, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val currentIdHex = if (isMeshRunning) {
            ephemeralIdRotator.getCurrentIdHex()
        } else {
            "Stopped"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.meshchat_service))
            .setContentText("Mesh: $currentIdHex")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                if (isMeshRunning) "Stop" else "Start",
                if (isMeshRunning) stopPendingIntent else startPendingIntent
            )
            .build()
    }
    
    /**
     * Get the current ephemeral ID
     */
    fun getCurrentEphemeralId(): ByteArray = ephemeralIdRotator.currentId.value
    
    /**
     * Get the current ephemeral ID as hex string
     */
    fun getCurrentEphemeralIdHex(): String = ephemeralIdRotator.getCurrentIdHex()
    
    /**
     * Check if mesh is currently running
     */
    fun isMeshCurrentlyRunning(): Boolean = isMeshRunning
    
    /**
     * Get the BLE manager for external access
     */
    fun getMeshBleManager(): MeshBleManager = meshBleManager
}
