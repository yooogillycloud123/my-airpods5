#ifndef PublishDir
  #error PublishDir is required
#endif
[Setup]
AppId=FileeOffline-Independent-1120
AppName=Filee Offline
AppVersion=1.12.0
AppVerName=Filee Offline 1.12.0
AppPublisher=Independent offline build of Filee
DefaultDirName={localappdata}\Programs\FileeOffline
DefaultGroupName=Filee Offline
PrivilegesRequired=lowest
OutputBaseFilename=Filee-1.12.0-Offline-Setup
Compression=lzma2/fast
SolidCompression=yes
WizardStyle=modern
SetupIconFile={#PublishDir}\Filee.ico
UninstallDisplayIcon={app}\Filee.exe
DisableProgramGroupPage=yes
CloseApplications=yes
RestartApplications=no
LicenseFile={#PublishDir}\LICENSE
InfoAfterFile={#PublishDir}\사용안내.txt
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
[Languages]
Name: "korean"; MessagesFile: "compiler:Languages\Korean.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"
[Files]
Source: "{#PublishDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
[Icons]
Name: "{group}\Filee Offline"; Filename: "{app}\Filee.exe"; WorkingDir: "{app}"
Name: "{autodesktop}\Filee Offline"; Filename: "{app}\Filee.exe"; WorkingDir: "{app}"
[Run]
Filename: "{app}\Filee.exe"; Description: "Filee Offline 실행"; Flags: nowait postinstall skipifsilent
