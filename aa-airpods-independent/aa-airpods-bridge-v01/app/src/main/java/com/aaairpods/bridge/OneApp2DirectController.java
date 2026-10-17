package com.aaairpods.bridge;

import android.content.Context;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Independent LAB direct routing backend. Every privileged operation is
 * performed by the NEW app's own authenticated :47652 shell daemon.
 * NO legacy Bridge/edge23/edge24 app dependencies; never sends to :47651.
 * As a hard guard, if the original Golden endpoint is listening, all route
 * mutations are refused. There is NO Android Auto reconnect fallback.
 *
 * Original Golden's process may still possess global AudioPolicy focus after
 * it stops listening; therefore this is a TEST candidate, never a physical
 * success assertion, and must only be used after a verified exclusive
 * ownership handoff.
 */
public final class OneApp2DirectController {
    public static final class Result {
        public final boolean ok;
        public final String stage;
        public final String details;
        private Result(boolean ok,String stage,String detail){
            this.ok=ok;this.stage=stage;this.details=detail;
        }
        public String report(){
            return "ONEAPP2_INTERNAL_ROUTE="+(ok?"POLICY_ACCEPTED":"BLOCKED_OR_FAILED")
                +"\nSTAGE="+stage
                +"\n"+details
                +"\nORIGINAL_HELPER_IPC=NONE"
                +"\nAA_FORCE_RECONNECT=NEVER"
                +"\nAUDIBLE_PCM=NOT_YET_CONFIRMED";
        }
    }
    private OneApp2DirectController(){}
    private static boolean originalPortListening(){
        try(Socket socket=new Socket()){
            socket.connect(new InetSocketAddress("127.0.0.1",47651),300);
            return true;
        }catch(java.io.IOException absent){
            return false;
        }
    }
    private static Result block(String why){
        return new Result(false,why,"NO_POLICY_WRITES=YES");
    }
    private static RouteDaemonClient own(Context app)throws Exception{
        if(app==null||!OneApp2Identity.isOneApp2(app.getPackageName()))
            throw new SecurityException("WRONG_INDEPENDENT_APP");
        if(originalPortListening())
            throw new SecurityException("ORIGINAL_GOLDEN_DAEMON_ALIVE_NO_DUAL_AUDIO");
        OneApp2DaemonHealth.Snapshot ready=OneApp2DaemonHealth.inspect(app);
        if(ready.state!=OneApp2DaemonHealth.State.READY)
            throw new IllegalStateException("OWN_DAEMON_NOT_AUTHENTICATED_"+ready.state);
        String token=app.getSharedPreferences(OneApp2Identity.OWN_PREFS,
                Context.MODE_PRIVATE).getString(OneApp2Identity.OWN_TOKEN,null);
        if(token==null||token.length()<32)
            throw new SecurityException("OWN_TOKEN_NOT_PRESENT");
        return new RouteDaemonClient(OneApp2Identity.PORT,token);
    }
    private static String worker(Context app,RouteDaemonClient daemon,
                                 String action,String data)throws Exception{
        String result=daemon.runWorker(app.getApplicationInfo().sourceDir,
                action,data==null?"":data);
        if(result==null || !result.contains("WORKER_EXIT=0")
                || result.contains("WORKER_UNKNOWN_OP")
                || result.contains("ONEAPP2_WORKER_OPERATION_NOT_ALLOWED")
                || result.contains("ONEAPP2_BLOCKED_ORIGINAL_GOLDEN"))
            throw new IllegalStateException("OWN_WORKER_"+action+"_FAILED");
        return result;
    }
    private static void require(String result,String marker)throws Exception{
        if(result==null||!result.contains(marker))
            throw new IllegalStateException("OWN_POLICY_STAGE_"+marker+"_FAILED");
    }
    /**
     * Explicit user-requested own-direct AA->AirPods operation. The
     * caller MUST independently obtain an exact bonded A2DP address and
     * ensure no original-owner activation. No retry loops/AA restart.
     */
    public static Result airpods(Context ctx,String verifiedAddress){
        if(verifiedAddress==null || !verifiedAddress.matches(
                "(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))
            return block("OWN_A2DP_ADDRESS_UNVERIFIED");
        if(originalPortListening())return block("GOLDEN_47651_STILL_RUNNING");
        Context app=ctx.getApplicationContext();
        try{
            RouteDaemonClient daemon=own(app);
            require(worker(app,daemon,"POLICY_PROBE",""),"POLICY_PROBE_DONE ok=1");
            require(worker(app,daemon,"CLEAN_A2DP_RENDER",verifiedAddress),
                    "CLEAN_DONE ok=1");
            String mix=daemon.setDirectRender(verifiedAddress);
            require(mix,"DIRECT_RENDER_DONE ok=1");
            if(originalPortListening())return block("GOLDEN_RESUMED_ABORT");
            try{
                require(worker(app,daemon,"AA_BYPASS_SET",""),
                        "AA_BYPASS_DONE ok=1");
            }catch(Exception failed){
                if(!originalPortListening()){
                    try{daemon.resetDirectRender();}catch(Exception ignored){}
                }
                return block("OWN_AA_BYPASS_FAILED_ROLLBACK_ATTEMPTED");
            }
            return new Result(true,"OWN_DIRECT_RENDER_BYPASS_POLICY_READY",
                "OWN_DAEMON=47652\nOWN_A2DP_ADDRESS_VERIFIED=YES"
                +"\nAUDIO_ACTUAL_OUTPUT=REQUIRES_LISTENING_TEST");
        }catch(Exception ex){
            return block("OWN_AIRPODS_EXCEPTION_"+ex.getClass().getSimpleName());
        }
    }
    /**
     * Explicit manual/physical-disconnect restore via OWN daemon only.
     * Never marks vehicle restored merely because a worker returns 0.
     * Never reconnects Android Auto, removes user-installed packages or
     * stops original Golden.
     */
    public static Result vehicle(Context ctx,String lastKnownAddress){
        if(originalPortListening())return block("GOLDEN_47651_STILL_RUNNING");
        Context app=ctx.getApplicationContext();
        try{
            RouteDaemonClient daemon=own(app);
            String reset=daemon.resetDirectRender();
            require(reset,"DIRECT_RENDER_RESET_DONE ok=1");
            if(lastKnownAddress!=null && lastKnownAddress.matches(
                    "(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))
                worker(app,daemon,"CLEAN_A2DP_RENDER",lastKnownAddress);
            require(worker(app,daemon,"AA_REFRESH_DEFAULT",""),
                    "AA_REFRESH_DEFAULT_DONE ok=1");
            String status=worker(app,daemon,"AA_STATUS","");
            if(!status.contains("AA_PRESENT=1"))
                return block("NO_ANDROID_AUTO_TARGET_OBSERVED");
            require(worker(app,daemon,"AA_VEHICLE_AFFINITY_SET",""),
                    "AA_VEHICLE_AFFINITY_DONE ok=1");
            worker(app,daemon,"REROUTE_DEFAULT","");
            String check=worker(app,daemon,"AA_ROUTE_CHECK","");
            if(check.contains("ACTIVE_MEDIA_PRESENT=1")
                    && check.contains("AA_ROUTE_MATCH=1"))
                return new Result(true,"VEHICLE_ACTIVE_MEDIA_ROUTE_CANDIDATE",
                    "INDEPENDENT_47652=YES\nVEHICLE_AUDIBLE_8S=NOT_MEASURED");
            return block("VEHICLE_EVIDENCE_INDETERMINATE_NO_AA_RECONNECT");
        }catch(Exception ex){
            return block("OWN_VEHICLE_EXCEPTION_"+ex.getClass().getSimpleName());
        }
    }
}
