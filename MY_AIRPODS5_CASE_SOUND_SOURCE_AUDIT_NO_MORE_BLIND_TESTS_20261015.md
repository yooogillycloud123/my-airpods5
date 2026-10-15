# AirPods 5 케이스 '나의 찾기 소리' — 소스 검증 결과와 구현 게이트 (2026-10-15)

> 판정: **소리 출력 미구현 / AirPods 5 전용 인증 제어 명령 검증 전 / 기존 GOLDEN 보호**.
> 대상: Galaxy S26 Ultra + 사용자 AirPods 5 무선 충전 케이스. 구매 이후 Galaxy에서만 사용했으며 Apple 계정 Find My에 등록한 이력 없음.
> 최종 수락 조건: 양쪽 이어버드 케이스 안, 뚜껑 닫힘, 음악용 BT 연결 없음, 갤럭시 앱 버튼으로 **케이스 자체 스피커에서 실제 찾기 소리**가 울려야 함.
> 이번 단계에서는 사용자에게 추가 35초 BLE/RSSI 실험이나 설치를 요청하지 않는다. 기존 실측 원문은 버리지 않는다.

## 1. 사용자 측정은 폐기물이 아니라 '현재 경로 검증에 실패했다'는 증거

| 실험 | 관측치 | 기술적 판정 |
|---|---|---|
| v112 | Classic 이어버드 주소로 GATT 시도, status 147 | `BluetoothGatt.GATT_CONNECTION_TIMEOUT`; 실제 케이스 타깃 미확인 |
| v113~114 | BLE 2570/2424건, Apple 93/138건, 형식 `07 11 06` 0건 | 핸드셋 BLE 수신 성공. 특정 연구의 케이스 배터리 광고형식 미검출 |
| v115 가까이/멀리 | Apple 12형 48→14, 길이 4:31→8 | 감소는 관찰됐으나 사례 1회로 케이스 신원 증거 없음 |
| v116 가까이/멀리 | Apple 12:02 길이4 **4→46**, 중앙 RSSI **-95→-94 dBm** | 이전 감소 가설 재현 실패. 잡힌 `12:02`를 케이스로 단정 금지 |
| v116 | Apple `02:15` 길이23 광고 720→795건 | Apple company ID 뒤 `02 15`는 일반 iBeacon 형식. Apple 자체 기기·케이스 인증 불가 |
| 모든 테스트 | Find My `FD44` 광고 0, AirGuard AirPods mask 0 | 해당 광고 항목 미검출. 내부 서비스 부재라고 확정할 수는 없음 |
| 모든 테스트 | 실제 스피커 소리 0회 / 소리 명령 미송신 | **음향 제어 기능 구현 PASS 아님** |

## 2. 실제 동작/개발 중인 소스 교차검증 (원문 SHA 고정)

### A. Apple 원천 지원과 구별
- Apple 공식 AirPods 설명: AirPods 5 무선 충전 케이스는 Find My 앱을 통한 **케이스 소리 재생 지원**.
- Apple 계정에 소유자 등록되지 않은 Galaxy-only 제품에서 앱이 동일한 인증 경로를 사용한다는 증거 없음.
- iCloud.com 웹으로 케이스 스피커 호출은 Apple 공식 문서상 지원하지 않음.
- https://support.apple.com/en-by/guide/airpods/dev8e8b93d71/web

### B. LibrePods: 앱 엔진 성공, 케이스 분실 소리는 **미구현**
- `librepods-org/librepods`의 `README.md`, blob SHA `18b862a341dceec65ba4bd41d06777827dde4b42`는 `Play sound through charging case to find it`을 추가 RE 필요 항목으로 구분.
- `docs/AAP Definitions.md`, blob SHA `87b23d9053c85bd0d3b8dbc5bb468f4d29c200fe`: case speaker 관련 문서화된 명령은 **충전 효과음 활성/비활성 토글**, 프로토콜 `12 3A 00 01 00 08 [setting]`. '잃어버린 케이스에 삐삐 명령' 아님.
- `AACPManager.kt`, SHA `ac6d356b7fb5b2e320b70524218f5fd0bd8d8c84`: `IN_CASE_TONE_CONFIG (0x31)`. **설정 명령 ≠ 실제 Find My 소리 시작**.
- PR #764 (미병합) `Charging Case Sounds Toggle` 역시 충전 효과음 설정이며 찾기 소리 기능의 실증이 아님.
- Issue #718은 'AACP play tone이면 된다'는 사용자 제안이었으나 **closed as not planned**, 검증 완료 명령·결과가 첨부되지 않음. 이것을 기능 성공 사례로 세지 않는다.
- https://github.com/librepods-org/librepods/blob/main/README.md
- https://github.com/librepods-org/librepods/blob/main/docs/AAP%20Definitions.md
- https://github.com/librepods-org/librepods/pull/764
- https://github.com/librepods-org/librepods/issues/718

### C. AirGuard: 일부 Apple 추적기 소리 재생을 **실제로 구현**, 조건이 다름
- `seemoo-lab/AirGuard` `AppleFindMy.kt` SHA `43952c579fe6325ec34bc5baf98d9814ecb4b305`: GATT `FD44` Find My/비소유자 제어 특성 `4F860003-...`, 시작 `01 00 03`, DULT 다른 경로를 포함. 
- `AirPods.kt` SHA `425066c9fe74b1587a6f551e340ea347053124c7`: 광고 필터 `12 19 18` mask `FF 00 18`, 우선순위 FindMy→DULT→AirTag.
- **의미**: 추적기 분리/원치 않는 추적 상태의 BLE '비소유자' 소리 구현 예시. **네 Apple 계정 미등록 케이스를 평상시 닫힌 상태에서 울린 성공 사례는 아님**.
- AirGuard 공식 제한 문서: Sound action은 기기 동작 모드에 따라 이용 불가능할 수 있음.
- https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AppleFindMy.kt
- https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AirPods.kt
- https://airguard.seemoo.de/en/docs/limits/

### D. OpenTagViewer: 소유자/비소유자 통신이 **분리된 실물 연구**
- `parawanderer/OpenTagViewer/docs/owner-connected-tags.md` blob SHA `1ba4dc4d00b2d540a0a2ecf8fafb9849ee1c1e4e`.
- 실제 소유자 명령 성공 사례는 **RAVEMEN ABF01 등 제3자 Find My 액세서리**, 암호화된 소유자 BLE 링크 `4F860002`와 소유자 LTK로 제어함.
- 동일 바이트를 **암호화 없이 Android로 송신할 때 GATT ACK가 떠도 소리가 나지 않는 실제 사례**가 기록됨.
- `4F860003` 비소유자 명령은 owner-nearby 상태에서 특성 자체가 없다는 AirTag 실측도 기록됨.
- **RAVEMEN 제3자 기기의 UUID·opcode가 AirPods5 케이스에서도 작동한다는 근거 없음.**
- `ENC_KEY`(근접 광고 복호화용)와 BLE 소유자 본딩 LTK는 서로 다른 자격증명. 둘을 혼용하지 않음.
- https://github.com/parawanderer/OpenTagViewer/blob/main/docs/owner-connected-tags.md
- https://github.com/parawanderer/OpenTagViewer/issues/193

### E. 기타 앱: 기능 소개 ≠ 재현된 케이스 스피커 성공
- CAPod는 AirPods 5 무선 충전 케이스 배터리/상태 감지를 지원하지만, 원본 README에 분실 케이스 스피커 Find My 명령 성공 기록은 없음.
- Google Play AFind은 케이스 소리 재생을 광고하지만, 2026-08-18 후기에는 AirPods Pro 2 USB-C의 케이스가 인식되지 않아 케이스에서 소리가 안 나고 이어버드를 꺼내야만 소리가 났다고 명시. 마케팅 문구만으로 Android AirPods5 실증 성공을 가정하지 않는다.
- https://github.com/d4rken-org/capod
- https://play.google.com/store/apps/details?id=com.dreamteammobile.find.my.airpods

## 3. 실제 병목 구분 및 실행 경로 (무효인 테스트 반복 금지)

1. **사용자 케이스 자체 신원:** BLE 회사 ID·Find My 0x12·RSSI만으로는 고유 소유자 신원 아님. 케이스 분리된 LE 주소는 이어버드 Classic MAC과 같다고 가정 불가.
2. **케이스의 제어 채널:** AirPods 5 무선 충전 케이스에서 소리 재생을 실제로 받는 Transport/서비스/Opcode가 소스·캡처로 검증되어야 함. 다른 제3자 Find My 기기의 FD44 서비스 적용은 금지.
3. **소유자 승인/암호화:** 케이스가 소유자 연결을 요구하면 해당 본딩·세션 성립 방법부터 확인해야 함. 사용자는 Apple 기기로 페어링한 이력이 없으므로 Apple Find My 소유자 등록이 돼 있다고 전제하면 안 됨.
4. **실제 출력:** 원격 명령 write 성공/ACK/특성 발견은 재생 성공이 아님. 사용자가 듣는 케이스 스피커 소리가 최종 통과 조건.

**No-Go:** 추가 BLE 35초 반복, 0x07 검색, 거리계/RSSI 재시험, FD44 추정 명령 주입, AACP 0x31을 '찾기 소리'로 포장, Golden 재서명, 회사에서 재설치 요구.

**Go when source evidence exists:** AirPods5 또는 같은 케이스 칩셋/펌웨어의 재현된 `closed case → authenticated command → audible case beep` 샘플/패킷 추적이 발견되면 그 경로만 안전한 LAB 브랜치에 이식하고 CI 검증. 그전에는 미검증 APK를 빌드해 사용자에게 설치하게 하지 않음. 해당 코드는 제3자 BLE 장치에 임의 연결하지 않는 소유자 기기 검증을 가져야 함.

## 4. 고정 변경 보호

- 사용자 검증 `com.myairpods5.personal` **v1.0.10 GOLDEN** 4모드/배터리 정상 그대로 유지.
- 이 조사 파일과 후속 검증은 기존 실행 앱, Android Auto 앱, shell daemon, 블루투스 페어링, 사용자 Apple 계정에 어떤 변경도 주지 않음.
- 실측 로그를 지우지 않는다. 이전 추정이 반박된 이유와 발견된 Source Boundaries를 영구 저장한다.
- **이번 결과를 '사용자 제품에서 불가능함'이라고 단정하지 않음. 다만 현재 공개된 검증 코드만으로 성공한 APK라고 말할 수 없음.**
