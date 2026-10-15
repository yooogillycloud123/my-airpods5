Attribute VB_Name = "modMXFlowExcel"
Option Explicit

' MX Return FLOW - Excel embedded PC engine
' - Company PC needs only this workbook.
' - No EXE/PS1/CMD needs to be carried in separately.
' - The embedded HID++ helper is self-extracted under LOCALAPPDATA at runtime.
' - Default: any monitor bottom edge -> phone.
' - Phone -> PC is handled by the Android MX Return FLOW app.
'
' IMPORTANT:
' This module intentionally uses a Windows timer so FLOW works even while another
' application is in front, as long as this Excel workbook remains open.

#If VBA7 Then
    Private Declare PtrSafe Function SetTimer Lib "user32" (ByVal hWnd As LongPtr, ByVal nIDEvent As LongPtr, ByVal uElapse As Long, ByVal lpTimerFunc As LongPtr) As LongPtr
    Private Declare PtrSafe Function KillTimer Lib "user32" (ByVal hWnd As LongPtr, ByVal nIDEvent As LongPtr) As Long
    Private Declare PtrSafe Function GetCursorPos Lib "user32" (ByRef lpPoint As MXF_POINT) As Long
    Private Declare PtrSafe Function SetCursorPos Lib "user32" (ByVal X As Long, ByVal Y As Long) As Long
    Private Declare PtrSafe Function MonitorFromRect Lib "user32" (ByRef lprc As MXF_RECT, ByVal dwFlags As Long) As LongPtr
    Private Declare PtrSafe Function GetMonitorInfoA Lib "user32" (ByVal hMonitor As LongPtr, ByRef lpmi As MXF_MONITORINFOEX) As Long
    Private Declare PtrSafe Function GetSystemMetrics Lib "user32" (ByVal nIndex As Long) As Long
#Else
    Private Declare Function SetTimer Lib "user32" (ByVal hWnd As Long, ByVal nIDEvent As Long, ByVal uElapse As Long, ByVal lpTimerFunc As Long) As Long
    Private Declare Function KillTimer Lib "user32" (ByVal hWnd As Long, ByVal nIDEvent As Long) As Long
    Private Declare Function GetCursorPos Lib "user32" (ByRef lpPoint As MXF_POINT) As Long
    Private Declare Function SetCursorPos Lib "user32" (ByVal X As Long, ByVal Y As Long) As Long
    Private Declare Function MonitorFromRect Lib "user32" (ByRef lprc As MXF_RECT, ByVal dwFlags As Long) As Long
    Private Declare Function GetMonitorInfoA Lib "user32" (ByVal hMonitor As Long, ByRef lpmi As MXF_MONITORINFOEX) As Long
    Private Declare Function GetSystemMetrics Lib "user32" (ByVal nIndex As Long) As Long
#End If

Private Type MXF_POINT
    X As Long
    Y As Long
End Type

Private Type MXF_RECT
    Left As Long
    Top As Long
    Right As Long
    Bottom As Long
End Type

Private Type MXF_MONITORINFOEX
    cbSize As Long
    rcMonitor As MXF_RECT
    rcWork As MXF_RECT
    dwFlags As Long
    szDevice As String * 32
End Type

Private Const MONITOR_DEFAULTTONEAREST As Long = 2
Private Const MONITORINFOF_PRIMARY As Long = 1
Private Const SM_CXSCREEN As Long = 0
Private Const SM_CYSCREEN As Long = 1
Private Const APP_NAME As String = "MXReturnFlow"
Private Const APP_SECTION As String = "Company"
Private Const HELPER_FILE As String = "mxswitch_flow_v040.ps1"

#If VBA7 Then
    Private gTimerId As LongPtr
#Else
    Private gTimerId As Long
#End If

Private gRunning As Boolean
Private gInTick As Boolean
Private gSwitching As Boolean
Private gHavePrev As Boolean
Private gPrev As MXF_POINT
Private gCandidate As Boolean
Private gCandidateSince As Double
Private gLastTriggerAt As Double
Private gAway As Boolean
Private gAwayPoint As MXF_POINT
Private gAwaySince As Double

' ---------------- PUBLIC ENTRY POINTS ----------------

Public Sub MXFlow_Start()
    On Error GoTo EH
    If gRunning Then Exit Sub

    MXF_EnsureDefaults
    MXF_EnsureHelper

    gRunning = True
    gHavePrev = False
    gCandidate = False
    gAway = False
    gLastTriggerAt = 0

#If VBA7 Then
    gTimerId = SetTimer(0, 0, MXF_CfgLong("PollMs", 40), AddressOf MXFlow_TimerProc)
#Else
    gTimerId = SetTimer(0, 0, MXF_CfgLong("PollMs", 40), AddressOf MXFlow_TimerProc)
#End If

    If gTimerId = 0 Then Err.Raise vbObjectError + 510, , "Windows timer creation failed."
    MXF_Log "START"
    Exit Sub
EH:
    gRunning = False
    MsgBox "MX FLOW 시작 실패" & vbCrLf & Err.Description, vbExclamation
End Sub

Public Sub MXFlow_Stop()
    On Error Resume Next
    If gTimerId <> 0 Then KillTimer 0, gTimerId
    gTimerId = 0
    gRunning = False
    gInTick = False
    gSwitching = False
    gCandidate = False
    MXF_Log "STOP"
End Sub

Public Sub MXFlow_Toggle()
    If gRunning Then
        MXFlow_Stop
        MsgBox "MX FLOW 꺼짐", vbInformation
    Else
        MXFlow_Start
        MsgBox "MX FLOW 켜짐", vbInformation
    End If
End Sub

Public Sub MXFlow_TestPhone()
    Dim rc As Long
    On Error GoTo EH
    MXF_EnsureDefaults
    MXF_EnsureHelper
    rc = MXF_SwitchToPhone()
    If rc = 0 Then
        MsgBox "스마트폰 전환 명령 전송 완료." & vbCrLf & _
               "키보드/마우스가 폰 채널로 이동했는지 확인하세요.", vbInformation
    Else
        MsgBox "스마트폰 전환 실패. 코드=" & rc & vbCrLf & _
               "MXFlow_ShowDiagnostics를 실행해 주세요.", vbExclamation
    End If
    Exit Sub
EH:
    MsgBox "MX FLOW 테스트 오류" & vbCrLf & Err.Description, vbExclamation
End Sub

Public Sub MXFlow_ShowDiagnostics()
    Dim p As MXF_POINT, mi As MXF_MONITORINFOEX
    Dim s As String
    On Error Resume Next
    GetCursorPos p
    MXF_GetMonitorAtPoint p, mi
    s = "MX FLOW running=" & CStr(gRunning) & vbCrLf & _
        "cursor=" & p.X & "," & p.Y & vbCrLf & _
        "monitor=" & MXF_DeviceName(mi) & vbCrLf & _
        "primary=" & CStr((mi.dwFlags And MONITORINFOF_PRIMARY) <> 0) & vbCrLf & _
        "exitMonitor=" & MXF_CfgText("ExitMonitor", "*") & vbCrLf & _
        "exitEdge=" & MXF_CfgText("ExitEdge", "BOTTOM") & vbCrLf & _
        "edgePx=" & MXF_CfgLong("EdgePx", 2) & vbCrLf & _
        "dwellMs=" & MXF_CfgLong("DwellMs", 220) & vbCrLf & _
        "phone keyboard channel=" & MXF_CfgLong("PhoneKeyboardChannel", 2) & vbCrLf & _
        "phone mouse channel=" & MXF_CfgLong("PhoneMouseChannel", 2) & vbCrLf & _
        "helper=" & MXF_HelperPath()
    MsgBox s, vbInformation, "MX FLOW 진단"
End Sub

Public Sub MXFlow_SetConfig(ByVal Key As String, ByVal Value As String)
    SaveSetting APP_NAME, APP_SECTION, Key, Value
End Sub

Public Sub MXFlow_ResetDefaults()
    On Error Resume Next
    DeleteSetting APP_NAME, APP_SECTION
    MXF_EnsureDefaults
    MsgBox "MX FLOW 설정을 기본값으로 초기화했습니다.", vbInformation
End Sub

' ---------------- TIMER / EDGE ENGINE ----------------

#If VBA7 Then
Public Sub MXFlow_TimerProc(ByVal hWnd As LongPtr, ByVal uMsg As Long, ByVal idEvent As LongPtr, ByVal dwTime As Long)
#Else
Public Sub MXFlow_TimerProc(ByVal hWnd As Long, ByVal uMsg As Long, ByVal idEvent As Long, ByVal dwTime As Long)
#End If
    Dim p As MXF_POINT
    Dim mi As MXF_MONITORINFOEX
    Dim nowMs As Double
    Dim hit As Boolean

    If Not gRunning Then Exit Sub
    If gInTick Or gSwitching Then Exit Sub

    gInTick = True
    On Error GoTo Clean

    If GetCursorPos(p) = 0 Then GoTo Clean
    nowMs = MXF_NowMs()

    If gAway Then
        If MXF_Elapsed(nowMs, gAwaySince) >= MXF_CfgLong("ReturnDetectAfterMs", 700) Then
            If Abs(p.X - gAwayPoint.X) > 2 Or Abs(p.Y - gAwayPoint.Y) > 2 Then
                MXF_WarpReturnCursor
                gAway = False
                gCandidate = False
                gHavePrev = False
                gLastTriggerAt = MXF_NowMs()
                MXF_Log "RETURN_DETECTED"
            End If
        End If
        GoTo Remember
    End If

    If Not MXF_GetMonitorAtPoint(p, mi) Then GoTo Remember
    hit = MXF_IsConfiguredExit(p, mi)

    If Not hit Then
        gCandidate = False
    ElseIf Not gCandidate Then
        If gHavePrev And MXF_MovingTowardEdge(gPrev, p, MXF_CfgText("ExitEdge", "BOTTOM")) Then
            gCandidate = True
            gCandidateSince = nowMs
        End If
    Else
        If MXF_Elapsed(nowMs, gCandidateSince) >= MXF_CfgLong("DwellMs", 220) And _
           MXF_Elapsed(nowMs, gLastTriggerAt) >= MXF_CfgLong("CooldownMs", 1200) Then
            gCandidate = False
            gSwitching = True
            gAwayPoint = p

            If MXF_SwitchToPhone() = 0 Then
                gAway = True
                gAwaySince = MXF_NowMs()
                gLastTriggerAt = gAwaySince
                MXF_Log "PC_TO_PHONE"
            Else
                gLastTriggerAt = MXF_NowMs()
                MXF_Log "PC_TO_PHONE_FAIL"
            End If

            gSwitching = False
        End If
    End If

Remember:
    gPrev = p
    gHavePrev = True
Clean:
    gInTick = False
End Sub

Private Function MXF_IsConfiguredExit(ByRef p As MXF_POINT, ByRef mi As MXF_MONITORINFOEX) As Boolean
    Dim edge As String, mon As String
    Dim px As Long
    Dim frac As Double, segA As Double, segB As Double
    Dim w As Long, h As Long

    mon = UCase$(Trim$(MXF_CfgText("ExitMonitor", "*")))
    If mon <> "*" Then
        If mon = "PRIMARY" Then
            If (mi.dwFlags And MONITORINFOF_PRIMARY) = 0 Then Exit Function
        ElseIf UCase$(MXF_DeviceName(mi)) <> mon Then
            Exit Function
        End If
    End If

    edge = UCase$(Trim$(MXF_CfgText("ExitEdge", "BOTTOM")))
    px = MXF_CfgLong("EdgePx", 2)
    If px < 1 Then px = 1

    segA = MXF_CfgDouble("SegmentStart", 0#)
    segB = MXF_CfgDouble("SegmentEnd", 1#)
    If segA < 0# Then segA = 0#
    If segB > 1# Then segB = 1#
    If segB < segA Then segB = segA

    w = mi.rcMonitor.Right - mi.rcMonitor.Left
    h = mi.rcMonitor.Bottom - mi.rcMonitor.Top

    Select Case edge
        Case "BOTTOM"
            If p.Y < mi.rcMonitor.Bottom - px Then Exit Function
            If w <= 0 Then Exit Function
            frac = (p.X - mi.rcMonitor.Left) / CDbl(w)
        Case "TOP"
            If p.Y >= mi.rcMonitor.Top + px Then Exit Function
            If w <= 0 Then Exit Function
            frac = (p.X - mi.rcMonitor.Left) / CDbl(w)
        Case "LEFT"
            If p.X >= mi.rcMonitor.Left + px Then Exit Function
            If h <= 0 Then Exit Function
            frac = (p.Y - mi.rcMonitor.Top) / CDbl(h)
        Case "RIGHT"
            If p.X < mi.rcMonitor.Right - px Then Exit Function
            If h <= 0 Then Exit Function
            frac = (p.Y - mi.rcMonitor.Top) / CDbl(h)
        Case Else
            Exit Function
    End Select

    MXF_IsConfiguredExit = (frac >= segA And frac <= segB)
End Function

Private Function MXF_MovingTowardEdge(ByRef prev As MXF_POINT, ByRef cur As MXF_POINT, ByVal edge As String) As Boolean
    Select Case UCase$(edge)
        Case "BOTTOM": MXF_MovingTowardEdge = (cur.Y > prev.Y)
        Case "TOP": MXF_MovingTowardEdge = (cur.Y < prev.Y)
        Case "LEFT": MXF_MovingTowardEdge = (cur.X < prev.X)
        Case "RIGHT": MXF_MovingTowardEdge = (cur.X > prev.X)
    End Select
End Function

' ---------------- LOGITECH SWITCH ----------------

Private Function MXF_SwitchToPhone() As Long
    Dim kb As Long, ms As Long
    Dim helper As String
    Dim shell As Object
    Dim cmd As String
    Dim rc As Long

    On Error GoTo EH

    helper = MXF_HelperPath()
    kb = MXF_CfgLong("PhoneKeyboardChannel", 2)
    ms = MXF_CfgLong("PhoneMouseChannel", 2)

    If kb < 1 Or kb > 3 Or ms < 1 Or ms > 3 Then
        MXF_SwitchToPhone = 91
        Exit Function
    End If

    Set shell = CreateObject("WScript.Shell")

    If kb = ms Then
        cmd = "powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File " & _
              MXF_Q(helper) & " -All -Channel " & CStr(kb)
        rc = shell.Run(cmd, 0, True)
        MXF_SwitchToPhone = rc
        Exit Function
    End If

    cmd = "powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File " & _
          MXF_Q(helper) & " -Name " & MXF_Q("Keys") & " -Channel " & CStr(kb)
    rc = shell.Run(cmd, 0, True)
    If rc <> 0 Then
        MXF_SwitchToPhone = rc
        Exit Function
    End If

    cmd = "powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File " & _
          MXF_Q(helper) & " -Name " & MXF_Q("Master") & " -Channel " & CStr(ms)
    rc = shell.Run(cmd, 0, True)
    MXF_SwitchToPhone = rc
    Exit Function

EH:
    MXF_Log "SWITCH_EXCEPTION " & Err.Number & " " & Err.Description
    MXF_SwitchToPhone = 99
End Function

Private Sub MXF_EnsureHelper()
    Dim path As String, folder As String
    Dim b64 As String
    Dim xml As Object, node As Object
    Dim bytes() As Byte
    Dim ff As Integer

    path = MXF_HelperPath()
    folder = Left$(path, InStrRev(path, "\") - 1)

    If Dir$(folder, vbDirectory) = vbNullString Then MkDir folder

    ' Always refresh on workbook start so helper and workbook logic stay in sync.
    b64 = MXF_AllB64()

    Set xml = CreateObject("MSXML2.DOMDocument.6.0")
    Set node = xml.createElement("b64")
    node.DataType = "bin.base64"
    node.Text = b64
    bytes = node.nodeTypedValue

    ff = FreeFile
    Open path For Binary Access Write As #ff
    Put #ff, , bytes
    Close #ff
End Sub

Private Function MXF_HelperPath() As String
    MXF_HelperPath = Environ$("LOCALAPPDATA") & "\MXReturnFlow\" & HELPER_FILE
End Function

' ---------------- MONITOR / RETURN ----------------

Private Function MXF_GetMonitorAtPoint(ByRef p As MXF_POINT, ByRef mi As MXF_MONITORINFOEX) As Boolean
    Dim r As MXF_RECT
#If VBA7 Then
    Dim hm As LongPtr
#Else
    Dim hm As Long
#End If

    r.Left = p.X
    r.Top = p.Y
    r.Right = p.X + 1
    r.Bottom = p.Y + 1

    hm = MonitorFromRect(r, MONITOR_DEFAULTTONEAREST)
    If hm = 0 Then Exit Function

    mi.cbSize = LenB(mi)
    If GetMonitorInfoA(hm, mi) = 0 Then Exit Function
    MXF_GetMonitorAtPoint = True
End Function

Private Function MXF_DeviceName(ByRef mi As MXF_MONITORINFOEX) As String
    Dim n As Long
    n = InStr(1, mi.szDevice, vbNullChar)
    If n > 0 Then
        MXF_DeviceName = Left$(mi.szDevice, n - 1)
    Else
        MXF_DeviceName = Trim$(mi.szDevice)
    End If
End Function

Private Sub MXF_WarpReturnCursor()
    Dim x As Long, y As Long, inset As Long
    Dim pos As Double
    Dim w As Long, h As Long

    ' First version targets PRIMARY for maximum reliability.
    ' The route/config layer already keeps the monitor target abstract so a later
    ' build can add DISPLAY1/DISPLAY2 return targeting without changing callers.
    w = GetSystemMetrics(SM_CXSCREEN)
    h = GetSystemMetrics(SM_CYSCREEN)
    inset = MXF_CfgLong("ReturnInsetPx", 24)
    If inset < 4 Then inset = 4

    pos = MXF_CfgDouble("ReturnPosition", 0.5)
    If pos < 0# Then pos = 0#
    If pos > 1# Then pos = 1#

    x = CLng(w * pos)
    y = h - inset
    SetCursorPos x, y
End Sub

' ---------------- CONFIG / UTILS ----------------

Private Sub MXF_EnsureDefaults()
    MXF_Default "PollMs", "40"
    MXF_Default "ExitMonitor", "*"
    MXF_Default "ExitEdge", "BOTTOM"
    MXF_Default "EdgePx", "2"
    MXF_Default "DwellMs", "220"
    MXF_Default "CooldownMs", "1200"
    MXF_Default "SegmentStart", "0"
    MXF_Default "SegmentEnd", "1"
    MXF_Default "PhoneKeyboardChannel", "2"
    MXF_Default "PhoneMouseChannel", "2"
    MXF_Default "ReturnMonitor", "PRIMARY"
    MXF_Default "ReturnEdge", "BOTTOM"
    MXF_Default "ReturnPosition", "0.5"
    MXF_Default "ReturnInsetPx", "24"
    MXF_Default "ReturnDetectAfterMs", "700"
End Sub

Private Sub MXF_Default(ByVal Key As String, ByVal Value As String)
    Dim s As String
    s = GetSetting(APP_NAME, APP_SECTION, Key, vbNullString)
    If Len(s) = 0 Then SaveSetting APP_NAME, APP_SECTION, Key, Value
End Sub

Private Function MXF_CfgText(ByVal Key As String, ByVal DefaultValue As String) As String
    MXF_CfgText = GetSetting(APP_NAME, APP_SECTION, Key, DefaultValue)
End Function

Private Function MXF_CfgLong(ByVal Key As String, ByVal DefaultValue As Long) As Long
    Dim s As String
    s = GetSetting(APP_NAME, APP_SECTION, Key, CStr(DefaultValue))
    If IsNumeric(s) Then MXF_CfgLong = CLng(s) Else MXF_CfgLong = DefaultValue
End Function

Private Function MXF_CfgDouble(ByVal Key As String, ByVal DefaultValue As Double) As Double
    Dim s As String
    s = GetSetting(APP_NAME, APP_SECTION, Key, CStr(DefaultValue))
    If IsNumeric(s) Then MXF_CfgDouble = CDbl(s) Else MXF_CfgDouble = DefaultValue
End Function

Private Function MXF_Q(ByVal s As String) As String
    MXF_Q = Chr$(34) & s & Chr$(34)
End Function

Private Function MXF_NowMs() As Double
    MXF_NowMs = CDbl(Timer) * 1000#
End Function

Private Function MXF_Elapsed(ByVal nowMs As Double, ByVal thenMs As Double) As Double
    If thenMs <= 0 Then
        MXF_Elapsed = 999999999#
    ElseIf nowMs >= thenMs Then
        MXF_Elapsed = nowMs - thenMs
    Else
        ' Timer() wrapped at midnight.
        MXF_Elapsed = (86400000# - thenMs) + nowMs
    End If
End Function

Private Sub MXF_Log(ByVal msg As String)
    Dim folder As String, path As String, ff As Integer
    On Error Resume Next
    folder = Environ$("LOCALAPPDATA") & "\MXReturnFlow"
    If Dir$(folder, vbDirectory) = vbNullString Then MkDir folder
    path = folder & "\flow.log"
    ff = FreeFile
    Open path For Append As #ff
    Print #ff, Format$(Now, "yyyy-mm-dd hh:nn:ss") & " " & msg
    Close #ff
End Sub

Private Function MXF_AllB64() As String
    MXF_AllB64 = MXF_B64_01() & _
        MXF_B64_02() & _
        MXF_B64_03() & _
        MXF_B64_04() & _
        MXF_B64_05() & _
        MXF_B64_06() & _
        MXF_B64_07() & _
        MXF_B64_08() & _
        MXF_B64_09()
End Function

Private Function MXF_B64_01() As String
    Dim t As String
    t = ""
    t = t & "IyBWZW5kb3JlZCBmb3IgTVggUmV0dXJuIEZMT1cuIE9yaWdpbmFsOiBtYXJjb2Nvc3RhOTcvbXhzd2l0Y2ggKHdpbmRvd3MvbXhzd2l0Y2gucHMxKQo8Iwou"
    t = t & "U1lOT1BTSVMKICAgIFN3aXRjaCBhIExvZ2l0ZWNoIEVhc3ktU3dpdGNoIGRldmljZSB0byBhbm90aGVyIGNoYW5uZWwgdmlhIEhJRCsrIENoYW5nZUhvc3Qu"
    t = t & "CgouREVTQ1JJUFRJT04KICAgIE5vIG1vZHVsZXMsIG5vIFB5dGhvbiwgbm8gYWRtaW4gcmlnaHRzLiBQL0ludm9rZXMgaGlkLmRsbCBhbmQgc2V0dXBhcGku"
    t = t & "ZGxsLAogICAgd2hpY2ggc2hpcCB3aXRoIFdpbmRvd3MuCgouRVhBTVBMRQogICAgLlxteHN3aXRjaC5wczEgLUxpc3QKICAgIC5cbXhzd2l0Y2gucHMxIC1J"
    t = t & "bmZvCiAgICAuXG14c3dpdGNoLnBzMSAtU3RhdHVzCiAgICAuXG14c3dpdGNoLnBzMSAtQ2hhbm5lbCAyCiAgICAuXG14c3dpdGNoLnBzMSAtQWxsIC1DaGFu"
    t = t & "bmVsIDIKICAgIC5cbXhzd2l0Y2gucHMxIC1Ub2dnbGUKCi5OT1RFUwogICAgSWYgdGhlIGV4ZWN1dGlvbiBwb2xpY3kgYmxvY2tzIHRoaXM6CiAgICAgICAg"
    t = t & "cG93ZXJzaGVsbCAtRXhlY3V0aW9uUG9saWN5IEJ5cGFzcyAtRmlsZSAuXG14c3dpdGNoLnBzMSAtQ2hhbm5lbCAyCiAgICBBZGQtVHlwZSB3aWxsIG5vdCB3"
    t = t & "b3JrIHVuZGVyIENvbnN0cmFpbmVkIExhbmd1YWdlIE1vZGU7IGNoZWNrIHdpdGgKICAgICAgICAkRXhlY3V0aW9uQ29udGV4dC5TZXNzaW9uU3RhdGUuTGFu"
    t = t & "Z3VhZ2VNb2RlCgogICAgQ29uZmlnIChvcHRpb25hbCksIGtleT12YWx1ZSBsaW5lcyBpbiAlTE9DQUxBUFBEQVRBJVxteHN3aXRjaFxjb25maWc6CiAgICAg"
    t = t & "ICAgY2hhbm5lbF9hPTEKICAgICAgICBjaGFubmVsX2I9MgogICAgICAgIGRldmljZT1wcmltYXJ5fGFsbAoKICAgIEV4aXQgY29kZXM6IDAgb2ssIDEgZGV2"
    t = t & "aWNlIG5vdCBmb3VuZCwgMiBiYWQgdXNhZ2UsIDMgcGFydGlhbCAtQWxsIGZhaWx1cmUuCiM+CltDbWRsZXRCaW5kaW5nKCldCnBhcmFtKAogICAgW1BhcmFt"
    t = t & "ZXRlcihQb3NpdGlvbiA9IDApXQogICAgW2ludF0kQ2hhbm5lbCwKCiAgICBbc3dpdGNoXSRJbmZvLAogICAgW3N3aXRjaF0kU3RhdHVzLAogICAgW3N3aXRj"
    t = t & "aF0kTGlzdCwKICAgIFtzd2l0Y2hdJEFsbCwKICAgIFtzd2l0Y2hdJFRvZ2dsZSwKICAgIFtzd2l0Y2hdJERyeVJ1biwKICAgIFtzd2l0Y2hdJEpzb24sCiAg"
    t = t & "ICBbc3RyaW5nXSRDb25maWcsCgogICAgIyBPbmx5IGFjdCBvbiBkZXZpY2VzIHdob3NlIHByb2R1Y3QgbmFtZSBjb250YWlucyBvbmUgb2YgdGhlc2Ugc3Vi"
    t = t & "c3RyaW5ncy4KICAgIFtWYWxpZGF0ZU5vdE51bGxPckVtcHR5KCldCiAgICBbc3RyaW5nW11dJE5hbWUsCgogICAgIyBTa2lwIHRoZSBjYWNoZWQgZGV2aWNl"
    t = t & "IHBhdGhzIGFuZCBmb3JjZSBhIGZ1bGwgcmVzY2FuLgogICAgW3N3aXRjaF0kTm9DYWNoZQopCgokRXJyb3JBY3Rpb25QcmVmZXJlbmNlID0gJ1N0b3AnCgoj"
    t = t & "IFdyaXRlLUVycm9yIHdvdWxkIHRlcm1pbmF0ZSB1bmRlciAkRXJyb3JBY3Rpb25QcmVmZXJlbmNlID0gJ1N0b3AnIGFuZCBwcmUtZW1wdAojIHRoZSBleGl0"
    t = t & "IGNvZGVzIGJlbG93LCBzbyB1c2VyLWZhY2luZyBmYWlsdXJlcyBnbyBzdHJhaWdodCB0byBzdGRlcnIgaW5zdGVhZC4KZnVuY3Rpb24gV3JpdGUtTXhFcnJv"
    t = t & "ciB7CiAgICBwYXJhbShbc3RyaW5nXSRNZXNzYWdlKQogICAgW0NvbnNvbGVdOjpFcnJvci5Xcml0ZUxpbmUoJE1lc3NhZ2UpCn0KCiRWSUQgPSAweDA0NkQK"
    t = t & "JFNXX0lEID0gMHgwQQokREVWSUNFX0lORElDRVMgPSBAKDB4RkYsIDEsIDIsIDMsIDQsIDUsIDYpCgojIEJ1aWx0IGV4cGxpY2l0bHkgc28gdGhlIGtleXMg"
    t = t & "YXJlIHVuYW1iaWd1b3VzbHkgaW50ZWdlcnMuCiRGUkFNRV9MRU4gPSBAe30KJEZSQU1FX0xFTltbaW50XTB4MTBdID0gNwokRlJBTUVfTEVOW1tpbnRdMHgx"
    t = t & "MV0gPSAyMAoKJENhY2hlRmlsZSA9IEpvaW4tUGF0aCAkZW52OkxPQ0FMQVBQREFUQSAnbXhzd2l0Y2hcZGV2aWNlLmpzb24nCiRDb25maWdGaWxlID0gSm9p"
    t = t & "bi1QYXRoICRlbnY6TE9DQUxBUFBEQVRBICdteHN3aXRjaFxjb25maWcnCgojIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0t"
    t = t & "LS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLQojIFB1cmUgcHJvdG9jb2wgaGVscGVycyAoaGFyZHdhcmUtZnJlZTsgZXhlcmNpc2VkIGJ5IHRlc3Rz"
    t = t & "KQojIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLQoKZnVuY3Rpb24g"
    t = t & "TmV3LUhpZHBwRnJhbWUgewogICAgcGFyYW0oW2ludF0kUmVwb3J0SWQsIFtpbnRdJERldklkeCwgW2ludF0kRmVhdHVyZUlkeCwgW2ludF0kRnVuY3Rpb24s"
    t = t & "CiAgICAgICAgICBbYnl0ZVtdXSRQYXJhbXMgPSBAKCkpCgogICAgJGxlbiA9ICRGUkFNRV9MRU5bW2ludF0kUmVwb3J0SWRdCiAgICBpZiAoLW5vdCAkbGVu"
    t = t & "KSB7IHRocm93ICJ1bmtub3duIHJlcG9ydCBpZCAkUmVwb3J0SWQiIH0KICAgIGlmICgkUGFyYW1zLkNvdW50IC1ndCAoJGxlbiAtIDQpKSB7IHRocm93ICJm"
    t = t & "cmFtZSB0b28gbG9uZyBmb3IgcmVwb3J0ICRSZXBvcnRJZCIgfQoKICAgICRmcmFtZSA9IE5ldy1PYmplY3QgYnl0ZVtdICRsZW4KICAgICRmcmFtZVswXSA9"
    t = t & "ICRSZXBvcnRJZAogICAgJGZyYW1lWzFdID0gJERldklkeAogICAgJGZyYW1lWzJdID0gJEZlYXR1cmVJZHgKICAgICRmcmFtZVszXSA9ICgkRnVuY3Rpb24g"
    t = t & "LXNobCA0KSAtYm9yICRTV19JRAogICAgZm9yICgkaSA9IDA7ICRpIC1sdCAkUGFyYW1zLkNvdW50OyAkaSsrKSB7ICRmcmFtZVs0ICsgJGldID0gJFBhcmFt"
    t = t & "c1skaV0gfQogICAgcmV0dXJuICwkZnJhbWUKfQoKZnVuY3Rpb24gR2V0LVJlcGx5VmVyZGljdCB7CiAgICBwYXJhbShbYnl0ZVtdXSRSZXBseSwgW2ludF0k"
    t = t & "RGV2SWR4LCBbaW50XSRGZWF0dXJlSWR4LCBbaW50XSRGdW5jdGlvbiA9IDAsIFtpbnRdJFN3SWQgPSAkU1dfSUQpCgogICAgaWYgKCRudWxsIC1lcSAkUmVw"
    t = t & "bHkgLW9yICRSZXBseS5MZW5ndGggLWx0IDUpIHsgcmV0dXJuICd0b29fc2hvcnQnIH0KICAgIGlmICgkUmVwbHlbMF0gLW5vdGluIEAoMHgxMCwgMHgxMSkp"
    t = t & "IHsgcmV0dXJuICdpZ25vcmUnIH0KICAgIGlmICgkUmVwbHkuTGVuZ3RoIC1sdCAkRlJBTUVfTEVOW1tpbnRdJFJlcGx5WzBdXSkgeyByZXR1cm4gJ3Rvb19z"
    t = t & "aG9ydCcgfQogICAgaWYgKCRSZXBseVsxXSAtbmUgJERldklkeCAtYW5kIC1ub3QgKCREZXZJZHggLWVxIDI1NSAtYW5kICRSZXBseVsxXSAtZXEgMCkpIHsg"
    t = t & "cmV0dXJuICdpZ25vcmUnIH0KICAgICRoZWFkZXIgPSAoJEZ1bmN0aW9uIC1zaGwgNCkgLWJvciAkU3dJZAogICAgaWYgKCRSZXBseVsyXSAtZXEgMHhGRiAt"
    t = t & "b3IgJFJlcGx5WzJdIC1lcSAweDhGKSB7CiAgICAgICAgaWYgKCRSZXBseS5MZW5ndGggLWx0IDYpIHsgcmV0dXJuICd0b29fc2hvcnQnIH0KICAgICAgICBp"
    t = t & "ZiAoJFJlcGx5WzNdIC1lcSAkRmVhdHVyZUlkeCAtYW5kICRSZXBseVs0XSAtZXEgJGhlYWRlcikgeyByZXR1cm4gJ2Vycm9yJyB9CiAgICAgICAgcmV0dXJu"
    t = t & "ICdpZ25vcmUnCiAgICB9CiAgICBpZiAoJFJlcGx5WzJdIC1uZSAkRmVhdHVyZUlkeCkgeyByZXR1cm4gJ2lnbm9yZScgfSAgICAgICAgICAgICAgICAgIyBu"
    t = t & "b3RpZmljYXRpb24KICAgIGlmICgkUmVwbHlbM10gLW5lICRoZWFkZXIpIHsgcmV0dXJuICdpZ25vcmUnIH0KICAgIHJldHVybiAnbWF0Y2gnCn0KCmZ1bmN0"
    t = t & "aW9uIEdldC1NeENvbmZpZyB7CiAgICBwYXJhbShbc3RyaW5nXSRQYXRoID0gJENvbmZpZ0ZpbGUpCgogICAgJGNmZyA9IFtvcmRlcmVkXUB7IGNoYW5uZWxf"
    t = t & "YSA9IDE7IGNoYW5uZWxfYiA9IDI7IGRldmljZSA9ICdwcmltYXJ5JzsgbmFtZSA9ICcnIH0KICAgIGlmICgkUGF0aCAtYW5kIChUZXN0LVBhdGggJFBhdGgp"
    t = t & "KSB7CiAgICAgICAgZm9yZWFjaCAoJGxpbmUgaW4gR2V0LUNvbnRlbnQgJFBhdGgpIHsKICAgICAgICAgICAgJGNsZWFuID0gKCRsaW5lIC1zcGxpdCAnIycs"
    t = t & "IDIpWzBdLlRyaW0oKQogICAgICAgICAgICBpZiAoLW5vdCAkY2xlYW4pIHsgY29udGludWUgfQogICAgICAgICAgICAkcGFydHMgPSAkY2xlYW4gLXNwbGl0"
    MXF_B64_01 = t
End Function

Private Function MXF_B64_02() As String
    Dim t As String
    t = ""
    t = t & "ICc9JywgMgogICAgICAgICAgICBpZiAoJHBhcnRzLkNvdW50IC1sdCAyKSB7IGNvbnRpbnVlIH0KICAgICAgICAgICAgJGtleSA9ICRwYXJ0c1swXS5Ucmlt"
    t = t & "KCkuVG9Mb3dlckludmFyaWFudCgpCiAgICAgICAgICAgICR2YWx1ZSA9ICRwYXJ0c1sxXS5UcmltKCkKICAgICAgICAgICAgc3dpdGNoICgka2V5KSB7CiAg"
    t = t & "ICAgICAgICAgICAgICAnY2hhbm5lbF9hJyB7ICRjZmcuY2hhbm5lbF9hID0gaWYgKCR2YWx1ZSAtbWF0Y2ggJ15bMTIzXSQnKSB7IFtpbnRdJHZhbHVlIH0g"
    t = t & "ZWxzZSB7IDAgfSB9CiAgICAgICAgICAgICAgICAnY2hhbm5lbF9iJyB7ICRjZmcuY2hhbm5lbF9iID0gaWYgKCR2YWx1ZSAtbWF0Y2ggJ15bMTIzXSQnKSB7"
    t = t & "IFtpbnRdJHZhbHVlIH0gZWxzZSB7IDAgfSB9CiAgICAgICAgICAgICAgICAnZGV2aWNlJyAgICB7ICRjZmcuZGV2aWNlID0gJHZhbHVlLlRvTG93ZXJJbnZh"
    t = t & "cmlhbnQoKSB9CiAgICAgICAgICAgICAgICAnbmFtZScgICAgICB7ICRjZmcubmFtZSA9ICR2YWx1ZSB9CiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9"
    t = t & "CiAgICByZXR1cm4gW3BzY3VzdG9tb2JqZWN0XSRjZmcKfQoKZnVuY3Rpb24gVGVzdC1NeENvbmZpZyB7CiAgICBwYXJhbSgkQ29uZmlnKQogICAgJHByb2Js"
    t = t & "ZW1zID0gQCgpCiAgICBpZiAoJENvbmZpZy5jaGFubmVsX2EgLW5vdGluIEAoMSwyLDMpIC1vciAkQ29uZmlnLmNoYW5uZWxfYiAtbm90aW4gQCgxLDIsMykp"
    t = t & "IHsKICAgICAgICAkcHJvYmxlbXMgKz0gJ2NoYW5uZWxfYSBhbmQgY2hhbm5lbF9iIG11c3QgYmUgMSwgMiBvciAzJwogICAgfQogICAgaWYgKCRDb25maWcu"
    t = t & "Y2hhbm5lbF9hIC1lcSAkQ29uZmlnLmNoYW5uZWxfYikgewogICAgICAgICRwcm9ibGVtcyArPSAnY2hhbm5lbF9hIGFuZCBjaGFubmVsX2IgbXVzdCBkaWZm"
    t = t & "ZXInCiAgICB9CiAgICBpZiAoJENvbmZpZy5kZXZpY2UgLW5vdGluIEAoJ3ByaW1hcnknLCAnYWxsJykpIHsKICAgICAgICAkcHJvYmxlbXMgKz0gJ2Rldmlj"
    t = t & "ZSBtdXN0IGJlIHByaW1hcnkgb3IgYWxsJwogICAgfQogICAgcmV0dXJuICRwcm9ibGVtcwp9CgpmdW5jdGlvbiBGb3JtYXQtTXhIZXggewogICAgcGFyYW0o"
    t = t & "W2J5dGVbXV0kQnl0ZXMpCiAgICAoJEJ5dGVzIHwgRm9yRWFjaC1PYmplY3QgeyAkXy5Ub1N0cmluZygneDInKSB9KSAtam9pbiAnJwp9CgojIERldmljZSBl"
    t = t & "bnVtZXJhdGlvbiBhbmQgdGhlIEMjIGhlbHBlciBhcmUgb25seSBuZWVkZWQgd2hlbiB0aGUgc2NyaXB0IGFjdHVhbGx5CiMgcnVuczsga2VlcGluZyB0aGVt"
    t = t & "IGJlaGluZCB0aGlzIGd1YXJkIGxldHMgdGVzdHMgZG90LXNvdXJjZSB0aGUgcHVyZSBoZWxwZXJzLgppZiAoLW5vdCAkZW52Ok1YU1dJVENIX1RFU1QpIHsK"
    t = t & "CkFkZC1UeXBlIC1UeXBlRGVmaW5pdGlvbiBAJwp1c2luZyBTeXN0ZW07CnVzaW5nIFN5c3RlbS5Db2xsZWN0aW9ucy5HZW5lcmljOwp1c2luZyBTeXN0ZW0u"
    t = t & "UnVudGltZS5JbnRlcm9wU2VydmljZXM7CnVzaW5nIE1pY3Jvc29mdC5XaW4zMi5TYWZlSGFuZGxlczsKdXNpbmcgU3lzdGVtLklPOwp1c2luZyBTeXN0ZW0u"
    t = t & "VGhyZWFkaW5nLlRhc2tzOwoKcHVibGljIGNsYXNzIEhpZCB7CiAgICBjb25zdCBpbnQgIERJR0NGX1BSRVNFTlQgPSAweDAyLCBESUdDRl9ERVZJQ0VJTlRF"
    t = t & "UkZBQ0UgPSAweDEwOwogICAgY29uc3QgdWludCBHRU5FUklDX1JFQUQgPSAweDgwMDAwMDAwLCBHRU5FUklDX1dSSVRFID0gMHg0MDAwMDAwMDsKICAgIGNv"
    t = t & "bnN0IHVpbnQgRklMRV9TSEFSRV9SVyA9IDB4MDMsIE9QRU5fRVhJU1RJTkcgPSAzLCBGSUxFX0ZMQUdfT1ZFUkxBUFBFRCA9IDB4NDAwMDAwMDA7CiAgICBj"
    t = t & "b25zdCBpbnQgIEhJRFBfU1RBVFVTX1NVQ0NFU1MgPSAweDAwMTEwMDAwOwoKICAgIFtTdHJ1Y3RMYXlvdXQoTGF5b3V0S2luZC5TZXF1ZW50aWFsKV0KICAg"
    t = t & "IHN0cnVjdCBHVUlEIHsgcHVibGljIHVpbnQgYTsgcHVibGljIHVzaG9ydCBiLCBjOyBbTWFyc2hhbEFzKFVubWFuYWdlZFR5cGUuQnlWYWxBcnJheSwgU2l6"
    t = t & "ZUNvbnN0PTgpXSBwdWJsaWMgYnl0ZVtdIGQ7IH0KCiAgICBbU3RydWN0TGF5b3V0KExheW91dEtpbmQuU2VxdWVudGlhbCldCiAgICBzdHJ1Y3QgU1BfREVW"
    t = t & "SUNFX0lOVEVSRkFDRV9EQVRBIHsgcHVibGljIGludCBjYlNpemU7IHB1YmxpYyBHVUlEIGd1aWQ7IHB1YmxpYyBpbnQgZmxhZ3M7IHB1YmxpYyBJbnRQdHIg"
    t = t & "cmVzZXJ2ZWQ7IH0KCiAgICBbU3RydWN0TGF5b3V0KExheW91dEtpbmQuU2VxdWVudGlhbCwgQ2hhclNldD1DaGFyU2V0LlVuaWNvZGUpXQogICAgc3RydWN0"
    t = t & "IFNQX0RFVklDRV9JTlRFUkZBQ0VfREVUQUlMX0RBVEFfVyB7CiAgICAgICAgcHVibGljIGludCBjYlNpemU7CiAgICAgICAgW01hcnNoYWxBcyhVbm1hbmFn"
    t = t & "ZWRUeXBlLkJ5VmFsVFN0ciwgU2l6ZUNvbnN0PTUxMildIHB1YmxpYyBzdHJpbmcgRGV2aWNlUGF0aDsKICAgIH0KCiAgICBbU3RydWN0TGF5b3V0KExheW91"
    t = t & "dEtpbmQuU2VxdWVudGlhbCldCiAgICBzdHJ1Y3QgSElERF9BVFRSSUJVVEVTIHsgcHVibGljIGludCBTaXplOyBwdWJsaWMgdXNob3J0IFZpZCwgUGlkLCBW"
    t = t & "ZXI7IH0KCiAgICBbU3RydWN0TGF5b3V0KExheW91dEtpbmQuU2VxdWVudGlhbCldCiAgICBzdHJ1Y3QgSElEUF9DQVBTIHsKICAgICAgICBwdWJsaWMgdXNo"
    t = t & "b3J0IFVzYWdlLCBVc2FnZVBhZ2UsIElucHV0TGVuLCBPdXRwdXRMZW4sIEZlYXR1cmVMZW47CiAgICAgICAgW01hcnNoYWxBcyhVbm1hbmFnZWRUeXBlLkJ5"
    t = t & "VmFsQXJyYXksIFNpemVDb25zdD0xNyldIHB1YmxpYyB1c2hvcnRbXSBSZXNlcnZlZDsKICAgICAgICBwdWJsaWMgdXNob3J0IExpbmtOb2RlcywgSW5CdG4s"
    t = t & "IEluVmFsLCBJbklkeCwgT3V0QnRuLCBPdXRWYWwsIE91dElkeCwgRnRCdG4sIEZ0VmFsLCBGdElkeDsKICAgIH0KCiAgICBbRGxsSW1wb3J0KCJoaWQuZGxs"
    t = t & "IildIHN0YXRpYyBleHRlcm4gdm9pZCBIaWREX0dldEhpZEd1aWQocmVmIEdVSUQgZyk7CiAgICBbRGxsSW1wb3J0KCJoaWQuZGxsIildIHN0YXRpYyBleHRl"
    t = t & "cm4gYm9vbCBIaWREX0dldEF0dHJpYnV0ZXMoU2FmZUZpbGVIYW5kbGUgaCwgcmVmIEhJRERfQVRUUklCVVRFUyBhKTsKICAgIFtEbGxJbXBvcnQoImhpZC5k"
    t = t & "bGwiKV0gc3RhdGljIGV4dGVybiBib29sIEhpZERfR2V0UHJlcGFyc2VkRGF0YShTYWZlRmlsZUhhbmRsZSBoLCBvdXQgSW50UHRyIHBwKTsKICAgIFtEbGxJ"
    t = t & "bXBvcnQoImhpZC5kbGwiKV0gc3RhdGljIGV4dGVybiBib29sIEhpZERfRnJlZVByZXBhcnNlZERhdGEoSW50UHRyIHBwKTsKICAgIFtEbGxJbXBvcnQoImhp"
    t = t & "ZC5kbGwiKV0gc3RhdGljIGV4dGVybiBpbnQgIEhpZFBfR2V0Q2FwcyhJbnRQdHIgcHAsIHJlZiBISURQX0NBUFMgYyk7CiAgICBbRGxsSW1wb3J0KCJoaWQu"
    t = t & "ZGxsIiwgQ2hhclNldD1DaGFyU2V0LlVuaWNvZGUpXSBzdGF0aWMgZXh0ZXJuIGJvb2wgSGlkRF9HZXRQcm9kdWN0U3RyaW5nKFNhZmVGaWxlSGFuZGxlIGgs"
    t = t & "IGNoYXJbXSBidWYsIGludCBsZW4pOwoKICAgIFtEbGxJbXBvcnQoInNldHVwYXBpLmRsbCIsIENoYXJTZXQ9Q2hhclNldC5Vbmljb2RlKV0KICAgIHN0YXRp"
    t = t & "YyBleHRlcm4gSW50UHRyIFNldHVwRGlHZXRDbGFzc0RldnNXKHJlZiBHVUlEIGcsIEludFB0ciBlLCBJbnRQdHIgdywgaW50IGYpOwogICAgW0RsbEltcG9y"
    t = t & "dCgic2V0dXBhcGkuZGxsIildCiAgICBzdGF0aWMgZXh0ZXJuIGJvb2wgU2V0dXBEaUVudW1EZXZpY2VJbnRlcmZhY2VzKEludFB0ciBzLCBJbnRQdHIgZCwg"
    t = t & "cmVmIEdVSUQgZywgaW50IGksIHJlZiBTUF9ERVZJQ0VfSU5URVJGQUNFX0RBVEEgYSk7CiAgICBbRGxsSW1wb3J0KCJzZXR1cGFwaS5kbGwiLCBDaGFyU2V0"
    t = t & "PUNoYXJTZXQuVW5pY29kZSldCiAgICBzdGF0aWMgZXh0ZXJuIGJvb2wgU2V0dXBEaUdldERldmljZUludGVyZmFjZURldGFpbFcoSW50UHRyIHMsIHJlZiBT"
    t = t & "UF9ERVZJQ0VfSU5URVJGQUNFX0RBVEEgYSwgcmVmIFNQX0RFVklDRV9JTlRFUkZBQ0VfREVUQUlMX0RBVEFfVyBkLCBpbnQgc2l6ZSwgSW50UHRyIHJlcSwg"
    t = t & "SW50UHRyIGluZm8pOwogICAgW0RsbEltcG9ydCgic2V0dXBhcGkuZGxsIildCiAgICBzdGF0aWMgZXh0ZXJuIGJvb2wgU2V0dXBEaURlc3Ryb3lEZXZpY2VJ"
    MXF_B64_02 = t
End Function

Private Function MXF_B64_03() As String
    Dim t As String
    t = ""
    t = t & "bmZvTGlzdChJbnRQdHIgcyk7CgogICAgW0RsbEltcG9ydCgia2VybmVsMzIuZGxsIiwgQ2hhclNldD1DaGFyU2V0LlVuaWNvZGUsIFNldExhc3RFcnJvcj10"
    t = t & "cnVlKV0KICAgIHN0YXRpYyBleHRlcm4gU2FmZUZpbGVIYW5kbGUgQ3JlYXRlRmlsZVcoc3RyaW5nIHBhdGgsIHVpbnQgYWNjZXNzLCB1aW50IHNoYXJlLCBJ"
    t = t & "bnRQdHIgc2VjLCB1aW50IGRpc3AsIHVpbnQgZmxhZ3MsIEludFB0ciB0bXBsKTsKICAgIC8vIENhbmNlbElvRXgsIG5vdCBDYW5jZWxJbzogdGhlIGFzeW5j"
    t = t & "IHJlYWQgbWF5IGhhdmUgYmVlbiBpc3N1ZWQgb24gYQogICAgLy8gdGhyZWFkLXBvb2wgdGhyZWFkLCBhbmQgQ2FuY2VsSW8gb25seSBjYW5jZWxzIHRoZSBj"
    t = t & "YWxsaW5nIHRocmVhZCdzIEkvTy4KICAgIFtEbGxJbXBvcnQoImtlcm5lbDMyLmRsbCIsIFNldExhc3RFcnJvcj10cnVlKV0KICAgIHN0YXRpYyBleHRlcm4g"
    t = t & "Ym9vbCBDYW5jZWxJb0V4KFNhZmVGaWxlSGFuZGxlIGgsIEludFB0ciBvdmVybGFwcGVkKTsKCiAgICBwdWJsaWMgY2xhc3MgSWZhY2UgewogICAgICAgIHB1"
    t = t & "YmxpYyBzdHJpbmcgUGF0aCwgUHJvZHVjdDsKICAgICAgICBwdWJsaWMgdXNob3J0IFVzYWdlUGFnZSwgVXNhZ2UsIFBpZCwgSW5MZW4sIE91dExlbjsKICAg"
    t = t & "IH0KCiAgICBwdWJsaWMgc3RhdGljIExpc3Q8SWZhY2U+IEVudW1lcmF0ZSh1c2hvcnQgdmlkKSB7CiAgICAgICAgdmFyIHJlc3VsdCA9IG5ldyBMaXN0PElm"
    t = t & "YWNlPigpOwogICAgICAgIHZhciBndWlkID0gbmV3IEdVSUQoKTsKICAgICAgICBIaWREX0dldEhpZEd1aWQocmVmIGd1aWQpOwogICAgICAgIEludFB0ciBz"
    t = t & "ZXQgPSBTZXR1cERpR2V0Q2xhc3NEZXZzVyhyZWYgZ3VpZCwgSW50UHRyLlplcm8sIEludFB0ci5aZXJvLAogICAgICAgICAgICAgICAgICAgICAgICAgICAg"
    t = t & "ICAgICAgICAgICAgICBESUdDRl9QUkVTRU5UIHwgRElHQ0ZfREVWSUNFSU5URVJGQUNFKTsKICAgICAgICBpZiAoc2V0ID09IG5ldyBJbnRQdHIoLTEpKSBy"
    t = t & "ZXR1cm4gcmVzdWx0OyAgIC8vIElOVkFMSURfSEFORExFX1ZBTFVFCiAgICAgICAgdHJ5IHsKICAgICAgICAgICAgdmFyIGRpZCA9IG5ldyBTUF9ERVZJQ0Vf"
    t = t & "SU5URVJGQUNFX0RBVEEoKTsKICAgICAgICAgICAgZGlkLmNiU2l6ZSA9IE1hcnNoYWwuU2l6ZU9mKGRpZCk7CiAgICAgICAgICAgIGZvciAoaW50IGkgPSAw"
    t = t & "OyBTZXR1cERpRW51bURldmljZUludGVyZmFjZXMoc2V0LCBJbnRQdHIuWmVybywgcmVmIGd1aWQsIGksIHJlZiBkaWQpOyBpKyspIHsKICAgICAgICAgICAg"
    t = t & "ICAgIHZhciBkZXRhaWwgPSBuZXcgU1BfREVWSUNFX0lOVEVSRkFDRV9ERVRBSUxfREFUQV9XKCk7CiAgICAgICAgICAgICAgICAvLyBEb2N1bWVudGVkIGZp"
    t = t & "eGVkIHZhbHVlLCBOT1QgU2l6ZU9mKCk6IDggb24geDY0LCA2IG9uIHg4Ni4KICAgICAgICAgICAgICAgIGRldGFpbC5jYlNpemUgPSBJbnRQdHIuU2l6ZSA9"
    t = t & "PSA4ID8gOCA6IDY7CiAgICAgICAgICAgICAgICBpZiAoIVNldHVwRGlHZXREZXZpY2VJbnRlcmZhY2VEZXRhaWxXKHNldCwgcmVmIGRpZCwgcmVmIGRldGFp"
    t = t & "bCwKICAgICAgICAgICAgICAgICAgICAgICAgTWFyc2hhbC5TaXplT2YoZGV0YWlsKSwgSW50UHRyLlplcm8sIEludFB0ci5aZXJvKSkgY29udGludWU7Cgog"
    t = t & "ICAgICAgICAgICAgICAgdXNpbmcgKHZhciBoID0gQ3JlYXRlRmlsZVcoZGV0YWlsLkRldmljZVBhdGgsIDAsIEZJTEVfU0hBUkVfUlcsCiAgICAgICAgICAg"
    t = t & "ICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBJbnRQdHIuWmVybywgT1BFTl9FWElTVElORywgMCwgSW50UHRyLlplcm8pKSB7CiAgICAgICAgICAg"
    t = t & "ICAgICAgICAgaWYgKGguSXNJbnZhbGlkKSBjb250aW51ZTsKICAgICAgICAgICAgICAgICAgICB2YXIgYXR0cnMgPSBuZXcgSElERF9BVFRSSUJVVEVTKCk7"
    t = t & "IGF0dHJzLlNpemUgPSBNYXJzaGFsLlNpemVPZihhdHRycyk7CiAgICAgICAgICAgICAgICAgICAgaWYgKCFIaWREX0dldEF0dHJpYnV0ZXMoaCwgcmVmIGF0"
    t = t & "dHJzKSB8fCBhdHRycy5WaWQgIT0gdmlkKSBjb250aW51ZTsKCiAgICAgICAgICAgICAgICAgICAgSW50UHRyIHBwOwogICAgICAgICAgICAgICAgICAgIGlm"
    t = t & "ICghSGlkRF9HZXRQcmVwYXJzZWREYXRhKGgsIG91dCBwcCkpIGNvbnRpbnVlOwogICAgICAgICAgICAgICAgICAgIHZhciBjYXBzID0gbmV3IEhJRFBfQ0FQ"
    t = t & "UygpOwogICAgICAgICAgICAgICAgICAgIGludCBvayA9IEhpZFBfR2V0Q2FwcyhwcCwgcmVmIGNhcHMpOwogICAgICAgICAgICAgICAgICAgIEhpZERfRnJl"
    t = t & "ZVByZXBhcnNlZERhdGEocHApOwogICAgICAgICAgICAgICAgICAgIGlmIChvayAhPSBISURQX1NUQVRVU19TVUNDRVNTKSBjb250aW51ZTsKICAgICAgICAg"
    t = t & "ICAgICAgICAgICBpZiAoY2Fwcy5Vc2FnZVBhZ2UgPCAweEZGMDApIGNvbnRpbnVlOyAgIC8vIHZlbmRvciBjb2xsZWN0aW9ucyBvbmx5CiAgICAgICAgICAg"
    t = t & "ICAgICAgICAgaWYgKGNhcHMuT3V0cHV0TGVuID09IDApIGNvbnRpbnVlOyAgICAgICAvLyBjYW5ub3Qgc2VuZCBhbnl0aGluZyBoZXJlCgogICAgICAgICAg"
    t = t & "ICAgICAgICAgIHZhciBuYW1lID0gbmV3IGNoYXJbMTI4XTsKICAgICAgICAgICAgICAgICAgICBIaWREX0dldFByb2R1Y3RTdHJpbmcoaCwgbmFtZSwgMjU2"
    t = t & "KTsKICAgICAgICAgICAgICAgICAgICByZXN1bHQuQWRkKG5ldyBJZmFjZSB7CiAgICAgICAgICAgICAgICAgICAgICAgIFBhdGggPSBkZXRhaWwuRGV2aWNl"
    t = t & "UGF0aCwgUHJvZHVjdCA9IG5ldyBzdHJpbmcobmFtZSkuVHJpbUVuZCgnXDAnKSwKICAgICAgICAgICAgICAgICAgICAgICAgVXNhZ2VQYWdlID0gY2Fwcy5V"
    t = t & "c2FnZVBhZ2UsIFVzYWdlID0gY2Fwcy5Vc2FnZSwgUGlkID0gYXR0cnMuUGlkLAogICAgICAgICAgICAgICAgICAgICAgICBJbkxlbiA9IGNhcHMuSW5wdXRM"
    t = t & "ZW4sIE91dExlbiA9IGNhcHMuT3V0cHV0TGVuIH0pOwogICAgICAgICAgICAgICAgfQogICAgICAgICAgICB9CiAgICAgICAgfSBmaW5hbGx5IHsgU2V0dXBE"
    t = t & "aURlc3Ryb3lEZXZpY2VJbmZvTGlzdChzZXQpOyB9CiAgICAgICAgcmV0dXJuIHJlc3VsdDsKICAgIH0KCiAgICBGaWxlU3RyZWFtIHN0cmVhbTsKICAgIFNh"
    t = t & "ZmVGaWxlSGFuZGxlIGhhbmRsZTsKICAgIGludCBpbkxlbiwgb3V0TGVuOwogICAgcHVibGljIGludCBPdXRMZW4geyBnZXQgeyByZXR1cm4gb3V0TGVuOyB9"
    t = t & "IH0KCiAgICBwdWJsaWMgSGlkKElmYWNlIGlmYWNlKSB7CiAgICAgICAgdmFyIGggPSBDcmVhdGVGaWxlVyhpZmFjZS5QYXRoLCBHRU5FUklDX1JFQUQgfCBH"
    t = t & "RU5FUklDX1dSSVRFLCBGSUxFX1NIQVJFX1JXLAogICAgICAgICAgICAgICAgICAgICAgICAgICAgSW50UHRyLlplcm8sIE9QRU5fRVhJU1RJTkcsIEZJTEVf"
    t = t & "RkxBR19PVkVSTEFQUEVELCBJbnRQdHIuWmVybyk7CiAgICAgICAgaWYgKGguSXNJbnZhbGlkKSB0aHJvdyBuZXcgSU9FeGNlcHRpb24oImNhbm5vdCBvcGVu"
    t = t & "ICIgKyBpZmFjZS5QYXRoKTsKICAgICAgICBpbkxlbiA9IGlmYWNlLkluTGVuOyBvdXRMZW4gPSBpZmFjZS5PdXRMZW47CiAgICAgICAgLy8gYnVmZmVyU2l6"
    t = t & "ZSAxIGRpc2FibGVzIEZpbGVTdHJlYW0ncyBpbnRlcm5hbCBidWZmZXJpbmcuIERldmljZSBJL08gbXVzdAogICAgICAgIC8vIHJlYWNoIHRoZSBkcml2ZXIg"
    t = t & "YXMgd2hvbGUgcmVwb3J0cywgbmV2ZXIgY29hbGVzY2VkIG9yIHNwbGl0LgogICAgICAgIHRyeSB7CiAgICAgICAgICAgIHN0cmVhbSA9IG5ldyBGaWxlU3Ry"
    t = t & "ZWFtKGgsIEZpbGVBY2Nlc3MuUmVhZFdyaXRlLCAxLCB0cnVlKTsKICAgICAgICB9IGNhdGNoIHsKICAgICAgICAgICAgaC5EaXNwb3NlKCk7CiAgICAgICAg"
    t = t & "ICAgIHRocm93OwogICAgICAgIH0KICAgICAgICBoYW5kbGUgPSBoOwogICAgfQoKICAgIC8qCiAgICAgKiBSZXR1cm5zIGZhbHNlIHJhdGhlciB0aGFuIHRo"
    t = t & "cm93aW5nLiBEaXNjb3ZlcnkgbmVjZXNzYXJpbHkgcHJvYmVzCiAgICAgKiBjb2xsZWN0aW9ucyB0aGF0IHdpbGwgcmVqZWN0IHRoZSBmcmFtZSAtIGEgY29s"
    t = t & "bGVjdGlvbiBvbmx5IGFjY2VwdHMgcmVwb3J0CiAgICAgKiBJRHMgZGVjbGFyZWQgaW4gaXRzIG93biBkZXNjcmlwdG9yLCBzbyBzZW5kaW5nIGEgMHgxMCBm"
    MXF_B64_03 = t
End Function

Private Function MXF_B64_04() As String
    Dim t As String
    t = ""
    t = t & "cmFtZSB0byBhCiAgICAgKiBsb25nLXJlcG9ydCBjb2xsZWN0aW9uIGZhaWxzIHdpdGggRVJST1JfSU5WQUxJRF9QQVJBTUVURVIuIFRoYXQgaXMgYQogICAg"
    t = t & "ICogbm9ybWFsLCBleHBlY3RlZCBvdXRjb21lIG9mIHByb2JpbmcsIG5vdCBhbiBlcnJvciB3b3J0aCBhYm9ydGluZyBvbi4KICAgICAqLwogICAgcHVibGlj"
    t = t & "IGJvb2wgV3JpdGUoYnl0ZVtdIGZyYW1lKSB7CiAgICAgICAgaWYgKG91dExlbiA8PSAwKSByZXR1cm4gZmFsc2U7CiAgICAgICAgdmFyIGJ1ZiA9IG5ldyBi"
    t = t & "eXRlW291dExlbl07CiAgICAgICAgQXJyYXkuQ29weShmcmFtZSwgYnVmLCBNYXRoLk1pbihmcmFtZS5MZW5ndGgsIG91dExlbikpOwogICAgICAgIHRyeSB7"
    t = t & "CiAgICAgICAgICAgIHN0cmVhbS5Xcml0ZShidWYsIDAsIG91dExlbik7CiAgICAgICAgICAgIHN0cmVhbS5GbHVzaCgpOwogICAgICAgICAgICByZXR1cm4g"
    t = t & "dHJ1ZTsKICAgICAgICB9IGNhdGNoIChJT0V4Y2VwdGlvbikgICAgICAgICAgeyByZXR1cm4gZmFsc2U7IH0KICAgICAgICAgIGNhdGNoIChVbmF1dGhvcml6"
    t = t & "ZWRBY2Nlc3NFeGNlcHRpb24pIHsgcmV0dXJuIGZhbHNlOyB9CiAgICB9CgogICAgLy8gQSBzeW5jaHJvbm91cyByZWFkIG9uIGEgZGV2aWNlIHdpdGggbm90"
    t = t & "aGluZyB0byBzYXkgbmV2ZXIgcmV0dXJucywgc28gdGhpcwogICAgLy8gcmFjZXMgdGhlIHJlYWQgYWdhaW5zdCBhIHRpbWVyIGFuZCBjYW5jZWxzIGl0IG9u"
    t = t & "IHRpbWVvdXQuIFRoZSBhYm9ydGVkIHJlYWQKICAgIC8vIG11c3QgYmUgZHJhaW5lZCBiZWZvcmUgcmV0dXJuaW5nOiBGaWxlU3RyZWFtIHNlcmlhbGlzZXMg"
    t = t & "YXN5bmMgb3BlcmF0aW9ucywKICAgIC8vIHNvIGEgc3RpbGwtcGVuZGluZyByZWFkIHdvdWxkIGJsb2NrIGV2ZXJ5IGxhdGVyIFJlYWRBc3luYyB1bnRpbCBp"
    t = t & "dCBsYW5kcy4KICAgIHB1YmxpYyBieXRlW10gUmVhZChpbnQgdGltZW91dE1zKSB7CiAgICAgICAgaWYgKGluTGVuIDw9IDApIHJldHVybiBuZXcgYnl0ZVsw"
    t = t & "XTsKICAgICAgICB2YXIgYnVmID0gbmV3IGJ5dGVbaW5MZW5dOwogICAgICAgIHRyeSB7CiAgICAgICAgICAgIHZhciB0YXNrID0gc3RyZWFtLlJlYWRBc3lu"
    t = t & "YyhidWYsIDAsIGluTGVuKTsKICAgICAgICAgICAgaWYgKFRhc2suV2FpdEFueShuZXcgVGFza1tdIHsgdGFzayB9LCB0aW1lb3V0TXMpICE9IDApIHsKICAg"
    t = t & "ICAgICAgICAgICAgIENhbmNlbElvRXgoaGFuZGxlLCBJbnRQdHIuWmVybyk7CiAgICAgICAgICAgICAgICB0cnkgeyB0YXNrLldhaXQoMTAwMCk7IH0gY2F0"
    t = t & "Y2ggKEV4Y2VwdGlvbikgeyB9CiAgICAgICAgICAgICAgICByZXR1cm4gbmV3IGJ5dGVbMF07CiAgICAgICAgICAgIH0KICAgICAgICAgICAgaWYgKHRhc2su"
    t = t & "SXNGYXVsdGVkIHx8IHRhc2suUmVzdWx0IDw9IDApIHJldHVybiBuZXcgYnl0ZVswXTsKICAgICAgICAgICAgdmFyIG91dGIgPSBuZXcgYnl0ZVt0YXNrLlJl"
    t = t & "c3VsdF07CiAgICAgICAgICAgIEFycmF5LkNvcHkoYnVmLCBvdXRiLCB0YXNrLlJlc3VsdCk7CiAgICAgICAgICAgIHJldHVybiBvdXRiOwogICAgICAgIH0g"
    t = t & "Y2F0Y2ggKEV4Y2VwdGlvbikgeyByZXR1cm4gbmV3IGJ5dGVbMF07IH0KICAgIH0KCiAgICBwdWJsaWMgdm9pZCBDbG9zZSgpIHsgdHJ5IHsgc3RyZWFtLkRp"
    t = t & "c3Bvc2UoKTsgfSBjYXRjaCB7fSB9Cn0KJ0AKfSAjIE5hdGl2ZSBBUEkgZGVmaW5pdGlvbnMgYXJlIG5vdCBuZWVkZWQgYnkgaGFyZHdhcmUtZnJlZSB0ZXN0"
    t = t & "cy4KCmZ1bmN0aW9uIEludm9rZS1IaWRwcENhbGwgewogICAgcGFyYW0oJERldiwgW2ludF0kUmVwb3J0SWQsIFtpbnRdJERldklkeCwgW2ludF0kRmVhdHVy"
    t = t & "ZUlkeCwgW2ludF0kRnVuY3Rpb24sCiAgICAgICAgICBbYnl0ZVtdXSRQYXJhbXMgPSBAKCksIFtpbnRdJFRpbWVvdXRNcyA9IDQwMCkKCiAgICAkZnJhbWUg"
    t = t & "PSBOZXctSGlkcHBGcmFtZSAkUmVwb3J0SWQgJERldklkeCAkRmVhdHVyZUlkeCAkRnVuY3Rpb24gJFBhcmFtcwogICAgJHN3SWQgPSBpZiAoJERldi5QU09i"
    t = t & "amVjdC5Qcm9wZXJ0aWVzLk5hbWUgLWNvbnRhaW5zICdOZXh0U3dJZCcpIHsgJERldi5OZXh0U3dJZCB9IGVsc2UgeyAkU1dfSUQgfQogICAgJG5leHQgPSA4"
    t = t & "ICsgKCgkc3dJZCAtIDggKyAxKSAlIDgpCiAgICAkRGV2IHwgQWRkLU1lbWJlciAtTm90ZVByb3BlcnR5TmFtZSBOZXh0U3dJZCAtTm90ZVByb3BlcnR5VmFs"
    t = t & "dWUgJG5leHQgLUZvcmNlCiAgICAkZnJhbWVbM10gPSAoJEZ1bmN0aW9uIC1zaGwgNCkgLWJvciAkc3dJZAogICAgaWYgKC1ub3QgJERldi5Xcml0ZSgkZnJh"
    t = t & "bWUpKSB7IHJldHVybiAkbnVsbCB9ICAgIyBjb2xsZWN0aW9uIHJlamVjdGVkIGl0CgogICAgJGRlYWRsaW5lID0gKEdldC1EYXRlKS5BZGRNaWxsaXNlY29u"
    t = t & "ZHMoJFRpbWVvdXRNcykKICAgIHdoaWxlICgoR2V0LURhdGUpIC1sdCAkZGVhZGxpbmUpIHsKICAgICAgICAkciA9ICREZXYuUmVhZCgxMDApCiAgICAgICAg"
    t = t & "JHZlcmRpY3QgPSBHZXQtUmVwbHlWZXJkaWN0IC1SZXBseSAkciAtRGV2SWR4ICREZXZJZHggLUZlYXR1cmVJZHggJEZlYXR1cmVJZHggLUZ1bmN0aW9uICRG"
    t = t & "dW5jdGlvbiAtU3dJZCAkc3dJZAogICAgICAgIGlmICgkdmVyZGljdCAtZXEgJ21hdGNoJykgeyByZXR1cm4gJHIgfQogICAgICAgIGlmICgkdmVyZGljdCAt"
    t = t & "ZXEgJ2Vycm9yJykgeyByZXR1cm4gJG51bGwgfQogICAgfQogICAgcmV0dXJuICRudWxsCn0KCmZ1bmN0aW9uIFJlc29sdmUtTXhGZWF0dXJlIHsKICAgIHBh"
    t = t & "cmFtKCREZXYsIFtpbnRdJFJlcG9ydElkLCBbaW50XSREZXZJZHgsIFtpbnRdJEZlYXR1cmVJZCkKICAgICRwcm9iZSA9IFtieXRlW11dQCgoKCRGZWF0dXJl"
    t = t & "SWQgLXNociA4KSAtYmFuZCAweEZGKSwgKCRGZWF0dXJlSWQgLWJhbmQgMHhGRiksIDApCiAgICAkciA9IEludm9rZS1IaWRwcENhbGwgJERldiAkUmVwb3J0"
    t = t & "SWQgJERldklkeCAwIDAgJHByb2JlIDMwMAogICAgaWYgKCRyKSB7IHJldHVybiBbaW50XSRyWzRdIH0KICAgIHJldHVybiAwCn0KCmZ1bmN0aW9uIEdldC1E"
    t = t & "ZXZpY2VSYW5rIHsKICAgIHBhcmFtKFtpbnRdJFVzYWdlUGFnZSwgW2ludF0kVXNhZ2UsIFtzdHJpbmddJFByb2R1Y3QpCiAgICAkbiA9ICIkUHJvZHVjdCIu"
    t = t & "VG9Mb3dlckludmFyaWFudCgpCiAgICAkbW91c2UgPSAoJFVzYWdlUGFnZSAtZXEgMHgwMDAxIC1hbmQgJFVzYWdlIC1lcSAweDAwMDIpIC1vcgogICAgICAg"
    t = t & "ICAgICAgKCRuIC1tYXRjaCAnbWFzdGVyfGFueXdoZXJlfG1vdXNlfGVyZ28nKQogICAgJGtiZCA9ICgkVXNhZ2VQYWdlIC1lcSAweDAwMDEgLWFuZCAkVXNh"
    t = t & "Z2UgLWVxIDB4MDAwNikgLW9yCiAgICAgICAgICAgKCRuIC1tYXRjaCAna2V5c3xrZXlib2FyZCcpCiAgICAkdGllciA9IGlmICgka2JkIC1hbmQgLW5vdCAk"
    t = t & "bW91c2UpIHsgMCB9IGVsc2VpZiAoJG1vdXNlKSB7IDEgfSBlbHNlIHsgMiB9CiAgICAkbm9uVmVuZG9yID0gaWYgKCRVc2FnZVBhZ2UgLWdlIDB4RkYwMCkg"
    t = t & "eyAwIH0gZWxzZSB7IDEgfQogICAgcmV0dXJuICgxMCAqICR0aWVyICsgJG5vblZlbmRvcikKfQoKIyBObyBmaWx0ZXIgc2V0LCBvciB0aGUgcHJvZHVjdCBu"
    t = t & "YW1lIGNvbnRhaW5zIGFueSBvZiB0aGUgc3Vic3RyaW5ncy4KZnVuY3Rpb24gVGVzdC1NeE5hbWUgewogICAgcGFyYW0oW3N0cmluZ10kUHJvZHVjdCwgW3N0"
    t = t & "cmluZ1tdXSROYW1lcykKICAgIGlmICgtbm90ICROYW1lcyAtb3IgJE5hbWVzLkNvdW50IC1lcSAwKSB7IHJldHVybiAkdHJ1ZSB9CiAgICAkcCA9ICIkUHJv"
    t = t & "ZHVjdCIuVG9Mb3dlckludmFyaWFudCgpCiAgICBmb3JlYWNoICgkbiBpbiAkTmFtZXMpIHsKICAgICAgICBpZiAoJG4gLWFuZCAkcC5Db250YWlucygiJG4i"
    t = t & "LlRvTG93ZXJJbnZhcmlhbnQoKSkpIHsgcmV0dXJuICR0cnVlIH0KICAgIH0KICAgIHJldHVybiAkZmFsc2UKfQoKIyBPdmVyIEJsdWV0b290aCBzb21lIGRl"
    t = t & "dmljZXMgKFBPUCBJY29uIEtleXMpIHJlcG9ydCBhbiBlbXB0eSBISUQgcHJvZHVjdAojIHN0cmluZyBvbiBXaW5kb3dzOyBmaWxsIGluIHRoZSBrbm93biBu"
    t = t & "YW1lIHNvIHJhbmtpbmcgYW5kIC1OYW1lIHN0aWxsIHdvcmsuCmZ1bmN0aW9uIFJlc29sdmUtTXhQcm9kdWN0IHsKICAgIHBhcmFtKFtzdHJpbmddJFByb2R1"
    MXF_B64_04 = t
End Function

Private Function MXF_B64_05() As String
    Dim t As String
    t = ""
    t = t & "Y3QsIFtpbnRdJFByb2R1Y3RJZCkKICAgIGlmICgkUHJvZHVjdCkgeyByZXR1cm4gJFByb2R1Y3QgfQogICAgaWYgKCRQcm9kdWN0SWQgLWVxIDB4QjM4Rikg"
    t = t & "eyByZXR1cm4gJ1BPUCBJY29uIEtleXMnIH0KICAgIHJldHVybiAnJwp9Cgo8IwogICAgRGlzY292ZXJ5IGNvc3RzIGEgcm91bmQgdHJpcCBwZXIgY2FuZGlk"
    t = t & "YXRlIGNvbGxlY3Rpb24gYW5kIGRldmljZSBpbmRleCwgc28KICAgIHRoZSByZXNvbHZlZCBhbnN3ZXIgaXMgY2FjaGVkLiBUaGlzIGlzIHB1cmVseSBhIGxh"
    t = t & "dGVuY3kgd2luOiBhIGNhY2hlZCBwYXRoCiAgICBmb3IgYSBkZXZpY2UgdGhhdCBoYXMgc2luY2UgZ29uZSB0byBzbGVlcCB3aWxsIG5vdCBvcGVuLCBhbmQg"
    t = t & "d2UgZmFsbCBiYWNrIHRvCiAgICBhIGZ1bGwgc2Nhbi4gVGhlIGNhY2hlIGlzIHZhbGlkYXRlZCBvbiBldmVyeSB1c2UgcmF0aGVyIHRoYW4gdHJ1c3RlZCwg"
    t = t & "c28gYQogICAgZmlybXdhcmUgdXBkYXRlIHRoYXQgbW92ZXMgdGhlIGZlYXR1cmUgaW5kZXggc2VsZi1jb3JyZWN0cy4KIz4KZnVuY3Rpb24gR2V0LUNhY2hl"
    t = t & "ZERldmljZXMgewogICAgaWYgKCROb0NhY2hlIC1vciAtbm90IChUZXN0LVBhdGggJENhY2hlRmlsZSkpIHsgcmV0dXJuIEAoKSB9CiAgICB0cnkgeyAkZW50"
    t = t & "cmllcyA9IEdldC1Db250ZW50ICRDYWNoZUZpbGUgLVJhdyB8IENvbnZlcnRGcm9tLUpzb24gfSBjYXRjaCB7IHJldHVybiBAKCkgfQogICAgaWYgKCRudWxs"
    t = t & "IC1lcSAkZW50cmllcykgeyByZXR1cm4gQCgpIH0KICAgIGlmICgkZW50cmllcyAtaXNub3QgW1N5c3RlbS5Db2xsZWN0aW9ucy5JRW51bWVyYWJsZV0gLW9y"
    t = t & "CiAgICAgICAgJGVudHJpZXMgLWlzIFtzdHJpbmddKSB7CiAgICAgICAgJGVudHJpZXMgPSBAKCRlbnRyaWVzKQogICAgfQoKICAgICR2YWxpZCA9IEAoKQog"
    t = t & "ICAgZm9yZWFjaCAoJGMgaW4gJGVudHJpZXMpIHsKICAgICAgICAkaWZhY2UgPSBOZXctT2JqZWN0ICdIaWQrSWZhY2UnCiAgICAgICAgJGlmYWNlLlBhdGgg"
    t = t & "PSAkYy5QYXRoOyAkaWZhY2UuUHJvZHVjdCA9ICRjLlByb2R1Y3QKICAgICAgICAkaWZhY2UuVXNhZ2VQYWdlID0gJGMuVXNhZ2VQYWdlOyAkaWZhY2UuVXNh"
    t = t & "Z2UgPSAkYy5Vc2FnZTsgJGlmYWNlLlBpZCA9ICRjLlBpZAogICAgICAgICRpZmFjZS5JbkxlbiA9ICRjLkluTGVuOyAkaWZhY2UuT3V0TGVuID0gJGMuT3V0"
    t = t & "TGVuCgogICAgICAgIHRyeSB7ICRkZXYgPSBOZXctT2JqZWN0IEhpZCAkaWZhY2UgfSBjYXRjaCB7CiAgICAgICAgICAgIGZvcmVhY2ggKCR2IGluICR2YWxp"
    t = t & "ZCkgeyAkdi5EZXYuQ2xvc2UoKSB9CiAgICAgICAgICAgIHJldHVybiBAKCkKICAgICAgICB9CgogICAgICAgICMgQ29uZmlybSB0aGUgY2FjaGVkIGZlYXR1"
    t = t & "cmUgaW5kZXggc3RpbGwgcmVzb2x2ZXMgdG8gQ2hhbmdlSG9zdC4gT25seQogICAgICAgICMgYWNjZXB0IHRoZSByZXBvcnQgSUQgdGhhdCBhY3R1YWxseSBw"
    t = t & "cm9kdWNlZCB0aGUgcmVwbHkuCiAgICAgICAgJHJpZCA9ICRjLlJlcG9ydElkCiAgICAgICAgJHIgPSBJbnZva2UtSGlkcHBDYWxsICRkZXYgJHJpZCAkYy5E"
    t = t & "ZXZJZHggMCAwIChbYnl0ZVtdXUAoMHgxOCwgMHgxNCwgMHgwMCkpIDI1MAogICAgICAgIGlmICgtbm90ICRyKSB7CiAgICAgICAgICAgICRhbHQgPSBpZiAo"
    t = t & "JHJpZCAtZXEgMHgxMSkgeyAweDEwIH0gZWxzZSB7IDB4MTEgfQogICAgICAgICAgICAkciA9IEludm9rZS1IaWRwcENhbGwgJGRldiAkYWx0ICRjLkRldklk"
    t = t & "eCAwIDAgKFtieXRlW11dQCgweDE4LCAweDE0LCAweDAwKSkgMjUwCiAgICAgICAgICAgIGlmICgkcikgeyAkcmlkID0gJGFsdCB9CiAgICAgICAgfQogICAg"
    t = t & "ICAgIGlmICgkciAtYW5kICRyWzRdIC1lcSAkYy5GZWF0dXJlSWR4IC1hbmQgJHJbNF0gLW5lIDApIHsKICAgICAgICAgICAgJHZhbGlkICs9IFtwc2N1c3Rv"
    t = t & "bW9iamVjdF1AeyBEZXYgPSAkZGV2OyBSZXBvcnRJZCA9ICRyaWQ7IERldklkeCA9ICRjLkRldklkeAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAg"
    t = t & "ICAgICAgICAgIEZlYXR1cmVJZHggPSAkYy5GZWF0dXJlSWR4OyBJZmFjZSA9ICRpZmFjZSB9CiAgICAgICAgfSBlbHNlIHsKICAgICAgICAgICAgJGRldi5D"
    t = t & "bG9zZSgpCiAgICAgICAgICAgIGZvcmVhY2ggKCR2IGluICR2YWxpZCkgeyAkdi5EZXYuQ2xvc2UoKSB9CiAgICAgICAgICAgIHJldHVybiBAKCkgICAgICAg"
    t = t & "ICAgIyBvbmUgc3RhbGUgZW50cnkgaW52YWxpZGF0ZXMgdGhlIGNhY2hlCiAgICAgICAgfQogICAgfQogICAgcmV0dXJuICR2YWxpZAp9CgpmdW5jdGlvbiBT"
    t = t & "YXZlLUNhY2hlZERldmljZXMgewogICAgcGFyYW0oJENvbnRleHRzKQogICAgaWYgKCROb0NhY2hlKSB7IHJldHVybiB9CiAgICB0cnkgewogICAgICAgICRk"
    t = t & "aXIgPSBTcGxpdC1QYXRoICRDYWNoZUZpbGUgLVBhcmVudAogICAgICAgIGlmICgtbm90IChUZXN0LVBhdGggJGRpcikpIHsgTmV3LUl0ZW0gLUl0ZW1UeXBl"
    t = t & "IERpcmVjdG9yeSAtUGF0aCAkZGlyIC1Gb3JjZSB8IE91dC1OdWxsIH0KICAgICAgICAkZW50cmllcyA9ICRDb250ZXh0cyB8IEZvckVhY2gtT2JqZWN0IHsK"
    t = t & "ICAgICAgICAgICAgW3BzY3VzdG9tb2JqZWN0XUB7CiAgICAgICAgICAgICAgICBQYXRoID0gJF8uSWZhY2UuUGF0aDsgUHJvZHVjdCA9ICRfLklmYWNlLlBy"
    t = t & "b2R1Y3QKICAgICAgICAgICAgICAgIFVzYWdlUGFnZSA9ICRfLklmYWNlLlVzYWdlUGFnZTsgVXNhZ2UgPSAkXy5JZmFjZS5Vc2FnZQogICAgICAgICAgICAg"
    t = t & "ICAgUGlkID0gJF8uSWZhY2UuUGlkOyBJbkxlbiA9ICRfLklmYWNlLkluTGVuOyBPdXRMZW4gPSAkXy5JZmFjZS5PdXRMZW4KICAgICAgICAgICAgICAgIERl"
    t = t & "dklkeCA9ICRfLkRldklkeDsgUmVwb3J0SWQgPSAkXy5SZXBvcnRJZDsgRmVhdHVyZUlkeCA9ICRfLkZlYXR1cmVJZHgKICAgICAgICAgICAgfQogICAgICAg"
    t = t & "IH0KICAgICAgICBDb252ZXJ0VG8tSnNvbiAtSW5wdXRPYmplY3QgQCgkZW50cmllcykgfCBTZXQtQ29udGVudCAkQ2FjaGVGaWxlCiAgICB9IGNhdGNoIHsg"
    t = t & "V3JpdGUtVmVyYm9zZSAnY291bGQgbm90IHdyaXRlIGNhY2hlJyB9Cn0KCmZ1bmN0aW9uIEdldC1NeEludGVyZmFjZXMgeyBbSGlkXTo6RW51bWVyYXRlKCRW"
    t = t & "SUQpIH0KZnVuY3Rpb24gT3Blbi1NeERldmljZSB7IHBhcmFtKCRJZmFjZSkgTmV3LU9iamVjdCBIaWQgJElmYWNlIH0KCmZ1bmN0aW9uIENsb3NlLU14RGV2"
    t = t & "aWNlcyB7CiAgICBwYXJhbSgkQ29udGV4dHMsICRLZWVwID0gJG51bGwpCiAgICAkY2xvc2VkID0gTmV3LU9iamVjdCAnU3lzdGVtLkNvbGxlY3Rpb25zLkdl"
    t = t & "bmVyaWMuTGlzdFtvYmplY3RdJwogICAgZm9yZWFjaCAoJGMgaW4gJENvbnRleHRzKSB7CiAgICAgICAgaWYgKFtvYmplY3RdOjpSZWZlcmVuY2VFcXVhbHMo"
    t = t & "JGMuRGV2LCAkS2VlcCkpIHsgY29udGludWUgfQogICAgICAgICRzaGFyZWQgPSAkZmFsc2UKICAgICAgICBmb3JlYWNoICgkZGV2IGluICRjbG9zZWQpIHsK"
    t = t & "ICAgICAgICAgICAgaWYgKFtvYmplY3RdOjpSZWZlcmVuY2VFcXVhbHMoJGRldiwgJGMuRGV2KSkgeyAkc2hhcmVkID0gJHRydWU7IGJyZWFrIH0KICAgICAg"
    t = t & "ICB9CiAgICAgICAgaWYgKC1ub3QgJHNoYXJlZCkgeyAkYy5EZXYuQ2xvc2UoKTsgJGNsb3NlZC5BZGQoJGMuRGV2KSB9CiAgICB9Cn0KCmZ1bmN0aW9uIEdl"
    t = t & "dC1NeE1ldGFkYXRhIHsKICAgIHBhcmFtKCREZXYsICRJZmFjZSwgW2ludF0kUmVwb3J0SWQsIFtpbnRdJERldklkeCkKICAgICRwcm9kdWN0ID0gUmVzb2x2"
    t = t & "ZS1NeFByb2R1Y3QgJElmYWNlLlByb2R1Y3QgJElmYWNlLlBpZAogICAgJGlkZW50aXR5ID0gKCRJZmFjZS5QYXRoLlRvTG93ZXJJbnZhcmlhbnQoKSAtcmVw"
    t = t & "bGFjZSAnJmNvbFswLTlhLWZdezJ9JywgJycpICsgIjokRGV2SWR4IgogICAgaWYgKCREZXZJZHggLW5lIDI1NSkgewogICAgICAgICRwcm9kdWN0ID0gInJl"
    t = t & "Y2VpdmVyIChzbG90ICREZXZJZHgpIgogICAgICAgICRpZHggPSBSZXNvbHZlLU14RmVhdHVyZSAkRGV2ICRSZXBvcnRJZCAkRGV2SWR4IDB4MDAwNQogICAg"
    t = t & "ICAgIGlmICgkaWR4KSB7CiAgICAgICAgICAgICRyID0gSW52b2tlLUhpZHBwQ2FsbCAkRGV2ICRSZXBvcnRJZCAkRGV2SWR4ICRpZHggMAogICAgICAgICAg"
    MXF_B64_05 = t
End Function

Private Function MXF_B64_06() As String
    Dim t As String
    t = ""
    t = t & "ICBpZiAoJHIgLWFuZCAkcls0XSkgewogICAgICAgICAgICAgICAgJGxlbmd0aCA9IFtpbnRdJHJbNF0KICAgICAgICAgICAgICAgICRieXRlcyA9IE5ldy1P"
    t = t & "YmplY3QgJ1N5c3RlbS5Db2xsZWN0aW9ucy5HZW5lcmljLkxpc3RbYnl0ZV0nCiAgICAgICAgICAgICAgICB3aGlsZSAoJGJ5dGVzLkNvdW50IC1sdCAkbGVu"
    t = t & "Z3RoKSB7CiAgICAgICAgICAgICAgICAgICAgJGZyYWdtZW50ID0gSW52b2tlLUhpZHBwQ2FsbCAkRGV2ICRSZXBvcnRJZCAkRGV2SWR4ICRpZHggMSAoW2J5"
    t = t & "dGVbXV1AKCRieXRlcy5Db3VudCkpCiAgICAgICAgICAgICAgICAgICAgaWYgKC1ub3QgJGZyYWdtZW50IC1vciAkZnJhZ21lbnQuTGVuZ3RoIC1sZSA0KSB7"
    t = t & "IGJyZWFrIH0KICAgICAgICAgICAgICAgICAgICAkbiA9IFtNYXRoXTo6TWluKCRmcmFnbWVudC5MZW5ndGggLSA0LCAkbGVuZ3RoIC0gJGJ5dGVzLkNvdW50"
    t = t & "KQogICAgICAgICAgICAgICAgICAgIGZvciAoJGkgPSAwOyAkaSAtbHQgJG47ICRpKyspIHsgJGJ5dGVzLkFkZCgkZnJhZ21lbnRbNCskaV0pIH0KICAgICAg"
    t = t & "ICAgICAgICAgIH0KICAgICAgICAgICAgICAgIGlmICgkYnl0ZXMuQ291bnQgLWVxICRsZW5ndGgpIHsgJHByb2R1Y3QgPSBbVGV4dC5FbmNvZGluZ106OlVU"
    t = t & "RjguR2V0U3RyaW5nKCRieXRlcy5Ub0FycmF5KCkpIH0KICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KICAgICRpZHggPSBSZXNvbHZlLU14RmVhdHVy"
    t = t & "ZSAkRGV2ICRSZXBvcnRJZCAkRGV2SWR4IDB4MDAwMwogICAgaWYgKCRpZHgpIHsKICAgICAgICAkciA9IEludm9rZS1IaWRwcENhbGwgJERldiAkUmVwb3J0"
    t = t & "SWQgJERldklkeCAkaWR4IDAKICAgICAgICBpZiAoJHIgLWFuZCAkci5MZW5ndGggLWdlIDkgLWFuZCAoJHJbNV0gLW9yICRyWzZdIC1vciAkcls3XSAtb3Ig"
    t = t & "JHJbOF0pKSB7CiAgICAgICAgICAgICRpZGVudGl0eSA9ICd1bml0OicgKyAoRm9ybWF0LU14SGV4IChbYnl0ZVtdXSRyWzUuLjhdKSkKICAgICAgICB9CiAg"
    t = t & "ICB9CiAgICBbcHNjdXN0b21vYmplY3RdQHsgUHJvZHVjdCA9ICRwcm9kdWN0OyBJZGVudGl0eSA9ICRpZGVudGl0eSB9Cn0KCiMgRXZlcnkgc2xvdCBpcyBk"
    t = t & "aXNjb3ZlcmVkIGJlZm9yZSBmaWx0ZXJpbmcgYW5kIHJhbmtpbmcgdGhlIHBlcmlwaGVyYWxzLgpmdW5jdGlvbiBGaW5kLU14RGV2aWNlcyB7CiAgICBwYXJh"
    t = t & "bShbc3dpdGNoXSRBbGwsIFtzdHJpbmdbXV0kTmFtZXMpCiAgICAkY2FjaGVkID0gQChHZXQtQ2FjaGVkRGV2aWNlcykKICAgIGlmICgkY2FjaGVkLkNvdW50"
    t = t & "IC1ndCAwIC1hbmQgLW5vdCAkQWxsIC1hbmQgLW5vdCAkTmFtZXMpIHsKICAgICAgICAkZmlyc3QgPSAkY2FjaGVkWzBdCiAgICAgICAgJG1ldGFkYXRhID0g"
    t = t & "R2V0LU14TWV0YWRhdGEgJGZpcnN0LkRldiAkZmlyc3QuSWZhY2UgJGZpcnN0LlJlcG9ydElkICRmaXJzdC5EZXZJZHgKICAgICAgICAkZmlyc3QuSWZhY2Uu"
    t = t & "UHJvZHVjdCA9ICRtZXRhZGF0YS5Qcm9kdWN0CiAgICAgICAgaWYgKChHZXQtRGV2aWNlUmFuayAkZmlyc3QuSWZhY2UuVXNhZ2VQYWdlICRmaXJzdC5JZmFj"
    t = t & "ZS5Vc2FnZSAkbWV0YWRhdGEuUHJvZHVjdCkgLWx0IDEwKSB7CiAgICAgICAgICAgIENsb3NlLU14RGV2aWNlcyAkY2FjaGVkIC1LZWVwICRmaXJzdC5EZXYK"
    t = t & "ICAgICAgICAgICAgcmV0dXJuIEAoJGZpcnN0KQogICAgICAgIH0KICAgIH0KICAgIENsb3NlLU14RGV2aWNlcyAkY2FjaGVkCiAgICAkY29udGV4dHMgPSBA"
    t = t & "KCk7ICRvcGVuZWQgPSBAKCk7ICRzZWVuID0gQHt9CiAgICB0cnkgewogICAgICAgICRpZmFjZXMgPSBAKEdldC1NeEludGVyZmFjZXMgfCBTb3J0LU9iamVj"
    t = t & "dCB7IEdldC1EZXZpY2VSYW5rICRfLlVzYWdlUGFnZSAkXy5Vc2FnZSAkXy5Qcm9kdWN0IH0pCiAgICAgICAgZm9yZWFjaCAoJGlmYWNlIGluICRpZmFjZXMp"
    t = t & "IHsKICAgICAgICAgICAgJHByZWZlcnJlZCA9ICRmYWxzZQogICAgICAgICAgICB0cnkgeyAkZGV2ID0gT3Blbi1NeERldmljZSAkaWZhY2UgfSBjYXRjaCB7"
    t = t & "IGNvbnRpbnVlIH0KICAgICAgICAgICAgJG9wZW5lZCArPSBbcHNjdXN0b21vYmplY3RdQHsgRGV2ID0gJGRldiB9CiAgICAgICAgICAgICRyZXBvcnRJZHMg"
    t = t & "PSBAKDB4MTEsIDB4MTApIHwgV2hlcmUtT2JqZWN0IHsgJEZSQU1FX0xFTltbaW50XSRfXSAtbGUgJGlmYWNlLk91dExlbiB9IHwKICAgICAgICAgICAgICAg"
    t = t & "IFNvcnQtT2JqZWN0IEB7IEV4cHJlc3Npb24gPSB7IGlmICgkRlJBTUVfTEVOW1tpbnRdJF9dIC1lcSAkaWZhY2UuT3V0TGVuKSB7IDAgfSBlbHNlIHsgMSB9"
    t = t & "IH0gfQogICAgICAgICAgICBmb3JlYWNoICgkZGlkeCBpbiAkREVWSUNFX0lORElDRVMpIHsKICAgICAgICAgICAgICAgICRtYXRjaGVkID0gJGZhbHNlCiAg"
    t = t & "ICAgICAgICAgICAgICBmb3JlYWNoICgkcmlkIGluICRyZXBvcnRJZHMpIHsKICAgICAgICAgICAgICAgICAgICBXcml0ZS1WZXJib3NlICgicHJvYmluZyB7"
    t = t & "MH0gaW5kZXg9MHh7MTp4Mn0gcmVwb3J0PTB4ezI6eDJ9IiAtZiAkaWZhY2UuUHJvZHVjdCwgJGRpZHgsICRyaWQpCiAgICAgICAgICAgICAgICAgICAgJGlk"
    t = t & "eCA9IFJlc29sdmUtTXhGZWF0dXJlICRkZXYgJHJpZCAkZGlkeCAweDE4MTQKICAgICAgICAgICAgICAgICAgICBpZiAoJGlkeCkgeyAkbWF0Y2hlZCA9ICR0"
    t = t & "cnVlOyBicmVhayB9CiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgICAgICBpZiAoLW5vdCAkbWF0Y2hlZCkgeyBjb250aW51ZSB9CiAgICAgICAgICAg"
    t = t & "ICAgICAkbWV0YWRhdGEgPSBHZXQtTXhNZXRhZGF0YSAkZGV2ICRpZmFjZSAkcmlkICRkaWR4CiAgICAgICAgICAgICAgICBpZiAoLW5vdCAkc2Vlbi5Db250"
    t = t & "YWluc0tleSgkbWV0YWRhdGEuSWRlbnRpdHkpIC1hbmQgKFRlc3QtTXhOYW1lICRtZXRhZGF0YS5Qcm9kdWN0ICROYW1lcykpIHsKICAgICAgICAgICAgICAg"
    t = t & "ICAgICAkc2VlblskbWV0YWRhdGEuSWRlbnRpdHldID0gJHRydWUKICAgICAgICAgICAgICAgICAgICAjIEVhY2ggcGVyaXBoZXJhbCBuZWVkcyBpdHMgb3du"
    t = t & "IHByb2R1Y3QgbmFtZSwgZXZlbiBvbiBhIHNoYXJlZCBoYW5kbGUuCiAgICAgICAgICAgICAgICAgICAgJGRldmljZUlmYWNlID0gW3BzY3VzdG9tb2JqZWN0"
    t = t & "XUB7IFBhdGggPSAkaWZhY2UuUGF0aDsgUHJvZHVjdCA9ICRtZXRhZGF0YS5Qcm9kdWN0CiAgICAgICAgICAgICAgICAgICAgICAgIFVzYWdlUGFnZSA9ICQo"
    t = t & "aWYgKCRkaWR4IC1lcSAyNTUpIHsgJGlmYWNlLlVzYWdlUGFnZSB9IGVsc2UgeyAwIH0pOwogICAgICAgICAgICAgICAgICAgICAgICBVc2FnZSA9ICQoaWYg"
    t = t & "KCRkaWR4IC1lcSAyNTUpIHsgJGlmYWNlLlVzYWdlIH0gZWxzZSB7IDAgfSk7IFBpZCA9ICRpZmFjZS5QaWQKICAgICAgICAgICAgICAgICAgICAgICAgSW5M"
    t = t & "ZW4gPSAkaWZhY2UuSW5MZW47IE91dExlbiA9ICRpZmFjZS5PdXRMZW4gfQogICAgICAgICAgICAgICAgICAgICRjb250ZXh0cyArPSBbcHNjdXN0b21vYmpl"
    t = t & "Y3RdQHsgRGV2ID0gJGRldjsgUmVwb3J0SWQgPSAkcmlkOyBEZXZJZHggPSAkZGlkeAogICAgICAgICAgICAgICAgICAgICAgICBGZWF0dXJlSWR4ID0gJGlk"
    t = t & "eDsgSWZhY2UgPSAkZGV2aWNlSWZhY2UgfQogICAgICAgICAgICAgICAgICAgIGlmICgtbm90ICRBbGwgLWFuZCAoR2V0LURldmljZVJhbmsgJGRldmljZUlm"
    t = t & "YWNlLlVzYWdlUGFnZSAkZGV2aWNlSWZhY2UuVXNhZ2UgJGRldmljZUlmYWNlLlByb2R1Y3QpIC1sdCAxMCkgewogICAgICAgICAgICAgICAgICAgICAgICAk"
    t = t & "cHJlZmVycmVkID0gJHRydWU7IGJyZWFrCiAgICAgICAgICAgICAgICAgICAgfQogICAgICAgICAgICAgICAgfQogICAgICAgICAgICAgICAgaWYgKCRkaWR4"
    t = t & "IC1lcSAyNTUpIHsgYnJlYWsgfQogICAgICAgICAgICB9CiAgICAgICAgICAgIGlmICgkcHJlZmVycmVkKSB7IGJyZWFrIH0KICAgICAgICB9CiAgICAgICAg"
    t = t & "JGNvbnRleHRzID0gQCgkY29udGV4dHMgfCBTb3J0LU9iamVjdCB7IEdldC1EZXZpY2VSYW5rICRfLklmYWNlLlVzYWdlUGFnZSAkXy5JZmFjZS5Vc2FnZSAk"
    t = t & "Xy5JZmFjZS5Qcm9kdWN0IH0pCiAgICAgICAgaWYgKC1ub3QgJEFsbCAtYW5kICRjb250ZXh0cy5Db3VudCAtZ3QgMSkgeyAkY29udGV4dHMgPSBAKCRjb250"
    MXF_B64_06 = t
End Function

Private Function MXF_B64_07() As String
    Dim t As String
    t = ""
    t = t & "ZXh0c1swXSkgfQogICAgICAgIGZvcmVhY2ggKCRvIGluICRvcGVuZWQpIHsKICAgICAgICAgICAgJHJldGFpbmVkID0gJGZhbHNlCiAgICAgICAgICAgIGZv"
    t = t & "cmVhY2ggKCRjIGluICRjb250ZXh0cykgewogICAgICAgICAgICAgICAgaWYgKFtvYmplY3RdOjpSZWZlcmVuY2VFcXVhbHMoJGMuRGV2LCAkby5EZXYpKSB7"
    t = t & "ICRyZXRhaW5lZCA9ICR0cnVlOyBicmVhayB9CiAgICAgICAgICAgIH0KICAgICAgICAgICAgaWYgKC1ub3QgJHJldGFpbmVkKSB7ICRvLkRldi5DbG9zZSgp"
    t = t & "IH0KICAgICAgICB9CiAgICAgICAgJG9wZW5lZCA9IEAoKQogICAgICAgIGlmICgkY29udGV4dHMuQ291bnQpIHsgU2F2ZS1DYWNoZWREZXZpY2VzICRjb250"
    t = t & "ZXh0cyB9CiAgICAgICAgcmV0dXJuICRjb250ZXh0cwogICAgfSBjYXRjaCB7IENsb3NlLU14RGV2aWNlcyAkb3BlbmVkOyB0aHJvdyB9Cn0KCmZ1bmN0aW9u"
    t = t & "IEdldC1NeFN0YXR1cyB7CiAgICBwYXJhbSgkQ3R4LCBbc3dpdGNoXSRXaXRoU3RhdHVzKQoKICAgICR0cmFuc3BvcnQgPSBpZiAoJEN0eC5EZXZJZHggLWVx"
    t = t & "IDB4RkYpIHsgJ2RpcmVjdCAoQlQvVVNCKScgfSBlbHNlIHsgJ3JlY2VpdmVyJyB9CiAgICAkbyA9IFtvcmRlcmVkXUB7CiAgICAgICAgcHJvZHVjdCA9ICRD"
    t = t & "dHguSWZhY2UuUHJvZHVjdAogICAgICAgIHRyYW5zcG9ydCA9ICR0cmFuc3BvcnQKICAgICAgICBkZXZpY2VfaW5kZXggPSAkQ3R4LkRldklkeAogICAgICAg"
    t = t & "IHJlcG9ydF9pZCA9ICRDdHguUmVwb3J0SWQKICAgICAgICBmZWF0dXJlX2luZGV4ID0gJEN0eC5GZWF0dXJlSWR4CiAgICB9CiAgICAkaCA9IEludm9rZS1I"
    t = t & "aWRwcENhbGwgJEN0eC5EZXYgJEN0eC5SZXBvcnRJZCAkQ3R4LkRldklkeCAkQ3R4LkZlYXR1cmVJZHggMAogICAgaWYgKCRoKSB7ICRvLmhvc3RzID0gJGhb"
    t = t & "NF07ICRvLmNoYW5uZWwgPSAkaFs1XSArIDEgfQoKICAgIGlmICgkV2l0aFN0YXR1cykgewogICAgICAgIGZvcmVhY2ggKCRmaWQgaW4gMHgxMDA0LCAweDEw"
    t = t & "MDApIHsKICAgICAgICAgICAgJGlkeCA9IFJlc29sdmUtTXhGZWF0dXJlICRDdHguRGV2ICRDdHguUmVwb3J0SWQgJEN0eC5EZXZJZHggJGZpZAogICAgICAg"
    t = t & "ICAgICBpZiAoLW5vdCAkaWR4KSB7IGNvbnRpbnVlIH0KICAgICAgICAgICAgJGZ1bmN0aW9uID0gaWYgKCRmaWQgLWVxIDB4MTAwNCkgeyAxIH0gZWxzZSB7"
    t = t & "IDAgfQogICAgICAgICAgICAkciA9IEludm9rZS1IaWRwcENhbGwgJEN0eC5EZXYgJEN0eC5SZXBvcnRJZCAkQ3R4LkRldklkeCAkaWR4ICRmdW5jdGlvbgog"
    t = t & "ICAgICAgICAgICBpZiAoJHIgLWFuZCAkci5MZW5ndGggLWdlIDcgLWFuZCAkcls0XSAtZ3QgMCAtYW5kICRyWzRdIC1sZSAxMDApIHsKICAgICAgICAgICAg"
    t = t & "ICAgICRvLmJhdHRlcnlfcGVyY2VudCA9ICRyWzRdCiAgICAgICAgICAgICAgICAkby5jaGFyZ2luZyA9ICRyWzZdIC1pbiBAKDEsMikKICAgICAgICAgICAg"
    t = t & "ICAgIGJyZWFrCiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICAgICAgJGZ3ID0gUmVzb2x2ZS1NeEZlYXR1cmUgJEN0eC5EZXYgJEN0eC5SZXBvcnRJZCAk"
    t = t & "Q3R4LkRldklkeCAweDAwMDMKICAgICAgICBpZiAoJGZ3KSB7CiAgICAgICAgICAgICRyID0gSW52b2tlLUhpZHBwQ2FsbCAkQ3R4LkRldiAkQ3R4LlJlcG9y"
    t = t & "dElkICRDdHguRGV2SWR4ICRmdyAwCiAgICAgICAgICAgIGlmICgkcikgewogICAgICAgICAgICAgICAgJGNvdW50ID0gW2ludF0kcls0XQogICAgICAgICAg"
    t = t & "ICAgICAgZm9yICgkZW50aXR5ID0gMDsgJGVudGl0eSAtbHQgJGNvdW50OyAkZW50aXR5KyspIHsKICAgICAgICAgICAgICAgICAgICAkciA9IEludm9rZS1I"
    t = t & "aWRwcENhbGwgJEN0eC5EZXYgJEN0eC5SZXBvcnRJZCAkQ3R4LkRldklkeCAkZncgMSAoW2J5dGVbXV1AKCRlbnRpdHkpKQogICAgICAgICAgICAgICAgICAg"
    t = t & "IGlmICgkciAtYW5kICRyLkxlbmd0aCAtZ2UgMTIgLWFuZCAoJHJbNF0gLWJhbmQgMTUpIC1lcSAwKSB7CiAgICAgICAgICAgICAgICAgICAgICAgICRvLmZp"
    t = t & "cm13YXJlID0gJ3swOlgyfS57MTpYMn0nIC1mICRyWzhdLCAkcls5XQogICAgICAgICAgICAgICAgICAgICAgICAkYnVpbGQgPSAoW2ludF0kclsxMF0gLXNo"
    t = t & "bCA4KSAtYm9yICRyWzExXQogICAgICAgICAgICAgICAgICAgICAgICBpZiAoJGJ1aWxkKSB7ICRvLmZpcm13YXJlICs9ICcuQnswOlg0fScgLWYgJGJ1aWxk"
    t = t & "IH0KICAgICAgICAgICAgICAgICAgICAgICAgYnJlYWsKICAgICAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgIH0KICAg"
    t = t & "ICAgICB9CiAgICB9CiAgICByZXR1cm4gW3BzY3VzdG9tb2JqZWN0XSRvCn0KCmZ1bmN0aW9uIFNob3ctTXhIdW1hbiB7CiAgICBwYXJhbSgkUmVwb3J0KQog"
    t = t & "ICAgJ2RldmljZSAgICAgOiB7MH0nIC1mICRSZXBvcnQucHJvZHVjdAogICAgJ3RyYW5zcG9ydCAgOiB7MH0gIGluZGV4PTB4ezE6eDJ9ICByZXBvcnQ9MHh7"
    t = t & "Mjp4Mn0nIC1mIGAKICAgICAgICAkUmVwb3J0LnRyYW5zcG9ydCwgJFJlcG9ydC5kZXZpY2VfaW5kZXgsICRSZXBvcnQucmVwb3J0X2lkCiAgICAnQ2hhbmdl"
    t = t & "SG9zdCA6IGZlYXR1cmUgaW5kZXggMHh7MDp4Mn0nIC1mICRSZXBvcnQuZmVhdHVyZV9pbmRleAogICAgaWYgKCRSZXBvcnQuUFNPYmplY3QuUHJvcGVydGll"
    t = t & "cy5OYW1lIC1jb250YWlucyAnaG9zdHMnKSB7CiAgICAgICAgJ2NoYW5uZWxzICAgOiB7MH0sIGN1cnJlbnRseSBvbiB7MX0nIC1mICRSZXBvcnQuaG9zdHMs"
    t = t & "ICRSZXBvcnQuY2hhbm5lbAogICAgfQogICAgaWYgKCRSZXBvcnQuUFNPYmplY3QuUHJvcGVydGllcy5OYW1lIC1jb250YWlucyAnYmF0dGVyeV9wZXJjZW50"
    t = t & "JykgewogICAgICAgICRzdGF0ZSA9IGlmICgkUmVwb3J0LmNoYXJnaW5nKSB7ICdjaGFyZ2luZycgfSBlbHNlIHsgJ2Rpc2NoYXJnaW5nJyB9CiAgICAgICAg"
    t = t & "J2JhdHRlcnkgICAgOiB7MH0lICh7MX0pJyAtZiAkUmVwb3J0LmJhdHRlcnlfcGVyY2VudCwgJHN0YXRlCiAgICB9CiAgICBpZiAoJFJlcG9ydC5QU09iamVj"
    t = t & "dC5Qcm9wZXJ0aWVzLk5hbWUgLWNvbnRhaW5zICdmaXJtd2FyZScpIHsKICAgICAgICAnZmlybXdhcmUgICA6IHswfScgLWYgJFJlcG9ydC5maXJtd2FyZQog"
    t = t & "ICAgfQp9CgojIC0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLQojIE1h"
    t = t & "aW4KIyAtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0KCmZ1bmN0aW9u"
    t = t & "IEludm9rZS1Td2l0Y2ggewogICAgcGFyYW0oJEN0eCwgW2ludF0kVGFyZ2V0Q2hhbm5lbCwgW2ludF0kQXR0ZW1wdHMgPSA0KQoKICAgICRjdXJyZW50ID0g"
    t = t & "SW52b2tlLUhpZHBwQ2FsbCAkQ3R4LkRldiAkQ3R4LlJlcG9ydElkICRDdHguRGV2SWR4ICRDdHguRmVhdHVyZUlkeCAwCiAgICBpZiAoJGN1cnJlbnQgLWFu"
    t = t & "ZCAkY3VycmVudC5MZW5ndGggLWdlIDYgLWFuZCAoJGN1cnJlbnRbNV0gKyAxKSAtZXEgJFRhcmdldENoYW5uZWwpIHsgcmV0dXJuICR0cnVlIH0KICAgICRm"
    t = t & "cmFtZSA9IE5ldy1IaWRwcEZyYW1lICRDdHguUmVwb3J0SWQgJEN0eC5EZXZJZHggJEN0eC5GZWF0dXJlSWR4IDEgKFtieXRlW11dQCgkVGFyZ2V0Q2hhbm5l"
    t = t & "bCAtIDEpKQogICAgZm9yICgkYXR0ZW1wdCA9IDE7ICRhdHRlbXB0IC1sZSAkQXR0ZW1wdHM7ICRhdHRlbXB0KyspIHsKICAgICAgICBXcml0ZS1WZXJib3Nl"
    t = t & "ICJzd2l0Y2ggYXR0ZW1wdCAkYXR0ZW1wdCIKICAgICAgICBpZiAoJEN0eC5EZXYuV3JpdGUoJGZyYW1lKSkgewogICAgICAgICAgICBTdGFydC1TbGVlcCAt"
    t = t & "TWlsbGlzZWNvbmRzIDI1MAogICAgICAgICAgICAkc3RpbGwgPSBJbnZva2UtSGlkcHBDYWxsICRDdHguRGV2ICRDdHguUmVwb3J0SWQgJEN0eC5EZXZJZHgg"
    t = t & "YAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICRDdHguRmVhdHVyZUlkeCAwIEAoKSAyNTAKICAgICAgICAgICAgaWYgKC1ub3QgJHN0"
    t = t & "aWxsIC1vciAoJHN0aWxsLkxlbmd0aCAtZ2UgNiAtYW5kICgkc3RpbGxbNV0gKyAxKSAtZXEgJFRhcmdldENoYW5uZWwpKSB7IHJldHVybiAkdHJ1ZSB9ICAg"
    MXF_B64_07 = t
End Function

Private Function MXF_B64_08() As String
    Dim t As String
    t = ""
    t = t & "ICAgICAjIGdvbmU6IGl0IHdvcmtlZAogICAgICAgICAgICBXcml0ZS1WZXJib3NlICdkZXZpY2Ugc3RpbGwgcmVzcG9uZGluZywgcmV0cnlpbmcnCiAgICAg"
    t = t & "ICAgfQogICAgICAgIFN0YXJ0LVNsZWVwIC1NaWxsaXNlY29uZHMgMjAwCiAgICB9CiAgICByZXR1cm4gJGZhbHNlCn0KCgppZiAoLW5vdCAkZW52Ok1YU1dJ"
    t = t & "VENIX1RFU1QpIHsKCiRtb2RlcyA9IEAoQCgkSW5mbywgJFN0YXR1cywgJExpc3QsICRUb2dnbGUpIHwgV2hlcmUtT2JqZWN0IHsgJF8gfSkKaWYgKCRtb2Rl"
    t = t & "cy5Db3VudCArIFtpbnRdJFBTQm91bmRQYXJhbWV0ZXJzLkNvbnRhaW5zS2V5KCdDaGFubmVsJykgLWd0IDEpIHsKICAgIFdyaXRlLU14RXJyb3IgJy1JbmZv"
    t = t & "LCAtU3RhdHVzLCAtTGlzdCBhbmQgLVRvZ2dsZSBhcmUgbXV0dWFsbHkgZXhjbHVzaXZlJwogICAgZXhpdCAyCn0KCmlmICgkTGlzdCkgewogICAgJGZvdW5k"
    t = t & "ID0gW0hpZF06OkVudW1lcmF0ZSgkVklEKQogICAgaWYgKC1ub3QgJGZvdW5kKSB7ICdObyBMb2dpdGVjaCB2ZW5kb3IgSElEIGNvbGxlY3Rpb25zIGZvdW5k"
    t = t & "Lic7IGV4aXQgMSB9CiAgICAndXNhZ2VQYWdlOnVzYWdlICAgcGlkICAgIGluICBvdXQgIHByb2R1Y3QnCiAgICAkZm91bmQgfCBGb3JFYWNoLU9iamVjdCB7"
    t = t & "CiAgICAgICAgJyAgIHswOng0fTp7MTp4NH0gICAgIHsyOng0fSAgezMsM30gezQsNH0gIHs1fScgLWYgYAogICAgICAgICAgICAkXy5Vc2FnZVBhZ2UsICRf"
    t = t & "LlVzYWdlLCAkXy5QaWQsICRfLkluTGVuLCAkXy5PdXRMZW4sICRfLlByb2R1Y3QKICAgIH0KICAgIGV4aXQgMAp9CgppZiAoJENvbmZpZyAtYW5kIC1ub3Qg"
    t = t & "KFRlc3QtUGF0aCAtTGl0ZXJhbFBhdGggJENvbmZpZyAtUGF0aFR5cGUgTGVhZikpIHsKICAgIFdyaXRlLU14RXJyb3IgIkNvbmZpZyBmaWxlIG5vdCBmb3Vu"
    t = t & "ZDogJENvbmZpZyI7IGV4aXQgMgp9CiRjZmcgPSBHZXQtTXhDb25maWcgLVBhdGggJChpZiAoJENvbmZpZykgeyAkQ29uZmlnIH0gZWxzZSB7ICRDb25maWdG"
    t = t & "aWxlIH0pCldyaXRlLVZlcmJvc2UgImNvbmZpZzogJChpZiAoJENvbmZpZykgeyAkQ29uZmlnIH0gZWxzZSB7ICRDb25maWdGaWxlIH0pIgokcHJvYmxlbXMg"
    t = t & "PSBAKFRlc3QtTXhDb25maWcgJGNmZykKaWYgKCRwcm9ibGVtcy5Db3VudCkgeyBXcml0ZS1NeEVycm9yICgiSW52YWxpZCBjb25maWc6ICIgKyAoJHByb2Js"
    t = t & "ZW1zIC1qb2luICc7ICcpKTsgZXhpdCAyIH0KJHdhbnRBbGwgPSAkQWxsIC1vciAkY2ZnLmRldmljZSAtZXEgJ2FsbCcKCiMgVW5pb24gb2YgLU5hbWUgYW5k"
    t = t & "IHRoZSBjb25maWcgYG5hbWVgIGxpc3QuCiRjZmdOYW1lcyA9IEAoKQppZiAoJGNmZy5uYW1lKSB7CiAgICAkY2ZnTmFtZXMgPSBAKCRjZmcubmFtZSAtc3Bs"
    t = t & "aXQgJywnIHwgRm9yRWFjaC1PYmplY3QgeyAiJF8iLlRyaW0oKSB9IHwKICAgICAgICAgICAgICAgICAgV2hlcmUtT2JqZWN0IHsgJF8gfSkKfQokbmFtZUZp"
    t = t & "bHRlciA9IEAoQCgkTmFtZSkgKyAkY2ZnTmFtZXMgfCBXaGVyZS1PYmplY3QgeyAkXyB9KQoKaWYgKC1ub3QgJFRvZ2dsZSAtYW5kIC1ub3QgJEluZm8gLWFu"
    t = t & "ZCAtbm90ICRTdGF0dXMgLWFuZCAtbm90ICRDaGFubmVsKSB7CiAgICBXcml0ZS1NeEVycm9yICdHaXZlIC1DaGFubmVsIDEuLjMsIC1JbmZvLCAtU3RhdHVz"
    t = t & "LCAtVG9nZ2xlIG9yIC1MaXN0JzsgZXhpdCAyCn0KaWYgKC1ub3QgJFRvZ2dsZSAtYW5kIC1ub3QgJEluZm8gLWFuZCAtbm90ICRTdGF0dXMgLWFuZCAkQ2hh"
    t = t & "bm5lbCAtbm90aW4gQCgxLDIsMykpIHsKICAgIFdyaXRlLU14RXJyb3IgJ0NoYW5uZWwgbXVzdCBiZSAxLCAyIG9yIDMnOyBleGl0IDIKfQoKJGZvdW5kID0g"
    t = t & "QChGaW5kLU14RGV2aWNlcyAtQWxsOiR3YW50QWxsIC1OYW1lcyAkbmFtZUZpbHRlcikKaWYgKCRmb3VuZC5Db3VudCAtZXEgMCkgewogICAgV3JpdGUtTXhF"
    t = t & "cnJvciAoJ05vIExvZ2l0ZWNoIGRldmljZSBzdXBwb3J0aW5nIENoYW5nZUhvc3QgZm91bmQuIENsaWNrIHRoZSBtb3VzZSAnICsKICAgICAgICAgICAgICAg"
    t = t & "ICAgICd0byB3YWtlIGl0LCB0aGVuIHJldHJ5LiBSdW4gd2l0aCAtTGlzdCB0byBzZWUgd2hhdCBlbnVtZXJhdGVkLCAnICsKICAgICAgICAgICAgICAgICAg"
    t = t & "ICdvciAtVmVyYm9zZSB0byB3YXRjaCB0aGUgcHJvYmUuJykKICAgIGV4aXQgMQp9CgppZiAoJEluZm8gLW9yICRTdGF0dXMpIHsKICAgICRyZXBvcnRzID0g"
    t = t & "QCgkZm91bmQgfCBGb3JFYWNoLU9iamVjdCB7IEdldC1NeFN0YXR1cyAkXyAtV2l0aFN0YXR1czokU3RhdHVzIH0pCiAgICBpZiAoJEpzb24pIHsKICAgICAg"
    t = t & "ICBDb252ZXJ0VG8tSnNvbiAtSW5wdXRPYmplY3QgQCgkcmVwb3J0cykgLURlcHRoIDQKICAgIH0gZWxzZSB7CiAgICAgICAgZm9yICgkaSA9IDA7ICRpIC1s"
    t = t & "dCAkcmVwb3J0cy5Db3VudDsgJGkrKykgewogICAgICAgICAgICBpZiAoJGkpIHsgJycgfQogICAgICAgICAgICBTaG93LU14SHVtYW4gJHJlcG9ydHNbJGld"
    t = t & "CiAgICAgICAgfQogICAgfQogICAgQ2xvc2UtTXhEZXZpY2VzICRmb3VuZAogICAgZXhpdCAwCn0KCmlmICgkVG9nZ2xlKSB7CiAgICAkaCA9IEludm9rZS1I"
    t = t & "aWRwcENhbGwgJGZvdW5kWzBdLkRldiAkZm91bmRbMF0uUmVwb3J0SWQgJGZvdW5kWzBdLkRldklkeCBgCiAgICAgICAgICAgICAgICAgICAgICAgICAgJGZv"
    t = t & "dW5kWzBdLkZlYXR1cmVJZHggMAogICAgaWYgKC1ub3QgJGgpIHsKICAgICAgICBXcml0ZS1NeEVycm9yICdDb3VsZCBub3QgcmVhZCB0aGUgY3VycmVudCBj"
    t = t & "aGFubmVsLicKICAgICAgICBDbG9zZS1NeERldmljZXMgJGZvdW5kCiAgICAgICAgZXhpdCAxCiAgICB9CiAgICAkY3VycmVudCA9ICRoWzVdICsgMQogICAg"
    t = t & "aWYgKCRjdXJyZW50IC1lcSAkY2ZnLmNoYW5uZWxfYSkgICAgIHsgJENoYW5uZWwgPSAkY2ZnLmNoYW5uZWxfYiB9CiAgICBlbHNlaWYgKCRjdXJyZW50IC1l"
    t = t & "cSAkY2ZnLmNoYW5uZWxfYikgeyAkQ2hhbm5lbCA9ICRjZmcuY2hhbm5lbF9hIH0KICAgIGVsc2UgewogICAgICAgIFdyaXRlLU14RXJyb3IgKCJDdXJyZW50"
    t = t & "IGNoYW5uZWwgJGN1cnJlbnQgaXMgbm90IGluIHRoZSBjb25maWd1cmVkIHBhaXIgIiArCiAgICAgICAgICAgICAgICAgICAgICAgIiQoJGNmZy5jaGFubmVs"
    t = t & "X2EpLCQoJGNmZy5jaGFubmVsX2IpLiBSZWZ1c2luZyB0byBndWVzcy4iKQogICAgICAgIENsb3NlLU14RGV2aWNlcyAkZm91bmQKICAgICAgICBleGl0IDEK"
    t = t & "ICAgIH0KfQoKaWYgKCRDaGFubmVsIC1sdCAxIC1vciAkQ2hhbm5lbCAtZ3QgMykgewogICAgV3JpdGUtTXhFcnJvciAnQ2hhbm5lbCBtdXN0IGJlIDEsIDIg"
    t = t & "b3IgMycKICAgIENsb3NlLU14RGV2aWNlcyAkZm91bmQKICAgIGV4aXQgMgp9CgppZiAoJERyeVJ1bikgewogICAgZm9yZWFjaCAoJGMgaW4gJGZvdW5kKSB7"
    t = t & "CiAgICAgICAgJGZyYW1lID0gTmV3LUhpZHBwRnJhbWUgJGMuUmVwb3J0SWQgJGMuRGV2SWR4ICRjLkZlYXR1cmVJZHggMSAoW2J5dGVbXV1AKCRDaGFubmVs"
    t = t & "IC0gMSkpCiAgICAgICAgJ3swfTogc3dpdGNoIHRvIGNoYW5uZWwgezF9IC0+IHsyfScgLWYgJGMuSWZhY2UuUHJvZHVjdCwgJENoYW5uZWwsIChGb3JtYXQt"
    t = t & "TXhIZXggJGZyYW1lKQogICAgfQogICAgQ2xvc2UtTXhEZXZpY2VzICRmb3VuZAogICAgZXhpdCAwCn0KCjwjCiAgICBzZXRDdXJyZW50SG9zdCBuZXZlciBy"
    t = t & "ZXBsaWVzIC0gdGhlIGxpbmsgaXMgdG9ybiBkb3duIGFzIHBhcnQgb2YgZXhlY3V0aW5nIGl0LgogICAgVGhhdCBtYWtlcyAiZGlkIGl0IHdvcms/IiBhd2t3"
    t = t & "YXJkOiBhIHdyaXRlIHRoYXQgcmV0dXJucyBjbGVhbmx5IHByb3ZlcyBvbmx5CiAgICB0aGF0IHRoZSBkcml2ZXIgYWNjZXB0ZWQgdGhlIGJ1ZmZlciwgbm90"
    t = t & "IHRoYXQgdGhlIGRldmljZSBhY3RlZCBvbiBpdC4gSWYgdGhlCiAgICBwcm9jZXNzIGV4aXRzIGhlcmUsIHRoZSBoYW5kbGUgY2xvc2VzIGFuZCBhIHJlcG9y"
    t = t & "dCBzdGlsbCBpbiBmbGlnaHQgY2FuIGJlCiAgICBsb3N0LCB3aGljaCBpcyB3aHkgYSBwbGFpbiB3cml0ZS1hbmQtZXhpdCBzdWNjZWVkcyBvbmx5IGludGVy"
    t = t & "bWl0dGVudGx5LgoKICAgIFNvIHZlcmlmeSBieSBhYnNlbmNlLiBBZnRlciB0aGUgc3dpdGNoIHRoZSBkZXZpY2Ugc2hvdWxkIHN0b3AgYW5zd2VyaW5nOyBp"
    MXF_B64_08 = t
End Function

Private Function MXF_B64_09() As String
    Dim t As String
    t = ""
    t = t & "ZgogICAgaXQgc3RpbGwgcmVwbGllcyB0byBnZXRIb3N0SW5mbywgdGhlIGZyYW1lIGRpZCBub3QgbGFuZCBhbmQgd2UgdHJ5IGFnYWluLgojPgoKIlN3aXRj"
    t = t & "aGluZyB0byBjaGFubmVsICRDaGFubmVsIC4uLiIKJGZhaWx1cmVzID0gMApmb3JlYWNoICgkYyBpbiAkZm91bmQpIHsKICAgIGlmIChJbnZva2UtU3dpdGNo"
    t = t & "ICRjICRDaGFubmVsKSB7CiAgICAgICAgIiAgezB9OiBjb21tYW5kIHNlbnQiIC1mICRjLklmYWNlLlByb2R1Y3QKICAgIH0gZWxzZSB7CiAgICAgICAgV3Jp"
    t = t & "dGUtTXhFcnJvciAoIiAgezB9OiB0aGUgZGV2aWNlIGRpZCBub3QgbGVhdmUgdGhpcyBob3N0LiBJdCBtYXkgYmUgYXNsZWVwLCBvciB0aGUgdGFyZ2V0IGNo"
    t = t & "YW5uZWwgbWF5IGJlIHVucGFpcmVkLiIgLWYgJGMuSWZhY2UuUHJvZHVjdCkKICAgICAgICAkZmFpbHVyZXMrKwogICAgfQp9CkNsb3NlLU14RGV2aWNlcyAk"
    t = t & "Zm91bmQKCmlmICgkZmFpbHVyZXMgLWVxIDApIHsgZXhpdCAwIH0KaWYgKCRmYWlsdXJlcyAtZXEgJGZvdW5kLkNvdW50KSB7IGV4aXQgMSB9CldyaXRlLU14"
    t = t & "RXJyb3IgIiRmYWlsdXJlcyBvZiAkKCRmb3VuZC5Db3VudCkgZGV2aWNlcyBmYWlsZWQuIgpleGl0IDMKCn0gICMgZW5kIGlmICgtbm90ICRlbnY6TVhTV0lU"
    t = t & "Q0hfVEVTVCkK"
    MXF_B64_09 = t
End Function


