package com.example.util

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { INFO, WARNING, ERROR }

data class AppLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val level: LogLevel,
    val tag: String,
    val message: String
)

object AppErrorLogger {
    private const val TAG = "AppErrorLogger"
    val logList = mutableStateListOf<AppLogEntry>()

    fun logInfo(tag: String, message: String) {
        val entry = AppLogEntry(level = LogLevel.INFO, tag = tag, message = message)
        logList.add(0, entry)
        if (logList.size > 100) logList.removeLast()
        Log.i(tag, message)
    }

    fun logWarning(tag: String, message: String) {
        val entry = AppLogEntry(level = LogLevel.WARNING, tag = tag, message = message)
        logList.add(0, entry)
        if (logList.size > 100) logList.removeLast()
        Log.w(tag, message)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message\n${throwable.localizedMessage}" else message
        val entry = AppLogEntry(level = LogLevel.ERROR, tag = tag, message = fullMsg)
        logList.add(0, entry)
        if (logList.size > 100) logList.removeLast()
        Log.e(tag, fullMsg, throwable)
    }

    fun clearLogs() {
        logList.clear()
    }
}
