package com.aaairpods.bridge;

/** Pure host JDK17 test; never connects to a phone or sends AudioFocus. */
public final class OneApp2IntegrationMatrixTest {
    private static int checks=0;
    private static void eq(OneApp2IntegrationMatrix.State expected,
                           OneApp2IntegrationMatrix.State actual) {
        if(actual!=expected)
            throw new AssertionError("expected="+expected+" actual="+actual);
        checks++;
    }
    private static OneApp2IntegrationMatrix.Evidence evidence(
        boolean p,boolean w,boolean d,boolean old,boolean owner,
        boolean parity,boolean audio) {
        return new OneApp2IntegrationMatrix.Evidence(p,w,d,old,owner,parity,audio);
    }
    public static void main(String[] args){
        String lab=OneApp2Identity.APP_ID;
        for(OneApp2IntegrationMatrix.Role role:OneApp2IntegrationMatrix.Role.values()){
            OneApp2IntegrationMatrix.Evidence full=evidence(
                true,true,true,false,true,true,true);
            eq(OneApp2IntegrationMatrix.State.WRONG_APP_ID,
               OneApp2IntegrationMatrix.inspect("com.aaairpods.r25fix",role,full));
            eq(OneApp2IntegrationMatrix.State.SOURCE_MISSING,
               OneApp2IntegrationMatrix.inspect(lab,role,
                    evidence(false,true,true,false,true,true,true)));
            eq(OneApp2IntegrationMatrix.State.RUNTIME_NOT_WIRED,
               OneApp2IntegrationMatrix.inspect(lab,role,
                    evidence(true,false,true,false,true,true,true)));
            eq(OneApp2IntegrationMatrix.State.ORIGINAL_GOLDEN_OWNS_AUDIO,
               OneApp2IntegrationMatrix.inspect(lab,role,
                    evidence(true,true,true,true,true,true,true)));
            eq(OneApp2IntegrationMatrix.State.ORIGINAL_GOLDEN_OWNS_AUDIO,
               OneApp2IntegrationMatrix.inspect(lab,role,
                    evidence(true,true,true,false,false,true,true)));
            eq(OneApp2IntegrationMatrix.State.PHYSICAL_RESULT_NOT_VERIFIED,
               OneApp2IntegrationMatrix.inspect(lab,role,
                    evidence(true,true,true,false,true,true,false)));
            eq(OneApp2IntegrationMatrix.State.READY_WITH_INDEPENDENT_EVIDENCE,
               OneApp2IntegrationMatrix.inspect(lab,role,full));
            OneApp2IntegrationMatrix.State current=
                 OneApp2IntegrationMatrix.inspect(
                     lab,role,OneApp2IntegrationMatrix.currentSourceOnly(role));
            if(current==OneApp2IntegrationMatrix.State.READY_WITH_INDEPENDENT_EVIDENCE)
                throw new AssertionError("unwired LAB must never be READY");
            checks++;
        }
        for(OneApp2IntegrationMatrix.Role role:new OneApp2IntegrationMatrix.Role[]{
             OneApp2IntegrationMatrix.Role.DIRECT_AA_BYPASS,
             OneApp2IntegrationMatrix.Role.CONTROLLER_DAEMON,
             OneApp2IntegrationMatrix.Role.RECOVERY,
             OneApp2IntegrationMatrix.Role.GENERIC_SECOND_CYCLE}) {
            eq(OneApp2IntegrationMatrix.State.OWN_DAEMON_NOT_VERIFIED,
                OneApp2IntegrationMatrix.inspect(lab,role,
                  evidence(true,true,false,false,true,true,true)));
        }
        for(OneApp2IntegrationMatrix.Role role:new OneApp2IntegrationMatrix.Role[]{
             OneApp2IntegrationMatrix.Role.DIRECT_AA_BYPASS,
             OneApp2IntegrationMatrix.Role.VEHICLE_MANUAL_EDGE23,
             OneApp2IntegrationMatrix.Role.VEHICLE_DISCONNECT_EDGE24,
             OneApp2IntegrationMatrix.Role.GENERIC_SECOND_CYCLE}) {
            eq(OneApp2IntegrationMatrix.State.UID_OR_FOCUS_PARITY_NOT_VERIFIED,
               OneApp2IntegrationMatrix.inspect(lab,role,
                   evidence(true,true,true,false,true,false,true)));
        }
        String report=OneApp2IntegrationMatrix.summarize(lab);
        if(!report.contains("INDEPENDENT_PORT_47652=NOT_STARTED")
            || !report.contains("GOLDEN_PORT_47651=UNCHANGED")
            || !report.contains("REAL_CAR_AUDIO_SUCCESS=NOT_TESTED"))
            throw new AssertionError("unsafe report");
        checks++;
        System.out.println("ONEAPP2_INDEPENDENT_INTEGRATION_FAILCLOSED_TESTS="+checks);
        System.out.println("ORIGINAL_GOLDEN_RUNTIME_MODIFICATIONS=0");
        System.out.println("ACTIVE_ROUTE_OR_FOCUS_DISPATCHES=0");
    }
}
