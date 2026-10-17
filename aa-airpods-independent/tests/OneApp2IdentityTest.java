package com.aaairpods.bridge;

/** Verify public independent routing has no implicit original Golden fallback. */
public final class OneApp2IdentityTest {
    private static int checks;
    private static void verify(boolean ok,String label) {
        if(!ok) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) {
        verify(OneApp2Identity.isOneApp2("com.aaairpods.oneapp.lab"),
            "separate LAB identity recognized");
        verify(OneApp2Identity.daemonPort("com.aaairpods.oneapp.lab")==47652,
            "own daemon endpoint only");
        verify(!OneApp2Identity.isOneApp2("com.aaairpods.r25fix"),
            "original installed R25 is never own identity");
        verify(OneApp2Identity.daemonPort("com.aaairpods.r25fix")==-1,
            "no legacy Golden port fallback");
        verify(OneApp2Identity.daemonPort("com.aaairpods.bridge")==-1,
            "old Bridge cannot inherit new daemon scope");
        verify(OneApp2Identity.daemonPort(null)==-1,
            "unknown package never permits daemon access");
        System.out.println("ONEAPP2_OWN_IDENTITY_ASSERTIONS="+checks);
        System.out.println("GOLDEN_PORT_FALLBACK=DISABLED");
        System.out.println("REAL_DEVICE_DAEMON=NOT_TESTED");
    }
}
