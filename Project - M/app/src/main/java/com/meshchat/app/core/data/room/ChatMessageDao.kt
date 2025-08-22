package com.meshchat.app.core.data.room

import androidx.room.*
import com.meshchat.app.core.data.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE peerAddress = :peerAddress ORDER BY timestamp DESC LIMIT :limit")
    fun getMessagesForPeer(peerAddress: String, limit: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE peerAddress = :peerAddress ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getMessagesForPeerPaginated(peerAddress: String, limit: Int, offset: Int): Flow<List<ChatMessage>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE peerAddress = :peerAddress")
    fun getMessageCountForPeer(peerAddress: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: UUID, status: ChatMessage.MessageStatus)

    @Query("UPDATE chat_messages SET status = :status WHERE msgId = :envelopeMsgId")
    suspend fun updateMessageStatusByEnvelopeId(envelopeMsgId: UUID, status: ChatMessage.MessageStatus)

    @Delete
    suspend fun deleteMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE peerAddress = :peerAddress")
    suspend fun deleteAllMessagesForPeer(peerAddress: String)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<ChatMessage>>
}
