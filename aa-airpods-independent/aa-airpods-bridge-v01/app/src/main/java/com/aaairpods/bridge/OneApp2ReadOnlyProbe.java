package com.aaairpods.bridge;

import android.content.Context;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Read-only independent diagnostic; reports stages, never commands routes. */
public final class OneApp2ReadOnlyProbe {
    private OneApp2ReadOnlyProbe(){}
    public static String inspect(Context context) {
        if(context==null||!OneApp2Identity.isOneApp2(context.getPackageName()))
            return "PROBE=WRONG_PACKAGE";
        boolean goldenListen=false;
        try(Socket sock=new Socket()){
            sock.connect(new InetSocketAddress("127.0.0.1",47651),250);
            goldenListen=true;
        }catch(java.io.IOException notListening){}
        OneApp2DaemonHealth.Snapshot own=OneApp2DaemonHealth.inspect(context);
        String prefix="PROBE=OWN_ONEAPP2\n"
            +"PACKAGE="+context.getPackageName()+"\n"
            +"GOLDEN_47651_LISTENER="+goldenListen+"\n"
            +"OWN_47652_STATE="+own.state+"\n";
        if(goldenListen)
            return prefix+"OWN_POLICY_PROBE=BLOCKED_BY_ORIGINAL\n"
                +"ORIGINAL_GLOBAL_FOCUS_OWNER=UNKNOWN\n"
                +"DEVICE_PCM=NOT_MEASURED";
        if(own.state!=OneApp2DaemonHealth.State.READY)
            return prefix+"OWN_POLICY_PROBE=NO_OWN_DAEMON\n"
                +"DEVICE_PCM=NOT_MEASURED";
        try{
            String token=context.getSharedPreferences(OneApp2Identity.OWN_PREFS,
                    Context.MODE_PRIVATE).getString(OneApp2Identity.OWN_TOKEN,null);
            if(token==null || token.length()<32)
                return prefix+"OWN_POLICY_PROBE=NO_OWN_CREDENTIAL";
            RouteDaemonClient client=new RouteDaemonClient(OneApp2Identity.PORT,token);
            String aa=client.runWorker(context.getApplicationInfo().sourceDir,"AA_STATUS","");
            String route=client.runWorker(context.getApplicationInfo().sourceDir,"AA_ROUTE_CHECK","");
            return prefix
                +"AA_STATUS_WORKER_OK="+aa.contains("WORKER_EXIT=0")+"\n"
                +"ANDROID_AUTO_PRESENT="+aa.contains("AA_PRESENT=1")+"\n"
                +"ROUTE_CHECK_WORKER_OK="+route.contains("AA_ROUTE_CHECK_DONE ok=1")+"\n"
                +"ACTIVE_MEDIA_PRESENT="+route.contains("ACTIVE_MEDIA_PRESENT=1")+"\n"
                +"AA_ROUTE_MATCH="+route.contains("AA_ROUTE_MATCH=1")+"\n"
                +"GOLDEN_FOCUS_ACTUAL_OWNER=NOT_IDENTIFIED"
                +"\nVEHICLE_SOUND_ACTUAL=USER_REPORT_REQUIRED";
        }catch(Exception e){
            return prefix+"OWN_READ_ONLY_PROBE_ERROR="+e.getClass().getSimpleName();
        }
    }
}
