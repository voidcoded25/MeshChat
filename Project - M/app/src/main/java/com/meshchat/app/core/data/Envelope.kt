package com.meshchat.app.core.data

import java.util.UUID

data class Envelope(
    val msgId: UUID,
    val topicId: Int,
    val timestamp: Long,
    val ttl: Int,
    val flags: Int,
    val payload: ByteArray,
    val isAck: Boolean = false,
    val ackForMsgId: UUID? = null
) {
    companion object {
        const val DEFAULT_TTL = 8
        const val LOCAL_TOPIC_ID = 0
        const val BROADCAST_TOPIC_ID = 1
        
        // Frame type flags
        const val FLAG_ACK = 0x01
        const val FLAG_URGENT = 0x02
        const val FLAG_ENCRYPTED = 0x04
        const val FLAG_SIGNED = 0x08
        
        fun createAck(originalMsgId: UUID, topicId: Int): Envelope {
            return Envelope(
                msgId = UUID.randomUUID(),
                topicId = topicId,
                timestamp = System.currentTimeMillis(),
                ttl = 0, // ACKs don't need TTL
                flags = FLAG_ACK,
                payload = ByteArray(0),
                isAck = true,
                ackForMsgId = originalMsgId
            )
        }
        
        fun createEncryptedMessage(
            msgId: UUID,
            topicId: Int,
            payload: ByteArray,
            ttl: Int = DEFAULT_TTL
        ): Envelope {
            return Envelope(
                msgId = msgId,
                topicId = topicId,
                timestamp = System.currentTimeMillis(),
                ttl = ttl,
                flags = FLAG_ENCRYPTED,
                payload = payload,
                isAck = false,
                ackForMsgId = null
            )
        }
        
        fun createSignedMessage(
            msgId: UUID,
            topicId: Int,
            payload: ByteArray,
            ttl: Int = DEFAULT_TTL
        ): Envelope {
            return Envelope(
                msgId = msgId,
                topicId = topicId,
                timestamp = System.currentTimeMillis(),
                ttl = ttl,
                flags = FLAG_SIGNED,
                payload = payload,
                isAck = false,
                ackForMsgId = null
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Envelope

        if (msgId != other.msgId) return false
        if (topicId != other.topicId) return false
        if (timestamp != other.timestamp) return false
        if (ttl != other.ttl) return false
        if (flags != other.flags) return false
        if (!payload.contentEquals(other.payload)) return false
        if (isAck != other.isAck) return false
        if (ackForMsgId != other.ackForMsgId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = msgId.hashCode()
        result = 31 * result + topicId
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + ttl
        result = 31 * result + flags
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + isAck.hashCode()
        result = 31 * result + (ackForMsgId?.hashCode() ?: 0)
        return result
    }

    fun getPayloadAsString(): String {
        return String(payload, Charsets.UTF_8)
    }

    fun getPayloadAsHex(): String {
        return payload.joinToString("") { "%02x".format(it) }
    }

    fun isExpired(): Boolean {
        return ttl <= 0
    }

    fun decrementTtl(): Envelope {
        return copy(ttl = (ttl - 1).coerceAtLeast(0))
    }
    
    fun isEncrypted(): Boolean = (flags and FLAG_ENCRYPTED) != 0
    fun isSigned(): Boolean = (flags and FLAG_SIGNED) != 0
    fun isUrgent(): Boolean = (flags and FLAG_URGENT) != 0
}
