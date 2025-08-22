package com.meshchat.app.core

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object Logger {
    private const val TAG = "MeshChat"
    private const val MAX_LOG_ENTRIES = 1000
    
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )
    
    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
        addToBuffer("D", tag, message)
    }
    
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
        addToBuffer("I", tag, message)
    }
    
    fun w(message: String, tag: String = TAG) {
        Log.w(tag, message)
        addToBuffer("W", tag, message)
    }
    
    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        addToBuffer("E", tag, message, throwable)
    }
    
    fun v(message: String, tag: String = TAG) {
        Log.v(tag, message)
        addToBuffer("V", tag, message)
    }
    
    private fun addToBuffer(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message, throwable)
        logBuffer.offer(entry)
        
        // Maintain ring buffer size
        while (logBuffer.size > MAX_LOG_ENTRIES) {
            logBuffer.poll()
        }
    }
    
    fun getLogs(): List<LogEntry> {
        return logBuffer.toList().reversed() // Most recent first
    }
    
    fun getLogsAsText(): String {
        return getLogs().joinToString("\n") { entry ->
            val timestamp = dateFormat.format(Date(entry.timestamp))
            val throwableInfo = entry.throwable?.let { " (${it.javaClass.simpleName}: ${it.message})" } ?: ""
            "$timestamp ${entry.level}/${entry.tag}: ${entry.message}$throwableInfo"
        }
    }
    
    fun clearLogs() {
        logBuffer.clear()
    }
    
    fun getLogCount(): Int = logBuffer.size
}
