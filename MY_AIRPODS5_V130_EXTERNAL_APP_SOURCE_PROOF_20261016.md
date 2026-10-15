# AirPods 5 A3530 — 타사 앱 실소스·실사용 증거 판정 보고서 (2026-10-16)

## 최종 요구 (변경 없음)
Galaxy S26 Ultra / AirPods 5 무선 충전 케이스 A3530. 이어버드 두 개 수납, 케이스 닫힘, 충전기 미연결, A2DP 미연결. 갤럭시 자체 앱에서 버튼 하나로 **케이스 물리 스피커** 소리 재생. 별도 Apple 기기·root·Shizuku 의존 없이. 이어버드 출력이나 전화기 소리 및 RSSI만 보는 앱은 **불합격**.

## 조사 방식 / 판정 기준
공개된 실제 Android/Kotlin/Java/C++ 코드는 UI의 Play 버튼에서 재생 실행까지 **호출 경로를 추적**했다. 비공개 앱은 Play 공식 설명·사용자 실측 리뷰·개발자 답변을 대조했다. 비공개 APK 원본/네트워크 트래픽을 확보하지 못했으므로, 마케팅 문구를 실행된 케이스 소리로 확정하지 않았다.

| 후보 | 광고/표시 | 실구현 및 실측 확인 | A3530 닫힌 케이스 직접 소리 |
|---|---|---|---|
| AFind (DreamTeam Mobile), 1.1.0 | Google Play "Play sound on AirPods or case" | 2026-08-18 실제 Pro2 USB-C 리뷰: **이어버드를 케이스 밖에 꺼낼 때만 소리**, 케이스 스피커 인식 실패. 개발자 답변은 케이스 지원을 주장하면서 **Apple Find My 연결 여부를 재질문**. 독립 소스/실행 패킷 비공개. 결론: **검증되지 않은 개발사 주장** | **미입증** |
| PodsLink, 2026-09-02 | 웹 Find My / Play Sound 광고 | 공식 최신 Google Play 지원 기기 목록에 AirPods 5 **미기재**; 실제 리뷰에서는 케이스 추적음 재생 불가. 본체 Find My GATT 명령 공개 코드 없음 | **미입증** |
| MagicPods for Windows 2.0.99.0 | AirPods5 신규 지원, 일부 Beats **케이스 효과음 ON/OFF 및 볼륨** | 앱 본체 소스는 공개되지 않음. 별도 오픈소스 **MagicPodsCore** 직접 조사: 공개 `AapSetToneVolume.cpp`는 **AACP ToneVolume(0x1F) 설정**만 보내며 충전 케이스 소리 재생 기능 아님. 자체 모델 맵에 AirPods5 포함되지만 등록과 음향 실행은 다름 | **미입증** |
| Androidpods | UI "Find My" 왼/양쪽/오른쪽 | Kotlin `FindMyCard.kt → ChimePlayer.play() → android.media.AudioTrack`; 스마트폰 PCM 출력이 활성 이어버드에 전달되는 방식. 원본 README "Case-speaker chimes ❌ Not implemented" | **불합격** |
| Pods-control | README Find My / Play | `HomeScreen.kt → commander.playFindSound(ear=0,1,2) → AacpFrameBuilder.buildPlayFindSound → 0x0C`; **이어버드 대상**, 실제 A3530 케이스 연결/소유자 인증/소리 명령 증거 없음 | **불합격** |
| GreenPods | 루트 없는 배터리·ANC 및 Find My 기술 설명 | 원본 README는 Find My를 Apple account service로 표시, 직접 제어 미지원 | **불합격** |
| AirPodsCompanion | Find / Find My | `ICloudFindLauncher.kt`는 정확히 `https://www.icloud.com/find/`을 Android Custom Tab으로 연다. 네이티브 케이스 GATT 제어 아님 | **불합격** |
| Poddy | 케이스 배터리, 연결 상태 | 원본 README에서 **케이스 닫히면 이어버드와 통신 불가**라고 명시 | **불합격** |
| OpenPods, PodsCompanion | 배터리·팝업 | 검토한 원본 기능/소스에서 CASE 소리 인증·쓰기 없음 | **미확인/미입증** |
| CAPod | AAP 통신, 케이스 관련 필드 | `DefaultAapDeviceProfile.kt` 명시: AACP `0x31`의 효과 **불명확**, 실제 케이스 충전음은 별도 **ATT**. 설정은 Find My case speaker PLAY 아님 | **불합격** |
| LibrePods | AAP 기능/배터리, 케이스 충전음 토글 연구 | 원개발자 issue #102: 이어버드에 보낸 충전음 설정 패킷 **실제 실패**; 별도 케이스 GATT owner-auth 미해결. TODO #20 'case authenticate' / 'case play sound' 미완료 | **불합격** |
| AirGuard 3.1.3 | Find My 분리 상태에서 소리 제어 | 네 갤럭시 실제 스크린샷에 **Safe Trackers → AirPods → Locate Tracker만**, Play Sound **없음**. 타겟 owner-auth CASE 경로와 다름 | **실측 불가** |
| FindMy.py, OpenTagViewer | Bluetooth GATT Find My SOUND 및 owner CP 연구 | AirTag/타사 액세서리 관련 비소유자·소유자 프로토콜. 사용자 기기 A3530 물리 소리 증거 없음; 인증된 암호화 링크와 소리 command가 별개 | **연구 자료만** |

## 주목할 실제 원문과 코드 경로
1. AFind Android Play: https://play.google.com/store/apps/details?id=com.dreamteammobile.find.my.airpods
   - 버전 1.1.0 / 2026-08-12 변경. 2026-08-18 리뷰에서 "out of the case" 조건만 성공, 케이스 스피커 인식 실패. 개발자는 Apple Find My 연결 여부를 질문. **Apple 최초 등록 없는 Galaxy 단독 성공의 증거가 아님**.
2. PodsLink Play: https://play.google.com/store/apps/details?id=net.podslink
   - 공식 소개: https://www.podslink.net/
   - 케이스 찾기 오디오 실제 불가 리뷰 확인; AirPods5 공식 2026 기기 목록 누락.
3. Androidpods:
   - UI: https://github.com/TimStebner/Androidpods/blob/main/app/src/main/kotlin/dev/androidpods/feature/findmy/FindMyCard.kt
   - 구현: https://github.com/TimStebner/Androidpods/blob/main/app/src/main/kotlin/dev/androidpods/core/audio/ChimePlayer.kt
   - 원본 README: https://github.com/TimStebner/Androidpods
4. Pods-control:
   - UI: https://github.com/wrfwdd/Pods-control/blob/main/feature-home/src/main/java/com/airpods/control/home/HomeScreen.kt
   - commander: https://github.com/wrfwdd/Pods-control/blob/main/core-aacp/src/main/java/com/airpods/control/core/aacp/AacpCommander.kt
   - build: https://github.com/wrfwdd/Pods-control/blob/main/core-aacp/src/main/java/com/airpods/control/core/aacp/AacpFrameBuilder.kt
5. MagicPods open source truly independent:
   - core: https://github.com/steam3d/MagicPodsCore
   - tone sender: https://github.com/steam3d/MagicPodsCore/blob/master/src/sdk/aap/setters/AapSetToneVolume.cpp
   - enum: https://github.com/steam3d/MagicPodsCore/blob/master/src/sdk/aap/enums/AapCmdSettings.h
   - commercial change log AirPods5: https://help.magicpods.app/changelog/
   - case sounds on other model: https://help.magicpods.app/headphones/apple-powerbeatspro2/
6. CAPod: https://github.com/d4rken-org/capod/blob/main/app/src/main/java/eu/darken/capod/pods/core/apple/aap/protocol/DefaultAapDeviceProfile.kt
7. LibrePods original maintainer owner GATT issue: https://github.com/librepods-org/librepods/issues/102 ; backlog: https://github.com/librepods-org/librepods/issues/20
8. AirPodsCompanion external iCloud URL actual Kotlin: https://github.com/neydermarquez/AirPodsCompanion/blob/main/app/src/main/java/com/soren/airpodscompanion/ICloudFindLauncher.kt
9. Poddy explicit case closed limitation: https://github.com/beeeegi/Poddy
10. Apple case charging sound is a real distinct **power-on event**: https://support.apple.com/ko-kr/119912

## 결론 — 기능이 존재하는 것과 성공한 것은 다르다
- 이 조사에서 여러 타사 앱의 **소리 버튼과 Find My 이름이 케이스 직접 스피커 인증/재생 구현이 아님**을 소스 수준에서 판별했다.
- AFind와 PodsLink는 **소스/프로토콜 미공개**. AFind는 오히려 케이스 스피커 실패 사용자 실측이 있고, 개발자의 Apple Find My 연결 질문이 있어 **Galaxy 단독 성공은 미확인**. '무조건 불가능'도 '지원 성공'도 입증할 수 없다.
- MagicPods **오픈소스 코어**까지 확인했지만 AACP 0x1F 톤 볼륨 제어일 뿐 Find My CASE 소리 재생과 별개임. 상용 본체의 비공개 기능을 오픈소스 코어 부재만으로 전부 부정하지는 않는다.
- 충전 케이블 A/B 실측은 **케이스 물리 스피커 및 충전 시작음**을 입증한다. 충전기 없이 Android Bluetooth로 같은 소리를 호출하는 명령은 이번 조사에서도 발견되지 않았다.
- **새로운 물리 CASE PLAY 지원 APK는 제작 불가**: 케이스 소유자 인증 + 실제 명령/프로토콜 증거 미확보. 소리가 나지 않는 실험 APK를 설치시키거나 원본 v1.0.10/Android Auto/기존 pairing을 위험에 빠뜨리지 말 것.

## 실제 APK 진전이 가능해지는 증거 기준
AFind나 독립 개발자에게 **AirPods5 A3530**, **두 이어버드 수납·케이스 닫힘·충전기 분리**, **Apple Find My 미등록**, **Galaxy에서 직접 PLAY**, **케이스에서 소리 발생한 연속 촬영** 여부와 구체적 GATT/ATT/L2CAP owner-auth 구현(개인 인증 키 제외)을 검증해야 함. 실패라면 코드 이식 금지. 애플/iCloud 링크, 폰에서 재생된 PCM, 이어버드만 울리는 것은 모두 실패.

### 별도 보존
개인 앱 v1.0.10 부분 실측 기준선, v126 battery LAB, Android Auto Golden 및 daemon 모두 불변. 실기 사용자를 더 반복 테스트시키지 않는다. 사용자 요구 최종 기능은 그대로다.
