package com.aaairpods.bridge;

import android.os.SystemClock;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * In-process, fixed-size diagnostic state machine journal.
 * Only symbolic stage names, numeric generation counters and event timing.
 * No Bluetooth addresses, user content, tokens, passwords or shell outputs.
 */
public final class OneApp2CycleJournal {
    private static final int MAX=350;
    private static final Deque<String> entries=new ArrayDeque<>();
    private static long sequence;
    private static SharedPreferences ownStore;
    private OneApp2CycleJournal(){}
    public static synchronized void initialize(Context context){
        if(context==null||!OneApp2Identity.isOneApp2(context.getPackageName()))
            return;
        if(ownStore!=null)return;
        ownStore=context.getApplicationContext().getSharedPreferences(
            "oneapp2_symbolic_history_v1",Context.MODE_PRIVATE);
        String previous=ownStore.getString("lines","");
        if(previous==null)return;
        for(String line:previous.split("\\n")){
            if(line.matches("[0-9]+ \\| [0-9]+ \\| monotonic_ms=[0-9]+ \\| [A-Z_]{1,40} \\| [A-Z0-9_]{1,96}")){
                entries.addLast(line);
                if(entries.size()>MAX)entries.removeFirst();
                try{
                    long number=Long.parseLong(line.substring(0,line.indexOf(' ')));
                    if(number>sequence)sequence=number;
                }catch(NumberFormatException ignored){}
            }
        }
    }
    public static synchronized void record(String component,String state){
        if(component==null||state==null ||
           !component.matches("[A-Z_]{1,40}") ||
           !state.matches("[A-Z0-9_]{1,96}"))return;
        String s=(++sequence)+" | "+System.currentTimeMillis()
               +" | monotonic_ms="+SystemClock.elapsedRealtime()
               +" | "+component+" | "+state;
        entries.addLast(s);
        while(entries.size()>MAX)entries.removeFirst();
        if(ownStore!=null){
            StringBuilder b=new StringBuilder();
            for(String entry:entries)b.append(entry).append('\n');
            ownStore.edit().putString("lines",b.toString()).apply();
        }
    }
    public static synchronized String report(){
        StringBuilder b=new StringBuilder();
        b.append("AA_ONEAPP2_LOCAL_EVENT_TIMELINE\n");
        b.append("SOURCE=NEW_INDEPENDENT_PACKAGE_ONLY\n");
        b.append("ORIGINAL_GOLDEN_LOG=NOT_ACCESSED\n");
        b.append("DEVICE_AUDIO_PROOF=NOT_INFERRED\n");
        for(String s:entries)b.append(s).append('\n');
        return b.toString();
    }
}
