package com.aaairpods.bridge;

/**
 * NEW independent app's repeated media-cycle coordinator.
 * Ports original Golden's evidence-first restore/rearm ordering into a
 * single generation-scoped state model. Android IPC/Focus writers are NOT
 * part of this component and cannot be invoked by it.
 *
 * Requires evidence from an independent source; legacy Bridge callbacks are
 * never accepted as proof that this OneApp2 instance routed audio.
 */
public final class OneApp2IndependentCycleEngine {
    public enum Stage { IDLE, AIRPODS_PENDING, AIRPODS_POLICY_ARMED,
                        VEHICLE_PENDING, VEHICLE_EVIDENCE_CONFIRMED }
    public enum Result { NEEDS_FOCUS_PREARM, FOCUS_ARMED,
                         REARM_REQUIRED, WAIT_VEHICLE_EVIDENCE,
                         VEHICLE_EVIDENCE_CONFIRMED, STALE, BLOCKED }

    private final OneApp2UniversalController route=
        new OneApp2UniversalController();
    private final OneApp2RepeatFocusPlanner focus=
        new OneApp2RepeatFocusPlanner();
    private Stage stage=Stage.IDLE;

    public synchronized long airpodsIntent(
            OneApp2MediaOwnerSnapshot media,
            OneApp2GoldenMechanics.Mode mode, boolean bypass,
            boolean directA2dp, boolean physicallyConnected) {
        if(media==null || !media.isEligibleOrdinaryMedia()
           || !OneApp2GoldenMechanics.positiveAirpodsIntent(mode,bypass,directA2dp)
           || !physicallyConnected)
            return -1;
        OneApp2UniversalController.Request r=route.request(
             OneApp2UniversalController.Output.AIRPODS,
             OneApp2UniversalController.Trigger.USER);
        OneApp2RepeatFocusPlanner.Action a=focus.airpodsRequested(
             r.generation,media,mode,bypass,directA2dp,physicallyConnected);
        if(a!=OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED)
            return -1;
        stage=Stage.AIRPODS_PENDING;
        return r.generation;
    }

    public synchronized Result confirmAirpodsFocus(
            long generation,OneApp2MediaOwnerSnapshot media,
            boolean externalPolicy,OneApp2GoldenMechanics.Mode mode,
            boolean bypass,boolean directA2dp,boolean connected) {
        if(stage!=Stage.AIRPODS_PENDING&&stage!=Stage.AIRPODS_POLICY_ARMED)
            return Result.BLOCKED;
        OneApp2RepeatFocusPlanner.Action a=focus.focusObserved(
            generation,media,externalPolicy,mode,bypass,directA2dp,connected);
        if(a==OneApp2RepeatFocusPlanner.Action.STALE_CALLBACK)
            return Result.STALE;
        if(a==OneApp2RepeatFocusPlanner.Action.REARM_REQUIRED ||
           a==OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED)
            return Result.REARM_REQUIRED;
        if(a!=OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED)
            return Result.BLOCKED;
        OneApp2UniversalController.Decision d=route.observe(
            generation,new OneApp2UniversalController.Evidence(
                directA2dp,bypass,true,false,false,false,false));
        if(d!=OneApp2UniversalController.Decision.AIRPODS_POLICY_READY)
            return Result.BLOCKED;
        stage=Stage.AIRPODS_POLICY_ARMED;
        return Result.FOCUS_ARMED;
    }

    public synchronized long vehicleIntent(boolean physicalDisconnect) {
        if(stage!=Stage.AIRPODS_POLICY_ARMED)return -1;
        OneApp2UniversalController.Trigger trigger=physicalDisconnect
             ? OneApp2UniversalController.Trigger.PHYSICAL_DISCONNECT
             : OneApp2UniversalController.Trigger.USER;
        OneApp2UniversalController.Request r=route.request(
            OneApp2UniversalController.Output.VEHICLE,trigger);
        OneApp2RepeatFocusPlanner.Action a=focus.vehicleRequested(
            r.generation,physicalDisconnect
                ? OneApp2RepeatFocusPlanner.RestoreCause.PHYSICAL_DISCONNECT
                : OneApp2RepeatFocusPlanner.RestoreCause.EXPLICIT_USER);
        if(a!=OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE)
            return -1;
        stage=Stage.VEHICLE_PENDING;
        return r.generation;
    }

    public synchronized Result vehicleEvidence(long generation,
                 OneApp2UniversalController.Evidence proof) {
        if(stage!=Stage.VEHICLE_PENDING)return Result.BLOCKED;
        OneApp2RepeatFocusPlanner.Action a=focus.vehicleEvidence(
            generation,proof);
        if(a==OneApp2RepeatFocusPlanner.Action.STALE_CALLBACK)
            return Result.STALE;
        if(a!=OneApp2RepeatFocusPlanner.Action.RELEASE_ONLY_AFTER_VEHICLE_PASS)
            return Result.WAIT_VEHICLE_EVIDENCE;
        OneApp2UniversalController.Decision confirmed=
            route.observe(generation,proof);
        if(confirmed!=OneApp2UniversalController.Decision.VEHICLE_EVIDENCE_READY)
            return Result.BLOCKED;
        stage=Stage.VEHICLE_EVIDENCE_CONFIRMED;
        return Result.VEHICLE_EVIDENCE_CONFIRMED;
    }

    public synchronized Stage stage(){return stage;}
    public synchronized int evidenceVerifiedCycles(){
        return route.completedCycles();
    }
    public synchronized long generation(){return route.generation();}
    public synchronized String report(){
        return "NEW_ONEAPP2_CYCLE_ENGINE_SOURCE_ONLY\n"
            +"STAGE="+stage.name()
            +"\nROUTE_GENERATION="+route.generation()
            +"\nVEHICLE_EVIDENCE_CYCLES="+route.completedCycles()
            +"\nFOCUS_EVIDENCE_CYCLES="+focus.verifiedVehicleCycles()
            +"\nOLD_GOLDEN_DAEMON_ACCESSED=NO"
            +"\nGLOBAL_AUDIO_POLICY_MODIFIED=NO";
    }
}
