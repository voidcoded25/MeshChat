package com.meshchat.app.core.ble

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.meshchat.app.core.Logger

/**
 * Manages Bluetooth runtime permissions for Android 12+.
 * Handles permission requests and checks for BLE operations.
 */
class PermissionManager(private val context: Context) {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        
        // Android 12+ Bluetooth permissions
        private val BLUETOOTH_PERMISSIONS_31 = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        
        // Legacy Bluetooth permissions
        private val BLUETOOTH_PERMISSIONS_LEGACY = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        
        // Location permissions (required for BLE on older devices)
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    /**
     * Check if all required Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            BLUETOOTH_PERMISSIONS_31.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            // Legacy Android
            BLUETOOTH_PERMISSIONS_LEGACY.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Check if location permissions are granted (required for BLE on older devices)
     */
    fun hasLocationPermissions(): Boolean {
        return LOCATION_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if all required permissions for BLE operations are granted
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasBluetoothPermissions() && hasLocationPermissions()
    }
    
    /**
     * Request all required permissions
     */
    fun requestPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()
        
        // Check Bluetooth permissions
        if (!hasBluetoothPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsToRequest.addAll(BLUETOOTH_PERMISSIONS_31)
            } else {
                permissionsToRequest.addAll(BLUETOOTH_PERMISSIONS_LEGACY)
            }
        }
        
        // Check location permissions
        if (!hasLocationPermissions()) {
            permissionsToRequest.addAll(LOCATION_PERMISSIONS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            Logger.i("Requesting permissions: ${permissionsToRequest.joinToString()}", "PermissionManager")
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            Logger.i("All required permissions already granted", "PermissionManager")
        }
    }
    
    /**
     * Check if permission request was successful
     */
    fun isPermissionRequestSuccessful(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                Logger.i("All requested permissions granted", "PermissionManager")
            } else {
                Logger.w("Some permissions were denied", "PermissionManager")
            }
            
            return allGranted
        }
        return false
    }
    
    /**
     * Get a list of missing permissions
     */
    fun getMissingPermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        // Check Bluetooth permissions
        if (!hasBluetoothPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                missingPermissions.addAll(BLUETOOTH_PERMISSIONS_31)
            } else {
                missingPermissions.addAll(BLUETOOTH_PERMISSIONS_LEGACY)
            }
        }
        
        // Check location permissions
        if (!hasLocationPermissions()) {
            missingPermissions.addAll(LOCATION_PERMISSIONS)
        }
        
        return missingPermissions
    }
    
    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
