package com.aaairpods.bridge;

/**
 * Independent OneApp2 output-cycle coordinator.
 *
 * This class is intentionally free of the old Golden B process, old Bridge
 * package, external helper package names and daemon file paths. It owns its
 * request generation and repeat-cycle state. The Android route/focus adapter
 * belongs to the NEW application, not the existing installation.
 *
 * A requested route is never reported as physical audio success. Incomplete
 * vehicle evidence is INDETERMINATE, not a reason to reconnect Android Auto.
 */
public final class OneApp2UniversalController {
    public enum Output { UNKNOWN, VEHICLE, AIRPODS }
    public enum Decision { WAITING, AIRPODS_POLICY_READY, VEHICLE_EVIDENCE_READY,
                           INDETERMINATE, FAILED, STALE_CALLBACK }
    public enum Trigger { USER, PHYSICAL_DISCONNECT }

    public static final class Request {
        public final long generation;
        public final Output target;
        public final Trigger trigger;
        private Request(long generation, Output target, Trigger trigger) {
            this.generation = generation;
            this.target = target;
            this.trigger = trigger;
        }
    }

    public static final class Evidence {
        public final boolean directA2dp;
        public final boolean aaBypass;
        public final boolean mediaFocusArmed;
        public final boolean vehicleRouteSeen;
        public final boolean vehiclePcmSeen;
        public final boolean vehicleFocusSeen;
        public final boolean disconnected;
        public final boolean sameMediaOwner, huFocusGain, bottomHalfAvailable;
        public final boolean mediaOwnerFocusGain, androidAutoDisconnected;
        public final long evidenceStableSeconds;
        public Evidence(boolean directA2dp, boolean aaBypass, boolean mediaFocusArmed,
                        boolean vehicleRouteSeen, boolean vehiclePcmSeen,
                        boolean vehicleFocusSeen, boolean disconnected) {
            this(directA2dp,aaBypass,mediaFocusArmed,vehicleRouteSeen,vehiclePcmSeen,
                 vehicleFocusSeen,disconnected,false,false,false,false,true,0);
        }
        public Evidence(boolean directA2dp, boolean aaBypass, boolean mediaFocusArmed,
                        boolean vehicleRouteSeen, boolean vehiclePcmSeen,
                        boolean vehicleFocusSeen, boolean disconnected,
                        boolean sameMediaOwner,boolean huFocusGain,
                        boolean bottomHalfAvailable,boolean mediaOwnerFocusGain,
                        boolean androidAutoDisconnected,long evidenceStableSeconds) {
            this.directA2dp = directA2dp;
            this.aaBypass = aaBypass;
            this.mediaFocusArmed = mediaFocusArmed;
            this.vehicleRouteSeen = vehicleRouteSeen;
            this.vehiclePcmSeen = vehiclePcmSeen;
            this.vehicleFocusSeen = vehicleFocusSeen;
            this.disconnected = disconnected;
            this.sameMediaOwner=sameMediaOwner;
            this.huFocusGain=huFocusGain;
            this.bottomHalfAvailable=bottomHalfAvailable;
            this.mediaOwnerFocusGain=mediaOwnerFocusGain;
            this.androidAutoDisconnected=androidAutoDisconnected;
            this.evidenceStableSeconds=evidenceStableSeconds;
        }
    }

    private Output confirmed = Output.UNKNOWN;
    private Request pending;
    private long generation;
    private int completedCycles;
    private Decision lastDecision = Decision.INDETERMINATE;

    public synchronized Request request(Output target, Trigger trigger) {
        if (target == null || target == Output.UNKNOWN || trigger == null)
            throw new IllegalArgumentException("explicit valid target required");
        if (generation == Long.MAX_VALUE)
            throw new IllegalStateException("generation exhausted");
        if (trigger == Trigger.PHYSICAL_DISCONNECT
                && (target != Output.VEHICLE || confirmed != Output.AIRPODS))
            throw new IllegalStateException("disconnect restore requires confirmed AirPods");
        // A new explicit request supersedes a late vehicle-return callback,
        // including AirPods re-entry immediately after a previous restore.
        pending = new Request(++generation, target, trigger);
        lastDecision = Decision.WAITING;
        return pending;
    }

    public synchronized Decision observe(long requestGeneration, Evidence e) {
        if (pending == null || pending.generation != requestGeneration)
            return Decision.STALE_CALLBACK;
        if (e == null) {
            lastDecision = Decision.INDETERMINATE;
            return lastDecision;
        }
        if (e.disconnected && pending.target == Output.AIRPODS) {
            lastDecision = Decision.FAILED;
            return lastDecision;
        }
        if (pending.target == Output.AIRPODS) {
            if (e.directA2dp && e.aaBypass && e.mediaFocusArmed) {
                confirmed = Output.AIRPODS;
                lastDecision = Decision.AIRPODS_POLICY_READY;
                pending = null;
                return lastDecision;
            }
        } else {
            if (e.vehicleRouteSeen && OneApp2GoldenMechanics.stableVehicleHandoff(
                    e.sameMediaOwner,e.vehicleRouteSeen,e.huFocusGain,
                    e.bottomHalfAvailable,e.mediaOwnerFocusGain,
                    e.androidAutoDisconnected,e.evidenceStableSeconds)) {
                if (confirmed == Output.AIRPODS) completedCycles++;
                confirmed = Output.VEHICLE;
                lastDecision = Decision.VEHICLE_EVIDENCE_READY;
                pending = null;
                return lastDecision;
            }
        }
        lastDecision = Decision.INDETERMINATE;
        return lastDecision;
    }

    public synchronized Output confirmedOutput() { return confirmed; }
    public synchronized Decision lastDecision() { return lastDecision; }
    public synchronized long generation() { return generation; }
    public synchronized int completedCycles() { return completedCycles; }
    public synchronized boolean hasPendingRequest() { return pending != null; }
}
