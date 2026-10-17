package com.aaairpods.bridge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * Android public-PackageManager-only side-by-side UID/service inventory.
 * No commands, filesystem inspection, audio policy, ADB or state mutation.
 * Reports actual installed package UIDs instead of assuming same-UID parity.
 */
public final class OneAppLabRoleInventory {
    private static final String[] ROUTING_PACKAGES={
        "com.aaairpods.r25fix",
        "com.aaairpods.bridge",
        "com.aaairpods.edge23",
        "com.aaairpods.edge24",
        "com.aaairpods.unified",
        "com.aaairpods.reboot",
        "com.aaairpods.rescue",
        "com.aaairpods.r3btst"
    };
    private OneAppLabRoleInventory(){}

    private static String digest(Signature sig){
        if(sig==null)return "UNKNOWN";
        try{
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(sig.toByteArray());
            StringBuilder out=new StringBuilder(64);
            for(byte b:bytes)out.append(String.format(Locale.US,"%02x",b&255));
            return out.toString();
        }catch(Exception ex){return "UNKNOWN";}
    }
    private static String one(Context context,String pkg){
        try{
            PackageManager pm=context.getPackageManager();
            int flags=PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SERVICES;
            PackageInfo p=Build.VERSION.SDK_INT>=33
                ?pm.getPackageInfo(pkg,PackageManager.PackageInfoFlags.of(flags))
                :pm.getPackageInfo(pkg,flags);
            ApplicationInfo a=p.applicationInfo;
            if(a==null)return pkg+"\n  APP_INFO_UNAVAILABLE";
            String signer="UNKNOWN";
            if(p.signingInfo!=null){
                Signature[] certs=p.signingInfo.getApkContentsSigners();
                if(certs!=null&&certs.length==1)signer=digest(certs[0]);
            }
            StringBuilder s=new StringBuilder();
            s.append(pkg).append("\n  uid=").append(a.uid)
             .append("  targetSdk=").append(a.targetSdkVersion)
             .append("  version=").append(p.getLongVersionCode())
             .append("\n  process=").append(a.processName==null?"UNKNOWN":a.processName)
             .append("\n  signerSha256=").append(signer);
            ServiceInfo[] svc=p.services;
            if(svc!=null){
                s.append("\n  declaredServices=").append(svc.length);
                for(int i=0;i<svc.length&&i<4;i++){
                    ServiceInfo item=svc[i];
                    s.append("\n    service=").append(item.name)
                     .append(" process=").append(item.processName)
                     .append(" exported=").append(item.exported);
                }
                if(svc.length>4)s.append("\n    +").append(svc.length-4).append(" more");
            }
            return s.toString();
        }catch(PackageManager.NameNotFoundException missing){
            return pkg+"\n  STATUS=NOT_INSTALLED_OR_NOT_VISIBLE";
        }catch(RuntimeException denied){
            return pkg+"\n  STATUS=READ_ONLY_QUERY_FAILED_"+
                denied.getClass().getSimpleName();
        }
    }
    public static String readOnlySnapshot(Context c){
        if(c==null)return "INVENTORY=BLOCK_NO_CONTEXT";
        StringBuilder out=new StringBuilder();
        out.append("INVENTORY=ANDROID_PACKAGE_MANAGER_READ_ONLY\n");
        out.append("LAB_ID=").append(c.getPackageName()).append("\n");
        out.append(one(c,c.getPackageName())).append("\n");
        for(String pkg:ROUTING_PACKAGES)
            if(!pkg.equals(c.getPackageName()))
                out.append("\n").append(one(c,pkg)).append("\n");
        out.append("\nFOCUS_OWNER_UID_EQUIVALENCE=UNVERIFIED");
        out.append("\nLEGACY_CONTROLLER_PROCESS=NOT_INSPECTED");
        out.append("\nAUDIO_POLICY_MUTATIONS=NONE");
        out.append("\nSHELL_OR_ADB_COMMANDS=NONE");
        out.append("\nORIGINAL_APP_DATA_MUTATION=NONE");
        return out.toString();
    }
}
