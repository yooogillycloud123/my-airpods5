package com.aaairpods.bridge;

public final class OneApp2MediaOwnerSnapshotTest {
    private static int n=0;
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
        n++;
    }
    private static void bad(String raw){
        try{
            OneApp2MediaOwnerSnapshot.parse(raw);
            throw new AssertionError("accepted malformed owner line: "+raw);
        }catch(IllegalArgumentException expected){n++;}
    }
    public static void main(String[] args){
        OneApp2MediaOwnerSnapshot a=OneApp2MediaOwnerSnapshot.parse(
                "com.google.android.apps.youtube.music|10189|22651|1721|9531|AIRPODS_A2DP|1|1");
        check(a.isEligibleOrdinaryMedia(),"ordinary media");
        check(a.route==OneApp2MediaOwnerSnapshot.Route.AIRPODS_A2DP,"airpods route");
        check(a.playing,"media session playing");
        check(a.uid==10189 && a.playerId==1721,"owner uid and player");
        OneApp2MediaOwnerSnapshot b=OneApp2MediaOwnerSnapshot.parse(
                "com.netflix.mediaclient|10311|33200|1840|9500|AA_REMOTE|2|0");
        check(b.isEligibleOrdinaryMedia(),"generic Netflix media");
        check(!b.playing && b.candidateCount==2,"candidate count");
        OneApp2MediaOwnerSnapshot empty=OneApp2MediaOwnerSnapshot.parse(
                "none|none|none|none|none|NONE|0|0");
        check(!empty.hasActiveMediaOwner(),"absent owner not inferred");
        OneApp2MediaOwnerSnapshot gearhead=OneApp2MediaOwnerSnapshot.parse(
                "com.google.android.projection.gearhead|10031|200|345|9500|AA_REMOTE|1|1");
        check(!gearhead.isEligibleOrdinaryMedia(),"exclude gearhead");
        bad(null);bad("");bad("bad|tuple");bad("com.demo.app|abc|1|1|1|AA_REMOTE|1|0");
        bad("com.demo.app|1|1|1|1|INVALID|1|0");
        bad("com.demo.app|1|1|1|1|AA_REMOTE|1|9");
        bad("com.demo.app|1|1|1|1|AA_REMOTE|1|0\nmore");
        bad("none|none|none|none|none|NONE|1|0");
        System.out.println("ONEAPP2_MEDIA_OWNER_CONTRACT_CHECKS="+n);
        System.out.println("ONEAPP2_ACTUAL_MEDIA_DEVICE_AUDIO=NOT_TESTED");
    }
}
