# 실물 AirPods 5 무선 충전 케이스 — Apple 기기 보유 시 확인/인증/키 분석 계획
2026-10-17. 사용자 새 조건: **배우자의 Apple 기기를 동의받아 사용 가능할 수 있음**. 종전 "Apple 기기가 절대 없음" 전제를 폐기. 실제 기기 모델/OS 버전/계정 소유자 권한과 Find My 성공은 아직 확인되지 않음. Apple Device를 빌려 소리가 난다고 Android 단독 Find My 암호화 키가 추출된 것은 아님.

## 0. Apple 공식 소스/확정 사실
1. `https://support.apple.com/ko-kr/104989` — AirPods 5를 iPhone에 정상 페어링하려면 **iOS 27 이상**. 우선 아내의 iPhone 모델과 설정→일반→정보 iOS 버전을 확인. 해당 OS 미지원이면 이 테스트 경로가 막힐 수 있음.
2. `https://support.apple.com/ko-kr/102648` — AirPods을 본인 Apple 계정의 나의 찾기에 넣으려면 Find My 활성화된 iPhone/iPad에 페어링.
3. `https://support.apple.com/ko-kr/guide/airpods/dev8e8b93d71/27/web/27` — AirPods5 무선 케이스 자체 재생 가능; **iCloud.com에서는 이 케이스 자체의 소리를 재생할 수 없음**. iPhone/iPad Find My 앱으로 확인.
4. `https://support.apple.com/ko-kr/102620` — Find My 액세서리는 **동시에 하나의 Apple 계정**과 결합. 아내 Apple 계정에 등록하면 Find My 소유자 주체는 아내 계정. 이미 다른 계정에 연결되어 있다면 기존 계정 제거는 **명시적인 소유자 동의** 후. 테스트 이유로 강제 리셋, 배우자의 iCloud 로그아웃/비밀번호 요청/계정 교체 금지.
5. `https://support.apple.com/ko-kr/guide/security/sec82597d97e/web` — 일반 Bluetooth 본딩/링크 키 저장, BLE IRK와 암호화, 인증은 분리. `https://support.apple.com/en-gb/guide/security/sec6cbc80fd0/web` — Find My 위치 암호화용 회전 비밀/개인키는 일반 BLE LTK와 별개.
6. `https://developer.apple.com/bluetooth/` — 공식 Bluetooth PacketLogger는 Additional Tools for Xcode에 포함. **Mac/Xcode 도구가 있어도 Apple 계정 키 또는 BLE LTK가 평문으로 추출된다는 보장은 없음**. 암호화/보안 권한에 의해 접근 제한될 수 있음.

## 1. 중단점이 분명한 1회 실물 검증 (부부 양측 명시적 동의)
- [A] 아내 Apple 단말 정확한 모델, OS iOS 버전, 현재 Find My 계정(본인 계정/아내 계정 중 누가 최초 등록할지) 확인. 비밀번호/Apple ID/기기 시리얼을 개발자나 GitHub에 보내지 않음.
- [B] Apple 계정 변경 없이 **소유자가 동의할 때만** AirPods 5 A3529 제품을 Apple 기기와 정상 페어링하고 Find My 네트워크를 활성화. Android Galaxy의 페어링/기존 앱/AA Golden을 먼저 지우거나 초기화하지 않음. 사용자 구매 상자 A3529 제품 라벨은 BLE 개별 기기 암호학적 인증이 아님. Apple 지원 문서 A3529/A3530 모델 명칭 충돌이 있으므로 실제 기기의 Find My 표시로 무선 케이스 호환성을 확인.
- [C] 양쪽 이어버드를 **케이스에 수납**, 뚜껑 **닫힘**, 음악 **A2DP 미연결**에서 iPhone **나의 찾기 → 기기 → 등록된 AirPods → 케이스 → 사운드 재생**을 사용자 직접 실행. '진짜 케이스 하단 스피커'에서 소리가 나는지 실물로 확인. 녹음/스크린샷에는 계정/기기 ID/실제 지도 위치/전화번호를 공개하지 않음.
- [D] 결과 PASS면 **실제 제어 경로가 살아 있음을 입증**. 우선 iPhone Bluetooth를 켠 근접 상태와 Bluetooth OFF(또는 범위 밖) 상태에서 오프라인/대기 메시지가 어떻게 달라지는지 **부담 없는 UI 관찰**로 로컬 BLE 지령인지 서비스 비동기 경로인지 분리할 수 있음; 무리한 반복 테스트 금지.
- [E] 원본 iOS 제어 트레이스가 추가로 꼭 필요하고 **Mac과 해당 도구가 확보된 경우**에만 Apple PacketLogger/지원되는 블루투스 진단으로 합법적 **자기 기기/동의받은 기기**에 대한 링크·GATT 흐름을 기록. BLE opcode, 접근에 필요한 로그인·본딩 키/토큰은 보안 데이터이며 업로드 전 로컬 분석/식별자 제거. 스누핑된 암호문만으로 LTK/Find My 개인키 유도되지 않음.

## 2. 이식에 필요한 실제 '키' 구분
- `BLE IRK`: 개인 주소 해결용 키, 신원 매칭에 유용하지만 사운드 명령 인증/권한을 부여하지 않음.
- `BLE LTK` / Bluetooth link key: iOS와 AirPods 간 생성되어 OS 본딩 보안 저장소에 보관되는 링크 암호화 키. 사용자 Apple 기기가 있어도 공개 API로 평문 추출 가능하다고 **가정 금지**. 또한 이어버드 링크 키 = 케이스 Find My 세션 키라고 가정 금지.
- `Find My 위치 암호화 개인/회전 키`: 오프라인 위치 보고 암호화·조회에 쓰이며 곧바로 GATT 스피커 제어 키가 아님.
- `액세서리 케이스 소유자 제어 세션/인증 토큰`: 케이스 자체 재생/중지 제어에 필요할 수 있는 자료. A3529 실제 요구 방식·키 파생·상태기계 **미확인**. iOS FindMy 앱에 상수 형태로 저장된 만능 키를 REA로 복사할 수 있다고 생각하면 안 됨.

## 3. 실현 가능한 종료 조건
- **Apple Find My 앱 CASE audible PASS, Android ring FAIL:** 실물 기기 문제를 제외하고 BLE owner-auth 및 Android 인증 수단을 추적한다. 이는 Apple 계정에 등록한 사람이 iPhone에서 **직접 사용할 수 있는 기능**만 우선 확인했음을 뜻함.
- **Apple Find My CASE audible FAIL:** 계정 등록/모델/iOS/거리·배터리/케이스 사운드 옵션부터 확인. Android APK를 새로 만들 시점이 아님.
- **Apple Find My CASE audible PASS 및 Apple owner's legitimate GATT session keys/protocol obtainable PASS:** 독립 Galaxy BLE 원격 재생을 처음 구현할 타당성 확보.
- **Apple CASE PASS지만 키가 iOS 보호영역에 묶임:** 단독 Android 기기에서 동일 owner identity를 원본 키 없이 재현한다는 목표는 계속 차단. iPhone을 경유하는 대체방식도 Find My 소리 기능이 iOS 공개 API/Shortcuts에서 지원되는지 확인 전 자동화 가능하다고 약속할 수 없음.
- 지금까지 v140 FD44 0/0 ACK=실물 무음. v141, v1.0.42 RF A/B 사진의 일부 12:02 주소 사라짐은 **개별 케이스 소유자 신원 증거 아님**. 반복 anonymous FD44 송신 금지.

**사용자에게 지금 필요한 한 가지 정보**: 아내 iPhone 모델 및 iOS 버전 (Apple Find My 소리 실기 시험 가능 여부 결정). 계정 전환/초기화/새 APK 없이 시작. 모든 테스트 및 분석은 사용자가 소유하거나 명시적으로 사용 허가받은 장치에서만.
