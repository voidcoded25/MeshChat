package com.meshchat.app.core.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Fake implementation of KeyStore for development and testing.
 * Generates deterministic keys and provides mock cryptographic operations.
 */
class FakeKeyStore : KeyStore {
    private var _identitySignPub: ByteArray? = null
    private var _identitySignPriv: ByteArray? = null
    private var _identityDhPub: ByteArray? = null
    private var _identityDhPriv: ByteArray? = null
    
    override val identitySignPub: ByteArray
        get() = _identitySignPub ?: generateIfMissing().let { _identitySignPub!! }
    
    override val identitySignPriv: ByteArray
        get() = _identitySignPriv ?: generateIfMissing().let { _identitySignPriv!! }
    
    override val identityDhPub: ByteArray
        get() = _identityDhPub ?: generateIfMissing().let { _identityDhPub!! }
    
    override val identityDhPriv: ByteArray
        get() = _identityDhPriv ?: generateIfMissing().let { _identityDhPriv!! }
    
    override fun generateIfMissing() {
        if (_identitySignPub == null) {
            val random = SecureRandom()
            
            // Generate fake signing keys (32 bytes each)
            _identitySignPriv = ByteArray(32).apply { random.nextBytes(this) }
            _identitySignPub = ByteArray(32).apply { random.nextBytes(this) }
            
            // Generate fake DH keys (32 bytes each)
            _identityDhPriv = ByteArray(32).apply { random.nextBytes(this) }
            _identityDhPub = ByteArray(32).apply { random.nextBytes(this) }
        }
    }
    
    override fun getSafetyNumber(): String {
        val combined = identitySignPub + identityDhPub
        val hash = MessageDigest.getInstance("SHA-256").digest(combined)
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }
    
    override fun getPublicKeysBase64(): Pair<String, String> {
        val signPubB64 = Base64.encodeToString(identitySignPub, Base64.NO_WRAP)
        val dhPubB64 = Base64.encodeToString(identityDhPub, Base64.NO_WRAP)
        return Pair(signPubB64, dhPubB64)
    }
}
