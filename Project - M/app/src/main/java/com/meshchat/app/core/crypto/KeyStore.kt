package com.meshchat.app.core.crypto

/**
 * Interface for managing cryptographic keys used in the mesh network.
 * Handles identity signing keys and Diffie-Hellman keys for key exchange.
 */
interface KeyStore {
    /**
     * Public signing key for identity verification
     */
    val identitySignPub: ByteArray
    
    /**
     * Private signing key for creating signatures
     */
    val identitySignPriv: ByteArray
    
    /**
     * Public Diffie-Hellman key for key exchange
     */
    val identityDhPub: ByteArray
    
    /**
     * Private Diffie-Hellman key for key exchange
     */
    val identityDhPriv: ByteArray
    
    /**
     * Generate cryptographic keys if they don't exist
     */
    fun generateIfMissing()
    
    /**
     * Get the safety number (hash of public keys) for this identity
     */
    fun getSafetyNumber(): String
    
    /**
     * Get base64 encoded public keys for QR code display
     */
    fun getPublicKeysBase64(): Pair<String, String>
}
