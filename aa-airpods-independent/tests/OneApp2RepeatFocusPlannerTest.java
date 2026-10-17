package com.aaairpods.bridge;

/** Deterministic offline replay: no device, adb, radio, policy or daemon. */
public final class OneApp2RepeatFocusPlannerTest {
    private static int checks;
    private static void eq(Object actual,Object expected,String what){
        if(!expected.equals(actual))throw new AssertionError(what+": "+actual+" != "+expected);
        checks++;
    }
    private static void ok(boolean good,String what){
        if(!good)throw new AssertionError(what);
        checks++;
    }
    private static OneApp2MediaOwnerSnapshot owner(String pkg,int uid){
        return OneApp2MediaOwnerSnapshot.parse(
            pkg+"|"+uid+"|22651|1721|9531|AIRPODS_A2DP|1|1");
    }
    private static OneApp2UniversalController.Evidence evidence(int stable,
                                                                 boolean hu,boolean bottom,
                                                                 boolean aaDisconnected) {
        return new OneApp2UniversalController.Evidence(false,false,false,
            true,false,true,false,
            true,hu,bottom,true,aaDisconnected,stable);
    }
    public static void main(String[] a) {
        OneApp2RepeatFocusPlanner p=new OneApp2RepeatFocusPlanner();
        OneApp2MediaOwnerSnapshot ytm=owner("com.google.android.apps.youtube.music",10189);
        OneApp2MediaOwnerSnapshot netflix=owner("com.netflix.mediaclient",10311);
        OneApp2MediaOwnerSnapshot youtube=owner("com.google.android.youtube",10401);
        OneApp2MediaOwnerSnapshot gearhead=owner("com.google.android.projection.gearhead",10031);
        OneApp2GoldenMechanics.Mode ON=OneApp2GoldenMechanics.Mode.TRUE;
        OneApp2GoldenMechanics.Mode OFF=OneApp2GoldenMechanics.Mode.FALSE;

        eq(p.airpodsRequested(1,ytm,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED,"initial prearm");
        eq(p.focusObserved(1,ytm,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED,"YTM focus");
        ok(p.hasVerifiedAirpodsFocus(),"focus confirmed");
        eq(p.huTakeoverObserved(1,true,true,true,true,ON,true,true),
           OneApp2RepeatFocusPlanner.Action.HOLD_FOCUS_DURING_HU,
           "Phone2/HU takeover cannot be manual restore");
        eq(p.vehicleRequested(2,OneApp2RepeatFocusPlanner.RestoreCause.EXPLICIT_USER),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,
           "do not release on click");
        eq(p.vehicleEvidence(2,evidence(4,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,"4sec not 8");
        eq(p.vehicleEvidence(2,evidence(8,true,false,false)),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,"bottom missing");
        eq(p.vehicleEvidence(2,evidence(8,false,true,false)),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,"HU missing");
        eq(p.vehicleEvidence(2,evidence(8,true,true,true)),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,"AA disconnect forbidden");
        eq(p.vehicleEvidence(2,evidence(8,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.RELEASE_ONLY_AFTER_VEHICLE_PASS,
           "vehicle positive evidence");
        eq(p.verifiedVehicleCycles(),1,"first cycle");

        eq(p.airpodsRequested(3,netflix,ON,true,false,true),
           OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED,"second-cycle explicit intent");
        eq(p.vehicleEvidence(2,evidence(9,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.STALE_CALLBACK,"cancel late restore");
        eq(p.focusObserved(2,ytm,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.STALE_CALLBACK,"ignore late YTM focus");
        eq(p.focusObserved(3,netflix,false,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED,"Netflix focus pre-arm");
        eq(p.focusObserved(3,netflix,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED,"Netflix focus");
        eq(p.huTakeoverObserved(3,true,true,true,true,ON,true,true),
           OneApp2RepeatFocusPlanner.Action.HOLD_FOCUS_DURING_HU,
           "second-cycle Phone2 takeover held");
        eq(p.focusObserved(3,youtube,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.REARM_REQUIRED,"new ordinary media owner");
        eq(p.focusObserved(3,youtube,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED,"YouTube re-arm confirmed");
        eq(p.focusObserved(3,youtube,false,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.REARM_REQUIRED,"external Focus dropped");
        eq(p.focusObserved(3,youtube,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED,"Focus restored");

        eq(p.vehicleRequested(4,OneApp2RepeatFocusPlanner.RestoreCause.EXPLICIT_USER),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,"second vehicle request");
        eq(p.vehicleEvidence(4,evidence(8,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.RELEASE_ONLY_AFTER_VEHICLE_PASS,
           "second positive return");
        eq(p.verifiedVehicleCycles(),2,"two complete cycles");
        eq(p.airpodsRequested(5,ytm,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED,"third entry");
        eq(p.focusObserved(5,ytm,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED,"third focus");
        eq(p.vehicleRequested(6,OneApp2RepeatFocusPlanner.RestoreCause.PHYSICAL_DISCONNECT),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,
           "physical disconnect allowed only after known AirPods");
        eq(p.vehicleEvidence(6,evidence(8,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.RELEASE_ONLY_AFTER_VEHICLE_PASS,
           "disconnect returns on vehicle evidence");
        eq(p.verifiedVehicleCycles(),3,"three confirmed return cycles");

        eq(p.airpodsRequested(7,gearhead,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.BLOCKED,"no Gearhead owner");
        eq(p.airpodsRequested(8,ytm,OFF,true,true,true),
           OneApp2RepeatFocusPlanner.Action.BLOCKED,"explicit mode required");
        eq(p.airpodsRequested(9,ytm,ON,true,true,false),
           OneApp2RepeatFocusPlanner.Action.BLOCKED,"A2DP required");
        eq(p.airpodsRequested(10,null,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.BLOCKED,"unknown owner no focus");
        eq(p.airpodsRequested(10,ytm,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.STALE_CALLBACK,"generation monotonic");
        eq(p.vehicleRequested(11,OneApp2RepeatFocusPlanner.RestoreCause.PHYSICAL_DISCONNECT),
           OneApp2RepeatFocusPlanner.Action.BLOCKED,"no unconfirmed disconnect");
        eq(p.vehicleRequested(11,OneApp2RepeatFocusPlanner.RestoreCause.EXPLICIT_USER),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,
           "explicit vehicle request accepted");
        eq(p.huTakeoverObserved(10,true,true,true,true,ON,true,true),
           OneApp2RepeatFocusPlanner.Action.STALE_CALLBACK,
           "old HU callback stale");
        eq(p.vehicleEvidence(11,evidence(9,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.VEHICLE_CONFIRMED_NO_OWNED_FOCUS,
           "vehicle proof without this app's Focus MUST NOT release another UID");
        eq(p.verifiedVehicleCycles(),3,"no false additional cycle");

        OneApp2RepeatFocusPlanner lostBeforeDisconnect=new OneApp2RepeatFocusPlanner();
        eq(lostBeforeDisconnect.airpodsRequested(1,ytm,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.PREARM_REQUIRED,
           "physical disconnect prearm new cycle");
        eq(lostBeforeDisconnect.focusObserved(1,ytm,true,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.FOCUS_CONFIRMED,
           "Focus was physically confirmed earlier");
        eq(lostBeforeDisconnect.focusObserved(1,ytm,false,ON,true,true,true),
           OneApp2RepeatFocusPlanner.Action.REARM_REQUIRED,
           "Focus dropped just before AirPods unplug");
        eq(lostBeforeDisconnect.vehicleRequested(2,
             OneApp2RepeatFocusPlanner.RestoreCause.PHYSICAL_DISCONNECT),
           OneApp2RepeatFocusPlanner.Action.WAIT_VEHICLE_EVIDENCE,
           "physical disconnect must still restore vehicle");
        eq(lostBeforeDisconnect.vehicleEvidence(2,evidence(8,true,true,false)),
           OneApp2RepeatFocusPlanner.Action.VEHICLE_CONFIRMED_NO_OWNED_FOCUS,
           "never abandon lost Focus after actual vehicle proof");
        eq(lostBeforeDisconnect.verifiedVehicleCycles(),0,
           "Focus-unowned vehicle return cannot falsely claim Focus cycle");
        System.out.println("PHYSICAL_DISCONNECT_AFTER_FOCUS_DROP=HOST_PASS");

        System.out.println("NO_UNOWNED_FOCUS_ABANDON=HOST_PASS");
        System.out.println("ONEAPP2_REPEAT_FOCUS_HOST_CHECKS="+checks);
        System.out.println("GENERIC_SECOND_CYCLE_PREARM_REARM=HOST_PASS");
        System.out.println("NO_FALSE_MANUAL_RESTORE_AND_NO_AA_RECONNECT=HOST_PASS");
        System.out.println("ACTIVE_ANDROID_FOCUS_REGISTERED=NO");
        System.out.println("PHYSICAL_CAR_TEST=NOT_DONE");
    }
}
