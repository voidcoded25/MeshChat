package com.meshchat.app.core.crypto

import com.meshchat.app.core.Logger

/**
 * Fake implementation of SessionCrypto for development and testing.
 * Simply returns plaintext without any encryption/decryption.
 */
class FakeSessionCrypto : SessionCrypto {
    private val activeSessions = mutableSetOf<String>()
    
    override fun encrypt(peerId: ByteArray, plaintext: ByteArray): ByteArray {
        Logger.d("FakeSessionCrypto: Encrypting ${plaintext.size} bytes for peer ${peerId.contentHashCode()}")
        // For now, just return plaintext (no encryption)
        return plaintext
    }
    
    override fun decrypt(peerId: ByteArray, ciphertext: ByteArray): ByteArray {
        Logger.d("FakeSessionCrypto: Decrypting ${ciphertext.size} bytes from peer ${peerId.contentHashCode()}")
        // For now, just return ciphertext as plaintext (no decryption)
        return ciphertext
    }
    
    override fun establishSession(peerId: ByteArray, peerDhPub: ByteArray): Boolean {
        val peerIdStr = peerId.contentHashCode().toString()
        activeSessions.add(peerIdStr)
        Logger.i("FakeSessionCrypto: Established fake session with peer $peerIdStr")
        return true
    }
    
    override fun hasSession(peerId: ByteArray): Boolean {
        val peerIdStr = peerId.contentHashCode().toString()
        return activeSessions.contains(peerIdStr)
    }
    
    override fun removeSession(peerId: ByteArray) {
        val peerIdStr = peerId.contentHashCode().toString()
        activeSessions.remove(peerIdStr)
        Logger.i("FakeSessionCrypto: Removed fake session with peer $peerIdStr")
    }
}
