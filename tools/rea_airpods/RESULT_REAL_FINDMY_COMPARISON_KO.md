# REA 공개 APK 실제 분석 결과

GitHub Actions 38920099271 SUCCESS. REA 6.5.0 / JADX 0.7.1으로 AirGuard 공식 3.1.3 APK(공식 SHA-256 검증, DEX 하나)를 분석했다. AppleFindMy.tryStartProtocol(4691자), stopSound(2512자), writeCharacteristicRaw(1038자) 세 메서드의 디컴파일 소스 본문을 실제 확인했고 모두 coverage=complete이다.

공식 AirGuard 3.1.3 원본 코드는 Apple Find My/DULT/AirTag 등 **비소유자 분리 추적기용** 사운드 경로를 구현한다. 에어팟 광고 필터는 가운데 바이트를 마스크로 무시하므로 `12:19`만 찾는 형태가 아니다. 사용자의 `12:02` 13회 측정치에는 세 번째 상태 바이트가 없어서 실제 필터 매칭은 판단할 수 없다.

사용자 v136 FD44 비소유자 characteristic 존재는 본인 A3530 케이스 인증이 아니다. v139에서 12:19가 없다는 사실도 AirGuard Sound 기능을 확정적으로 무력화한다고 단정할 수 없다. **독립된 케이스 소유자 인증과 케이스 스피커 구동 성공은 미확인**이다. 반복 BLE 실험이나 새 LAB APK를 요구하지 않는다.

프로토콜 출처: https://github.com/seemoo-lab/AirGuard/tree/3.1.3/app/src/main/java/de/seemoo/at_tracking_detection/database/models/device/types/ . 실제 바이너리 검사: https://github.com/yooogillycloud123/my-airpods5/actions/runs/38920099271
