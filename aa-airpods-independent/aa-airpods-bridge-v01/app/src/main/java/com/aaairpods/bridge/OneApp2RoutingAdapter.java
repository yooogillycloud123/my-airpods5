package com.aaairpods.bridge;

import android.content.Context;

/**
 * NEW standalone-app adapter for the previously verified in-package
 * RouteController routing primitive. It cannot dispatch to external Bridge,
 * edge23/24 launcher activities or the old Golden controller.
 *
 * Caller must independently verify physical PCM and media focus after
 * requesting a route; policy acceptance alone is NOT audible success.
 */
public final class OneApp2RoutingAdapter {
    public static final class Attempt {
        public final long generation;
        public final boolean policyAccepted;
        public final OneApp2UniversalController.Output target;
        public final String detail;
        private Attempt(long generation,boolean policyAccepted,
                        OneApp2UniversalController.Output target,String detail){
            this.generation=generation;
            this.policyAccepted=policyAccepted;
            this.target=target;
            this.detail=detail;
        }
    }
    private final Context app;
    private final OneApp2UniversalController state=new OneApp2UniversalController();

    public OneApp2RoutingAdapter(Context context) {
        if(context==null || !OneApp2Identity.isOneApp2(context.getPackageName()))
            throw new SecurityException("Only standalone OneApp2 identity may use adapter");
        app=context.getApplicationContext();
    }

    /** Explicitly user-requested transition, not an automatic AA reconnect. */
    public synchronized Attempt request(boolean airpods) {
        OneApp2UniversalController.Output target=airpods
            ? OneApp2UniversalController.Output.AIRPODS
            : OneApp2UniversalController.Output.VEHICLE;
        OneApp2UniversalController.Request request=state.request(
            target,OneApp2UniversalController.Trigger.USER);
        // IMPORTANT: the frozen RouteController.client() still checks the
        // legacy Golden daemon identity. Even when it selects port :47652,
        // routing through it is NOT an independent OneApp2 runtime contract.
        // Never issue an audio-policy write through that legacy path.
        // Own-worker bindings and exclusive Focus owner are pending a
        // separately reviewed independent startup implementation.
        return new Attempt(request.generation,false,target,
            "ONEAPP2_ROUTE_BINDING_NOT_READY: independent daemon/worker/" +
            "Controller/Focus not active; no Golden or audio-policy writes");
    }

    /**
     * Evidence must be collected from the live audio subsystem and verified
     * vehicle PCM independently of the policy command's return code.
     */
    public synchronized OneApp2UniversalController.Decision observe(
        long generation,OneApp2UniversalController.Evidence evidence) {
        return state.observe(generation,evidence);
    }

    public synchronized int completedVehicleCycles() {
        return state.completedCycles();
    }
}
