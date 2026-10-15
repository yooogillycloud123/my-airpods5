/*
 * AirPods 5 case identity R&D. Adapted from LibrePods BluetoothCryptography.verifyRPA:
 * https://github.com/librepods-org/librepods/blob/main/android/app/src/main/java/me/kavishdevar/librepods/utils/BluetoothCryptography.kt
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Matching a proximity IRK from EARBUD AACP is NOT sufficient to prove the
 * independent CASE GATT endpoint and NEVER authorizes a sound/GATT command.
 */
package caseproto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

enum class KeySource { EARBUD_AACP_PROXIMITY, CASE_OWNER_HANDSHAKE_VERIFIED }

enum class RpaDecision {
    INVALID_INPUT, NOT_RESOLVABLE_PRIVATE_ADDRESS, KEY_MISMATCH,
    MATCHES_KEY_BUT_CASE_IDENTITY_UNPROVEN,
    MATCHES_VERIFIED_CASE_KEY_STILL_NOT_SOUND_AUTHORIZED
}

object CaseRpaIdentity {
    private fun reverseAndEncrypt(key: ByteArray, data: ByteArray): ByteArray {
        require(key.size == 16 && data.size == 16)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.reversedArray(), "AES"))
        return cipher.doFinal(data.reversedArray()).reversedArray()
    }

    // Bluetooth ah(k,r), same byte ordering as upstream LibrePods.
    fun addressHash(irk: ByteArray, prand: ByteArray): ByteArray {
        require(irk.size == 16 && prand.size == 3)
        val padded = ByteArray(16)
        prand.copyInto(padded, 0)
        return reverseAndEncrypt(irk, padded).copyOfRange(0, 3)
    }

    fun assess(rotatingAddress: String, irk: ByteArray?, source: KeySource): RpaDecision {
        if (irk == null || irk.size != 16) return RpaDecision.INVALID_INPUT
        val fields = rotatingAddress.split(':')
        if (fields.size != 6 || fields.any { it.length != 2 || !it.matches(Regex("[0-9a-fA-F]{2}")) })
            return RpaDecision.INVALID_INPUT
        val addrLe = fields.reversed().map { it.toInt(16).toByte() }.toByteArray()
        val random = addrLe.copyOfRange(3, 6)
        if (random[2].toInt() and 0xc0 != 0x40)
            return RpaDecision.NOT_RESOLVABLE_PRIVATE_ADDRESS
        val advertisedHash = addrLe.copyOfRange(0, 3)
        val computed = addressHash(irk, random)
        if (!java.security.MessageDigest.isEqual(advertisedHash, computed))
            return RpaDecision.KEY_MISMATCH
        return when (source) {
            KeySource.EARBUD_AACP_PROXIMITY ->
                RpaDecision.MATCHES_KEY_BUT_CASE_IDENTITY_UNPROVEN
            KeySource.CASE_OWNER_HANDSHAKE_VERIFIED ->
                RpaDecision.MATCHES_VERIFIED_CASE_KEY_STILL_NOT_SOUND_AUTHORIZED
        }
    }
}
