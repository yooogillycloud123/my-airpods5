# MX Return FLOW public build mirror

이 디렉터리는 기존 private 저장소의 `mxreturn-flow-v040` 작업을 public Actions 빌드용으로 옮긴 미러입니다.

- Android 소스: `app/`
- Windows FLOW 에이전트: `pc-flow/`
- Android 빌드: 저장소 루트의 `.github/workflows/build-mx-return-flow.yml`
- 브랜치: `mxreturn-flow-v040`

기존 OHO+ 슬롯 방식은 fallback으로 유지하고, v0.4.0부터는 화면 가장자리 마우스 이동 기반 FLOW를 추가합니다.
