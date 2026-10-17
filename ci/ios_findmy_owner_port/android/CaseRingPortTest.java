package com.myairpods5.casering.core;

public final class CaseRingPortTest {
    private static int count;
    private static void check(boolean condition) {
        count++;
        if (!condition) throw new AssertionError("Check "+count);
    }
    private static final class Mock implements CaseRingPort.OwnerCaseResolver,
         CaseRingPort.OwnerProvisioner, CaseRingPort.CaseSoundProtocol,
         CaseRingPort.AudioObserver, CaseRingPort.Recorder {
        boolean owned, authorized, protocol, audible, start=true, stop=true;
        int startCalls,stopCalls,events;
        public boolean hasCryptographicallyVerifiedOwnCaseIdentity(){return owned;}
        public boolean hasAuthenticatedOwnCaseControlSession(){return authorized;}
        public boolean hasVerifiedCaseSpecificStartAndStop(){return protocol;}
        public boolean startOnAuthenticatedCase(){startCalls++;return start;}
        public boolean stopOnAuthenticatedCase(){stopCalls++;return stop;}
        public boolean hadActualAudibleCaseSoundInClosedCaseState(){return audible;}
        public void accept(CaseRingPort.Stage stage){events++;}
    }
    private static CaseRingPort make(Mock m) {return new CaseRingPort(m,m,m,m,m);}

    public static void main(String[] args) {
        Mock m=new Mock();
        CaseRingPort r=make(m);
        check(r.readiness()==CaseRingPort.Code.OWN_CASE_UNIDENTIFIED);
        check(r.requestOneBoundedRing()==CaseRingPort.Code.OWN_CASE_UNIDENTIFIED);
        check(m.startCalls==0 && m.stopCalls==0); // anonymous nearby packets
        m.owned=true;
        check(r.readiness()==CaseRingPort.Code.OWNER_SESSION_UNPROVISIONED);
        check(r.requestOneBoundedRing()==CaseRingPort.Code.OWNER_SESSION_UNPROVISIONED);
        check(m.startCalls==0);
        m.authorized=true;
        check(r.readiness()==CaseRingPort.Code.CASE_PROTOCOL_UNVERIFIED);
        check(r.requestOneBoundedRing()==CaseRingPort.Code.CASE_PROTOCOL_UNVERIFIED);
        check(m.startCalls==0); // public nonowner FD44 never accepted
        m.protocol=true;
        check(r.readiness()==CaseRingPort.Code.READY);
        check(r.requestOneBoundedRing()==CaseRingPort.Code.CASE_AUDIBLE_NOT_VALIDATED);
        check(m.startCalls==1 && m.stopCalls==1);
        // v140: even ACK 0/0 means nothing without REAL case sound
        m.start=true; m.stop=true; m.audible=false;
        check(r.requestOneBoundedRing()==CaseRingPort.Code.CASE_AUDIBLE_NOT_VALIDATED);
        m.audible=true;
        check(r.requestOneBoundedRing()==CaseRingPort.Code.READY);
        m.stop=false;
        check(r.requestOneBoundedRing()==CaseRingPort.Code.CASE_AUDIBLE_NOT_VALIDATED);
        m.stop=true; m.start=false; m.stopCalls=0;
        check(r.requestOneBoundedRing()==CaseRingPort.Code.CASE_AUDIBLE_NOT_VALIDATED);
        check(m.stopCalls==1); // START may have reached device before failure; always STOP
        final int[] stages={0};
        CaseRingPort denied=CaseRingPort.createUnprovisioned(s -> stages[0]++);
        check(denied.requestOneBoundedRing()==CaseRingPort.Code.OWN_CASE_UNIDENTIFIED);
        check(stages[0]==1);
        System.out.println("APPLE_FINDMY_ANDROID_PORT_PROOF_GATE=PASS");
        System.out.println("ANDROID_PORT_OFFLINE_ASSERTIONS="+count);
        System.out.println("V140_ACK_ZERO_ZERO_WITH_PHYSICAL_SILENCE=NOT_A_SUCCESS");
        System.out.println("ACTUAL_APPLE_A3529_CASE_RING=NOT_IMPLEMENTED");
    }
}
