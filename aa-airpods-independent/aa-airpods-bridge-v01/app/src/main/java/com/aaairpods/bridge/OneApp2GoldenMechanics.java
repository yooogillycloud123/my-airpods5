package com.aaairpods.bridge;

/**
 * Pure, in-package behavioral port of the ACTUAL physical-pass Golden B
 * Controller SHA-256 49364923ba83acaa02d4e6a2b58251df7697182ff82a4b33ae22abbc94764623.
 *
 * This is NOT the original shell controller and never accesses original app
 * private storage. Its predicates intentionally match Golden's intent,
 * re-entry cancel, and evidence-first vehicle handoff rules. Runtime routing,
 * focus/owner probes, and service lifecycle are separate integrations.
 */
public final class OneApp2GoldenMechanics {
    public static final int MANUAL_RESTORE_GUARD_SECONDS=6;
    public static final int AIRPODS_DISCONNECT_GUARD_SECONDS=1;
    public static final int VEHICLE_EVIDENCE_STABLE_SECONDS=8;
    public static final int FIRST_HANDOFF_WAIT_SECONDS=14;
    public static final int BOUNDED_LOCAL_RETRY_SECONDS=12;
    public static final int VEHICLE_LOG_SCAN_MAX_BYTES=262144;
    private OneApp2GoldenMechanics(){}
    public enum Mode { TRUE, FALSE, UNKNOWN }

    public static boolean positiveAirpodsIntent(Mode explicitMode,
                                                 boolean aaBypass,
                                                 boolean directA2dp) {
        return explicitMode==Mode.TRUE && (aaBypass || directA2dp);
    }

    /** Exact Golden re-entry cancellation: mode AND bypass AND direct. */
    public static boolean cancelStaleVehicleRestore(Mode explicitMode,
                                                     boolean aaBypass,
                                                     boolean directA2dp) {
        return explicitMode==Mode.TRUE && aaBypass && directA2dp;
    }

    /** Golden generic focus pre-arm is a one-shot after an attempted restore. */
    public static boolean shouldPrearm(boolean rearmNeeded,
                                       boolean currentOwnerKnown,
                                       boolean airpodsConnected,
                                       Mode explicitMode,
                                       boolean aaBypass,
                                       boolean directA2dp) {
        return rearmNeeded && currentOwnerKnown && airpodsConnected
            && positiveAirpodsIntent(explicitMode,aaBypass,directA2dp);
    }

    /** Phone2/HU takeover is NOT user Basic Output while AirPods intent is positive. */
    public static boolean suppressFalseManualRestore(
            boolean sameMediaOwner,
            boolean previouslyAirpods,
            boolean nowAaRemote,
            boolean airpodsConnected,
            Mode explicitMode,
            boolean aaBypass,
            boolean directA2dp) {
        return sameMediaOwner && previouslyAirpods && nowAaRemote
            && airpodsConnected
            && positiveAirpodsIntent(explicitMode,aaBypass,directA2dp);
    }

    /** Exact Golden handoff: 8 real seconds of continuous HU + bottom + focus chain. */
    public static boolean stableVehicleHandoff(
            boolean sameMediaOwner,
            boolean nowAaRemote,
            boolean huFocusGain,
            boolean bottomHalfAvailable,
            boolean mediaOwnerFocusGain,
            boolean androidAutoDisconnected,
            long evidenceStableSeconds) {
        return sameMediaOwner && nowAaRemote && huFocusGain
            && bottomHalfAvailable && mediaOwnerFocusGain
            && !androidAutoDisconnected && evidenceStableSeconds>=8;
    }

    /**
     * If the vehicle route and HU with bottom-half or PCM appear but full
     * focus proof is absent, Golden does NOT force an AA reconnect or label PASS.
     */
    public static boolean vehicleEvidenceIndeterminate(
            boolean vehicleRouteSeen,
            boolean huSeen,
            boolean bottomHalfSeen,
            boolean pcmSeen,
            boolean androidAutoDisconnected) {
        return vehicleRouteSeen && huSeen
            && (bottomHalfSeen || pcmSeen) && !androidAutoDisconnected;
    }
}
