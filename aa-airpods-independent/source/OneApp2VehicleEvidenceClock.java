package com.aaairpods.bridge;

/**
 * Strict generation-scoped wall-clock evidence validator for vehicle handoff.
 *
 * This is deliberately a read-only state machine: it never reconnects Android
 * Auto, changes an audio route, requests focus or touches the Golden daemon.
 * Snapshots must come from independently observed real runtime state.
 *
 * An isolated positive snapshot with "stable=8" cannot prove eight seconds.
 * The producer must supply several monotonic samples with bounded gaps.
 */
public final class OneApp2VehicleEvidenceClock {
    public static final long REQUIRED_MS=8_000L;
    public static final long MAX_SAMPLE_GAP_MS=2_500L;

    private long generation=-1, firstMs=-1, previousMs=-1;
    private int positiveSamples;

    public static final class Result {
        public final long generation, stableMs;
        public final int positiveSamples;
        public final boolean stable;
        Result(long g,long ms,int count,boolean pass){
            generation=g;stableMs=ms;positiveSamples=count;stable=pass;
        }
    }
    public synchronized Result observe(long requestGeneration,long monotonicMs,
          boolean sameMediaOwner,boolean vehicleRoute,boolean huFocusGain,
          boolean bottomHalfAvailable,boolean mediaOwnerFocusGain,
          boolean androidAutoDisconnected){
        if(requestGeneration<=0 || monotonicMs<0){
            clear();
            return new Result(requestGeneration,0,0,false);
        }
        if(generation!=requestGeneration){
            clear();
            generation=requestGeneration;
        }
        boolean verified=OneApp2GoldenMechanics.stableVehicleHandoff(
            sameMediaOwner,vehicleRoute,huFocusGain,bottomHalfAvailable,
            mediaOwnerFocusGain,androidAutoDisconnected,0)
            || (sameMediaOwner && vehicleRoute && huFocusGain
                && bottomHalfAvailable && mediaOwnerFocusGain
                && !androidAutoDisconnected);
        if(!verified){
            firstMs=-1;previousMs=-1;positiveSamples=0;
            return new Result(generation,0,0,false);
        }
        if(previousMs>=0 && (monotonicMs<=previousMs
             || monotonicMs-previousMs>MAX_SAMPLE_GAP_MS)){
            firstMs=-1;previousMs=-1;positiveSamples=0;
        }
        if(firstMs<0)firstMs=monotonicMs;
        previousMs=monotonicMs;
        positiveSamples++;
        long stableMs=monotonicMs-firstMs;
        return new Result(generation,stableMs,positiveSamples,
             stableMs>=REQUIRED_MS && positiveSamples>=5);
    }
    public synchronized void clear(){
        generation=-1;firstMs=-1;previousMs=-1;positiveSamples=0;
    }
}
