package com.example.data

object ApiEndpoints {
    // AlAdhan
    const val PRAYER_TIMES = "https://api.aladhan.com/v1/timingsByCity"
    const val NEXT_PRAYER = "https://api.aladhan.com/v1/nextPrayer"
    const val HIJRI_CALENDAR = "https://api.aladhan.com/v1/gToH"

    // Quran.com
    const val QURAN_CHAPTERS = "https://api.quran.com/v4/chapters"
    fun quranVersesByChapter(chapter: Int) = "https://api.quran.com/v4/verses/by_chapter/$chapter"
    fun quranAudioByChapter(recitationId: Int, chapter: Int) = "https://api.quran.com/v4/recitations/$recitationId/by_chapter/$chapter"

    // External Audio
    const val ADHAN_AUDIO = "https://download.quranicaudio.com/quran/adhan/adhan.mp3"
}
