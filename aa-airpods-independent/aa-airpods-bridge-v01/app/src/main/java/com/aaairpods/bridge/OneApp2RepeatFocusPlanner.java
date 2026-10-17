package com.aaairpods.bridge;

/**
 * Pure decision-only re-arm planner for the separate OneApp2 integration.
 * No AudioManager, no ADB, no shell commands and no calls into the installed
 * Golden controller. An Action is a recommendation to a future reviewed
 * coordinator, NOT an executed Focus or routing operation.
 *
 * Every callback is generation-scoped. The only Focus release recommendation
 * requires positive vehicle/HU/bottom/media-owner evidence (8 continuous
 * seconds), never a route transition or a Phone2 focus takeover alone.
 */
public final class OneApp2RepeatFocusPlanner {
    public enum Phase { IDLE, AIRPODS_WAIT_FOCUS, AIRPODS_FOCUS_READY,
                        VEHICLE_WAIT_EVIDENCE, VEHICLE_CONFIRMED, BLOCKED }
    public enum Action { PREARM_REQUIRED, FOCUS_CONFIRMED, REARM_REQUIRED,
                         HOLD_FOCUS_DURING_HU, WAIT_VEHICLE_EVIDENCE,
                         RELEASE_ONLY_AFTER_VEHICLE_PASS, BLOCKED,
                         INDETERMINATE, STALE_CALLBACK }
    public enum RestoreCause { EXPLICIT_USER, PHYSICAL_DISCONNECT }

    private long generation;
    private Phase phase=Phase.IDLE;
    private String ownerKey;
    private boolean priorAirpodsConfirmed;
    private int verifiedVehicleCycles;

    private static String key(OneApp2MediaOwnerSnapshot s) {
        if(s==null || !s.isEligibleOrdinaryMedia() || s.uid<0) return null;
        return s.packageName+"|"+s.uid;
    }

    public synchronized Action airpodsRequested(
            long nextGeneration, OneApp2MediaOwnerSnapshot owner,
            OneApp2GoldenMechanics.Mode mode, boolean bypass,
            boolean directA2dp, boolean connected) {
        if(nextGeneration<=generation) return Action.STALE_CALLBACK;
        generation=nextGeneration;
        priorAirpodsConfirmed=false;
        ownerKey=key(owner);
        if(!connected || ownerKey==null ||
                !OneApp2GoldenMechanics.positiveAirpodsIntent(mode,bypass,directA2dp)) {
            phase=Phase.BLOCKED;
            return Action.BLOCKED;
        }
        phase=Phase.AIRPODS_WAIT_FOCUS;
        return Action.PREARM_REQUIRED;
    }

    public synchronized Action focusObserved(
            long callbackGeneration, OneApp2MediaOwnerSnapshot owner,
            boolean externalPolicyRegistered,
            OneApp2GoldenMechanics.Mode mode, boolean bypass,
            boolean directA2dp, boolean connected) {
        if(callbackGeneration!=generation) return Action.STALE_CALLBACK;
        if(phase!=Phase.AIRPODS_WAIT_FOCUS && phase!=Phase.AIRPODS_FOCUS_READY)
            return Action.BLOCKED;
        if(!connected ||
                !OneApp2GoldenMechanics.positiveAirpodsIntent(mode,bypass,directA2dp))
            return Action.INDETERMINATE;
        String observed=key(owner);
        if(observed==null) return Action.INDETERMINATE;
        if(!observed.equals(ownerKey)) {
            // New media owner after app switch: one new arm cycle.
            ownerKey=observed;
            phase=Phase.AIRPODS_WAIT_FOCUS;
            priorAirpodsConfirmed=false;
            return Action.REARM_REQUIRED;
        }
        if(!externalPolicyRegistered) {
            // Missing Focus while intent remains positive is a re-arm
            // *recommendation*, never a permission to kill or reset a route.
            Action next=phase==Phase.AIRPODS_FOCUS_READY
                ? Action.REARM_REQUIRED : Action.PREARM_REQUIRED;
            phase=Phase.AIRPODS_WAIT_FOCUS;
            priorAirpodsConfirmed=false;
            return next;
        }
        phase=Phase.AIRPODS_FOCUS_READY;
        priorAirpodsConfirmed=true;
        return Action.FOCUS_CONFIRMED;
    }

    public synchronized Action huTakeoverObserved(
            long callbackGeneration, boolean sameMediaOwner,
            boolean previousA2dp, boolean nowAaRemote,
            boolean airpodsConnected, OneApp2GoldenMechanics.Mode mode,
            boolean bypass, boolean directA2dp) {
        if(callbackGeneration!=generation) return Action.STALE_CALLBACK;
        if(phase!=Phase.AIRPODS_FOCUS_READY && phase!=Phase.AIRPODS_WAIT_FOCUS)
            return Action.INDETERMINATE;
        if(OneApp2GoldenMechanics.suppressFalseManualRestore(
                sameMediaOwner,previousA2dp,nowAaRemote,airpodsConnected,
                mode,bypass,directA2dp))
            return Action.HOLD_FOCUS_DURING_HU;
        return Action.INDETERMINATE;
    }

    public synchronized Action vehicleRequested(long nextGeneration,
                                                 RestoreCause cause) {
        if(nextGeneration<=generation) return Action.STALE_CALLBACK;
        if(cause==null) return Action.BLOCKED;
        if(cause==RestoreCause.PHYSICAL_DISCONNECT && !priorAirpodsConfirmed)
            return Action.BLOCKED;
        generation=nextGeneration;
        // Do not release Focus based solely on Basic Output intent.
        phase=Phase.VEHICLE_WAIT_EVIDENCE;
        return Action.WAIT_VEHICLE_EVIDENCE;
    }

    public synchronized Action vehicleEvidence(
            long callbackGeneration, OneApp2UniversalController.Evidence e) {
        if(callbackGeneration!=generation) return Action.STALE_CALLBACK;
        if(phase!=Phase.VEHICLE_WAIT_EVIDENCE || e==null)
            return Action.INDETERMINATE;
        if(!OneApp2GoldenMechanics.stableVehicleHandoff(
                e.sameMediaOwner,e.vehicleRouteSeen,e.huFocusGain,
                e.bottomHalfAvailable,e.mediaOwnerFocusGain,
                e.androidAutoDisconnected,e.evidenceStableSeconds))
            return Action.WAIT_VEHICLE_EVIDENCE;
        if(priorAirpodsConfirmed) verifiedVehicleCycles++;
        priorAirpodsConfirmed=false;
        ownerKey=null;
        phase=Phase.VEHICLE_CONFIRMED;
        return Action.RELEASE_ONLY_AFTER_VEHICLE_PASS;
    }

    public synchronized Phase phase(){return phase;}
    public synchronized long generation(){return generation;}
    public synchronized int verifiedVehicleCycles(){return verifiedVehicleCycles;}
    public synchronized boolean hasVerifiedAirpodsFocus(){return priorAirpodsConfirmed;}
}
