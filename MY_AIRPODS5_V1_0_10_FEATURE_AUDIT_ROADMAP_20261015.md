# MY AIRPODS 5 — v1.0.10 기능 감사 / 안전한 후속 개발 후보
Date: 2026-10-15 (Asia/Seoul)
Reference lock: MY_AIRPODS5_V1_0_10_USER_VERIFIED_GOLDEN_LOCK_20261015.md
Baseline: r21 release exact SHA b46c8e38f28332f7af0f98bce7b8b27234f62a83d103363cf0697a2fd127445b

## 검증 범주
- USER-PASS (스마트폰 실물): 앱 실행/유지/백그라운드 복귀, 배터리 UI 표시, 청취 모드 동작.
- SOURCE-PRESENT / USER-UNKNOWN: 아래 기능은 LibrePods 기반 소스에 구현되었으나 이 개인용 빌드에서 아직 항목별 실기 검증 안 됨. 기능 코드를 보유한 사실 != 갤럭시와 연결된 AirPods5에서 정상 작동한 사실.
- MODEL-CAPABILITY: AirPods5 소스의 capability는 LISTENING_MODE, CONVERSATION_AWARENESS, HEAD_GESTURES, ADAPTIVE_AUDIO, SLEEP_DETECTION, ADAPTIVE_VOLUME, STEM_CONFIG. AirPods5 Wireless Charging Case 분기는 SWIPE_FOR_VOLUME도 포함. 모델 인식/펌웨어 및 권한에 따른 차이를 고려.

## 소스 코드별 목록
1. 핵심 실측 성공: 4가지 청취 모드 OFF/TRANSPARENCY/ADAPTIVE/NOISE_CANCELLATION → AirPodsSettingsScreen.kt, AACP listening-mode command. AirPods 자체에서 실제 DSP 동작; 앱이 소리를 다시 인코딩하는 미디어 라우터가 아님.
2. 배터리: left/right/case 컴포넌트, 충전/연결 상태, BatteryView.kt. 이 화면의 한 개 100% 링은 둘이 거의 같은 배터리(±3%)일 때 통합 표시하는 로직을 가질 수 있음. 세 요소 정확도와 사례값은 미검증.
3. 기기명 변경, 마이크 자동/왼쪽/오른쪽, 통화 제어, L/R 길게 누르기 별도 설정 → AirPodsSettingsScreen.kt, MicrophoneSettingsScreen.kt, CallControlScreen.kt, PressAndHoldSettingsScreen.kt.
4. 자동 착용 감지 / 자동 연결, 최적화 충전, 수면 감지, 고개 끄덕임/흔들기 제스처 → AirPodsSettingsScreen.kt, HeadTrackingScreen.kt, data/AirPods.kt.
5. 적응형 볼륨, 대화 인식(볼륨 변경/일시정지), 적응형 오디오 강도, 사용자 EQ → AirPodsSettingsScreen.kt, AppSettingsScreen.kt, AdaptiveStrengthScreen.kt, EqualizerScreen.kt.
6. 접근성: 한쪽 에어팟 ANC, 누름 속도/길게 누르기 시간, 차임음 크기, (모델 지원 시) 스와이프 볼륨 설정; 주변음 상세(증폭/균형/음색/주변음 감소/대화 부스트) → AccessibilitySettingsScreen.kt, TransparencySettingsScreen.kt.
7. UI/빠른 접근: 배터리 및 청취 모드 위젯, 빠른설정 ANC 타일, 알림, 연결 팝업/아일랜드, 테마 → BatteryWidget.kt, NoiseControlWidget.kt, AirPodsQSService.kt, PopupWindow.kt, IslandWindow.kt, AppSettingsScreen.kt.
8. 연결/기기 인계 정책: 상태별 takeover 설정 (연결 해제/대기/음악/통화/벨울림/미디어 시작) → AppSettingsScreen.kt. 실제 차량 AA 오디오 정책 재라우팅과는 별개.
9. hearing aid 및 청각 보호/PPE 기능은 일부 Pro/모델 전용 capability gate가 있음. AirPods5에서 당연히 사용 가능한 '프리미엄' 기능으로 혼동 금지.
10. 카메라 원격 제어 관련 코드 일부는 주석/미완성. 정상 작동 기능으로 표기 금지.
11. '프리미엄'의 소스 로컬 feature switch와 실제 기기/권한/펌웨어 지원은 별개임. 루트/Xposed 관련 패킷, 제조사 흉내 기능은 별도 권한 필수, 현재 무루트 환경에 임의 적용하지 않는다.

## 안전한 후속 개선안 (진행 전 사용자 합의)
P0. GOLDEN 동결: r21/APK SHA/인증서 기준으로 앱 정상/ANC 회귀 테스트 1회, 새 후보는 별도 Git branch/Release. 현재 설치 앱/AA 복구 데몬 변경 금지.
P1. 배터리 표기: 항상 좌/우/케이스 개별 슬롯, 미수신은 '—'로 표시 (0%와 혼동 금지), 충전 상태/최근 갱신 시각, 연결 전환/한쪽 착용/케이스 열림 상태 로깅. 실제 제공되는 값의 정밀도를 넘어서 1% 값을 가짜로 생성하지 말 것.
P2. 한국어/개인화: UI 모든 영어/불필요한 개발자/후원/로그 메뉴 숨김 또는 사용자 선택, AirPods5 실물에 맞는 정확한 모델 그림 (현재 A5 소스가 AirPods Pro 2 drawable을 재사용), 경량 모드.
P3. 알림/위젯/타일: 사용자 설정형 세 요소, 작은/중간 크기, 청취 모드 버튼, 즉시 반응/실제 상태 확인, 필요한 때만 갱신, 접근성 라벨 개선.
P4. 온디바이스 진단: 진단 팝업 기본 숨김, 옵션에 '로그 내보내기'; 로그 상단 정적 '1.0.9'를 BuildConfig.VERSION_NAME으로 교체; 개인정보 자동 업로드 금지.
P5. 영구 업데이트: 공개 저장소에 키 유출 없이 안정된 서명키 관리 후 **이전 r21과의 인증서 불일치를 정직하게 처리** (기존 r21에 대해 무손실 덮어쓰기 불가하면 별도 1회 이관 경로 또는 기존 r21 유지).
P6. 모델/권한별 기능 가시성: 미지원 항목 숨김, 연결 실패 시 문제 이유, 실기 검증 전 유료/프리미엄 전 기능 PASS 표기 금지.
P7. 배터리/전력 최적화: UI repaint, scanning, receiver/observer 중복 확인. 실제 배터리 소모 측정 전 '저전력' 검증 주장 금지.

## 하드 금지
- '광고/후원/이메일 제거'를 이유로 Bluetooth/AACP/ANC 프로토콜 재작성 금지.
- Android Auto 출력 라우팅/AA Golden 프로젝트 기능을 이 앱 기능이라고 설명 금지.
- 긴급 장애 없는 경우 user-verified GOLDEN APK 덮어쓰기/다시 빌드 금지.
