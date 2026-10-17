package com.aaairpods.bridge;

/**
 * Independent new-app functional parity ledger, not the original Golden router.
 * No Android imports, no installed Bridge, no daemon commands and no Focus calls.
 *
 * A source file that compiles is not automatically an active replacement for
 * the physical-pass Golden Bridge/edge23/edge24 packages. Evidence must be
 * collected for the NEW app and its NEW UID/process ownership.
 */
public final class OneApp2IntegrationMatrix {
    public enum Role {
        DIRECT_AA_BYPASS,
        VEHICLE_MANUAL_EDGE23,
        VEHICLE_DISCONNECT_EDGE24,
        GENERIC_SECOND_CYCLE,
        CONTROLLER_DAEMON,
        RECOVERY,
        SELF_HEAL,
        UPDATE_ROLLBACK
    }

    public enum State {
        WRONG_APP_ID,
        SOURCE_MISSING,
        RUNTIME_NOT_WIRED,
        OWN_DAEMON_NOT_VERIFIED,
        ORIGINAL_GOLDEN_OWNS_AUDIO,
        UID_OR_FOCUS_PARITY_NOT_VERIFIED,
        PHYSICAL_RESULT_NOT_VERIFIED,
        READY_WITH_INDEPENDENT_EVIDENCE
    }

    /** All flags apply to the independent app ONLY, never original Golden. */
    public static final class Evidence {
        public final boolean sourcePorted;
        public final boolean runtimeWired;
        public final boolean ownDaemonVerified;
        public final boolean originalAudioOwnerActive;
        public final boolean exclusiveOwnerHandoffVerified;
        public final boolean uidFocusParityVerified;
        public final boolean physicallyAudibleAndRepeated;

        public Evidence(boolean ported, boolean wired, boolean daemon,
                        boolean originalActive, boolean handoff,
                        boolean parity, boolean audible) {
            sourcePorted=ported;
            runtimeWired=wired;
            ownDaemonVerified=daemon;
            originalAudioOwnerActive=originalActive;
            exclusiveOwnerHandoffVerified=handoff;
            uidFocusParityVerified=parity;
            physicallyAudibleAndRepeated=audible;
        }
    }

    private OneApp2IntegrationMatrix(){}

    /**
     * Fail closed in all ambiguous cases. A separate TCP port is NOT an
     * independent AudioPolicy/Focus owner. The original Golden never stops
     * automatically, even if this returns READY.
     */
    public static State inspect(String appPackage, Role role, Evidence e) {
        if(!OneApp2Identity.APP_ID.equals(appPackage))
            return State.WRONG_APP_ID;
        if(role==null || e==null || !e.sourcePorted)
            return State.SOURCE_MISSING;
        if(!e.runtimeWired)
            return State.RUNTIME_NOT_WIRED;
        if(!e.ownDaemonVerified &&
            (role==Role.DIRECT_AA_BYPASS
             || role==Role.CONTROLLER_DAEMON
             || role==Role.RECOVERY
             || role==Role.GENERIC_SECOND_CYCLE))
            return State.OWN_DAEMON_NOT_VERIFIED;
        if(e.originalAudioOwnerActive || !e.exclusiveOwnerHandoffVerified)
            return State.ORIGINAL_GOLDEN_OWNS_AUDIO;
        if(!e.uidFocusParityVerified &&
            (role==Role.VEHICLE_MANUAL_EDGE23
             || role==Role.VEHICLE_DISCONNECT_EDGE24
             || role==Role.GENERIC_SECOND_CYCLE
             || role==Role.DIRECT_AA_BYPASS))
            return State.UID_OR_FOCUS_PARITY_NOT_VERIFIED;
        if(!e.physicallyAudibleAndRepeated)
            return State.PHYSICAL_RESULT_NOT_VERIFIED;
        return State.READY_WITH_INDEPENDENT_EVIDENCE;
    }

    /**
     * Current engineering truth. None of these creates or calls an old helper.
     * All modules are source-only or dormant until independently wired/tested.
     */
    public static Evidence currentSourceOnly(Role role) {
        if(role==null)throw new IllegalArgumentException("role required");
        final boolean sourcePorted =
            role==Role.VEHICLE_MANUAL_EDGE23
            || role==Role.VEHICLE_DISCONNECT_EDGE24
            || role==Role.GENERIC_SECOND_CYCLE
            || role==Role.CONTROLLER_DAEMON;
        return new Evidence(sourcePorted,false,false,true,false,false,false);
    }

    public static String summarize(String appPackage) {
        StringBuilder s=new StringBuilder("ONEAPP2_INDEPENDENT_ROLE_PARITY\n");
        for(Role role:Role.values())
            s.append(role.name()).append("=")
                .append(inspect(appPackage,role,currentSourceOnly(role)).name())
                .append('\n');
        s.append("PREINSTALLED_GOLDEN_HELPER_DEPENDENCY=NOT_ACCEPTED_AS_FINAL\n")
         .append("GOLDEN_PORT_47651=UNCHANGED\n")
         .append("INDEPENDENT_PORT_47652=NOT_STARTED\n")
         .append("GLOBAL_AUDIO_POLICY_WRITER=NONE_ADDED\n")
         .append("REAL_CAR_AUDIO_SUCCESS=NOT_TESTED");
        return s.toString();
    }
}
