package com.meshchat.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.meshchat.app.core.data.room.UUIDConverter
import java.util.UUID

@Entity(tableName = "chat_messages")
@TypeConverters(UUIDConverter::class)
data class ChatMessage(
    @PrimaryKey
    val id: UUID,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val peerAddress: String,
    val status: MessageStatus,
    val msgId: UUID? = null, // Link to original envelope
    val topicId: Int = 0
) {
    enum class MessageStatus {
        SENDING,    // Message is being sent
        SENT,       // Message sent to local router
        RELAYED,    // Message acknowledged by local router
        DELIVERED,  // Message acknowledged by peer
        READ,       // Message read by peer
        FAILED,     // Message failed to send
        RECEIVED    // Message received from peer
    }

    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }

    fun isRecent(): Boolean {
        val now = System.currentTimeMillis()
        return (now - timestamp) < 300000 // 5 minutes
    }

    companion object {
        fun fromEnvelope(envelope: Envelope, isFromMe: Boolean, peerAddress: String): ChatMessage {
            return ChatMessage(
                id = UUID.randomUUID(),
                content = envelope.getPayloadAsString(),
                timestamp = envelope.timestamp,
                isFromMe = isFromMe,
                peerAddress = peerAddress,
                status = if (isFromMe) MessageStatus.SENT else MessageStatus.RECEIVED,
                msgId = envelope.msgId,
                topicId = envelope.topicId
            )
        }

        fun createTextMessage(content: String, peerAddress: String): ChatMessage {
            return ChatMessage(
                id = UUID.randomUUID(),
                content = content,
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                peerAddress = peerAddress,
                status = MessageStatus.SENDING
            )
        }
    }
}
