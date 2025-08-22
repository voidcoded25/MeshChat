package com.meshchat.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshchat.app.core.crypto.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QRViewModel(
    private val cryptoManager: CryptoManager
) : ViewModel() {
    
    private val _identityInfo = MutableStateFlow(IdentityInfo())
    val identityInfo: StateFlow<IdentityInfo> = _identityInfo.asStateFlow()
    
    init {
        loadIdentityInfo()
    }
    
    private fun loadIdentityInfo() {
        val safetyNumber = cryptoManager.getLocalSafetyNumber()
        val (signPubB64, dhPubB64) = cryptoManager.getLocalPublicKeys()
        
        _identityInfo.value = IdentityInfo(
            safetyNumber = safetyNumber,
            signingPublicKey = signPubB64,
            dhPublicKey = dhPubB64,
            qrData = generateQRData(safetyNumber, signPubB64, dhPubB64)
        )
    }
    
    private fun generateQRData(safetyNumber: String, signPubB64: String, dhPubB64: String): String {
        // Format: meshchat://identity?safety=XXXX&sign=YYYY&dh=ZZZZ
        return "meshchat://identity?safety=$safetyNumber&sign=$signPubB64&dh=$dhPubB64"
    }
    
    fun refreshIdentityInfo() {
        loadIdentityInfo()
    }
    
    data class IdentityInfo(
        val safetyNumber: String = "",
        val signingPublicKey: String = "",
        val dhPublicKey: String = "",
        val qrData: String = ""
    )
}
