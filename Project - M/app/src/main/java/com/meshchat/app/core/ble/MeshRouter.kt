package com.meshchat.app.core.ble

import com.meshchat.app.core.Logger
import com.meshchat.app.core.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MeshRouter(private val scope: CoroutineScope) {
    private val activeLinks = ConcurrentHashMap<String, MeshLink>()
    private val _localInbox = MutableStateFlow<List<Envelope>>(emptyList())
    val localInbox: StateFlow<List<Envelope>> = _localInbox.asStateFlow()
    
    private val outgoingQueue = ConcurrentHashMap<UUID, OutgoingMessage>()
    private val _routerStats = MutableStateFlow(RouterStats())
    val routerStats: StateFlow<RouterStats> = _routerStats.asStateFlow()
    
    init {
        startStatsMonitoring()
    }
    
    fun registerLink(peerAddress: String, link: MeshLink) {
        activeLinks[peerAddress] = link
        Logger.i("Registered link for peer: $peerAddress", "MeshRouter")
        updateStats()
    }
    
    fun unregisterLink(peerAddress: String) {
        activeLinks.remove(peerAddress)
        Logger.i("Unregistered link for peer: $peerAddress", "MeshRouter")
        updateStats()
    }
    
    fun processIncomingEnvelope(envelope: Envelope, sourceAddress: String) {
        Logger.d("Processing incoming envelope: ${envelope.msgId} from $sourceAddress", "MeshRouter")
        
        if (envelope.isAck) {
            // Handle ACK frame
            handleAck(envelope, sourceAddress)
            return
        }
        
        // Check if this is a local message
        if (envelope.topicId == Envelope.LOCAL_TOPIC_ID) {
            // Deliver to local inbox
            deliverToLocalInbox(envelope)
            return
        }
        
        // Check TTL and flood if appropriate
        if (envelope.ttl > 0) {
            val floodedEnvelope = envelope.decrementTtl()
            floodMessage(floodedEnvelope, sourceAddress)
        }
        
        // Update stats
        updateStats()
    }
    
    private fun handleAck(envelope: Envelope, sourceAddress: String) {
        val originalMsgId = envelope.ackForMsgId ?: return
        
        Logger.d("Received ACK for message: $originalMsgId from $sourceAddress", "MeshRouter")
        
        // Update outgoing message status
        val outgoingMessage = outgoingQueue[originalMsgId]
        if (outgoingMessage != null) {
            outgoingMessage.status = OutgoingMessage.Status.DELIVERED
            outgoingMessage.deliveredAt = System.currentTimeMillis()
            outgoingQueue[originalMsgId] = outgoingMessage
            
            Logger.i("Message $originalMsgId marked as DELIVERED", "MeshRouter")
        }
        
        // Update stats
        updateStats()
    }
    
    private fun deliverToLocalInbox(envelope: Envelope) {
        val currentInbox = _localInbox.value.toMutableList()
        currentInbox.add(envelope)
        _localInbox.value = currentInbox
        
        Logger.i("Delivered message ${envelope.msgId} to local inbox", "MeshRouter")
    }
    
    fun sendToPeer(envelope: Envelope, peerAddress: String) {
        val link = activeLinks[peerAddress] ?: run {
            Logger.w("No active link for peer: $peerAddress", "MeshRouter")
            return
        }
        
        if (!link.isConnected) {
            Logger.w("Link not connected for peer: $peerAddress", "MeshRouter")
            return
        }
        
        // Add to outgoing queue
        val outgoingMessage = OutgoingMessage(
            msgId = envelope.msgId,
            envelope = envelope,
            targetPeer = peerAddress,
            status = OutgoingMessage.Status.SENDING,
            createdAt = System.currentTimeMillis()
        )
        outgoingQueue[envelope.msgId] = outgoingMessage
        
        // Attempt to send
        attemptSend(outgoingMessage)
    }
    
    fun broadcastMessage(envelope: Envelope) {
        Logger.i("Broadcasting message: ${envelope.msgId} to ${activeLinks.size} peers", "MeshRouter")
        
        activeLinks.keys.forEach { peerAddress ->
            sendToPeer(envelope, peerAddress)
        }
    }
    
    private fun attemptSend(outgoingMessage: OutgoingMessage) {
        val link = activeLinks[outgoingMessage.targetPeer] ?: return
        
        scope.launch {
            try {
                // Chunk the envelope
                val chunker = Chunker()
                val chunks = chunker.chunkEnvelope(outgoingMessage.envelope, link.mtuSize)
                
                Logger.d("Sending ${chunks.size} chunks for message ${outgoingMessage.msgId}", "MeshRouter")
                
                // Send chunks
                chunks.forEach { chunk ->
                    val chunkBytes = chunkToBytes(chunk)
                    link.send(chunkBytes)
                }
                
                // Update status to SENT
                outgoingMessage.status = OutgoingMessage.Status.SENT
                outgoingMessage.sentAt = System.currentTimeMillis()
                outgoingQueue[outgoingMessage.msgId] = outgoingMessage
                
                Logger.i("Message ${outgoingMessage.msgId} sent successfully", "MeshRouter")
                
                // Update stats
                updateStats()
                
            } catch (e: Exception) {
                Logger.e("Failed to send message ${outgoingMessage.msgId}", "MeshRouter", e)
                outgoingMessage.status = OutgoingMessage.Status.FAILED
                outgoingMessage.failedAt = System.currentTimeMillis()
                outgoingQueue[outgoingMessage.msgId] = outgoingMessage
            }
        }
    }
    
    private fun chunkToBytes(chunk: Chunk): ByteArray {
        val bytes = ByteArray(Chunk.HEADER_SIZE + chunk.data.size)
        var offset = 0
        
        // Write message sequence (u32)
        writeUInt32(bytes, offset, chunk.msgSeq)
        offset += 4
        
        // Write index (u16)
        writeUInt16(bytes, offset, chunk.index)
        offset += 2
        
        // Write total (u16)
        writeUInt16(bytes, offset, chunk.total)
        offset += 2
        
        // Write data
        System.arraycopy(chunk.data, 0, bytes, offset, chunk.data.size)
        
        return bytes
    }
    
    private fun writeUInt32(bytes: ByteArray, offset: Int, value: UInt) {
        bytes[offset] = (value and 0xFFu).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFFu).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFFu).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFFu).toByte()
    }
    
    private fun writeUInt16(bytes: ByteArray, offset: Int, value: UShort) {
        bytes[offset] = (value and 0xFFu).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFFu).toByte()
    }
    
    private fun floodMessage(envelope: Envelope, excludeAddress: String) {
        val targetPeers = activeLinks.keys.filter { it != excludeAddress }
        
        if (targetPeers.isNotEmpty()) {
            Logger.d("Flooding message ${envelope.msgId} to ${targetPeers.size} peers", "MeshRouter")
            targetPeers.forEach { peerAddress ->
                sendToPeer(envelope, peerAddress)
            }
        }
    }
    
    private fun startStatsMonitoring() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // Update every 10 seconds
                updateStats()
            }
        }
    }
    
    private fun updateStats() {
        val currentStats = _routerStats.value
        val newStats = currentStats.copy(
            activeConnections = activeLinks.size,
            totalRelayedMessages = currentStats.totalRelayedMessages + 1,
            outgoingQueueSize = outgoingQueue.size,
            deliveredMessages = outgoingQueue.values.count { it.status == OutgoingMessage.Status.DELIVERED },
            failedMessages = outgoingQueue.values.count { it.status == OutgoingMessage.Status.FAILED }
        )
        _routerStats.value = newStats
    }
    
    fun getStats(): RouterStats = _routerStats.value
    
    fun clearStats() {
        _routerStats.value = RouterStats()
    }
    
    data class OutgoingMessage(
        val msgId: UUID,
        val envelope: Envelope,
        val targetPeer: String,
        var status: Status,
        val createdAt: Long,
        var sentAt: Long? = null,
        var deliveredAt: Long? = null,
        var failedAt: Long? = null
    ) {
        enum class Status {
            SENDING,
            SENT,
            DELIVERED,
            FAILED
        }
    }
    
    data class RouterStats(
        val activeConnections: Int = 0,
        val totalRelayedMessages: Long = 0,
        val outgoingQueueSize: Int = 0,
        val deliveredMessages: Int = 0,
        val failedMessages: Int = 0
    )
}
