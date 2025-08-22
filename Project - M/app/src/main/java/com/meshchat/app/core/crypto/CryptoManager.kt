package com.meshchat.app.core.crypto

import com.meshchat.app.core.Logger

/**
 * Manages cryptographic operations for the mesh network.
 * Coordinates between KeyStore and SessionCrypto implementations.
 */
class CryptoManager(
    private val keyStore: KeyStore,
    private val sessionCrypto: SessionCrypto
) {
    
    init {
        // Ensure keys are generated
        keyStore.generateIfMissing()
        Logger.i("CryptoManager initialized with keys", "CryptoManager")
    }
    
    /**
     * Encrypt a message for a specific peer
     */
    fun encryptMessage(peerId: ByteArray, plaintext: ByteArray): ByteArray {
        return try {
            sessionCrypto.encrypt(peerId, plaintext)
        } catch (e: Exception) {
            Logger.e("Failed to encrypt message", "CryptoManager", e)
            // Fallback to plaintext on encryption failure
            plaintext
        }
    }
    
    /**
     * Decrypt a message from a specific peer
     */
    fun decryptMessage(peerId: ByteArray, ciphertext: ByteArray): ByteArray {
        return try {
            sessionCrypto.decrypt(peerId, ciphertext)
        } catch (e: Exception) {
            Logger.e("Failed to decrypt message", "CryptoManager", e)
            // Fallback to ciphertext on decryption failure
            ciphertext
        }
    }
    
    /**
     * Establish a secure session with a peer
     */
    fun establishSession(peerId: ByteArray, peerDhPub: ByteArray): Boolean {
        return try {
            sessionCrypto.establishSession(peerId, peerDhPub)
        } catch (e: Exception) {
            Logger.e("Failed to establish session", "CryptoManager", e)
            false
        }
    }
    
    /**
     * Check if a secure session exists with a peer
     */
    fun hasSession(peerId: ByteArray): Boolean {
        return sessionCrypto.hasSession(peerId)
    }
    
    /**
     * Remove a session with a peer
     */
    fun removeSession(peerId: ByteArray) {
        try {
            sessionCrypto.removeSession(peerId)
        } catch (e: Exception) {
            Logger.e("Failed to remove session", "CryptoManager", e)
        }
    }
    
    /**
     * Get the local identity's safety number
     */
    fun getLocalSafetyNumber(): String = keyStore.getSafetyNumber()
    
    /**
     * Get the local identity's public keys in base64 format
     */
    fun getLocalPublicKeys(): Pair<String, String> = keyStore.getPublicKeysBase64()
    
    /**
     * Get the local identity's signing public key
     */
    fun getLocalSigningPublicKey(): ByteArray = keyStore.identitySignPub
    
    /**
     * Get the local identity's DH public key
     */
    fun getLocalDhPublicKey(): ByteArray = keyStore.identityDhPub
}
