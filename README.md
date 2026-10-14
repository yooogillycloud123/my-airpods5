# 내 에어팟 5 · My AirPods 5 (Korean LibrePods Edition)

이 저장소는 **원본 [LibrePods](https://github.com/librepods-org/librepods)의 GPL-3.0-or-later 오픈소스 Android 앱**을 기반으로 한 별도 한국어 에어팟 5 앱을 빌드하기 위한 공개 저장소입니다.

- 패키지: `com.myairpods5.personal`
- 입력: GitHub 저장소 루트의 소스 ZIP (LibrePods 기반 수정 소스 및 GPL 라이선스 포함)
- 빌드: `.github/workflows/build-my-airpods5.yml`
- 결과: 이 저장소의 **Releases**에 APK 테스트 후보가 생성됩니다.
- 이전에 삼성 기기 보호에서 차단된 **재패키징 APK는 사용하지 않습니다**.
- 실제 삼성 휴대전화 설치, 에어팟 5 연결 및 청취 모드·배터리 기능은 별도 실기기 검증이 필요합니다.
- GitHub Actions 빌드마다 디버그 서명키가 달라질 수 있으므로 동일 앱 업데이트 시 서명 충돌이 발생할 수 있습니다. 공개 저장소에 개인 서명키를 업로드하지 마세요.
- 빌드는 공개 저장소 표준 GitHub Actions 실행기를 이용하며, 사용 조건은 GitHub 정책을 따릅니다.

GPL 원본 프로젝트 및 라이선스: https://github.com/librepods-org/librepods
