package com.meshchat.app.core.data

/**
 * Represents a chunk of a larger message that has been split for transmission.
 * Each chunk contains a header with sequencing information and a portion of the original data.
 */
data class Chunk(
    val msgSeq: UInt,
    val index: UShort,
    val total: UShort,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chunk

        if (msgSeq != other.msgSeq) return false
        if (index != other.index) return false
        if (total != other.total) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = msgSeq.hashCode()
        result = 31 * result + index.hashCode()
        result = 31 * result + total.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
    
    /**
     * Check if this is the first chunk (index == 0)
     */
    fun isFirst(): Boolean = index == 0u
    
    /**
     * Check if this is the last chunk (index == total - 1)
     */
    fun isLast(): Boolean = index == total - 1u
    
    /**
     * Check if this is a single chunk message (total == 1)
     */
    fun isSingle(): Boolean = total == 1u
    
    /**
     * Get the chunk size in bytes
     */
    fun size(): Int = data.size
    
    companion object {
        /**
         * Header size for each chunk (msgSeq + index + total)
         */
        const val HEADER_SIZE = 8 // 4 bytes for msgSeq + 2 bytes for index + 2 bytes for total
    }
}
