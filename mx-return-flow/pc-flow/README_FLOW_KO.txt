MX Return FLOW - 회사 PC 에이전트

기본 배치
- 회사 PC의 모든 모니터(*) 아래쪽 끝 -> 스마트폰
- 스마트폰 대상: MX Keys S 채널 2 + MX Master 3S 채널 2
- 스마트폰에서 PC로 돌아오면 첫 마우스 움직임 때 PRIMARY 모니터 아래 중앙으로 커서 이동

파일
- MXReturnFlow.ps1 : 화면 끝 감지/라우팅
- mxswitch.ps1 : Logitech HID++ ChangeHost 전송
- flow-config.json : 화면 연결도 설정
- Start_MXReturnFlow.cmd : 테스트용
- Start_MXReturnFlow_Hidden.cmd : 평상시 숨김 실행

모니터 이름 확인:
powershell -ExecutionPolicy Bypass -File .\MXReturnFlow.ps1 -ListMonitors

설정 다시 생성:
powershell -ExecutionPolicy Bypass -File .\MXReturnFlow.ps1 -CreateConfig

확장:
- monitor="*" + edge="bottom" : 모든 모니터 아래쪽
- monitor="\\.\DISPLAY1" : 특정 모니터만
- edge는 top/bottom/left/right
- segmentStart/segmentEnd로 가장자리 일부 구간만 활성화
- targets에 다른 목적지를 추가 가능
- returnWarp.monitor/edge/position으로 스마트폰 -> PC 진입 위치 변경

회사 PC에서 PowerShell이 ConstrainedLanguage 모드면 Add-Type이 차단될 수 있습니다.
확인:
powershell -NoProfile -Command "$ExecutionContext.SessionState.LanguageMode"
