# AirPods 5 프로젝트 × Reverse Engineer Anything (REA)

**현재 상태: REA + JADX 실제 GitHub Actions 실행 SUCCESS.** Android 원본 v139 LAB APK를 대상으로 공식 upstream의 REA CLI를 실행하고 DEX 14개 및 실제 클래스 검색까지 확인했습니다. 케이스 소리 재생은 **아직 미구현**입니다.

## 소스 조사 / 설치 근거
- 공식 upstream: https://github.com/morluto/rea (MIT)
- GitHub REA 릴리스: `rea-agents-6.5.0`, commit `3b79af28e80785afce9f5998d8f03289402a1847`, npm `rea-agents@6.5.0`.
- Android 지원: `docs/android-analysis.md`; **REA 단독으로 안드로이드 DEX를 분석하지 않습니다.** 별도 JADX headless 0.7.1 JAR + full JDK 17+ + Node 24 + `ps` 필요.
- 외부 JADX 파일 SHA-256 고정: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`.
- 실제 실행 환경: GitHub Actions Ubuntu 24.04 / Node 24 / Java 21 / `rea-agents@6.5.0` / JADX v0.7.1.
- 자세한 고정 사양: `rea-upstream-lock.json`. 대형 REA 원본을 우리 앱 소스 내부에 복제하지 않고 **공식 배포물의 고정 버전**을 빌드 시 설치합니다. REA 원저작물의 라이선스를 보존하며 사용자 앱을 재배포하지 않습니다.

## 이미 완료한 실제 적용
- 단 하나의 **기존 프로젝트 소유 공개 APK** `my-airpods5-v139-one-lab-replace-r11`에서 SHA-256 `380a26787fc0217ee0068dc24653ffb04731279f5e505e78b0d24deb683ca055` 일치를 검사.
- REA 자체 `inspect-android-package`, `search-android-classes`를 실제 실행.
- 관찰 결과: `com.myairpods5.personal.radiolab`, DEX **14개**, `AirPodsService`/ `CaseRadioEvidenceV138`/`CaseLabInventoryV139` 검색 성공.
- GitHub Actions run: https://github.com/yooogillycloud123/my-airpods5/actions/runs/38916199284 (SUCCESS).
- 산출물: `rea_airpods_v139_static_evidence_summary` (Action artifact, 데이터 삭제/유효기간은 GitHub에서 관리).
- **정확한 한계:** 이것은 정적 APK 디컴파일이고, 실제 BLE 패킷 송수신, Apple Find My 소유자 인증, 닫힌 충전 케이스에서 발생하는 소리 증명은 **아닙니다**.

## 사용법: GitHub Actions로 ChatGPT와 이어서 분석
GitHub 프로젝트를 읽고 수정할 수 있는 ChatGPT GitHub 연결로 `.github/workflows/rea-airpods-apk-source-audit.yml` 내용을 변경하면 해당 브랜치에 push 할 때 REA 작업이 자동 실행됩니다. GitHub Actions run과 정제된 결과를 ChatGPT가 읽어 다음 소스 연구에 활용할 수 있습니다.

**중요: GitHub 소스를 추가한 것 자체가 이 ChatGPT 앱에 REA MCP 서버를 자동 설치하는 것은 아닙니다.** 이 세션의 ChatGPT MCP 목록에는 REA가 연결되지 않았고, Plugin 검색에서도 공식 REA 플러그인을 확인하지 못했습니다. 대신 **REA 실사용 실행 환경은 GitHub runner에서 검증**했습니다. REA의 로컬 MCP가 필요한 다른 CLI 에이전트에서는 공식 `npx rea-agents@6.5.0 setup`로 승인 후 등록해야 하며 일반 ChatGPT 채팅에는 임의 MCP 설치 권한이 없습니다.

## 안전/진행 제한
- 개인 부분 검증 v1.0.10, Android Auto Golden 및 v139 설치 앱 모두 무변경.
- 테스트 반복, Android 강제 재페어링, 사용자 설치 앱 삭제 없음.
- 비공개 타사 APK·앱 계정 정보·Bluetooth 키·IRK·MAC·기기 시리얼·원본 디컴파일 전문을 공용 GitHub Actions artifact/log에 보관하지 않음.
- `12:02` 근접 신호, `12:19` 비소유자 FD44, `0x3020` 이어버드 모델, 충전 시작 딩 소리 ≠ A3530 케이스 소유자 인증/Find My 명령.
- 실제 케이스 소리 APK는 모델별 케이스 신원, 인증된 GATT 세션, 실제 재생 명령, 물리적 소리 확인 전 출시하지 않음.

## 다음 연구 단계
1. REA/JADX로 **권한 있는** 실제 동작 APK의 GATT/ATT/Find My 제어 구현을 식별하고, 앱이 이어버드만 울리는지 충전 케이스를 울리는지 소스 증거로 구분.
2. case owner GATT 별도 transport/인증 근거가 있으면 기존 `ci/case_protocol_v119` 및 `v123` 오프라인 HCI 검사에 합류. (원본 파일은 공개 저장소에 업로드하지 않음.)
3. GATT 인증·케이스 실제 play/stop 근거가 없는 단순 모델 광고/마케팅 설명은 APK 구현 성공으로 취급하지 않음.

관리 파일: `tools/rea_airpods/run_owned_apk_audit.py` · `test_rea_integration.py` · `rea-upstream-lock.json` · `AGENTS.md`.
