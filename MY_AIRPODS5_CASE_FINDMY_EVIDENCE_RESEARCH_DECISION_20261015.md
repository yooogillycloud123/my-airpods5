# 내 에어팟 5 — 케이스 스피커 찾기: 외부 근거 기반 설계 검토 v1.0 (2026-10-15)

> **구현 성공 선언이 아닌 기술적 의사결정 기록.** 본 문서는 2026-10-15 실제 사용자 로그 + 소스 + Apple/AirGuard/LibrePods/Find My 연구자료를 교차 확인해 작성했다.
> 사용자 요구: Galaxy S26 Ultra / AirPods 5 무선 충전 케이스 / 양쪽 이어버드 보관 / 뚜껑 닫힘 / A2DP 미연결 / 케이스 스피커에서 실제 찾기 소리. **이것보다 낮은 조건으로 성공을 선언하지 않는다.**

## 1. 현재 실제 데이터

| 실기 결과 | 닫힘 | 열림 | 단정할 수 있는 것 |
|---|---:|---:|---|
| Android BLE 스캔 콜백 | 2570 | 2424 | 갤럭시 일반 BLE 스캔 작동 |
| Apple Company ID 0x004C | 93 | 138 | Apple 제조사 광고는 실제 수신 |
| 제조사 데이터 첫 바이트 0x07 | 0 | 0 | 현재 주변 스캔에서 특정 proximity 형식 미관찰 |
| 구형 case 07 11 06 | 0 | 0 | 그 형식의 케이스 신호 미관찰 |
| 개인용 키 구조 일치 | 0 | 0 | **복호화 대상 없음 — 키 오류라고 할 수 없음** |

이 표에서 Apple 광고 93→138 증가만으로 사용자 케이스의 존재/광고 전환을 증명할 수 없다. 원시 MAC, 암호화키, BLE payload 공개 저장 금지. v1.0.10 GOLDEN 자체 수정·제거 금지.

## 2. 외부 근거 확인: 예전 0x07 전용 접근은 '케이스 찾기 소리' 구현 경로로 불충분

**Apple 공식 지원**
- 무선 충전 케이스 AirPods 5에는 '나의 찾기'용 외부 스피커가 있다.
- iPhone/iPad/Mac/Apple Watch의 Find My에서 케이스 자체의 사운드 재생 가능.
- Apple 공식 지원은 **iCloud.com 웹 버전에서는 AirPods 5 충전 케이스의 사운드를 재생할 수 없다고 명시**한다. 단순히 갤럭시 웹뷰에서 iCloud 로그인 후 사운드를 누르는 방안은 목표 해결책이 아니다.
- Apple Find My 네트워크 사용에는 **Apple 계정으로 로그인한 Apple 기기에 AirPods를 페어링하고 나의 찾기를 활성화**하는 선행 조건이 있다. Android와의 일반 블루투스 페어링만으로 소유자용 Find My 등록이 되는 것은 아니다.
  - https://support.apple.com/ko-kr/guide/airpods/dev8e8b93d71/27/web/27
  - https://www.apple.com/kr/airpods-5/specs/

**공개된 Case BLE 배터리 분석**
- github.com/stuffz/airpods 의 `docs/ble.md`와 `docs/ble-research.md`는 특정 장치/펌웨어에서 0x07 proximity broadcaster의 구형 `07 11 06` encrypted case format을 관찰했다.
- 이것은 케이스 **배터리/뚜껑 상태 확인**에 관한 수동 advertisement 형식이다. '나의 찾기 케이스 스피커'를 울리는 인증/명령 규격이 아니다.
- 다른 AirPods 5 모델/펌웨어의 광고 형식 일치나 Galaxy Android 17에서 무조건 관찰된다는 사실은 없음.
  - https://github.com/stuffz/airpods/blob/main/docs/ble.md
  - https://github.com/stuffz/airpods/blob/main/docs/ble-research.md

**AirGuard 실물 오픈소스에서 새로 확인한 매우 중요한 별도 경로**
- seemoo-lab/AirGuard `app/.../device/types/AirPods.kt`의 Find My/원치 않는 추적자 탐지 필터는 **제조사 데이터 `12 19 18`** 마스크를 사용한다. 우리가 검사한 0x07 형식과 아예 별개의 경로.
- `AppleFindMy.kt`는 AirPods 우선순위를 **FINDMY → DULT → AIRTAG**로 선언하고, 서비스 `0xFD44` / `4F860003` 비소유자 소리 경로 등 실제 구현을 포함한다.
- AirGuard 공식 지원표는 AirPods의 `Play Sound`를 지원하되 **특정 추적/분리 상태일 때만 가능**함을 분명히 한다.
  - https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AirPods.kt
  - https://github.com/seemoo-lab/AirGuard/blob/main/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/AppleFindMy.kt
  - https://airguard.seemoo.de/en/docs/limits/

**DULT/IETF 공개 문서 — 잘 구분해야 함**
- 비소유자/원치 않는 추적 기기의 소리 출력은 장치가 `separated`이면서 GATT에 연결 가능한 조건의 별도 표준.
- `DULT Sound_Start 0x0300` 등은 **분리된 추적자 탐지용 비소유자 제어**이지, 소유자가 곁에 있는 본인 AirPods 케이스에서 언제나 재생하는 명령이 아니다.
  - https://datatracker.ietf.org/doc/html/draft-ledvina-dult-accessory-protocol

**실제 소유자 명령/암호화 장애**
- parawanderer/OpenTagViewer의 2026년 소유자 BLE 분석: `4F860003` 비소유자 경로는 소유자 가까이에서 없거나 사용 불가. 제3자 Find My 액세서리에서 `4F860002` 소유자 제어 경로와 LE link encryption(LTK)이 HCI 로그로 확인됐다.
- 소유자 암호화 없이 Android에서 바이트 그대로 써도 **GATT ACK만 되고 소리가 나지 않은 사례**가 기록됐다.
- 이 경로/UUID/키가 AirPods 5 케이스에도 동일하다는 증거는 없다.
- 앱이 가진 `ENC_KEY`는 proximity 광고 payload AES 복호화용 키이며 **BLE owner pairing/bonding LTK와 다르다**. 바꿔 쓸 수 없다.
- 공개 Android SDK에 임의 외부 LTK로 기존 링크를 암호화하는 API가 없어서 **루트 없는 임의 BLE 원격 소유자 사운드에 실질적인 장애**가 존재한다.
  - https://qgithub.com/parawanderer/OpenTagViewer/blob/main/docs/owner-connected-tags.md
  - https://github.com/parawanderer/OpenTagViewer/issues/193
  - https://github.com/parawanderer/OpenTagViewer/issues/201

**LibrePods 원래 지원 상태**
- `Play sound through charging case to find it`은 원본 README의 **planned / further reverse engineering required / might need root** 항목이다. 당장 실행 가능한 기능을 기존 AACP에 추가하는 작업이라고 오해하지 않는다.
  - https://github.com/librepods-org/librepods#find-my

## 3. 비교·선택

| 대안 | 근거 | 사용자 목표 대비 | 결정 |
|---|---|---|---|
| 기존 0x07 암호화 케이스 광고 연결 | 구형 모델 연구의 **배터리** 자료 | 소리 재생과 다른 프로토콜; 실기 0건 | 주요 제어 경로에서 제외, 수동 보조 진단용만 |
| **AirGuard Find My 0x12 분리 모드** | AirGuard Android 실제 소스 및 지원표 | 반경 내에서 **소유자-분리 조건이 충족될 때만** 비소유자 울리기 가능성; 본인 가까운 평상시와 다름 | **프로토콜 분리 검증 우선.** 다른 사람 기기 접근/출력 금지 |
| 소유자 BLE `4F860002` 혹은 Apple 전용 L2CAP | Find My 연구에서 제3자 액세서리 실증 | 본인 근처 케이스를 평상시 울리려면 핵심 후보 | **지속 연구 1순위**; AirPods 5 실물과 소유자 인증 요구 확인 전 opcode 시도 금지 |
| Apple Find My 웹 iCloud.com | Apple 공식 매뉴얼 | **충전 케이스 소리 불가** | 명확히 제외 |
| 별도 본인 Apple 기기 Find My 앱 | Apple 공식 매뉴얼 | **스피커 동작이 인증된 경로**; Apple 계정 등록/기기 접근 필요 | **즉시 사용 가능한 검증용 기준(Baseline)**. 사용자에게 Apple 기기 보유 여부 확인 |
| 비프/충전음 토글 AACP 0x31 | LibrePods 공개 코드 | 충전 케이스 동작음 제어 ≠ 내 케이스 위치 추적 소리 | 거짓 대체 기능 금지 |

## 4. 새 개발 계획 — 위험 적고 증거가 생기는 순서

### Gate A — 소유자 Find My 등록 상태 먼저 확인
- 사용자 소유 에어팟이 Apple 계정 '나의 찾기'에 등록돼 있는가? Apple iPhone/iPad/Mac을 보유하거나 소유자 기기로 사용한 적 있는가?
- '없음'이면 소유자 Find My 통신 세션이 생성되지 않았을 가능성을 **가설로** 구분하고, 페어링 강제 초기화 없이 해당 하드웨어 기능과 별도 문제로 기록한다.
- Apple 기기가 있다면 Apple Find My 앱에서 **양쪽 안에 둔 닫힌 케이스**의 실제 사운드 재생을 소유자 기기 기준으로 먼저 확인할 수 있다.
- Apple 기기가 없어도 Android passive scan 연구는 별도로 가능하지만 '등록된 Apple owner case' 흐름이라고 단정하지 않는다.

### Gate B — v1.0.14 0x07 로그 반복 금지. 실제 0x12 / FD44 / 기타 Apple 형식 분류
- 소스 패치를 할 때 다음 후보만 먼저 분류하고 **개별 타기기 식별은 금지**:
  - Apple manufacturer type `0x12`(Find My tracker frame, 특히 AirGuard의 AirPods mask 12 19 18)
  - `0x07`(proximity/battery)
  - 나머지 Apple subtype 개수와 길이 분포
  - 0xFD44 서비스 UUID(해당 장치가 실제 광고하는 경우에만)
  - `separated/connected` 상태 카테고리 (판독 근거 존재 시)
- 검색은 사용자가 이미 사용한 정상 세팅하에서 본인 케이스 단독/근접/이격한 **통제된 차이 실험**으로 할 것. 타기기 대상으로 GATT 통신 금지.
- 광고의 첫 3바이트 집계도 타기기의 전파가 섞인 결과이므로 **본인 케이스 동일성 확정 아님**. 실제 소유 케이스는 추가 식별/암호화 인증이 필요하다.
- Android 정책/권한/타이밍 및 앱 별도 설치가 필요한 실험은 실기 확인 전 성공 주장 금지.

### Gate C — 실제 케이스 'Find My' 소리 제어 경로 평가
- Find My 0x12 유형 자체 수신 성공 ⇒ 비소유자 DULT/AirGuard 경로가 가능한 별도 상태인지 확인. 사용자 케이스인지 인증 못 하면 정지.
- 소유자 상태에서 `4F860002` 또는 Apple 전용 제어 채널이 있으면 **소유자 링크 암호화**가 충족되는지 확인해야 함. 명령 '쓰기 ACK' ≠ 케이스 소리 성공.
- 선택된 제어 경로에서 **실제 케이스 스피커의 소리**가 난 경우에만 단계 PASS.
- AirPods/Apple 생태계가 로그인/인증을 요구한다면 공개 SDK로 불가능한 항목은 '막힘'을 그대로 기록하고, 다른 기기/프로토콜 구성을 재검토. 무근거 root 필요 단정도, root 없이 된다는 확정도 금지.

### Gate D — Android 앱 통합은 물리 성공 후
- 1.0.10 USER-GOLDEN 4개 청취 모드/배터리 앱과 AA Golden daemon 전혀 변경 금지.
- 외부 오픈소스 AirGuard 프로토콜 사용 시 Apache-2.0 원본 저작권·라이선스 유지.
- 통신형 앱은 소유자 검증/명시적 스피커 재생 동의/중지 타이머 필수.
- 설치본의 무손실 업데이트는 별도 장기 signing 문제: v1.0.10 개인 key 분실 상태로 최초 승격 절차 합의 필요.

## 5. 지금 시점에 하지 않을 것

- 이미 세 번 실패한 v112~v114의 무근거 루틴에 또 다른 opcode 대입하여 '완성본'을 배포하지 않는다.
- `07=0`로 Find My 하드웨어 미지원, 암호화키 오류, 벽돌, 소리 출력 불가능 확정 금지.
- AirGuard의 **남의 추적기 소리 울리기**를 본인 근처 케이스의 Find My owner action과 동일시 금지.
- iCloud.com 웹 방식으로 충전 케이스 스피커를 원격 재생할 수 있다는 잘못된 안내 금지.
- 상대방 BLE 식별자/키, 사용자 Apple 계정 자격증명을 공개 GitHub/로그에 기록하지 않음.

## 6. 현재 상태 결론

- Apple 공식 하드웨어/Find My로 케이스 소리 가능: **근거 확인**
- Android에서 Apple non-owner Find My BLE beep 코드는 AirGuard에 존재: **소스 확인, owner nearby는 미지원 보장 없음**
- AirPods 5 **본인 케이스**, 닫힘/이어버드 보관/A2DP 미연결 상태로 root 없이 직접 ring: **아직 구현 또는 독립적으로 입증된 소스 없음**
- 가장 우선 확인할 누락 정보는 **AirPods 5가 기존 Apple 계정의 나의 찾기에 등록돼 있는지**.
- 다음 앱 개발은 '배터리 07' 가설에 의존하지 않는, **Apple 0x12/Find My 보조 광고 및 owner control 경로 근거 확인**부터.
