package com.aaairpods.bridge;

/**
 * Installation identity and endpoint mapping for a separately installed
 * OneApp2.  The golden production and original Bridge endpoints are not
 * modified and are never read or written from OneApp2.
 */
public final class OneApp2Identity {
    public static final String APP_ID="com.aaairpods.oneapp.lab";
    public static final int PORT=47652; // separate from Golden :47651
    public static final String OWN_PREFS="route_daemon";
    public static final String OWN_TOKEN="token";
    public static final String STAGED_DAEMON_APK=
        "/data/local/tmp/aaairpods_oneapp2_staged.apk";
    private OneApp2Identity(){}
    public static boolean isOneApp2(String pkg) {
        return APP_ID.equals(pkg);
    }
    public static int daemonPort(String pkg) {
        return isOneApp2(pkg) ? PORT : RouteDaemon.DEFAULT_PORT;
    }
}
