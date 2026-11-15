# CarDNA

Android Automotive OS(Pleos Connect)에서 와이퍼 블레이드 사용률을 추적하고, 교체 시점을 안내하는 1차 제출용 MVP입니다.

## 실행 방법

1. Android Studio로 프로젝트 루트를 엽니다.
2. `automotive` 실행 구성(run configuration)을 Android Automotive 에뮬레이터(아래 "테스트 환경" 참고)에 실행합니다.
3. 또는 커맨드라인에서:
   ```bash
   ./gradlew :automotive:installDebug
   adb shell am start -n com.example.cardna/.MainActivity
   ```
4. 화면 우측 "데이터 소스" 스위치로 Pleos Vehicle SDK ↔ 시뮬레이션을 전환할 수 있습니다. 기본값은 시뮬레이션입니다.
5. "교체 완료" 버튼을 누르면 누적 사용량이 초기화되고 보정계수가 갱신됩니다.

### Release APK

루트의 `CarDNA_release.apk`가 서명된 release 빌드입니다. 서명에 사용한 로컬 keystore(`cardna-release.jks`, `keystore.properties`)는 git에 포함되어 있지 않습니다.

release 빌드는 R8 `optimization`(패키지 리패키징)을 끈 상태입니다. 켜두면 AndroidX Startup과 Kotlin stdlib 조합에서 `IllegalAccessError`가 발생해 앱이 시작하자마자 죽는 것을 실제 기기에서 확인해서, MVP 우선순위(빌드되고 실행되는 것)에 따라 비활성화했습니다.

## 테스트 환경

| 항목 | 값 |
|---|---|
| OS | macOS (Darwin, arm64 / Apple Silicon) |
| Android Studio | 2026.2.2 (AI-262.10968.63.2622.16840690) |
| JDK | OpenJDK 25 (Android Studio 번들 JBR) |
| Gradle | 9.6.0 (프로젝트 Wrapper) |
| AGP | 9.5.1 |
| Kotlin | 2.2.10 |
| 에뮬레이터 | `CarDNA_Automotive` — Android Automotive with Google APIs, API 35 (ext15), arm64-v8a (`system-images;android-35-ext15;android-automotive;arm64-v8a`) |
| 화면 규격 | 1024 x 768 (가로, `automotive_1024p_landscape` 디바이스 프로필) |

실제 앱 화면(`automotive` 모듈 MainActivity)을 위 에뮬레이터에 설치해 실행 화면을 확인했습니다.

## 구현 범위

### 1차 MVP (이번 제출)

- `VehicleDataSource` 인터페이스 + 두 구현체(`PleosVehicleSource`, `SimulatedVehicleSource`)를 화면 토글로 전환
- 와이퍼 "꺼짐/저속/고속" 상태를 시간 가중 누적 → 사용률(%) 계산 (`UsageEngine`, SharedPreferences 저장)
- "교체 완료" 시 보정계수(k) 재계산 및 누적값 초기화
- 가로 차량 화면(Compose): 원형 게이지, 상태 문구(정상/다음 정비 때 함께/지금 확인), 데이터 소스 토글, 시뮬레이션 배지
- 주차 브레이크 해제→체결 전이 시 한 줄 음성 안내 (Android 기본 TextToSpeech)
- 디버그/릴리스 빌드, 로컬 keystore 서명

### 2차 예정 (이번 범위 밖)

- 실제 주차 브레이크 신호 연동 (아래 "경로 A/B" 참고 — 현재 SDK에 해당 API 없음)
- Pleos TextToSpeech SDK 실연동 (CAAS 서비스 등록/인증 플로우 포함)
- 와이퍼 외 블레이드(헤드라이트, 타이어 등) 부품으로 확장
- 사용 이력 그래프, 여러 차량/프로필 지원, Room 기반 영속화

## 경로 A / B — 실제로 확인한 것과 시뮬레이션인 것

이 프로젝트는 "전부 A" 또는 "전부 B"가 아니라, **신호별로 실제 SDK 존재 여부를 직접 확인한 뒤 결정**했습니다. 추측으로 API를 만들지 않았습니다.

- **공식 샘플**: `https://github.com/Pleos-Playground/pleos-sample-vehicle-app` 를 clone해 Manifest(권한 목록), Gradle 의존성 선언 방식(`ai.pleos.playground:Vehicle`, 전용 Nexus 저장소), `Vehicle`/`Wiper`/`Brake` 등 실제 사용 패턴을 확인했습니다.
- **SDK 실물 확인**: 샘플이 가리키는 Nexus 저장소(`nexus-playground.pleos.ai`)에서 `Vehicle-2.3.2.aar`, `TextToSpeech-3.3.1.aar`를 내려받아 `classes.jar`를 직접 디컴파일(`javap`)해서 실제 클래스/메서드 시그니처를 확인했습니다.

| 신호 | 경로 | 비고 |
|---|---|---|
| 와이퍼 상태/강도 | **경로 A (실제 SDK)** | `ai.pleos.playground.vehicle.api.Wiper` — `registerWiperState`, `getCurrentWiperSwitch` 등 실제 메서드 사용. 단, 이 개발 환경에는 Pleos Connect 차량 서비스가 없어 **런타임 연결(실제 차량 신호 수신)은 검증하지 못했습니다.** 컴파일/타입 레벨까지 실제 SDK로 구현되어 있습니다. |
| 주차 브레이크 | **경로 B (시뮬레이션)** | Pleos Vehicle SDK 2.3.2에는 주차 브레이크 API가 **존재하지 않습니다.** `Brake` 도메인은 브레이크 페달 밟음 여부(`getCurrentBrakePedalStatus`)만, `Safety` 도메인은 브레이크 액 레벨만 제공합니다. 추측해서 API를 만들지 않고, 두 데이터 소스 모두에서 주차 브레이크는 시뮬레이션 사이클로 대체했습니다(코드 내 TODO/주석 명시). |
| 음성 안내(TTS) | **Android 기본 TextToSpeech** | Pleos TextToSpeech SDK(`ai.pleos.playground:TextToSpeech:3.3.1`)도 실제로 확인했지만, 별도의 CAAS 백그라운드 서비스(최소 버전 4.1.29, 토큰 등록/스트리밍)에 바인딩해야 동작하는 구조이고 공식 샘플에도 사용 예제가 없어, 이 환경에서 런타임 검증이 불가능했습니다. 마감 우선순위에 따라 Android 기본 TTS로 핵심 루프를 완성했고, 실제 Pleos TTS 연동 지점은 `tts/PleosVoiceAnnouncerTodo.kt`에 실제 클래스명과 함께 TODO로 남겨두었습니다. |

즉, **"시뮬레이션 데이터로 동작"하는 부분은 주차 브레이크 신호 전체와, 데이터 소스 토글을 시뮬레이션으로 두었을 때의 와이퍼 신호입니다.** 토글을 "Pleos Vehicle SDK"로 전환하면 와이퍼는 실제 SDK 호출 경로를 타지만(실차량/Pleos Connect 환경에서만 실제 값이 들어옴), 이 테스트 환경에서는 차량 서비스가 없어 신호가 들어오지 않아 0%/OFF로 보일 수 있습니다. 이는 의도된 동작이며, 실제 연동한 것처럼 포장하지 않았습니다.

## 가정치

- 와이퍼 블레이드 "저속 환산" 기준 사용 시간(`UsageEngine.BASELINE_USAGE_HOURS` = 90시간)은 실제 제품 수명 데이터가 아니라, 데모에서 핵심 루프(정상→다음 정비 때 함께→지금 확인)를 짧은 시간 안에 관찰할 수 있도록 의도적으로 축소한 가정치입니다.
- 시뮬레이션 모드는 "1초 = 1시간"으로 시간을 가속합니다.
