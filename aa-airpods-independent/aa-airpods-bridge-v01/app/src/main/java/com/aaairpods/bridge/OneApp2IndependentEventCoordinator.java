package com.aaairpods.bridge;

/**
 * Own-process event ingress: real Android A2DP removal -> the new app's
 * independent cycle state. It never calls existing Bridge, edge23 or
 * edge24. A real route restore still needs an exclusively owned new worker.
 */
public final class OneApp2IndependentEventCoordinator {
    private final OneApp2IndependentCycleEngine cycle=
        new OneApp2IndependentCycleEngine();
    private long physicalEvents;
    private long gatedRestoreIntents;
    private String last="NO_DEVICE_EVENT";
    public synchronized String accept(OneApp2LiveOutputObserver.Snapshot live){
        if(live==null)return report();
        if(live.verifiedAirpodsRemoved){
            physicalEvents++;
            if(cycle.stage()==OneApp2IndependentCycleEngine.Stage.AIRPODS_POLICY_ARMED){
                long generation=cycle.vehicleIntent(true);
                if(generation>0){
                    gatedRestoreIntents++;
                    last="INTERNAL_VEHICLE_RESTORE_INTENT_"+generation;
                }else last="DISCONNECT_RESTORE_NOT_ARMED";
            }else last="PHYSICAL_DISCONNECT_DETECTED_BUT_OWN_FOCUS_NOT_PROVEN";
        }else if(live.airpods==OneApp2LiveOutputObserver.Airpods.VERIFIED_A2DP){
            last="EXACT_OWNER_A2DP_OUTPUT_VISIBLE";
        }else{
            last="NO_EXACT_OWNER_A2DP_ROUTE_PROOF";
        }
        return report();
    }
    public synchronized String report(){
        return "ONEAPP2_INTERNAL_DEVICE_CYCLE_INGRESS\n"
            +"LAST_EVENT="+last
            +"\nVERIFIED_DISCONNECT_EVENTS="+physicalEvents
            +"\nGATED_NEW_APP_RESTORE_INTENTS="+gatedRestoreIntents
            +"\nNEW_APP_CYCLE_STAGE="+cycle.stage().name()
            +"\nINDEPENDENT_WORKER_ACTIVE=NO"
            +"\nVEHICLE_PCM_PHYSICAL_PROOF=NO"
            +"\nEXTERNAL_ORIGINAL_HELPERS_CALLED=NO"
            +"\nAUTOMATIC_ANDROID_AUTO_RECONNECT=NO";
    }
}
