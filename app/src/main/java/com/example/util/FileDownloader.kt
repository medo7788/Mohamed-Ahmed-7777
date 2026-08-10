package com.example.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FileDownloader {

    val VOICE_URLS = mapOf(
        "makkah" to "https://cdn.aladhan.com/audio/adhan/Makkah.mp3",
        "madinah" to "https://cdn.aladhan.com/audio/adhan/Madinah.mp3",
        "alafasy" to "https://cdn.aladhan.com/audio/adhan/Alafasy.mp3",
        "abdulbasit" to "https://cdn.aladhan.com/audio/adhan/Abdulbasit.mp3"
    )

    fun downloadAdhanVoice(
        context: Context,
        voiceKey: String,
        onProgress: (Int) -> Unit = {}
    ): File? {
        val url = VOICE_URLS[voiceKey] ?: return null
        val file = downloadFileWithProgress(context, url, "$voiceKey.mp3", onProgress)
        return file
    }

    private fun downloadFileWithProgress(
        context: Context,
        fileUrl: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): File? {
        return try {
            val directory = File(context.filesDir, "adhan_sounds")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(file)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(((total * 100) / fileLength).toInt())
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getLocalFile(context: Context, fileName: String): File? {
        val file = File(File(context.filesDir, "adhan_sounds"), fileName)
        return if (file.exists() && file.length() > 1000) file else null
    }

    fun isVoiceCached(context: Context, voiceKey: String): Boolean {
        return getLocalFile(context, "$voiceKey.mp3") != null
    }

    fun deleteVoiceCache(context: Context, voiceKey: String): Boolean {
        val file = File(File(context.filesDir, "adhan_sounds"), "$voiceKey.mp3")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun getTotalCacheSizeInMb(context: Context): Double {
        val directory = File(context.filesDir, "adhan_sounds")
        if (!directory.exists() || !directory.isDirectory) return 0.0
        val totalBytes = directory.listFiles()?.sumOf { it.length() } ?: 0L
        return totalBytes.toDouble() / (1024 * 1024)
    }

    fun clearAllVoiceCache(context: Context) {
        val directory = File(context.filesDir, "adhan_sounds")
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { it.delete() }
        }
    }
}
