package com.meshchat.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meshchat.app.core.crypto.CryptoManager
import com.meshchat.app.core.crypto.FakeKeyStore
import com.meshchat.app.core.crypto.FakeSessionCrypto
import com.meshchat.app.core.data.*
import com.meshchat.app.core.data.room.AppDatabase
import com.meshchat.app.core.data.room.ChatMessageDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val chatMessageDao: ChatMessageDao = database.chatMessageDao()
    
    // Initialize crypto system
    private val keyStore = FakeKeyStore()
    private val sessionCrypto = FakeSessionCrypto()
    private val cryptoManager = CryptoManager(keyStore, sessionCrypto)
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _peerDevice = MutableStateFlow<PeerDevice?>(null)
    val peerDevice: StateFlow<PeerDevice?> = _peerDevice.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private var currentPeerAddress: String? = null
    private var currentPage = 0
    private val pageSize = 50
    private var hasMoreMessages = true
    
    fun initializeChat(peer: PeerDevice) {
        _peerDevice.value = peer
        currentPeerAddress = peer.address
        currentPage = 0
        hasMoreMessages = true
        
        // Load initial messages
        loadMessages()
        
        // Add welcome message
        addSystemMessage("Chat started with ${peer.addressHash}")
        
        // Establish crypto session if we have the peer's DH public key
        establishCryptoSession(peer)
    }
    
    private fun establishCryptoSession(peer: PeerDevice) {
        // For now, use a mock DH public key
        // In a real implementation, this would come from the peer's identity
        val mockPeerDhPub = ByteArray(32).apply { 
            Random().nextBytes(this) 
        }
        
        val success = cryptoManager.establishSession(
            peer.address.toByteArray(Charsets.UTF_8),
            mockPeerDhPub
        )
        
        if (success) {
            addSystemMessage("Secure session established")
        } else {
            addSystemMessage("Failed to establish secure session")
        }
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank() || currentPeerAddress == null) return
        
        viewModelScope.launch {
            _isSending.value = true
            
            try {
                // Create chat message
                val message = ChatMessage.createTextMessage(content, currentPeerAddress!!)
                
                // Add to local messages immediately
                addMessage(message)
                
                // Save to database
                chatMessageDao.insertMessage(message)
                
                // Encrypt the message payload
                val plaintext = content.toByteArray(Charsets.UTF_8)
                val peerId = currentPeerAddress!!.toByteArray(Charsets.UTF_8)
                val encryptedPayload = cryptoManager.encryptMessage(peerId, plaintext)
                
                // Create envelope with encrypted payload
                val envelope = if (cryptoManager.hasSession(peerId)) {
                    Envelope.createEncryptedMessage(
                        msgId = message.msgId ?: UUID.randomUUID(),
                        topicId = Envelope.LOCAL_TOPIC_ID,
                        payload = encryptedPayload
                    )
                } else {
                    Envelope(
                        msgId = message.msgId ?: UUID.randomUUID(),
                        topicId = Envelope.LOCAL_TOPIC_ID,
                        timestamp = System.currentTimeMillis(),
                        ttl = Envelope.DEFAULT_TTL,
                        flags = 0, // No encryption
                        payload = plaintext
                    )
                }
                
                // Update message with envelope ID
                val updatedMessage = message.copy(msgId = envelope.msgId)
                chatMessageDao.updateMessage(updatedMessage)
                
                // TODO: Send via MeshRouter
                // For now, simulate sending
                simulateMessageDelivery(message.id, envelope.msgId)
                
            } finally {
                _isSending.value = false
            }
        }
    }
    
    fun loadMessages() {
        if (currentPeerAddress == null || !hasMoreMessages || _isLoadingMore.value) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            try {
                val offset = currentPage * pageSize
                val newMessages = chatMessageDao.getMessagesForPeerPaginated(
                    currentPeerAddress!!, 
                    pageSize, 
                    offset
                ).first()
                
                if (newMessages.isNotEmpty()) {
                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.addAll(newMessages)
                    _messages.value = currentMessages.sortedBy { it.timestamp }
                    
                    currentPage++
                    hasMoreMessages = newMessages.size >= pageSize
                } else {
                    hasMoreMessages = false
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    
    fun loadMoreMessages() {
        if (hasMoreMessages && !_isLoadingMore.value) {
            loadMessages()
        }
    }
    
    fun addMessage(message: ChatMessage) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(message)
        _messages.value = currentMessages.sortedBy { it.timestamp }
    }
    
    fun updateMessageStatus(messageId: UUID, status: ChatMessage.MessageStatus) {
        viewModelScope.launch {
            chatMessageDao.updateMessageStatus(messageId, status)
            
            val currentMessages = _messages.value.map { message ->
                if (message.id == messageId) {
                    message.copy(status = status)
                } else {
                    message
                }
            }
            _messages.value = currentMessages
        }
    }
    
    fun updateMessageStatusByEnvelopeId(envelopeMsgId: UUID, status: ChatMessage.MessageStatus) {
        viewModelScope.launch {
            chatMessageDao.updateMessageStatusByEnvelopeId(envelopeMsgId, status)
            
            val currentMessages = _messages.value.map { message ->
                if (message.msgId == envelopeMsgId) {
                    message.copy(status = status)
                } else {
                    message
                }
            }
            _messages.value = currentMessages
        }
    }
    
    fun receiveMessage(envelope: Envelope) {
        if (envelope.isAck) {
            // Handle ACK
            handleAck(envelope)
            return
        }
        
        // Decrypt the message if it's encrypted
        val decryptedPayload = if (envelope.isEncrypted()) {
            val peerId = currentPeerAddress?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
            cryptoManager.decryptMessage(peerId, envelope.payload)
        } else {
            envelope.payload
        }
        
        // Create envelope with decrypted payload for processing
        val decryptedEnvelope = envelope.copy(payload = decryptedPayload)
        
        val message = ChatMessage.fromEnvelope(decryptedEnvelope, false, currentPeerAddress ?: "")
        
        viewModelScope.launch {
            // Save to database
            chatMessageDao.insertMessage(message)
            
            // Add to local messages
            addMessage(message)
            
            // Send ACK
            sendAck(envelope.msgId, envelope.topicId)
        }
    }
    
    private fun handleAck(envelope: Envelope) {
        val originalMsgId = envelope.ackForMsgId ?: return
        
        // Update message status to DELIVERED
        updateMessageStatusByEnvelopeId(originalMsgId, ChatMessage.MessageStatus.DELIVERED)
    }
    
    private fun sendAck(originalMsgId: UUID, topicId: Int) {
        val ackEnvelope = Envelope.createAck(originalMsgId, topicId)
        
        // TODO: Send ACK via MeshRouter
        // For now, just log it
        com.meshchat.app.core.Logger.d("Sending ACK for message $originalMsgId")
    }
    
    fun setConnectionStatus(connected: Boolean) {
        _isConnected.value = connected
        
        if (connected) {
            addSystemMessage("Connected to peer")
        } else {
            addSystemMessage("Disconnected from peer")
        }
    }
    
    private fun addSystemMessage(content: String) {
        val systemMessage = ChatMessage(
            id = UUID.randomUUID(),
            content = content,
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            peerAddress = currentPeerAddress ?: "",
            status = ChatMessage.MessageStatus.RECEIVED
        )
        addMessage(systemMessage)
    }
    
    private fun simulateMessageDelivery(messageId: UUID, envelopeMsgId: UUID) {
        viewModelScope.launch {
            // Simulate SENT -> RELAYED -> DELIVERED progression
            kotlinx.coroutines.delay(100)
            updateMessageStatus(messageId, ChatMessage.MessageStatus.SENT)
            
            kotlinx.coroutines.delay(500)
            updateMessageStatus(messageId, ChatMessage.MessageStatus.RELAYED)
            
            kotlinx.coroutines.delay(1000)
            updateMessageStatus(messageId, ChatMessage.MessageStatus.DELIVERED)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
}
