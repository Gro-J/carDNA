package com.example.cardna.tts

/**
 * 실제 Pleos TextToSpeech SDK 연동 자리.
 *
 * 확인한 실제 진입점(디컴파일로 확인, 추측 아님):
 *   - ai.pleos.playground.tts.factory.TtsFactory
 *   - ai.pleos.playground.tts.factory.Tts (StreamTts / OnDeviceStreamTts 구현)
 *   - ai.pleos.playground.tts.service.TtsServiceManager (registerApp, getTokenSuspend 등)
 *
 * 이 SDK는 차량에 설치된 별도의 CAAS 서비스(ai.pleos.playground.caas, 최소 버전 4.1.29)에
 * 바인딩되고 앱 등록/토큰 발급까지 거치는 구조다. 공식 샘플 저장소(pleos-sample-vehicle-app)에
 * 사용 예제가 없고, 이 개발/테스트 환경에는 CAAS 서비스가 없어 실제 호출을 검증할 방법이
 * 없었다. 1차 제출 MVP는 시간 제약상 [com.example.cardna.tts.AndroidVoiceAnnouncer]
 * (Android 기본 TTS)로 동일한 [VoiceAnnouncer] 인터페이스를 구현해 핵심 루프를 완성했다.
 *
 * 2차 구현 시: TtsFactory로 Tts 인스턴스를 얻어 registerApp 이후 doSpeak를 호출하는
 * 흐름이 될 것으로 보이나, 정확한 초기화 순서는 Pleos 측 공식 문서/지원을 통해
 * 확인이 필요하다. 지금 여기서 메서드를 추측해 구현하지 않는다.
 */
object PleosVoiceAnnouncerTodo
