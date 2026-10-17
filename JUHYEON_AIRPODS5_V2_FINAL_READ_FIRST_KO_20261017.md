# 주현에어팟5 v2.0.0 — 갤럭시용 한국어 개인 FOSS 앱

## 목적
갤럭시 S26 Ultra에서 **주현에어팟5** 하나로 AirPods 5의 일반 기능을 사용하는 개인 버전.
사용자가 보류한 **케이스 스피커 Find My 소리 찾기** 기능과 모든 연구용 라디오/LAB/FD44 실험은 **포함하지 않는다**.

원본 LibrePods GPL-3.0-or-later의 정확한 동작/안정화 패치 v1.0.10을 출발점으로 하고, 확실하지 않은 BLE 오너 명령·AA 차량/데몬 코드를 섞지 않는다.

## 실제 구현 및 빌드에 포함되는 것
- 한국어 런처 및 앱 이름 **주현에어팟5**; 개인용 설정 제목과 안내.
- AirPods 5의 기본 AACP/ATT/BLE 연결 엔진, 수신 배터리 표시, 착용 감지, 연결 알림·팝업·배터리 위젯, 기기에서 지원하는 청취·제어 설정을 **원본 로직 그대로 유지**. 기기/Android 지원 제약이 있는 옵션(Xposed 등)은 임의 활성화하지 않는다.
- 과거 확인된 앱 서비스 재연결/Compose 반복 바인딩과 오버레이 VideoView BadToken 충돌의 v1.0.10 안전 수정 유지. 개인 로컬 오류기록 유지(자동 업로드 없음).
- 앱 설치/설정 단계의 한글 안내와 결제 없는 FOSS 기능, 대화 감지/팝업 등 로컬 설정이 구독 없이 작동하도록 기존 가짜 `Premium` UI 잠금 제거.
- **삭제**: Play Billing SDK·실제 BillingProvider, 외부 sponsor 결제 페이지로 이동하는 코드, 구매/결제 배너·메일·계좌 등록·기존 개발자 문의 폼. 기존 화면 이동 구조의 안 쓰는 Purchase 목적지는 **무료 개인용 안내만 출력**(크래시 방지).
- **보존**: 원개발자/기여자 저작권과 GPL 라이선스, 오픈소스 라이선스 화면/소스 공개 의무. "후원 삭제"가 라이선스 삭제를 뜻하지 않음.
- v1.0.11의 테스트용 자동 업데이트/로그 연구 코드, v117+ Find My 케이스 추정 라디오/다른 앱 패키지, Android Auto Golden 및 충전 케이스 소리실험 코드는 **절대 임의 병합하지 않음**.

## 기술 배포
- 출처 SHA256 고정 소스 ZIP `My_AirPods5_Clean_Standalone_Korean_SourceBuild_20261014-2.zip`, `0403d20dcc83ae02a974d69b1340e5686ac408b43f77e341e2068fe9991829da`.
- 재현 가능한 패치 `ci/repair_korean_onboarding_and_service.py` / `repair_personal_ui_v106.py` / `repair_personal_activity_v107.py` / `repair_personal_oneear_v108.py` / `add_personal_diagnostics_v109.py` / `fix_evidenced_videoview_badtoken_v110.py` / **`ci/build_juhyeon_final_ko_v2.py`**.
- 새로운 workflow `.github/workflows/juhyeon-airpods5-final-korean.yml`: 정확한 소스 ZIP 고정 SHA, frozen AACP 관련 원파일 SHA 대조, 한국어/결제코드 제거 확인, Java21/Android36/NDK28 앱 빌드, ZIP/Android v2 APK 서명·패키지·DEX, 그리고 실제 **REA 6.5 + SHA 핀된 JADX 0.7.1** 독립 소스 역검증, GPL 전체 수정 소스 ZIP 동봉.
- 패키지 `com.myairpods5.personal` (기존 개인용과 동일 → 별도 여섯 번째 앱을 만들지 않음); versionCode `20000`, versionName `2.0.0-juhyeon-ko-final-debug`.
- CI static PASS와 **Galaxy S26 Ultra 실제 앱 사용 PASS는 서로 별개**. ANC·케이스 배터리 정밀도/팝업 등 현장 검증 전 성공 주장 금지.

## 설치: 기존 서명 충돌을 절대 숨기지 말 것
기존 개인 `com.myairpods5.personal` v1.0.10의 서명과 이번 GitHub Actions의 새 디버그 서명은 일반적으로 **다르다**. Android에서 동일한 패키지명의 서로 다른 서명은 **덮어쓰기 불가능**하다. 사용자가 기존 개인 앱의 데이터/설정(향후 암호화 키가 포함될 수도 있음)을 백업할 방법을 직접 확인하고 **원할 경우에 한해** 그 개인 앱 하나만 삭제한 뒤 새 v2를 설치할 수 있다. 사용자가 동의하지 않으면 **현 설치 앱을 유지**하는 쪽이 안전하다.

절대 삭제하지 말 것: 이전 별도 IRK/실험 LAB, AA Golden/데몬/Recovery/SelfHeal, 기기 Bluetooth 페어링, Apple Find My 연결. **앱 ID가 다르면 이 개인용 앱으로 통합된 것이 아니므로 "한 앱"이라고 오해하지 말 것.**

향후 업데이트 때도 디버그 인증서가 매 실행 바뀌면 직접 덮어쓰기 어려움. 보안상 서명키를 공개 저장소에 올리지 않고 별도의 안전한 비공개 서명 관리가 검증되기 전에는 **자동 업데이트 지원을 가장하지 않는다.**

## 사용자 실기 기능 검증 (1회)
1. 앱 실행, Bluetooth/근처 기기 권한 및 정상 시작 → 재시작해도 반복 팝업/강제종료 없음.
2. 주현에어팟5 케이스 열기·이어버드 착용: 정확한 본인 장치 연결·배터리 표시(없는 데이터는 0%로 조작하지 않음).
3. 팝업과 알림/위젯이 정상 보이는지 확인.
4. 기기 지원 시 청취 모드(적응형/주변음 허용/소음 제어)와 착용 감지·제스처가 동작하는지 확인. Android/Xposed 지원 제약은 숨기지 않음.
5. 설정/앱 화면에서 결제·후원·계정 입력이 전혀 없어야 함.
6. 케이스 Find My 삐삐/원격 인증 메뉴는 의도대로 없음.

## 소스 라이선스와 안전성
원본 LibrePods GPL-3.0-or-later, 개인정보 자동 전송 SDK 추가 없음. 수정된 전체 소스는 해당 릴리스에 별도 GPL source ZIP으로 배포. REA는 개발자 CI 정적 분석 도구일 뿐 휴대폰 앱 안에 관리자/루트 에이전트로 설치되지 않음.

**이 앱은 Galaxy에서 일반 에어팟 기능을 쓰기 위한 개인용 사용 후보이며, 보류한 케이스 찾기 기술의 완성품이라고 주장하지 않는다.**
