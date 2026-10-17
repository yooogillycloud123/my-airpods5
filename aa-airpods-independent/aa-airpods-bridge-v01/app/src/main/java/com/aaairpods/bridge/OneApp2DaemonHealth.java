package com.aaairpods.bridge;

import android.content.Context;

/**
 * Read-only health observation for the independent OneApp2 daemon endpoint.
 * Uses only the OneApp2 private credential and port; it does not start,
 * stop, replace, or contact any Golden process or previous app.
 */
public final class OneApp2DaemonHealth {
    public enum State {
        WRONG_INSTALLATION, NOT_PROVISIONED, NOT_RUNNING,
        UNEXPECTED_IDENTITY, READY
    }

    public static final class Snapshot {
        public final State state;
        public final int port;
        public final String detail;
        Snapshot(State state, int port, String detail) {
            this.state=state;
            this.port=port;
            this.detail=detail;
        }
    }

    private OneApp2DaemonHealth() {}

    public static Snapshot inspect(Context context) {
        if(context==null || !OneApp2Identity.isOneApp2(context.getPackageName()))
            return new Snapshot(State.WRONG_INSTALLATION, OneApp2Identity.PORT,
                    "Only new independent OneApp2 installation is inspected");
        String token=context.getSharedPreferences(OneApp2Identity.OWN_PREFS,
                Context.MODE_PRIVATE).getString(OneApp2Identity.OWN_TOKEN,null);
        if(token==null || token.length()<32)
            return new Snapshot(State.NOT_PROVISIONED,OneApp2Identity.PORT,
                    "Independent OneApp2 credential is not provisioned");
        try {
            String result=new RouteDaemonClient(OneApp2Identity.PORT,token).ping();
            // The Golden daemon has the same wire protocol, but not this
            // installation identity. An old daemon rebound to the new port
            // must NEVER be reported as an independently provisioned READY.
            final String ownPongPrefix =
                    "OK PONG uid=2000 shell daemon=alive "
                    + "daemon=aa-airpods-oneapp2 proto=2 ";
            if(result!=null && result.trim().startsWith(ownPongPrefix))
                return new Snapshot(State.READY,OneApp2Identity.PORT,
                        "Own OneApp2 daemon uid=2000, protocol=2, authenticated");
            return new Snapshot(State.UNEXPECTED_IDENTITY,OneApp2Identity.PORT,
                    "Endpoint is not the expected authenticated shell daemon");
        }catch(Exception unavailable){
            return new Snapshot(State.NOT_RUNNING,OneApp2Identity.PORT,
                    "Independent daemon did not authenticate/respond");
        }
    }
}
