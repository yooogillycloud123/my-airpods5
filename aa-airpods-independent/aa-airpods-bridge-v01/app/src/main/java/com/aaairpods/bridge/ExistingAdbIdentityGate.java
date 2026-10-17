package com.aaairpods.bridge;

/**
 * Safe same-package update and daemon recovery rule.
 * A partially present cached ADB identity must NEVER be overwritten to
 * "repair" a connection: Android still trusts the old public certificate.
 *
 * Pure classifier; has no filesystem, radio, daemon, pairing or network I/O.
 */
public final class ExistingAdbIdentityGate {
    public enum State {
        FRESH_IDENTITY_ALLOWED,
        PRESERVE_EXISTING_IDENTITY,
        PARTIAL_IDENTITY_BLOCK_REPLACEMENT,
        INVALID_OR_MISMATCHED_IDENTITY_BLOCK_REPLACEMENT
    }
    private ExistingAdbIdentityGate() {}

    public static State classify(boolean privateKeyExists, boolean certExists,
                                 boolean privateKeyDecoded, boolean certDecoded,
                                 boolean pairCryptographicallyMatched) {
        if(!privateKeyExists&&!certExists) return State.FRESH_IDENTITY_ALLOWED;
        if(!privateKeyExists||!certExists)
            return State.PARTIAL_IDENTITY_BLOCK_REPLACEMENT;
        if(!privateKeyDecoded||!certDecoded||!pairCryptographicallyMatched)
            return State.INVALID_OR_MISMATCHED_IDENTITY_BLOCK_REPLACEMENT;
        return State.PRESERVE_EXISTING_IDENTITY;
    }

    public static boolean mayCreateNewIdentity(State state) {
        return state==State.FRESH_IDENTITY_ALLOWED;
    }
    public static String diagnostic(State state) {
        return "EXISTING_ADB_IDENTITY="+(state==null?"UNKNOWN":state.name())
            +"\nMISSING_OR_INVALID_PAIR_AUTOREPLACEMENT=FORBIDDEN"
            +"\nANDROID_PAIRED_DEVICES_NOT_MUTATED=YES"
            +"\nEXISTING_GOLDEN_TOKEN_NOT_TOUCHED=YES";
    }
}
