package caseproto

object CaseRpaIdentityTest {
    private fun checkEqual(got: RpaDecision, expected: RpaDecision, name: String) {
        check(got == expected) { "$name expected $expected got $got" }
        println("PASS $name")
    }

    @JvmStatic fun main(args: Array<String>) {
        val irk = ByteArray(16) { (it * 7 + 3).toByte() }
        val prand = byteArrayOf(0x15, 0x27, 0x41)
        val hash = CaseRpaIdentity.addressHash(irk, prand)
        // Independently checked with Python cryptography AES-ECB, not self-derived.
        check(hash.joinToString("") { "%02x".format(it.toInt() and 255) } == "2874ee")
        println("PASS independent_AES128_IRK_vector")
        val addrLittle = hash + prand
        val address = addrLittle.reversedArray().joinToString(":") { "%02X".format(it.toInt() and 255) }
        check(address == "41:27:15:EE:74:28")
        checkEqual(CaseRpaIdentity.assess(address, irk, KeySource.EARBUD_AACP_PROXIMITY),
            RpaDecision.MATCHES_KEY_BUT_CASE_IDENTITY_UNPROVEN,
            "earbud_key_is_not_case_proof")
        checkEqual(CaseRpaIdentity.assess(address, irk, KeySource.CASE_OWNER_HANDSHAKE_VERIFIED),
            RpaDecision.MATCHES_VERIFIED_CASE_KEY_STILL_NOT_SOUND_AUTHORIZED,
            "even_matched_case_key_not_sound_authorized")
        val wrong = ByteArray(16) { (it * 11 + 4).toByte() }
        checkEqual(CaseRpaIdentity.assess(address, wrong, KeySource.EARBUD_AACP_PROXIMITY),
            RpaDecision.KEY_MISMATCH, "wrong_key_rejected")
        checkEqual(CaseRpaIdentity.assess("not_a_mac", irk, KeySource.EARBUD_AACP_PROXIMITY),
            RpaDecision.INVALID_INPUT, "malformed_mac_rejected")
        checkEqual(CaseRpaIdentity.assess(address, null, KeySource.EARBUD_AACP_PROXIMITY),
            RpaDecision.INVALID_INPUT, "missing_key_rejected")
        checkEqual(CaseRpaIdentity.assess("83:22:11:33:22:11", irk, KeySource.EARBUD_AACP_PROXIMITY),
            RpaDecision.NOT_RESOLVABLE_PRIVATE_ADDRESS, "non_RPA_random_address_rejected")
        val altered = address.replaceRange(15, 17,
            if (address.substring(15,17) == "01") "02" else "01")
        checkEqual(CaseRpaIdentity.assess(altered, irk, KeySource.EARBUD_AACP_PROXIMITY),
            RpaDecision.KEY_MISMATCH, "altered_address_rejected")
        println("SOURCE_BASED_OFFLINE_EIGHT_TESTS=PASS")
    }
}
