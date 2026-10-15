# Excel-embedded MX Return FLOW

목표: 회사 PC에는 기존 업무 XLSB 하나만 반입/사용.

구조:
- `modMXFlowExcel.bas`: 모니터 가장자리 감지 + Logitech HID++ helper self-extract + PC→폰 전환.
- `ThisWorkbook_PATCH.txt`: 기존 Workbook_Open/BeforeClose에 추가할 최소 훅.
- 외부 EXE 없음.
- 별도 PS1/CMD 반입 없음. PowerShell helper는 VBA 코드 안에 내장되어 LOCALAPPDATA에 자동 생성됨.

기본 회사 배치:
- 모든 모니터(*) 아래쪽
- 2px 감지
- 220ms 유지
- 스마트폰 키보드 채널 2 / 마우스 채널 2
- 폰→PC 복귀 시 첫 마우스 입력을 감지해 PRIMARY 모니터 아래 중앙으로 커서를 이동

테스트:
1. 모듈 통합
2. MXFlow_TestPhone 실행
3. 성공하면 MXFlow_Start
4. 메인/보조 모니터 아래로 마우스를 밀어 테스트

설정 변경 예:
MXFlow_SetConfig "ExitEdge", "RIGHT"
MXFlow_SetConfig "ExitMonitor", "PRIMARY"
MXFlow_SetConfig "DwellMs", "300"
MXFlow_SetConfig "PhoneKeyboardChannel", "2"
MXFlow_SetConfig "PhoneMouseChannel", "2"

한계:
- Windows PowerShell/Add-Type가 회사 보안정책(ConstrainedLanguage/AppLocker)으로 막혀 있으면 HID helper 실행이 차단될 수 있음.
- 이 경우 다음 fallback은 PowerShell 의존성을 제거한 pure-VBA HID++ 엔진으로 교체해야 함.
