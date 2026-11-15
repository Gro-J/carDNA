package com.example.cardna.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

private const val TAG = "AndroidVoiceAnnouncer"

/**
 * Android 기본 TextToSpeech 엔진을 사용하는 구현체.
 *
 * Pleos TextToSpeech SDK(ai.pleos.playground:TextToSpeech:3.3.1)도 실제로 존재하지만,
 * 내부적으로 별도의 CAAS 백그라운드 서비스(버전 4.1.29+)에 바인딩되어 토큰 등록과
 * 스트리밍 연결까지 거쳐야 동작하는 구조이고 공식 샘플 저장소에도 사용 예제가 없어
 * 이 환경에서 런타임 동작을 검증할 수 없었다. 마감 우선순위에 따라 핵심 루프를
 * 확실히 보여줄 수 있는 Android 기본 TTS로 구현했다. [PleosVoiceAnnouncerTodo] 참고.
 */
class AndroidVoiceAnnouncer(context: Context) : VoiceAnnouncer {

    private var isReady = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            isReady = true
        } else {
            Log.w(TAG, "TextToSpeech init failed with status $status")
        }
    }

    override fun announce(text: String) {
        if (!isReady) {
            Log.w(TAG, "TTS not ready yet; dropping announcement: $text")
            return
        }
        Log.i(TAG, "Announcing: $text")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cardna-announcement")
    }

    override fun release() {
        tts.stop()
        tts.shutdown()
    }
}
