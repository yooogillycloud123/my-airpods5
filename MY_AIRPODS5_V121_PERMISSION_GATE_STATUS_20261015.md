# Galaxy 1.0.20 permission failure (2026-10-15)

User SDK37, LAB package, no Java fatal. The CaseOwnerProbeV120.open method checks BLUETOOTH_SCAN, BLUETOOTH_CONNECT and ACCESS_FINE_LOCATION but has no runtime request and prints a generic permission denial. It did not reach Bluetooth scan or GATT. Permission missing category unspecified.

v121 lab patch: move IRK existence check before sensitive permission prompts; request missing Bluetooth permissions and ACCESS_COARSE_LOCATION+ACCESS_FINE_LOCATION together (Android 12+ requirement); add settings fallback. CI builds separate Lab with user-v1.0.10 Golden unchanged. The case speaker command is still unknown. The immediate existing-v120 workaround is permission grant in Android app settings (nearby devices and precise location) without reinstall.
