package com.meshchat.app.core.ble

import com.meshchat.app.core.data.Envelope
import com.meshchat.app.core.data.Chunk
import java.util.UUID

/**
 * Splits Envelopes into chunks for transmission over BLE.
 * Each chunk is limited by MTU size and includes sequencing information.
 */
class Chunker {
    
    private var nextMsgSeq: UInt = 0u
    
    /**
     * Split an Envelope into chunks that fit within the specified MTU.
     * @param envelope The envelope to chunk
     * @param mtu The maximum transmission unit size
     * @return List of chunks ready for transmission
     */
    fun chunkEnvelope(envelope: Envelope, mtu: Int): List<Chunk> {
        val frameBytes = envelopeToBytes(envelope)
        val maxChunkSize = mtu - Chunk.HEADER_SIZE
        
        if (frameBytes.size <= maxChunkSize) {
            // Single chunk
            val chunk = Chunk(
                msgSeq = nextMsgSeq++,
                index = 0u,
                total = 1u,
                data = frameBytes
            )
            return listOf(chunk)
        }
        
        // Multiple chunks
        val chunks = mutableListOf<Chunk>()
        val totalChunks = ((frameBytes.size - 1) / maxChunkSize + 1).toUShort()
        
        for (i in 0 until totalChunks.toInt()) {
            val start = i * maxChunkSize
            val end = minOf(start + maxChunkSize, frameBytes.size)
            val chunkData = frameBytes.copyOfRange(start, end)
            
            val chunk = Chunk(
                msgSeq = nextMsgSeq,
                index = i.toUShort(),
                total = totalChunks,
                data = chunkData
            )
            chunks.add(chunk)
        }
        
        nextMsgSeq++
        return chunks
    }
    
    /**
     * Convert an Envelope to a length-prefixed byte array.
     * Format: [u32 length][u128 msgId][u32 topicId][u64 timestamp][u32 ttl][u32 flags][u32 payloadLength][payload]
     */
    private fun envelopeToBytes(envelope: Envelope): ByteArray {
        val payloadLength = envelope.payload.size
        val totalLength = 4 + 16 + 4 + 8 + 4 + 4 + 4 + payloadLength + 1 + 16 // +1 for isAck flag, +16 for ackForMsgId
        
        val bytes = ByteArray(totalLength)
        var offset = 0
        
        // Write total length (u32)
        writeUInt32(bytes, offset, totalLength.toUInt())
        offset += 4
        
        // Write message ID (u128 as 16 bytes)
        val msgIdBytes = uuidToBytes(envelope.msgId)
        System.arraycopy(msgIdBytes, 0, bytes, offset, 16)
        offset += 16
        
        // Write topic ID (u32)
        writeUInt32(bytes, offset, envelope.topicId.toUInt())
        offset += 4
        
        // Write timestamp (u64)
        writeUInt64(bytes, offset, envelope.timestamp.toULong())
        offset += 8
        
        // Write TTL (u32)
        writeUInt32(bytes, offset, envelope.ttl.toUInt())
        offset += 4
        
        // Write flags (u32)
        writeUInt32(bytes, offset, envelope.flags.toUInt())
        offset += 4
        
        // Write payload length (u32)
        writeUInt32(bytes, offset, payloadLength.toUInt())
        offset += 4
        
        // Write payload
        System.arraycopy(envelope.payload, 0, bytes, offset, payloadLength)
        offset += payloadLength
        
        // Write isAck flag (u8)
        bytes[offset] = if (envelope.isAck) 1 else 0
        offset += 1
        
        // Write ackForMsgId (u128 as 16 bytes, or 0 if null)
        val ackForMsgIdBytes = envelope.ackForMsgId?.let { uuidToBytes(it) } ?: ByteArray(16)
        System.arraycopy(ackForMsgIdBytes, 0, bytes, offset, 16)
        offset += 16
        
        return bytes
    }
    
    /**
     * Write a 32-bit unsigned integer to a byte array
     */
    private fun writeUInt32(bytes: ByteArray, offset: Int, value: UInt) {
        bytes[offset] = (value and 0xFFu).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFFu).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFFu).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFFu).toByte()
    }
    
    /**
     * Write a 64-bit unsigned integer to a byte array
     */
    private fun writeUInt64(bytes: ByteArray, offset: Int, value: ULong) {
        writeUInt32(bytes, offset, (value and 0xFFFFFFFFu).toUInt())
        writeUInt32(bytes, offset + 4, ((value shr 32) and 0xFFFFFFFFu).toUInt())
    }
    
    /**
     * Convert UUID to 16-byte array
     */
    private fun uuidToBytes(uuid: UUID): ByteArray {
        val bytes = ByteArray(16)
        val mostSigBits = uuid.mostSignificantBits
        val leastSigBits = uuid.leastSignificantBits
        
        for (i in 0..7) {
            bytes[i] = ((mostSigBits shr (8 * (7 - i))) and 0xFF).toByte()
        }
        for (i in 8..15) {
            bytes[i] = ((leastSigBits shr (8 * (15 - i))) and 0xFF).toByte()
        }
        
        return bytes
    }
    
    /**
     * Get the current message sequence number
     */
    fun getCurrentMsgSeq(): UInt = nextMsgSeq
    
    /**
     * Reset the message sequence counter (useful for testing)
     */
    fun resetMsgSeq() {
        nextMsgSeq = 0u
    }
}
