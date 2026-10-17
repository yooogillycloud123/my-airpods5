package com.aaairpods.bridge;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Isolated OneApp2 user-approved ADB shell transport.
 * It never selects or starts the old :47651 daemon and never copies the
 * original application's pairing identity. Callers choose only their
 * own validated commands after verifying the new package identity.
 */
public final class OneApp2AdbShell {
    private OneApp2AdbShell(){}
    public static String quote(String text) {
        if(text==null)throw new IllegalArgumentException("null shell argument");
        return "'" + text.replace("'", "'\\''") + "'";
    }
    public static String runShell(AbsAdbConnectionManager manager,String command)
        throws Exception {
        if(manager==null || command==null || command.isEmpty())
            throw new IllegalArgumentException("missing ADB shell session");
        try(AdbStream stream=manager.openStream("shell:"+command);
            InputStream input=stream.openInputStream();
            ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] bytes=new byte[4096];
            int count;
            while((count=input.read(bytes))>=0) {
                if(count==0)continue;
                if(output.size()+count>1048576)
                    throw new java.io.IOException("ONEAPP2_SHELL_OUTPUT_LIMIT");
                output.write(bytes,0,count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
