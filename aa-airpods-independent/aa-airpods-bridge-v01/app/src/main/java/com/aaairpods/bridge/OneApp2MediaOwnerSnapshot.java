package com.aaairpods.bridge;

/**
 * Strict parser for the proven Golden R24/R25 USAGE_MEDIA owner detector's
 * single-line, eight-field wire format:
 * package|uid|pid|piid|deviceId|route|candidateCount|sessionPlaying
 *
 * This is only the decoder; identifying the active player still requires
 * collecting real runtime evidence. It does not initiate shell commands or
 * mutate Android Auto, Bluetooth, Golden runtime, credentials or audio focus.
 */
public final class OneApp2MediaOwnerSnapshot {
    public enum Route {
        AIRPODS_A2DP, AA_REMOTE, GV80_A2DP, OTHER_A2DP, SPEAKER, OTHER, NONE
    }
    public final String packageName;
    public final int uid, pid, playerId, deviceId, candidateCount;
    public final boolean playing;
    public final Route route;
    public final boolean valid;

    private OneApp2MediaOwnerSnapshot(String pkg, int u, int p, int player,
                                      int device, Route r, int candidates,
                                      boolean isPlaying, boolean isValid) {
        packageName=pkg;
        uid=u; pid=p; playerId=player; deviceId=device;
        route=r; candidateCount=candidates; playing=isPlaying; valid=isValid;
    }

    public boolean hasActiveMediaOwner() {
        return valid && uid>=0 && pid>=0 && candidateCount>0
                && !"none".equals(packageName);
    }

    public boolean isEligibleOrdinaryMedia() {
        return hasActiveMediaOwner()
                && !"android".equals(packageName)
                && !packageName.startsWith("com.android.")
                && !packageName.startsWith("com.aaairpods.")
                && !"com.google.android.projection.gearhead".equals(packageName);
    }

    private static int number(String s) {
        if(s.length()==0 || s.length()>10)throw new IllegalArgumentException("bad numeric field");
        for(int i=0;i<s.length();i++)if(s.charAt(i)<'0'||s.charAt(i)>'9')
            throw new IllegalArgumentException("non-numeric field");
        return Integer.parseInt(s);
    }

    public static OneApp2MediaOwnerSnapshot parse(String raw) {
        if(raw==null || raw.length()>1024 || raw.indexOf('\n')>=0
                || raw.indexOf('\r')>=0)
            throw new IllegalArgumentException("single bounded line required");
        String[] f=raw.trim().split("\\|",-1);
        if(f.length!=8)throw new IllegalArgumentException("owner tuple field count");
        if("none".equals(f[0])) {
            // Match the exact absent-owner record emitted by the user's
            // proven aa_r25_owner.sh. Never normalize contradictory fields
            // into a seemingly valid "no media" event.
            if(!"none".equals(f[1]) || !"none".equals(f[2])
                    || !"none".equals(f[3]) || !"none".equals(f[4])
                    || !"NONE".equals(f[5]) || !"0".equals(f[6])
                    || !"0".equals(f[7]))
                throw new IllegalArgumentException("invalid absent-owner tuple");
            return new OneApp2MediaOwnerSnapshot("none",-1,-1,-1,-1,
                    Route.NONE,0,false,true);
        }
        if(!f[0].matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))
            throw new IllegalArgumentException("invalid package");
        int uid=number(f[1]), pid=number(f[2]), piid=number(f[3]);
        int device="none".equals(f[4]) ? -1 : number(f[4]);
        int count=number(f[6]);
        if(!"0".equals(f[7]) && !"1".equals(f[7]))
            throw new IllegalArgumentException("invalid media-session playing bit");
        Route route;
        try{route=Route.valueOf(f[5]);}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("invalid output route",e);}
        return new OneApp2MediaOwnerSnapshot(f[0],uid,pid,piid,device,
                route,count,"1".equals(f[7]),true);
    }
}
