package com.example.cardna.tts

/** 한 줄 음성 안내를 재생하는 공통 인터페이스. */
interface VoiceAnnouncer {
    fun announce(text: String)
    fun release()
}
