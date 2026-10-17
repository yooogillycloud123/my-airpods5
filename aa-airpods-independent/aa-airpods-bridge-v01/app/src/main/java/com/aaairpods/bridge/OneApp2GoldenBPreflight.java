package com.aaairpods.bridge;

/**
 * Read-only Golden B repeat-cycle safety preflight for the independent app.
 * Does not request AudioFocus, change any AudioPolicy, run shell commands,
 * connect to legacy helpers, or assume vehicle audio is audible.
 */
public final class OneApp2GoldenBPreflight {
    public enum Result {
        ORIGINAL_GOLDEN_ACTIVE, GLOBAL_FOCUS_OWNER_UNKNOWN,
        OWN_DAEMON_UNVERIFIED, MEDIA_OWNER_UNVERIFIED,
        REARM_GENERATION_UNVERIFIED, AIRPODS_ROUTE_UNVERIFIED,
        VEHICLE_HU_PCM_UNVERIFIED, USER_AUDIBLE_CONFIRMATION_REQUIRED,
        CANDIDATE_EVIDENCE_READY
    }
    public static final class Evidence {
        public final boolean originalGoldenListening, exclusiveFocusOwnerMeasured,
            independentDaemonAuthenticated, mediaOwnerUidObserved,
            newFocusGrantObserved, aaBypassOn, airpodsDirectRenderOn,
            vehicleRouteObserved, huFocusObserved, bottomHalfOrPcmObserved,
            userHeardVehicle;
        public final long newFocusGeneration, vehicleStableMs;
        public Evidence(boolean original,boolean exclusive,boolean daemon,
                        boolean owner,long generation,boolean focusGrant,
                        boolean bypass,boolean direct,boolean vehicle,
                        boolean hu,boolean pcm,long stableMs,boolean heard){
            originalGoldenListening=original;
            exclusiveFocusOwnerMeasured=exclusive;
            independentDaemonAuthenticated=daemon;mediaOwnerUidObserved=owner;
            newFocusGeneration=generation;newFocusGrantObserved=focusGrant;
            aaBypassOn=bypass;airpodsDirectRenderOn=direct;
            vehicleRouteObserved=vehicle;huFocusObserved=hu;
            bottomHalfOrPcmObserved=pcm;vehicleStableMs=stableMs;
            userHeardVehicle=heard;
        }
    }
    private long previousGeneration, passedCycles;
    public synchronized Result check(Evidence e){
        if(e==null)return Result.GLOBAL_FOCUS_OWNER_UNKNOWN;
        if(e.originalGoldenListening)return Result.ORIGINAL_GOLDEN_ACTIVE;
        if(!e.exclusiveFocusOwnerMeasured)return Result.GLOBAL_FOCUS_OWNER_UNKNOWN;
        if(!e.independentDaemonAuthenticated)return Result.OWN_DAEMON_UNVERIFIED;
        if(!e.mediaOwnerUidObserved)return Result.MEDIA_OWNER_UNVERIFIED;
        if(!e.newFocusGrantObserved || e.newFocusGeneration<=previousGeneration)
            return Result.REARM_GENERATION_UNVERIFIED;
        if(!e.aaBypassOn||!e.airpodsDirectRenderOn)
            return Result.AIRPODS_ROUTE_UNVERIFIED;
        if(!e.vehicleRouteObserved||!e.huFocusObserved||
                !e.bottomHalfOrPcmObserved||e.vehicleStableMs<8000)
            return Result.VEHICLE_HU_PCM_UNVERIFIED;
        if(!e.userHeardVehicle)return Result.USER_AUDIBLE_CONFIRMATION_REQUIRED;
        previousGeneration=e.newFocusGeneration;
        passedCycles++;
        return Result.CANDIDATE_EVIDENCE_READY;
    }
    public synchronized long passedCycles(){return passedCycles;}
}
