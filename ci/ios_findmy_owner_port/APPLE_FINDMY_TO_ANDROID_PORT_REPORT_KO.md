# Apple 나의 찾기 → Galaxy AirPods 5 A3529 CASE SOUND: REA 역공학/이식 기술 보고서
2026-10-17. 모든 실제 성공/불확실/미구현을 구분하는 **소스/실제 빌드 기반** 인계서.

## 0. 실제로 완료한 결과 (이번 작업)
- 소스/배포 기술: `morluto/rea` 정식 npm `rea-agents@6.5.0`, Node 24, JDK 21.
- 6개 PINNED 외부 원자료를 직접 취합해 각각 SHA256, 존재 여부 및 정확한 메서드/selector로 검증:
  - `blacktop/ipsw-diffs@af456047...` iOS 26.5 → 27.0 공개 Apple **Mach-O metadata diff** 세 종: `FindMy.app/FindMy.md`, `SPOwner.framework/SPOwner.md`, `searchpartyd.md`.
  - `EthanArbuckle/iPhone18-3_26.1_23B85_Restore@90aa0cfe...` 세 종: `SPOwnerSession.mm`, `SPBTFindingSession.mm`, `SPAccessoryDiscoveryAndPairingSession.mm` 공개 역컴파일 의사코드.
- 실제 GitHub Actions에서 REA 6.5.0 `import-reference-source --detail summary --json`를 위 파일들에 실행, **PASS**.
- 별도로 Android에서 재사용할 수 있는 순수 Java/Android `CaseRingPort.java` 컴파일, 네 가지 인증/재생 gate와 19개 오프라인 assert **PASS**. `CaseRingPortTest.java`로 무인증 상황에서 GATT 전송이 일어나지 않음을 확인.
- 현재 재료로는 **원본 Apple Mach-O 실행파일을 확보하지 못함**: 공개 GitHub `.md` diff와 `.mm` 의사코드는 원본 바이너리가 **아니다**. 따라서 REA/Ghidra가 **실제 FindMy.app 함수 본문을 신규 직접 디컴파일했다고 주장하지 않음**. 선택적으로 실행할 수 있는 실바이너리 sha/헤더 검증 도구 `analyze_actual_findmy_macho_when_available.sh` 제공.

## 1. Apple 실구현에서 직접 확인한 함수 및 경계
| 구성 | 원자료에서 실제 확인한 요소 | 기술적 범위 |
|---|---|---|
| iOS FindMy.app UI | `FMFindingExperienceCoordinator.startPlaySound`, `stopPlaySound`, `startPlaySoundForBeacon:completion:` | Mach-O symbol/string 목록에서 존재 확인. 내부 구현/body 직접 복원은 아님 |
| Search Party 프레임워크 | `SPOwnerSession.executeUTPlaySoundCommand:`의 `executeCommand:` 호출 | 실제 역컴파일 의사코드에서 명령을 상위 owner session 경로로 넘김을 확인. **UT는 Unauthorized Tracking 관련 문맥이며 보유자 Find My 인증의 증거 아님** |
| 별도 비소유자 추적음 | `SPOwnerSession.playUnauthorizedSoundOnBeaconByUUID:` → `userAgentProxy playUnauthorizedSoundOnBeaconUUID:` | 위 command와 **구별되는 추적 방지 API**. 케이스 소유자 재생 경로와 혼동 금지 |
| 탐색 XPC 인터페이스 | `SPBTFindingSession.proxy` → `com.apple.searchparty.btfindingsession` | iOS 사설 XPC 경계 실증. 일반 Android 앱에서 이 서비스를 직접 호출할 수 없음 |
| 초기 액세서리 연결 | `SPAccessoryDiscoveryAndPairingSession.proxy` → `com.apple.icloud.searchpartyd.accessorydiscoverysession` | Apple Find My 등록·pairing session 경로 존재. **Android 전용 owner enrollment 구현을 확보한 것은 아님** |
| 소유 장치 연결 관련 wrapper | `SPOwnerSession.connectUsingMACAddress:longTermKey:completion:` | Apple 관련 시스템에 LTK 경로가 있음을 시사. 정확한 A3529 케이스의 적용 여부 및 Android API를 통한 전송 방식 확인되지 않음 |
| 소유 액세서리 연결 재료 | `SPOwnerSession.peripheralConnectionMaterialForAccessoryIdentifier:completion:` | 연결 재료를 `userAgentProxy`에 요청하는 의사코드 발견. 구체적인 반환 키, 인증 핸드셰이크, 케이스 사운드 opcode는 드러나지 않음 |
| 백엔드 소리 메서드 | `searchpartyd` symbol metadata: `startPlaySound(beacon:completion:)` / `stopPlaySound(beacon:completion:)` | 백엔드 명령명 관찰. FindMy UI와 실제 네 케이스까지 이어지는 완전 호출 그래프가 **검증되지는 않음** |

**확인된 기술적 방향은 UI의 공개 이름만 복사하는 게 아니라 `SPOwner`의 권한/소유자 결속, XPC 백엔드 그리고 *별도 액세서리 BLE 소리 명령*을 따라가야 한다는 점**이다. Private XPC 문자열을 갤럭시에 그대로 붙이거나 `FD44/4F860003` 비소유자 특성에 쓰는 것은 원격 케이스 제어가 아니다.

## 2. 네 실제 Galaxy 실기와 어떻게 맞물리는가
- 실물 박스 **AirPods 5 with Wireless Charging Case · A3439/A3440/A3529** 확정. 이 표기는 케이스 모델이지 BLE rotating MAC의 소유자 인증이 아님.
- Galaxy S26 Ultra v140에서 12:02 3회, 12:19 0회, 연결 가능 후보 1개. v140의 후보가 `12:19`라고 표시된 것은 v141 소스 수정으로 `12:02`로 바로잡음.
- 별도 BLE/GATT 직접 연결 성공, **시작·중지 GATT status=0/0**, 사용자 실제 청취 **충전 케이스 무음**. 따라서 `ACK=0`만으로 케이스 소리 성공 판정 금지.
- v141 실험앱은 12초 1회 수동 스캐너 + 읽기전용 후보. 재설치/수회 재검색/다른 주변 기기에 무작정 FD44 명령 전송 **금지**.

## 3. Android 자체 이식: 파일과 실행 상태
`android/CaseRingPort.java`는 원본 소리 미구현을 가장한 빈 버튼이 아니다. 다음 **독립적인 조건을 통과해야만** 실제 송신 adapter가 호출되는 상태 제어 인터페이스다.

1. `OwnerCaseResolver`: 주변 12:02 임시 주소나 AirPods 모델 광고가 아닌 **내 개별 A3529 실물 케이스의 암호학적 신원 결속**.
2. `OwnerProvisioner`: Apple 소유자 세션에 해당하는 합법적이고 재현 가능한 **갤럭시 단독 인증 증거**.
3. `CaseSoundProtocol`: **케이스 전용** 재생/중지 제어 흐름 및 실제 성공 핸드셰이크. 일반 비소유자 FD44 opcode 대입 금지.
4. `AudioObserver`: 이어버드 둘 다 넣음, 케이스 닫힘, A2DP 미연결 상태의 **물리적 스피커 소리** 실제 확인.

`createUnprovisioned()` 기본값은 1부터 명확히 DENIED, BLE write=0. 가정용/mocked 성공 응답은 `production-ready`의 근거 아님. 미래의 재생 세션에서는 START 요청 이후 ACK/timeout 불확실성이라도 **STOP 시도**를 수행하도록 구현.

실제 Java21 컴파일/테스트: `javac ...CaseRingPort.java ...CaseRingPortTest.java` + `java -ea ...CaseRingPortTest` PASS. 이식 엔진은 앱에서 재사용 가능한 **안전 경계/검증 상태기계**이지, 현재 충전 케이스 소리를 울릴 수 있는 라이브 구현이 **아님**.

## 4. 다음 기술 작업의 정확한 Gate
| Gate | 조건 | 현 상태 |
|---|---|---|
| A | iOS 정적 원자료 symbol/의사코드 cross-check | **PASS** |
| B | REA 6.5.0이 참조소스 그래프 실제 해석 | **PASS**, source graph only |
| C | 실제 Apple FindMy, SPOwner, searchpartyd **원본 ARM64 Mach-O Ghidra decomp/cross-references** | **NOT RUN**, 원본 Mach-O 없음 |
| D | A3529 케이스 실제 BLE endpoint 확인 / owner provisioning 자료 | **NOT VERIFIED** |
| E | 독립된 케이스 명령/암호화 및 play-stop 물리적 검증 | **NOT VERIFIED** |
| F | Android live transport adapter → 네 갤럭시에서 닫힌 케이스 삐삐 | **NOT IMPLEMENTED** |

Gate C: 공개 Apple 공식 IPSW 등 **권한 있게 획득한 원본**과 SHA256 및 Mach-O 매직 확인 후 `analyze_actual_findmy_macho_when_available.sh` 사용. REA + Ghidra 12.1.x, Java21 필요. raw code/키는 공개 GitHub에 올리지 않음. Ghidra가 실행되어도 함수 존재만으로 owner session/라디오 opcode 알았다고 판단 금지.

Gate D/E: Apple Account/기기 없이 Find My enrollment을 생성하는 공개 인증 경로가 없다면 **요구한 범위의 기능은 여전히 차단**된다. 우회 성공이라 가장하거나 네게 아이폰을 구하라 요구하지 않는다. iOS private XPC는 원격으로 재사용하는 Android API가 아니다.

## 5. 사용자 장치/기존 프로젝트 보호
`com.myairpods5.personal` 기존 개인 앱, `com.myairpods5.personal.radiolab` v141, 오래된 개인 IRK 관련 LAB, Android Auto AA Golden/daemon, 삼성 기기 블루투스 페어링 **이번 작업은 변경하지 않음**. 사용자 Android 휴대폰에서 앱 6개를 늘리지 않음. 단순 분석 이유만으로 신규 APK 설치를 강요하지 않음.

## 증거 원문
- blacktop iOS Mach-O symbol diff `https://github.com/blacktop/ipsw-diffs/tree/af456047befbab058df4c3d7c7094462ad4753d6/iOS/26_5_23F77_vs_27_0_24A5355q`
- Ethan iOS 26.1 SPOwner *third-party reconstructed pseudocode* `https://github.com/EthanArbuckle/iPhone18-3_26.1_23B85_Restore/tree/90aa0cfe59d9682b4265e1354c8b19ec3c7823ab/System/Library/PrivateFrameworks/SPOwner.framework/SPOwner`
- REA Ghidra/Apple `https://github.com/morluto/rea/blob/main/docs/ghidra-provider.md`, `https://github.com/morluto/rea/blob/main/docs/apple-application-analysis.md`.
- 모든 패키지 기록 `dist/REA_Apple_FindMy_26_Source_Port_PROOF.json` CI 요약; 원본 디컴파일 코드는 업로드하지 않음.
