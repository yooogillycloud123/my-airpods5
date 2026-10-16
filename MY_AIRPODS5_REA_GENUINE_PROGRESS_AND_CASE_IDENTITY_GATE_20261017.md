# AirPods 5 무선 충전 케이스 — REA 실제 적용 상황 및 케이스 모델/소유자 인증 차단점 (2026-10-17)

## 요구 기능 (바꾸지 않음)
Galaxy S26 Ultra Android, Apple 기기/Apple Find My 등록 없이, AirPods 5 무선 충전 케이스 양쪽 이어버드 수납·닫힘·A2DP 연결 해제 상태에서 휴대폰 버튼으로 충전 케이스 자체 외장 스피커의 찾기 삐삐 소리를 재생/중지한다. 무선 충전기가 연결된 상태에서만 충전 시작 딩 소리 = 목표 불일치.

## 오늘 발견한 매우 중요한 출처 불일치
과거 프로젝트는 **"A3530 = AirPods 5 무선 충전 케이스"**라고 거의 확정 사실처럼 수십 차례 기술했지만, **사용자가 실제 케이스 하우징의 모델 번호를 제공했다는 자료가 없다**.
Apple 자체 공개 자료들이 현재 검색 가능한 버전에서 서로 다르게 표기한다.

| Apple 1차 출처 | 무선 충전 케이스 표기 | 일반 유선 충전 케이스 |
|---|---|---|
| 한국 Apple 제품 사양 https://www.apple.com/kr/airpods-5/specs/ | **A3529** (AirPods 5 with Wireless Charging Case SKU) | **A3530** (AirPods 5 SKU) |
| 한국 Apple 지원 https://support.apple.com/ko-kr/109525 | **A3530** (일부 검색 색인/로캘 응답) | **A3529** |
| English Apple support https://support.apple.com/en-kg/109525 | **A3529** | **A3530** |

**검색 색인/로캘별 최신성 차이나 원문 오류 가능성을 배제할 수 없다.** 어느 쪽이 사실인지 지금 임의로 정하지 말고 **사용자 실제 하드웨어 케이스가 A3529인지 A3530인지 확인 전 A3530 하드코딩을 인증 proof로 쓰지 않는다.** 모델 번호는 BLE 광고에서 관찰한 0x3020(이어버드 주변광고 모델)과 전혀 다른 차원이다. **실물 케이스 표기**도 Find My BLE '소유자 인증'을 의미하지 않는다.

## REA에서 실제 확인한 범위
- 공식 REA/npm 6.5.0 + SHA-256 검증된 JADX 0.7.1 headless/Java21 GitHub Actions.
- 기존 개인 프로젝트 v139 APK 해시고정 디컴파일 및 클래스 검사, 개인 원본 앱 무변경.
- AirGuard 공식 GitHub APK 3.1.3 (SHA-256 `6fd3e5d1c3c1255f44ac29ed27e50fba760180dcdc4f452d7560c1e28ca12a93`)의 AppleFindMy 사운드 메서드 세 개 **실제 완전 디컴파일**. Action `38920099271`, main 반복 성공 `38921296160`.
- AirGuard BLE mfg `12 19 18` / mask `FF 00 18`: 12:02도 세 번째 상태 바이트가 일치하면 일치할 수 있음. 사용자 v139은 12:02 13회와 임시주소 5개만 확인, 그 세 번째 byte 미수집이므로 실제 AirGuard 필터 매칭 판단 불가.
- AirGuard `fd44/4F860003...` 공개 분리추적기용 비소유자 시작/중지 경로는 구현됨. **이를 인증된 본인 무선 충전 케이스의 스피커 제어로 착각하면 안 됨**. `LibrePods #102`에서는 케이스 자체 GATT 서버에 owner 인증 필요하다고 밝힘.

## 이번 실제 GitHub 변경 (실행 프로그램)
- `tools/rea_airpods/case_target_identity.py`: 3개 Apple 1차 출처의 모순된 모델 분류를 **분리된 source claim**으로 보존하고, 검증되지 않은 사용자 하드웨어 모델을 `UNKNOWN`으로 유지.
- `tools/rea_airpods/test_case_target_identity.py`: 8개 회귀 검증으로 0x3020 이어버드 근접광고, 단순 실물 모델, 가상의 성공 Boolean, FD44 발견만으로 케이스 스피커 제어 READY 통과 못하게 차단.
- `.github/workflows/rea-findmy-real-protocol-compare.yml`: REA 실제 APK 분석 및 공개 AirGuard 프로토콜 검사에 **동일 identity verifier 수행/JSON 아티팩트**를 결합.
- 사설 BLE 키, MAC, 앱 데이터, 원본 APK 배포, GATT write, 앱 삭제/재페어링은 실행하지 않음.

## 현재 개발 단계
- **0/5** 기존 앱 정상 기능 및 AA Golden 보호: PASS (변경 없음).
- **1/5** REA/JADX 실제 공개 APK 사운드 경로 분석: PASS (AirGuard 공개 비소유자 GATT).
- **2/5** 정확한 본인 **무선 충전 케이스 모델 확인**: UNKNOWN / Apple 출처 간 표기 불일치 확인, 이전 A3530 단정 철회.
- **3/5** 이 충전 케이스와 갤럭시의 **소유자 GATT 세션 결속 및 인증**: NOT VERIFIED.
- **4/5** 해당 인증 경로의 케이스 전용 사운드 시작/중지 실제 패킷: NOT VERIFIED.
- **5/5** 닫힌 케이스 양쪽 수납 상태 사용자 실기 audible 삐삐: NOT VERIFIED.

## 다음 작업을 허용하는 증거
실제 케이스에만 귀속되는 주소/키/암호화 및 authenticated GATT path가 필요하다. 공개 문서·권한 있는 실제 제어 구현·민감정보 제거된 케이스 세션 증거에서 **검증 가능**해야 하며 단순 Apple 제조사 패킷과 일반 FD44 비소유자 characteristic은 불충분하다. 이 증거를 얻으면 현재 v139 패키지에서 **증거 기반 단일 업데이트판**으로 통합하고 기존 메인 앱/AA 코어 기능은 그대로 둔다. 공식 REA 설치만으로 iPhone owner provisioning 대체, Apple 서버 암호화를 자동 우회 또는 케이스 소리 성공을 주장하지 않는다.

## 실제 결과·검증
- 진행 브랜치: `fix/case-model-proof-rea-20261017`, GitHub Actions `REA actual Find My protocol APK comparison` 워크플로 실행 및 산출물 참조.
- 앱/블루투스 실험 반복·재설치: **0회**.
- 새 케이스 스피커 APK: **없음** (owner auth 원천 차단점 미해결).
