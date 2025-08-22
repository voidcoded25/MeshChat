package com.meshchat.app.core.crypto

/**
 * Interface for session-based encryption/decryption of messages between peers.
 * Handles the cryptographic operations for secure communication.
 */
interface SessionCrypto {
    /**
     * Encrypt a message for a specific peer
     * @param peerId The peer's identifier
     * @param plaintext The message to encrypt
     * @return The encrypted ciphertext
     */
    fun encrypt(peerId: ByteArray, plaintext: ByteArray): ByteArray
    
    /**
     * Decrypt a message from a specific peer
     * @param peerId The peer's identifier
     * @param ciphertext The encrypted message to decrypt
     * @return The decrypted plaintext
     */
    fun decrypt(peerId: ByteArray, ciphertext: ByteArray): ByteArray
    
    /**
     * Establish a secure session with a peer
     * @param peerId The peer's identifier
     * @param peerDhPub The peer's public Diffie-Hellman key
     * @return True if session establishment was successful
     */
    fun establishSession(peerId: ByteArray, peerDhPub: ByteArray): Boolean
    
    /**
     * Check if a secure session exists with a peer
     * @param peerId The peer's identifier
     * @return True if a secure session exists
     */
    fun hasSession(peerId: ByteArray): Boolean
    
    /**
     * Remove a session with a peer
     * @param peerId The peer's identifier
     */
    fun removeSession(peerId: ByteArray)
}
