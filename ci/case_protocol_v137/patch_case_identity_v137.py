#!/usr/bin/env python3
"""Patch v136 candidate picker with v137 readonly identity fields and safe standalone install."""
import pathlib, shutil, sys
app, repo = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
base = app/'android/app/src/main/java/me/kavishdevar/librepods'
src = base/'CasePathV134.kt'
text = src.read_text(encoding='utf-8')
def one(old, new):
    global text
    assert text.count(old) == 1, ('BAD_ANCHOR', old[:120], text.count(old))
    text = text.replace(old, new, 1)

one('        private var gatt: BluetoothGatt? = null',
    '        private var gatt: BluetoothGatt? = null\n        private var identityProbe: CaseIdentityReadV137? = null')
start_mark='                            val fd44 = g.services.any { s ->'
end_mark='\n                        }\n                    }, BluetoothDevice.TRANSPORT_LE)'
assert text.count(start_mark)==1 and text.count(end_mark)==1
st=text.index(start_mark)
en=text.index(end_mark,st)
assert 'finish("비소유자 소리 서비스 조회 결과"' in text[st:en]
text=text[:st]+'''                            // Read-only: no sound or owner writes, no pairing request.
                            handler.post {
                                if (!done.get()) {
                                    identityProbe = CaseIdentityReadV137(g) { result ->
                                        finish("Find My 후보 신원 분류 · 읽기 전용", result)
                                    }
                                    identityProbe?.start()
                                }
                            }'''+text[en:]
one('                        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {',
'''                        @Suppress("DEPRECATION")
                        override fun onCharacteristicRead(
                            g: BluetoothGatt,
                            characteristic: android.bluetooth.BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            if (android.os.Build.VERSION.SDK_INT < 33 && !done.get())
                                identityProbe?.onRead(characteristic, characteristic.value ?: byteArrayOf(), status)
                        }
                        override fun onCharacteristicRead(
                            g: BluetoothGatt,
                            characteristic: android.bluetooth.BluetoothGattCharacteristic,
                            value: ByteArray,
                            status: Int
                        ) {
                            if (!done.get()) identityProbe?.onRead(characteristic, value, status)
                        }
                        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {''')
one('}, 10000)', '}, 20000)')
one('"본인 기기라고 판단할 수 있는 후보만 직접 선택하면 GATT 서비스 식별만 합니다. " +',
    '"후보는 내 케이스라고 자동 판정되지 않습니다. 본인 기기임을 확인할 수 없으면 취소하세요. " +')
src.write_text(text, encoding='utf-8')
shutil.copy2(repo/'ci/case_protocol_v137/CaseIdentityReadV137.kt', base/'CaseIdentityReadV137.kt')

gradle=app/'android/app/build.gradle.kts'
s=gradle.read_text(encoding='utf-8')
for before,after in [
 ('applicationId = "com.myairpods5.personal.caseprobe"', 'applicationId = "com.myairpods5.personal.identityprobe"'),
 ('versionCode = 10036','versionCode = 10037'),
 ('versionName = "1.0.36-candidate-picker-parallel-safe"','versionName = "1.0.37-readonly-case-identity"')
]:
    assert s.count(before)==1,(before,s.count(before))
    s=s.replace(before,after,1)
gradle.write_text(s,encoding='utf-8')
manifest=app/'android/app/src/main/AndroidManifest.xml'
m=manifest.read_text(encoding='utf-8')
assert m.count('android:label="케이스 후보 확인 · LAB"')==1
m=m.replace('android:label="케이스 후보 확인 · LAB"', 'android:label="케이스 신원 확인 · LAB"',1)
assert 'android:authorities="com.myairpods5.personal.caseprobe' not in m
manifest.write_text(m,encoding='utf-8')
assert 'setItems(labels)' in text and 'CaseIdentityReadV137(g)' in text
assert 'createBond(' not in text and 'writeCharacteristic(' not in text and 'writeDescriptor(' not in text
ident=(base/'CaseIdentityReadV137.kt').read_text(encoding='utf-8')
assert 'readCharacteristic(' in ident and 'writeCharacteristic(' not in ident and 'createBond(' not in ident
assert '00002a24-' in ident and '00002a29-' in ident and '6aa50003-' in ident
assert '00002a25-' not in ident, 'serial reads must be excluded'
print('V137_READONLY_IDENTITY_CLASSIFIER=PASS')
print('V137_NO_GATT_WRITES_OR_PAIRING_CHANGES=PASS')
print('V137_NO_SERIAL_ADDRESS_RAW_TEXT=PASS')
print('V137_INDEPENDENT_PACKAGE_PRESERVES_V136=PASS')
