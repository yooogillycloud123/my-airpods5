# AirPods 5 v1.0.20 — 실제 소스 수정·서명 APK 릴리스·기기 실험 1회용 체크포인트 (2026-10-15)

## 제품 목표 / 증거 경계
- Galaxy S26 Ultra, 사용자 AirPods 5 무선 충전 케이스, 양쪽 이어버드 보관, 뚜껑 닫힘, 미디어 A2DP 미연결, **케이스 스피커에서 실제 Find My 소리**. 목표 변경 없음.
- **v1.0.20은 케이스 찾기 소리 기능 완성판이 아니다.** 이어버드 AACP 소유 IRK로 BLE RPA의 연결 가능한 주소를 확인하고, 사용자 동의 뒤 읽기 전용 GATT UUID만 조사하는 첫 독립 실험판.
- 개인 정상 GOLDEN v1.0.10 `com.myairpods5.personal` 및 AA Golden/데몬은 전혀 수정하지 않음. 이번에는 사용자에게 새 설치본을 요청했으므로 실제로 설치할 APK를 제공한다.

## 이미 진행·검증한 소스
- 브랜치 `airpods-personal-v111-updater-diagnostics`
- `ci/case_protocol_v117/CaseRpaIdentity.kt`: LibrePods 소스와 독립 AES128 vector로 검증한 Bluetooth RPA resolver; earbud IRK vs verified case key를 구분하고 RPA 일치를 case-owner auth와 절대로 동등시하지 않는다.
- `ci/case_protocol_v120/CaseOwnerProbeV120.kt`: LAB 앱 설정의 `내 케이스 제어 경로 확인 · 연구` 버튼. BLE/위치 권한 확인 → LAB 개인정보 저장소에 AACP IRK가 실제 있을 때만 12초 BLE 주소 비교 → **연결 가능한 IRK 일치 대상**이 있을 때만 사용자 별도 확인 후 READ-ONLY GATT 서비스 검색(max 10초) → Apple Find My FD44/4F860002(제3자 owner 제어)/4F860003(비소유자 제어) 존재 여부와 전체 서비스 count/UUID를 집계. 다른 기기 GATT 임의 접속·write/암호화 권한 상승/소리 opcode 전송 없음.
- `ci/case_protocol_v120/patch_lab_preflight.py`는 해당 실험실 메뉴만 교체하고 `com.myairpods5.personal.lab` versionCode=10020 패치. 기존 AACP, ANC, Golden 코어 SHA 체크 PASS. 코드는 Golden APK를 수정하지 않음.
- 빌드 CI `.github/workflows/build-case-gatt-preflight-v120.yml`, 실제 GitHub Actions **38707003529, SUCCESS**; 로그 `BUILD SUCCESSFUL in 3m48s`; signed ZIP/package/Dex and engine freeze static gates all pass.

## 결과물 (다운로드)
- GitHub release `my-airpods5-v120-lab-r2`.
- APK `My_AirPods5_v1.0.20_OWNER_CASE_GATT_PREFLIGHT_LAB.apk`
- URL https://github.com/yooogillycloud123/my-airpods5/releases/download/my-airpods5-v120-lab-r2/My_AirPods5_v1.0.20_OWNER_CASE_GATT_PREFLIGHT_LAB.apk
- SHA256 `3f6702b23179561648f30f4c189573f163992cc60af89ce4ff635059ff60d134`
- Signer SHA256 `44f595fac773815ca53b8efdd93c8763cf13bd9848c24803bfd52d766b420395`
- Bytes 51,392,317, package `com.myairpods5.personal.lab`, version `1.0.20-owner-gatt-preflight-debug`, code `10020`.

## 설치·실험 (사용자 시간 절약)
1. **정상 개인 v1.0.10 GOLDEN은 삭제 금지**. 다른 서명으로 갱신이 안 되면 기존 `내 에어팟 5 실험실` (package `.lab`) 앱만 제거 후 설치. 이 경우 LAB 저장 키/세팅은 초기화되지만 Golden 앱은 영향 없음.
2. 실험실 앱이 이어버드와 AACP를 한번이라도 연결하여 설정 저장소에 `IRK`를 받은 상태여야 이 실험이 의미 있음. 부족하면 앱에 `내 AirPods IRK 미수신` 안내가 나오며 타기기 스캔/연결하지 않음. 키는 개인별 비밀로 절대 전송/공개 로그에 포함하지 않음.
3. 실험실 앱 `설정→내 케이스 제어 경로 확인 · 연구`. Galaxy Bluetooth scan/connect + **정확한 위치** 권한을 수동 승인할 경우 12초 BLE 체크. 후보 발견 시 별도 `GATT 읽기 조사` 동의를 받고 UUID만 조회. 진단 복사 결과의 UUID나 MAC이 개인식별/민감하게 느껴질 경우 앱에 공유하지 않아도 됨; MAC과 키는 진단에 포함되지 않음.
4. 최종 보고는 아래 중 한 번의 단계만 요청: `IRK 미수신` / `IRK 일치 0` / `연결 가능한 IRK 일치` / `GATT 서비스 UUID 발견`; 계속 35초 거리 스캔은 하지 않음.
5. 실제 케이스 찾기 사운드 **미구현/미검증**. IRK 일치나 GATT UUID 존재를 찾기 사운드 성공이라 주장 금지. 앞으로 필요: 실제 AirPods5 **케이스 자체** owner auth + "Find My Play Case Sound" opcode verified source; 다른 모델 제3자 기기 `4F860002` 값을 사용자 케이스에 보내지 않음.

## 선행 외부 출처
- https://github.com/librepods-org/librepods/issues/20
- https://github.com/librepods-org/librepods/issues/102
- https://github.com/librepods-org/librepods/blob/main/android/app/src/main/java/me/kavishdevar/librepods/utils/BluetoothCryptography.kt
- https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AppleFindMy.kt
- https://github.com/parawanderer/OpenTagViewer/blob/main/docs/owner-connected-tags.md
