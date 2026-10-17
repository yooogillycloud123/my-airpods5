package com.aaairpods.bridge;

import android.content.Context;
import java.security.SecureRandom;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;

/**
 * Independent :47652 bootstrap, self-contained in NEW lab app only.
 * No copy of old Golden pairing keys, no contact with original token,
 * process, controller, bridge, Focus policy or Android Auto.
 *
 * The user explicitly pairs THIS app's own ADB identity before calling.
 * Only starts its own shell process: never kills/recycles either daemon.
 */
public final class OneApp2Bootstrap {
    private OneApp2Bootstrap(){}
    private static String token(Context app){
        android.content.SharedPreferences p=app.getSharedPreferences(
            OneApp2Identity.OWN_PREFS,Context.MODE_PRIVATE);
        String existing=p.getString(OneApp2Identity.OWN_TOKEN,null);
        if(existing!=null&&existing.matches("[a-f0-9]{48}"))return existing;
        if(existing!=null)throw new SecurityException("OWN_EXISTING_TOKEN_INVALID_PRESERVE");
        byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);
        StringBuilder b=new StringBuilder(48);
        for(byte value:bytes)b.append(String.format("%02x",value&255));
        String fresh=b.toString();
        p.edit().putString(OneApp2Identity.OWN_TOKEN,fresh).apply();
        return fresh;
    }
    public static synchronized String start(Context context)throws Exception{
        if(context==null||!OneApp2Identity.isOneApp2(context.getPackageName()))
            return "ONEAPP2_INSTALL_ID_NOT_ALLOWED";
        Context app=context.getApplicationContext();
        String own=token(app);
        OneApp2DaemonHealth.Snapshot health=OneApp2DaemonHealth.inspect(app);
        if(health.state==OneApp2DaemonHealth.State.READY)
            return "ONEAPP2_ALREADY_RUNNING=YES\n"+health.detail;
        if(health.state==OneApp2DaemonHealth.State.UNEXPECTED_IDENTITY)
            return "ONEAPP2_PORT_OCCUPIED_FOREIGN_DAEMON_NO_STOP";
        AbsAdbConnectionManager manager=
            LocalAdbConnectionManager.getInstance(app);
        if(!manager.isConnected() && !manager.autoConnect(app,7000))
            return "ONEAPP2_PAIRING_REQUIRED_IN_THIS_APP\nWIRELESS_DEBUGGING_NEEDED_FOR_FIRST_START";
        String identity=OneApp2AdbShell.runShell(manager,"id");
        if(!identity.contains("uid=2000"))
            return "ONEAPP2_BOOTSTRAP_NOT_SHELL_UID2000";
        String path=app.getApplicationInfo().sourceDir;
        String staged=OneApp2Identity.STAGED_DAEMON_APK;
        String quote=OneApp2AdbShell.quote(path),dst=OneApp2AdbShell.quote(staged);
        String copy=OneApp2AdbShell.runShell(manager,
            "cp "+quote+" "+dst+
            " && chmod 0644 "+dst+
            " && test -r "+dst+" && echo ONEAPP2_STAGING_SUCCESS");
        if(!copy.contains("ONEAPP2_STAGING_SUCCESS"))
            return "ONEAPP2_STAGE_FAILED_NEW_APP_ONLY";
        // Exit immediately via daemonized redirected subprocess; do not wait
        // for the forever-running app_process and never touch :47651.
        String cmd="( CLASSPATH="+dst+
            " /system/bin/app_process /system/bin "+
            "com.aaairpods.bridge.OneApp2IndependentDaemon "+
            OneApp2Identity.PORT+" "+OneApp2AdbShell.quote(own)+
            " </dev/null >>/data/local/tmp/aaairpods_oneapp2_route.log 2>&1 & )"+
            " && echo ONEAPP2_START_REQUESTED";
        String launched=OneApp2AdbShell.runShell(manager,cmd);
        if(!launched.contains("ONEAPP2_START_REQUESTED"))
            return "ONEAPP2_START_NOT_CONFIRMED";
        for(int attempts=0;attempts<16;attempts++){
            Thread.sleep(300);
            health=OneApp2DaemonHealth.inspect(app);
            if(health.state==OneApp2DaemonHealth.State.READY)
                return "ONEAPP2_DAEMON_STARTED=YES\n"+
                   "INDEPENDENT_PORT=47652\n"+
                   "ORIGINAL_GOLDEN=NOT_MODIFIED\n"+
                   "DIRECT_OUTPUT=NOT_REQUESTED";
            if(health.state==OneApp2DaemonHealth.State.UNEXPECTED_IDENTITY)
                return "ONEAPP2_INDEPENDENT_IDENTITY_MISMATCH";
        }
        return "ONEAPP2_SHELL_START_TIMEOUT_OWN_PORT_ONLY";
    }
}
