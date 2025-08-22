package com.meshchat.app.core.ble

import com.meshchat.app.core.data.Envelope
import com.meshchat.app.core.data.Chunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Reassembler(private val scope: CoroutineScope) {
    private val partialMessages = ConcurrentHashMap<UInt, PartialMessage>()
    private val seenMessageIds = ConcurrentHashMap.newKeySet<UUID>()
    
    companion object {
        private const val CHUNK_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    init {
        startTimeoutMonitoring()
    }
    
    fun addChunk(chunk: Chunk): Envelope? {
        val partial = partialMessages.computeIfAbsent(chunk.msgSeq) { PartialMessage(chunk.msgSeq) }
        
        synchronized(partial) {
            partial.chunks[chunk.index.toInt()] = chunk
            partial.lastChunkTime = System.currentTimeMillis()
            
            // Check if we have all chunks
            if (partial.chunks.size == chunk.total.toInt()) {
                val frameBytes = reconstructFrame(partial)
                partialMessages.remove(chunk.msgSeq)
                
                return try {
                    val envelope = bytesToEnvelope(frameBytes)
                    
                    // Check for duplicates
                    if (seenMessageIds.contains(envelope.msgId)) {
                        com.meshchat.app.core.Logger.d("Dropping duplicate message: ${envelope.msgId}")
                        return null
                    }
                    
                    seenMessageIds.add(envelope.msgId)
                    envelope
                } catch (e: Exception) {
                    com.meshchat.app.core.Logger.e("Failed to parse envelope from reassembled frame", "Reassembler", e)
                    null
                }
            }
        }
        
        return null
    }
    
    private fun reconstructFrame(partial: PartialMessage): ByteArray {
        val totalSize = partial.chunks.values.sumOf { it.data.size }
        val frameBytes = ByteArray(totalSize)
        var offset = 0
        
        for (i in 0 until partial.chunks.size) {
            val chunk = partial.chunks[i] ?: continue
            System.arraycopy(chunk.data, 0, frameBytes, offset, chunk.data.size)
            offset += chunk.data.size
        }
        
        return frameBytes
    }
    
    fun bytesToEnvelope(bytes: ByteArray): Envelope {
        var offset = 0
        
        // Read total length (u32)
        val totalLength = readUInt32(bytes, offset)
        offset += 4
        
        // Read message ID (u128 as 16 bytes)
        val msgId = bytesToUuid(bytes, offset)
        offset += 16
        
        // Read topic ID (u32)
        val topicId = readUInt32(bytes, offset).toInt()
        offset += 4
        
        // Read timestamp (u64)
        val timestamp = readUInt64(bytes, offset)
        offset += 8
        
        // Read TTL (u32)
        val ttl = readUInt32(bytes, offset).toInt()
        offset += 4
        
        // Read flags (u32)
        val flags = readUInt32(bytes, offset).toInt()
        offset += 4
        
        // Read payload length (u32)
        val payloadLength = readUInt32(bytes, offset).toInt()
        offset += 4
        
        // Read payload
        val payload = bytes.copyOfRange(offset, offset + payloadLength)
        offset += payloadLength
        
        // Read isAck flag (u8)
        val isAck = bytes[offset] != 0.toByte()
        offset += 1
        
        // Read ackForMsgId (u128 as 16 bytes)
        val ackForMsgId = if (isAck) {
            val ackBytes = bytes.copyOfRange(offset, offset + 16)
            if (ackBytes.all { it == 0.toByte() }) null else bytesToUuid(ackBytes, 0)
        } else null
        offset += 16
        
        return Envelope(
            msgId = msgId,
            topicId = topicId,
            timestamp = timestamp,
            ttl = ttl,
            flags = flags,
            payload = payload,
            isAck = isAck,
            ackForMsgId = ackForMsgId
        )
    }
    
    private fun readUInt32(bytes: ByteArray, offset: Int): UInt {
        return (bytes[offset].toUByte() or
                (bytes[offset + 1].toUByte() shl 8) or
                (bytes[offset + 2].toUByte() shl 16) or
                (bytes[offset + 3].toUByte() shl 24))
    }
    
    private fun readUInt64(bytes: ByteArray, offset: Int): Long {
        val low = readUInt32(bytes, offset)
        val high = readUInt32(bytes, offset + 4)
        return (high.toLong() shl 32) or low.toLong()
    }
    
    private fun bytesToUuid(bytes: ByteArray, offset: Int): UUID {
        val mostSigBits = (bytes[offset].toLong() and 0xFF shl 56) or
                (bytes[offset + 1].toLong() and 0xFF shl 48) or
                (bytes[offset + 2].toLong() and 0xFF shl 40) or
                (bytes[offset + 3].toLong() and 0xFF shl 32) or
                (bytes[offset + 4].toLong() and 0xFF shl 24) or
                (bytes[offset + 5].toLong() and 0xFF shl 16) or
                (bytes[offset + 6].toLong() and 0xFF shl 8) or
                (bytes[offset + 7].toLong() and 0xFF)
        
        val leastSigBits = (bytes[offset + 8].toLong() and 0xFF shl 56) or
                (bytes[offset + 9].toLong() and 0xFF shl 48) or
                (bytes[offset + 10].toLong() and 0xFF shl 40) or
                (bytes[offset + 11].toLong() and 0xFF shl 32) or
                (bytes[offset + 12].toLong() and 0xFF shl 24) or
                (bytes[offset + 13].toLong() and 0xFF shl 16) or
                (bytes[offset + 14].toLong() and 0xFF shl 8) or
                (bytes[offset + 15].toLong() and 0xFF)
        
        return UUID(mostSigBits, leastSigBits)
    }
    
    private fun startTimeoutMonitoring() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Check every 5 seconds
                cleanupExpiredPartials()
            }
        }
    }
    
    private fun cleanupExpiredPartials() {
        val now = System.currentTimeMillis()
        val expiredKeys = partialMessages.entries
            .filter { (_, partial) -> now - partial.lastChunkTime > CHUNK_TIMEOUT_MS }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            partialMessages.remove(key)
            com.meshchat.app.core.Logger.w("Removed expired partial message: $key", "Reassembler")
        }
    }
    
    fun getStats(): ReassemblerStats {
        return ReassemblerStats(
            partialMessageCount = partialMessages.size,
            seenMessageCount = seenMessageIds.size
        )
    }
    
    fun clear() {
        partialMessages.clear()
        seenMessageIds.clear()
    }
    
    private data class PartialMessage(
        val msgSeq: UInt,
        val chunks: MutableMap<Int, Chunk> = mutableMapOf(),
        var lastChunkTime: Long = System.currentTimeMillis()
    )
    
    data class ReassemblerStats(
        val partialMessageCount: Int,
        val seenMessageCount: Int
    )
}
