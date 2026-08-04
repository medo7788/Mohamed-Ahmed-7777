package com.example.util

import android.content.Context
import java.io.File
import java.net.URL

object FileDownloader {

    val VOICE_URLS = mapOf(
        "makkah" to "https://cdn.aladhan.com/audio/adhan/Makkah.mp3",
        "madinah" to "https://cdn.aladhan.com/audio/adhan/Madinah.mp3",
        "alafasy" to "https://cdn.aladhan.com/audio/adhan/Alafasy.mp3",
        "abdulbasit" to "https://cdn.aladhan.com/audio/adhan/Abdulbasit.mp3"
    )

    fun downloadAdhanVoice(context: Context, voiceKey: String): File? {
        val url = VOICE_URLS[voiceKey] ?: return null
        android.util.Log.d("FileDownloader", "Attempting to download adhan voice: $voiceKey from $url")
        val file = downloadFile(context, url, "$voiceKey.mp3")
        if (file != null) {
            android.util.Log.d("FileDownloader", "Successfully downloaded: ${file.absolutePath}")
        } else {
            android.util.Log.e("FileDownloader", "Failed to download adhan voice: $voiceKey")
        }
        return file
    }

    fun downloadFile(context: Context, fileUrl: String, fileName: String): File? {
        return try {
            val directory = File(context.filesDir, "adhan_sounds")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            
            android.util.Log.d("FileDownloader", "Starting download to: ${file.absolutePath}")
            URL(fileUrl).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            android.util.Log.e("FileDownloader", "Download error", e)
            e.printStackTrace()
            null
        }
    }

    fun getLocalFile(context: Context, fileName: String): File? {
        val file = File(File(context.filesDir, "adhan_sounds"), fileName)
        return if (file.exists()) file else null
    }
}
