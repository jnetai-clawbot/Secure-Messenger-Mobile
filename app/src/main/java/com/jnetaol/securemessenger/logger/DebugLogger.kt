package com.jnetaol.securemessenger.logger

import android.util.Log
import com.jnetaol.securemessenger.BuildConfig

object DebugLogger {
    private const val TAG = "SecureMessenger"
    private var logBuffer = mutableListOf<String>()
    private const val MAX_BUFFER_SIZE = 500

    fun d(tag: String, method: String, code: String, message: String = "") {
        val log = "[DEBUG][$code][$tag.$method] $message"
        Log.d(TAG, log)
        if (BuildConfig.ENABLE_EXTRA_LOGGING) {
            addToBuffer(log)
        }
    }

    fun i(tag: String, method: String, code: String, message: String = "") {
        val log = "[INFO][$code][$tag.$method] $message"
        Log.i(TAG, log)
        addToBuffer(log)
    }

    fun w(tag: String, method: String, code: String, message: String = "") {
        val log = "[WARN][$code][$tag.$method] $message"
        Log.w(TAG, log)
        addToBuffer(log)
    }

    fun e(tag: String, method: String, code: String, message: String = "", throwable: Throwable? = null) {
        val log = "[ERROR][$code][$tag.$method] $message"
        Log.e(TAG, log, throwable)
        addToBuffer(log)
        if (throwable != null) {
            addToBuffer("[ERROR][$code] StackTrace: ${throwable.stackTraceToString()}")
        }
    }

    private fun addToBuffer(log: String) {
        synchronized(logBuffer) {
            logBuffer.add(log)
            if (logBuffer.size > MAX_BUFFER_SIZE) {
                logBuffer.removeAt(0)
            }
        }
    }

    fun getLogs(): List<String> = synchronized(logBuffer) {
        logBuffer.toList()
    }

    fun clearLogs() {
        synchronized(logBuffer) {
            logBuffer.clear()
        }
    }
}
