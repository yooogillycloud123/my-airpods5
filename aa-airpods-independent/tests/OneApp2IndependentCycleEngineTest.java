package com.aaairpods.bridge;

/**
 * Regression for the real independent route/Focusing state machine.
 * This runs on Java 17 without a phone and never claims audible playback.
 */
public final class OneApp2IndependentCycleEngineTest {
    private static int checks;
    private static void check(boolean value,String label) {
        if(!value)throw new AssertionError(label);
        checks++;
    }
    private static void eq(Object got,Object want,String label) {
        check(want.equals(got),label+" got="+got+" expected="+want);
    }
    private static OneApp2MediaOwnerSnapshot owner(String pkg,int uid) {
        return OneApp2MediaOwnerSnapshot.parse(
            pkg+"|"+uid+"|22651|1721|9531|AIRPODS_A2DP|1|1");
    }
    private static OneApp2UniversalController.Evidence vehicleProof(long seconds) {
        return new OneApp2UniversalController.Evidence(
            false,false,false,true,true,true,false,
            true,true,true,true,false,seconds);
    }
    public static void main(String[] args) {
        OneApp2IndependentCycleEngine engine=new OneApp2IndependentCycleEngine();
        OneApp2MediaOwnerSnapshot ytm=owner("com.google.android.apps.youtube.music",10189);
        OneApp2MediaOwnerSnapshot netflix=owner("com.netflix.mediaclient",10311);
        OneApp2MediaOwnerSnapshot youtube=owner("com.google.android.youtube",10401);
        OneApp2GoldenMechanics.Mode on=OneApp2GoldenMechanics.Mode.TRUE;
        OneApp2MediaOwnerSnapshot[] initial={ytm,netflix,youtube};
        for(int cycle=1;cycle<=3;cycle++) {
            OneApp2MediaOwnerSnapshot current=initial[cycle-1];
            long airpods=engine.airpodsIntent(current,on,true,true,true);
            check(airpods>0,"cycle "+cycle+" new AirPods generation");
            eq(engine.confirmAirpodsFocus(airpods,current,true,on,true,true,true),
                OneApp2IndependentCycleEngine.Result.FOCUS_ARMED,
                "cycle "+cycle+" initial grant");
            // Switch between ordinary media apps without changing route generation.
            OneApp2MediaOwnerSnapshot switched=initial[cycle%3];
            eq(engine.confirmAirpodsFocus(airpods,switched,true,on,true,true,true),
                OneApp2IndependentCycleEngine.Result.REARM_REQUIRED,
                "cycle "+cycle+" app switch demands focus rearm");
            eq(engine.confirmAirpodsFocus(airpods,switched,true,on,true,true,true),
                OneApp2IndependentCycleEngine.Result.FOCUS_ARMED,
                "cycle "+cycle+" second grant does not replay stale route observe");
            eq(engine.confirmAirpodsFocus(airpods,switched,false,on,true,true,true),
                OneApp2IndependentCycleEngine.Result.REARM_REQUIRED,
                "cycle "+cycle+" dropped policy rearm");
            eq(engine.confirmAirpodsFocus(airpods,switched,true,on,true,true,true),
                OneApp2IndependentCycleEngine.Result.FOCUS_ARMED,
                "cycle "+cycle+" reacquired policy in same generation");

            long vehicle=engine.vehicleIntent(false);
            check(vehicle>airpods,"cycle "+cycle+" vehicle generation advances");
            eq(engine.vehicleEvidence(vehicle,vehicleProof(7)),
                OneApp2IndependentCycleEngine.Result.WAIT_VEHICLE_EVIDENCE,
                "cycle "+cycle+" vehicle focus evidence not yet stable");
            eq(engine.vehicleEvidence(vehicle,vehicleProof(8)),
                OneApp2IndependentCycleEngine.Result.VEHICLE_EVIDENCE_CONFIRMED,
                "cycle "+cycle+" 8s HU and bottom evidence");
            check(engine.evidenceVerifiedCycles()==cycle,
                "cycle "+cycle+" verified return counted exactly once");
        }
        System.out.println("ONEAPP2_INDEPENDENT_CYCLE_HOST_ASSERTIONS="+checks);
        System.out.println("REPEAT_3_CYCLES_APP_SWITCH_FOCUS_REARM=HOST_PASS");
        System.out.println("ACTUAL_EXTERNAL_AUDIO_POLICY=NOT_TESTED");
        System.out.println("ANDROID_APK=NOT_BUILT");
    }
}
