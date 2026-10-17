package com.myairpods5.casering.core;

import java.util.Objects;

/**
 * Android-compatible, platform-neutral owner-case Find My transport boundary.
 * This is intentionally NOT an Apple credential bypass or an AirPods BLE opcode.
 *
 * v140 user physical: Apple FindMy FD44 NONOWNER START+STOP GATT ACK 0/0,
 * own A3529 case NEVER beeped. Never use this generic command for OWN ring.
 *
 * Production provisioning adapter = ABSENT until publicly reproducible,
 * owner-consented A3529 evidence can be established without Apple hardware.
 */
public final class CaseRingPort {
    public enum Code {
        READY, OWN_CASE_UNIDENTIFIED, OWNER_SESSION_UNPROVISIONED,
        CASE_PROTOCOL_UNVERIFIED, CASE_AUDIBLE_NOT_VALIDATED, BUSY
    }
    public enum Stage { IDLE, DENIED, STARTING, SOUND_REQUEST_ACK, STOPPING, DONE }

    /** No rotating MAC, advertised model, or arbitrary nonowner FD44 accepted. */
    public interface OwnerCaseResolver {
        /** An authenticated binding to the user's exact individual A3529 case. */
        boolean hasCryptographicallyVerifiedOwnCaseIdentity();
    }
    public interface OwnerProvisioner {
        /** Proof-bound owner session; must reject anonymous/nonowner trackers. */
        boolean hasAuthenticatedOwnCaseControlSession();
    }
    public interface CaseSoundProtocol {
        /** Verified on OWN A3529 case; never guessed from nonowner AirGuard. */
        boolean hasVerifiedCaseSpecificStartAndStop();
        /** Actual device-scoped authenticated control request, not music/A2DP. */
        boolean startOnAuthenticatedCase();
        boolean stopOnAuthenticatedCase();
    }
    public interface AudioObserver {
        /** Physical audition after command and two buds docked / lid closed. */
        boolean hadActualAudibleCaseSoundInClosedCaseState();
    }
    public interface Recorder { void accept(Stage stage); }

    private final OwnerCaseResolver owner;
    private final OwnerProvisioner provisioner;
    private final CaseSoundProtocol protocol;
    private final AudioObserver acoustics;
    private final Recorder recorder;
    private boolean busy;

    public CaseRingPort(OwnerCaseResolver owner, OwnerProvisioner provisioner,
                        CaseSoundProtocol protocol, AudioObserver acoustics,
                        Recorder recorder) {
        this.owner=Objects.requireNonNull(owner);
        this.provisioner=Objects.requireNonNull(provisioner);
        this.protocol=Objects.requireNonNull(protocol);
        this.acoustics=Objects.requireNonNull(acoustics);
        this.recorder=Objects.requireNonNull(recorder);
    }

    public synchronized Code readiness() {
        if (busy) return Code.BUSY;
        if (!owner.hasCryptographicallyVerifiedOwnCaseIdentity())
            return Code.OWN_CASE_UNIDENTIFIED;
        if (!provisioner.hasAuthenticatedOwnCaseControlSession())
            return Code.OWNER_SESSION_UNPROVISIONED;
        if (!protocol.hasVerifiedCaseSpecificStartAndStop())
            return Code.CASE_PROTOCOL_UNVERIFIED;
        return Code.READY;
    }

    /** Never calls BLE until ALL three independent proof gates are passed. */
    public synchronized Code requestOneBoundedRing() {
        Code gate=readiness();
        if (gate!=Code.READY) {
            recorder.accept(Stage.DENIED);
            return gate;
        }
        busy=true;
        recorder.accept(Stage.STARTING);
        boolean started=false, stopped=false;
        try {
            started=protocol.startOnAuthenticatedCase();
            if (started) recorder.accept(Stage.SOUND_REQUEST_ACK);
        } finally {
            // Always attempt STOP after starting, including future failure paths.
            if (started) {
                recorder.accept(Stage.STOPPING);
                try { stopped=protocol.stopOnAuthenticatedCase(); }
                catch (RuntimeException ignored) { stopped=false; }
            }
            busy=false;
            recorder.accept(Stage.DONE);
        }
        if (!started || !stopped || !acoustics.hadActualAudibleCaseSoundInClosedCaseState())
            return Code.CASE_AUDIBLE_NOT_VALIDATED;
        return Code.READY;
    }

    /** Actual v141 Android boundary: fail-closed, no Bluetooth I/O ever. */
    public static CaseRingPort createUnprovisioned(Recorder recorder) {
        return new CaseRingPort(
            () -> false, () -> false,
            new CaseSoundProtocol() {
                public boolean hasVerifiedCaseSpecificStartAndStop() { return false; }
                public boolean startOnAuthenticatedCase() { return false; }
                public boolean stopOnAuthenticatedCase() { return false; }
            },
            () -> false, recorder);
    }
}
