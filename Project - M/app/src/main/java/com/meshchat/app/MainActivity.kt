package com.meshchat.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.meshchat.app.core.ble.MeshForegroundService
import com.meshchat.app.core.ble.PermissionManager
import com.meshchat.app.ui.navigation.MeshChatNavigation
import com.meshchat.app.ui.theme.MeshChatTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Start the mesh service when permissions are granted
            startMeshService()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionManager = PermissionManager(this)
        
        // Check and request permissions if needed
        if (permissionManager.hasAllRequiredPermissions()) {
            startMeshService()
        } else {
            requestPermissions()
        }
        
        setContent {
            MeshChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeshChatNavigation()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = permissionManager.getMissingPermissions().toTypedArray()
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun startMeshService() {
        // Start the mesh foreground service
        val serviceIntent = Intent(this, MeshForegroundService::class.java)
        startForegroundService(serviceIntent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // The service will continue running as a foreground service
    }
}
