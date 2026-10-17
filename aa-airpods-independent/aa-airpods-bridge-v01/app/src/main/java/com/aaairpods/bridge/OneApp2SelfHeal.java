package com.aaairpods.bridge;

import android.content.Context;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Missing-only recovery of THIS APP'S own daemon.
 * Deliberately never stops the existing Golden daemon or its helpers.
 * Only acts on an explicit user tap; zero Android Auto reconnects.
 */
public final class OneApp2SelfHeal {
    private OneApp2SelfHeal(){}
    public static String restoreOnlyOwn(Context context){
        if(context==null||!OneApp2Identity.isOneApp2(context.getPackageName()))
            return "ONEAPP2_SELFHEAL_WRONG_INSTALLATION";
        try(Socket guard=new Socket()){
            guard.connect(new InetSocketAddress("127.0.0.1",47651),300);
            return "ONEAPP2_SELFHEAL_BLOCK_ORIGINAL_GOLDEN_LISTENING";
        }catch(java.io.IOException goldenNotListening) {
            // No evidence about stale global Focus here; no route changes.
        }
        OneApp2DaemonHealth.Snapshot first=OneApp2DaemonHealth.inspect(context);
        if(first.state==OneApp2DaemonHealth.State.READY)
            return "ONEAPP2_SELFHEAL_OWN_DAEMON_ALREADY_OK_NO_CHANGES";
        if(first.state==OneApp2DaemonHealth.State.UNEXPECTED_IDENTITY)
            return "ONEAPP2_SELFHEAL_UNEXPECTED_PORT_OWNER_NO_STOP";
        try{
            String attempt=OneApp2Bootstrap.start(context);
            OneApp2DaemonHealth.Snapshot second=OneApp2DaemonHealth.inspect(context);
            if(second.state==OneApp2DaemonHealth.State.READY)
                return "ONEAPP2_SELFHEAL_OWN_DAEMON_RESTART_OK"
                     +"\nAA_AUTO_RECONNECT=NO"
                     +"\nAUDIO_POLICY_CHANGED=NO";
            if(attempt.contains("ONEAPP2_PAIRING_REQUIRED"))
                return "ONEAPP2_SELFHEAL_REQUIRES_EXPLICIT_NEW_APP_ADB_PAIRING";
            return "ONEAPP2_SELFHEAL_NOT_RECOVERED_"+
                second.state.name();
        }catch(Exception e){
            return "ONEAPP2_SELFHEAL_FAILED_"+e.getClass().getSimpleName();
        }
    }
}
