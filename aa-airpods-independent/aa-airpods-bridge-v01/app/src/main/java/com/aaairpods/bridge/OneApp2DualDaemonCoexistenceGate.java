package com.aaairpods.bridge;

/**
 * Pure host-testable decision model for TWO independently running shell
 * daemons on one Android device. It performs no socket, process, ADB, Focus
 * or Android AudioPolicy operations.
 *
 * Separate servers are permitted; AUDIO POLICY OWNERSHIP is not separated
 * by TCP port. This model never authorizes active routing or startup.
 */
public final class OneApp2DualDaemonCoexistenceGate {
    public enum Decision {
        INVALID_IDENTITY, COLLIDING_RESOURCES, SHADOW_READ_ONLY_OK,
        BLOCK_DUAL_AUDIO_WRITERS, BLOCK_UNVERIFIED_POLICY_OWNERSHIP,
        ISOLATED_ACTIVE_DESIGN_REQUIRES_REVIEW
    }
    public static final class Identity {
        public final String packageName, processName, logPath, stagedApkPath;
        public final String pongDaemonName, tokenScope;
        public final int port, shellUid;
        public Identity(String pkg, String process, String log, String apk,
                        String pong, String token, int port, int uid) {
            packageName=pkg; processName=process; logPath=log; stagedApkPath=apk;
            pongDaemonName=pong; tokenScope=token; this.port=port;
            shellUid=uid;
        }
    }

    /** Describes evidence already collected, NOT an instruction to alter it. */
    public static final class Ownership {
        public final boolean oldAudioWriterActive, newAudioWriterRequested;
        public final boolean oldFocusPolicyActive, newFocusPolicyRequested;
        public final boolean exclusiveHandoffIndependentlyVerified;
        public Ownership(boolean oldWrite,boolean newWrite,boolean oldFocus,
                         boolean newFocus,boolean verified) {
            oldAudioWriterActive=oldWrite;newAudioWriterRequested=newWrite;
            oldFocusPolicyActive=oldFocus;newFocusPolicyRequested=newFocus;
            exclusiveHandoffIndependentlyVerified=verified;
        }
    }

    private OneApp2DualDaemonCoexistenceGate() {}
    private static boolean valid(String s) {
        return s!=null && !s.trim().isEmpty();
    }
    private static boolean same(String a,String b) {
        return a.equals(b);
    }
    private static boolean valid(Identity i) {
        return i!=null && valid(i.packageName) && valid(i.processName)
            && valid(i.logPath) && valid(i.stagedApkPath)
            && valid(i.pongDaemonName) && valid(i.tokenScope)
            && i.port>0 && i.port<=65535 && i.shellUid==2000;
    }

    public static Decision assess(Identity old,Identity candidate,
                                  Ownership control) {
        if(!valid(old) || !valid(candidate) || control==null)
            return Decision.INVALID_IDENTITY;
        if(same(old.packageName,candidate.packageName)
            || old.port==candidate.port
            || same(old.processName,candidate.processName)
            || same(old.logPath,candidate.logPath)
            || same(old.stagedApkPath,candidate.stagedApkPath)
            || same(old.pongDaemonName,candidate.pongDaemonName)
            || same(old.tokenScope,candidate.tokenScope))
            return Decision.COLLIDING_RESOURCES;
        // Both process UIDs CAN be shell=2000; that is not an identity collision.
        // Neither a unique port nor a unique process implies unique audio policy.
        if(!control.newAudioWriterRequested && !control.newFocusPolicyRequested)
            return Decision.SHADOW_READ_ONLY_OK;
        if((control.oldAudioWriterActive && control.newAudioWriterRequested)
            || (control.oldFocusPolicyActive && control.newFocusPolicyRequested))
            return Decision.BLOCK_DUAL_AUDIO_WRITERS;
        if(!control.exclusiveHandoffIndependentlyVerified)
            return Decision.BLOCK_UNVERIFIED_POLICY_OWNERSHIP;
        // Does NOT green-light bootstrap, live switching, or a car test.
        return Decision.ISOLATED_ACTIVE_DESIGN_REQUIRES_REVIEW;
    }

    public static Identity goldenReference() {
        return new Identity("com.aaairpods.r25fix",
            "aa_airpods_route_daemon","/data/local/tmp/aaairpods_route.log",
            "/data/local/tmp/aaairpods_stable_daemon.apk",
            "aa-airpods-stable","golden-legacy-own-app-credential",
            47651,2000);
    }

    /** Desired independent identity, NOT a claim about compiled binaries. */
    public static Identity proposedOneApp2() {
        return new Identity("com.aaairpods.oneapp.lab",
            "aa_airpods_oneapp2_daemon","/data/local/tmp/aaairpods_oneapp2_route.log",
            "/data/local/tmp/aaairpods_oneapp2_staged.apk",
            "aa-airpods-oneapp2","oneapp2-own-app-credential",
            47652,2000);
    }
}
