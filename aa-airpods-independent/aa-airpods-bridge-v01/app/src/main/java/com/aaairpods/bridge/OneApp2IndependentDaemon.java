package com.aaairpods.bridge;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public final class OneApp2IndependentDaemon {
    public static final int DEFAULT_PORT = 47652;
    private static final String VERSION = "daemon=aa-airpods-oneapp2 proto=2 core=20261016 caps=direct_render,precheck,diag,diag_compact,worker";

    private static volatile boolean running = true;
    private static String authToken;

    private static Object activeMix;
    private static String activeAddress;

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("usage: OneApp2IndependentDaemon <port> <token>");
                return;
            }

            int port = Integer.parseInt(args[0]);
            if (port != DEFAULT_PORT || android.os.Process.myUid() != 2000)
                throw new SecurityException("ONEAPP2_REQUIRES_OWN_SHELL_PORT");
            if (args[1] == null || args[1].length() < 32)
                throw new SecurityException("ONEAPP2_TOKEN_MISSING");
            authToken = args[1];

            exemptHiddenApis();

            try (ServerSocket server = new ServerSocket(
                    port, 8, InetAddress.getByName("127.0.0.1"))) {
                System.out.println("READY uid=shell port=" + port + " " + VERSION);
                System.out.flush();

                while (running) {
                    try {
                        Socket socket = server.accept();
                        handle(socket);
                    } catch (Throwable t) {
                        if (running) {
                            System.err.println("accept: " + rootMessage(t));
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("FATAL " + rootMessage(t));
            t.printStackTrace(System.err);
        }
    }

    private static void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true)) {

            s.setSoTimeout(7000);
            String line = br.readLine();
            if (line == null) {
                out.println("ERR empty");
                return;
            }

            String[] parts = line.split("\\|", 3);
            if (parts.length < 2 || !constantTimeEquals(authToken, parts[0])) {
                out.println("ERR auth");
                return;
            }

            String cmd = parts[1];
            // The old Golden daemon has priority; no second AudioPolicy writer.
            // Detection is a local port check, never a STOP/restart request.
            if (("MIX_SET".equals(cmd) || "MIX_RESET".equals(cmd)
                 || "WORKER".equals(cmd)) && originalGoldenPortOpen()) {
                out.println("ONEAPP2_BLOCKED_ORIGINAL_GOLDEN_47651_ALIVE");
                return;
            }
            if ("PING".equals(cmd)) {
                out.println("OK PONG uid=2000 shell daemon=alive " + VERSION);
            } else if ("MIX_SET".equals(cmd)) {
                if (parts.length < 3 || parts[2].isEmpty()) {
                    out.println("ERR missing-address");
                } else {
                    out.print(setDirectRenderMix(parts[2]));
                    out.flush();
                }
            } else if ("MIX_RESET".equals(cmd)) {
                out.print(resetDirectRenderMix());
                out.flush();
            } else if ("PRECHECK".equals(cmd)) {
                if (parts.length < 3 || parts[2].isEmpty()) {
                    out.println("ERR missing-address");
                } else {
                    out.print(precheckDirectRender(parts[2]));
                    out.flush();
                }
            } else if ("DIAG".equals(cmd)) {
                String address = parts.length >= 3 ? parts[2] : activeAddress;
                out.print(diagnoseRouting(address));
                out.flush();
            } else if ("DIAG_COMPACT".equals(cmd)) {
                String address = parts.length >= 3 ? parts[2] : activeAddress;
                out.print(diagnoseCompactRouting(address));
                out.flush();
            } else if ("WORKER".equals(cmd)) {
                if (parts.length < 3 || parts[2].isEmpty()) {
                    out.println("ERR missing-worker-payload");
                } else {
                    out.print(runWorker(parts[2]));
                    out.flush();
                }
            } else if ("STOP".equals(cmd)) {
                out.println("OK stopping");
                running = false;
            } else {
                out.println("ERR unknown-command");
            }
        } catch (Throwable t) {
            try {
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                out.println("ERR " + rootMessage(t));
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean originalGoldenPortOpen(){
        try(java.net.Socket check=new java.net.Socket()){
            check.connect(new java.net.InetSocketAddress("127.0.0.1",47651),230);
            return true;
        }catch(java.io.IOException notListening){return false;}
    }
    private static boolean allowedOneAppWorkerOp(String op){
        return "WORKER_PING".equals(op) || "AA_STATUS".equals(op) ||
               "AA_ROUTE_CHECK".equals(op) || "AA_BYPASS_SET".equals(op) ||
               "AA_BYPASS_RESET".equals(op) || "AA_REFRESH_DEFAULT".equals(op) ||
               "AA_VEHICLE_AFFINITY_SET".equals(op) ||
               "AA_VEHICLE_AFFINITY_CLEAR".equals(op) ||
               "REROUTE_DEFAULT".equals(op) || "MEDIA_REBIND".equals(op) ||
               "CLEAN_A2DP_RENDER".equals(op) || "POLICY_PROBE".equals(op);
    }

    private static String runWorker(String payloadB64) {
        StringBuilder out = new StringBuilder();
        Process process = null;
        try {
            String payload = new String(
                    Base64.getDecoder().decode(payloadB64),
                    StandardCharsets.UTF_8);
            String[] fields = payload.split("\\n", 3);
            if (fields.length < 2) {
                return "WORKER_ERR bad-payload\n";
            }

            String apk = fields[0];
            String op = fields[1];
            String arg = fields.length >= 3 ? fields[2] : "";
            // Only independent routing/auditing operations, never legacy
            // controller/guardian repair/rollback handlers with shared paths.
            if (!allowedOneAppWorkerOp(op))
                return "ONEAPP2_WORKER_OPERATION_NOT_ALLOWED\n";

            if (apk == null || !apk.startsWith("/data/app/") || !apk.endsWith(".apk")) {
                return "WORKER_ERR bad-apk-path\n";
            }
            if (op == null || !op.matches("[A-Z0-9_]{1,64}")) {
                return "WORKER_ERR bad-op\n";
            }

            String argB64 = Base64.getEncoder().encodeToString(
                    arg.getBytes(StandardCharsets.UTF_8));

            ProcessBuilder pb = new ProcessBuilder(
                    "/system/bin/app_process",
                    "/system/bin",
                    "com.aaairpods.bridge.RouteWorker",
                    op,
                    argB64);
            pb.environment().put("CLASSPATH", apk);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int chars = 0;
                while ((line = reader.readLine()) != null) {
                    if (chars + line.length() > 120000) {
                        out.append("...WORKER_OUTPUT_LIMIT...\n");
                        break;
                    }
                    out.append(line).append('\n');
                    chars += line.length() + 1;
                }
            }

            int exit = process.waitFor();
            out.append("WORKER_EXIT=").append(exit).append('\n');
        } catch (Throwable t) {
            out.append("WORKER_ERR ").append(rootMessage(t)).append('\n');
        } finally {
            if (process != null) {
                try { process.destroy(); } catch (Throwable ignored) {}
            }
        }
        return out.toString();
    }

    private static String setDirectRenderMix(String address) {
        StringBuilder out = new StringBuilder();
        Object mix = null;
        try {
            // Clean up any prior direct-render state from this daemon.
            try { removeUserAffinity(); } catch (Throwable ignored) {}
            try {
                if (activeMix != null) unregisterMix(activeMix);
            } catch (Throwable ignored) {}
            activeMix = null;
            activeAddress = null;

            int internalA2dp = internalA2dpType();
            mix = buildRenderMix(internalA2dp, address);

            out.append("RENDER_RULES=normal+system playback usages\n");
            int registerCode = registerMix(mix);
            out.append("RENDER_MIX_REGISTER result=")
                    .append(registerCode)
                    .append(" type=0x")
                    .append(Integer.toHexString(internalA2dp))
                    .append(" addr=").append(mask(address))
                    .append('\n');

            if (registerCode != 0) {
                out.append("DIRECT_RENDER_DONE ok=0 stage=register\n");
                return out.toString();
            }

            int affinityCode = setUserAffinity(internalA2dp, address);
            out.append("USER_AFFINITY user=0 result=")
                    .append(affinityCode).append('\n');

            if (affinityCode != 0) {
                try { unregisterMix(mix); } catch (Throwable ignored) {}
                out.append("DIRECT_RENDER_DONE ok=0 stage=affinity\n");
                return out.toString();
            }

            activeMix = mix;
            activeAddress = address;

            out.append("DIRECT_RENDER_DONE ok=1\n");
            out.append("NOTE route=dynamic-policy-render no-capture no-reencode\n");
            out.append("TARGET user=0 device=A2DP addr=")
                    .append(mask(address)).append('\n');
        } catch (Throwable t) {
            if (mix != null) {
                try { unregisterMix(mix); } catch (Throwable ignored) {}
            }
            try { removeUserAffinity(); } catch (Throwable ignored) {}
            activeMix = null;
            activeAddress = null;
            out.append("DIRECT_RENDER_FATAL=")
                    .append(rootMessage(t)).append('\n');
        }
        return out.toString();
    }

    private static String diagnoseCompactRouting(String address) {
        StringBuilder out = new StringBuilder();
        out.append("=== COMPACT POLICY DIAG ===\n");
        out.append("daemon=").append(VERSION)
                .append(" activeMix=").append(activeMix != null)
                .append(" activeAddr=").append(mask(activeAddress))
                .append("\n");
        if (address != null && !address.isEmpty()) {
            out.append("requestedAddr=").append(mask(address)).append("\n");
        }

        out.append("\n--- NATIVE AUDIO POLICY MIXES ---\n");
        out.append(runDumpsysSection("media.audio_policy",
                "Audio Policy Mix:", "AllowedCapturePolicies:", 220));

        out.append("\n--- ACTIVE PLAYBACK + POLICY REGISTRATION ---\n");
        final String[] audioKeys = new String[]{
                "audioplaybackconfiguration", "state:started", "deviceids:[",
                "registeraudiopolicy", "route flags", "match usage",
                "uid device affinities", "userid device affinities",
                "remote_submix", "remote-submix", "bt_a2dp", "a2dp"
        };
        out.append(runDumpsysFiltered("audio", audioKeys, 170));

        out.append("\n--- NATIVE POLICY TARGETS / PATCHES ---\n");
        final String[] policyKeys = new String[]{
                "audio policy mix", "route flags", "mix type", "criteria",
                "usage", "uid", "userid", "device", "address",
                "remote_submix", "remote-submix", "a2dp", "patch",
                "output"
        };
        out.append(runDumpsysFiltered("media.audio_policy", policyKeys, 180));

        out.append("\n=== END COMPACT POLICY DIAG ===\n");
        return sanitizeMacs(out.toString());
    }

    private static String diagnoseRouting(String address) {
        StringBuilder out = new StringBuilder();
        out.append("=== LIVE ROUTING DIAG ===\n");
        out.append("daemon=").append(VERSION)
                .append(" activeMix=").append(activeMix != null)
                .append(" activeAddr=").append(mask(activeAddress))
                .append("\n");
        if (address != null && !address.isEmpty()) {
            out.append("requestedAddr=").append(mask(address)).append("\n");
        }

        final String[] policyKeys = new String[]{
                "audio policy", "policy mix", "mix", "remote", "submix",
                "a2dp", "bluetooth", "output", "device", "patch",
                "active", "route", "strategy", "affinity", "direct"
        };
        final String[] flingerKeys = new String[]{
                "playback", "thread", "track", "active", "standby",
                "output", "device", "a2dp", "bluetooth", "remote",
                "submix", "direct", "offload"
        };
        final String[] audioKeys = new String[]{
                "route", "device", "bluetooth", "a2dp", "remote",
                "submix", "mode", "force", "broker", "communication",
                "media", "active"
        };

        out.append("\n--- NATIVE AVAILABLE OUTPUT DEVICES (raw section) ---\n");
        out.append(runDumpsysSection("media.audio_policy",
                "Available output", "Available input", 220));
        out.append("\n--- NATIVE AUDIO POLICY MIXES (raw section) ---\n");
        out.append(runDumpsysSection("media.audio_policy",
                "Audio Policy Mix:", "AllowedCapturePolicies:", 260));
        out.append("\n--- dumpsys media.audio_policy (filtered) ---\n");
        out.append(runDumpsysFiltered("media.audio_policy", policyKeys, 420));
        out.append("\n--- dumpsys media.audio_flinger (filtered) ---\n");
        out.append(runDumpsysFiltered("media.audio_flinger", flingerKeys, 260));
        out.append("\n--- dumpsys audio (filtered) ---\n");
        out.append(runDumpsysFiltered("audio", audioKeys, 220));
        out.append("\n=== END LIVE ROUTING DIAG ===\n");
        return sanitizeMacs(out.toString());
    }

    private static String runDumpsysSection(
            String service, String startPattern, String endPattern, int maxLines) {
        StringBuilder out = new StringBuilder();
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/dumpsys", service)
                    .redirectErrorStream(true)
                    .start();

            boolean inSection = false;
            int kept = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!inSection && line.contains(startPattern)) {
                        inSection = true;
                    }
                    if (!inSection) continue;
                    if (kept > 0 && endPattern != null && !endPattern.isEmpty()
                            && line.contains(endPattern)) {
                        break;
                    }
                    out.append(line).append('\n');
                    kept++;
                    if (kept >= maxLines) {
                        out.append("...SECTION_LIMIT_REACHED...\n");
                        break;
                    }
                }
            }
            try { process.waitFor(); } catch (Throwable ignored) {}
            if (out.length() == 0) {
                out.append("(section not found: ").append(startPattern).append(")\n");
            }
        } catch (Throwable t) {
            out.append("DUMPSYS_SECTION_ERROR service=").append(service)
                    .append(" ").append(rootMessage(t)).append('\n');
        } finally {
            if (process != null) {
                try { process.destroy(); } catch (Throwable ignored) {}
            }
        }
        return out.toString();
    }

    private static String runDumpsysFiltered(String service, String[] keys, int maxLines) {
        StringBuilder out = new StringBuilder();
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/dumpsys", service)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                int lineNo = 0;
                int kept = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    String lower = line.toLowerCase(java.util.Locale.US);
                    boolean match = false;
                    for (String key : keys) {
                        if (lower.contains(key)) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        out.append(lineNo).append(": ").append(line).append('\n');
                        kept++;
                        if (kept >= maxLines) {
                            out.append("...FILTER_LIMIT_REACHED...\n");
                            break;
                        }
                    }
                }
            }

            try { process.waitFor(); } catch (Throwable ignored) {}
            if (out.length() == 0) out.append("(no matching lines)\n");
        } catch (Throwable t) {
            out.append("DUMPSYS_ERROR service=").append(service)
                    .append(" ").append(rootMessage(t)).append('\n');
        } finally {
            if (process != null) {
                try { process.destroy(); } catch (Throwable ignored) {}
            }
        }
        return out.toString();
    }

    private static String sanitizeMacs(String s) {
        if (s == null) return "";
        return s.replaceAll(
                "(?i)([0-9a-f]{2}:){4}[0-9a-f]{2}:([0-9a-f]{2})",
                "**:**:**:**:**:$2");
    }

    private static String precheckDirectRender(String address) {
        StringBuilder out = new StringBuilder();
        out.append("PRECHECK_BEGIN addr=").append(mask(address)).append('\n');
        String set = setDirectRenderMix(address);
        out.append(set);
        boolean setOk = set.contains("DIRECT_RENDER_DONE ok=1");
        if (!setOk) {
            out.append("PRECHECK_DONE ok=0 stage=set\n");
            return out.toString();
        }

        String reset = resetDirectRenderMix();
        out.append(reset);
        boolean resetOk = reset.contains("DIRECT_RENDER_RESET_DONE ok=1");
        out.append("PRECHECK_DONE ok=").append(resetOk ? 1 : 0)
                .append(resetOk ? "\n" : " stage=reset\n");
        return out.toString();
    }

    private static String resetDirectRenderMix() {
        StringBuilder out = new StringBuilder();
        try {
            int affinityCode = removeUserAffinity();
            out.append("USER_AFFINITY_RESET user=0 result=")
                    .append(affinityCode).append('\n');

            int unregisterCode = 0;
            if (activeMix != null) {
                unregisterCode = unregisterMix(activeMix);
            } else if (activeAddress != null && !activeAddress.isEmpty()) {
                Object mix = buildRenderMix(internalA2dpType(), activeAddress);
                unregisterCode = unregisterMix(mix);
            }
            out.append("RENDER_MIX_UNREGISTER result=")
                    .append(unregisterCode).append('\n');

            activeMix = null;
            activeAddress = null;
            out.append("DIRECT_RENDER_RESET_DONE ok=")
                    .append((affinityCode == 0 && unregisterCode == 0) ? 1 : 0)
                    .append('\n');
        } catch (Throwable t) {
            out.append("DIRECT_RENDER_RESET_FATAL=")
                    .append(rootMessage(t)).append('\n');
        }
        return out.toString();
    }

    private static Object buildRenderMix(int internalType, String address) throws Exception {
        Class<?> ruleClass = Class.forName(
                "android.media.audiopolicy.AudioMixingRule");
        Class<?> ruleBuilderClass = Class.forName(
                "android.media.audiopolicy.AudioMixingRule$Builder");

        Object ruleBuilder = ruleBuilderClass.getConstructor().newInstance();

        Method setTargetRole = findMethod(ruleBuilderClass, "setTargetMixRole", 1);
        if (setTargetRole != null) {
            setTargetRole.invoke(ruleBuilder, 0); // MIX_ROLE_PLAYERS
        }

        // Samsung/Android 16 rejects an empty AudioMixingRule.
        // Add every normal playback usage as positive criteria. Rules of the same
        // dimension are ORed by AudioPolicy, so any of these usages can match.
        Method addRule = null;
        try {
            addRule = ruleBuilderClass.getMethod(
                    "addRule", AudioAttributes.class, int.class);
        } catch (Throwable ignored) {
        }
        if (addRule == null) {
            addRule = findMethod(ruleBuilderClass, "addRule", 2);
        }
        if (addRule == null) {
            throw new IllegalStateException("AudioMixingRule.addRule unavailable");
        }

        final int RULE_MATCH_ATTRIBUTE_USAGE = 1;
        // SDK + hidden virtual-source usage values accepted by AudioAttributes.Builder.setUsage().
        final int[] usages = new int[]{
                0,  // UNKNOWN
                1,  // MEDIA
                2,  // VOICE_COMMUNICATION
                3,  // VOICE_COMMUNICATION_SIGNALLING
                4,  // ALARM
                5,  // NOTIFICATION
                6,  // NOTIFICATION_RINGTONE
                7,  // NOTIFICATION_COMMUNICATION_REQUEST
                8,  // NOTIFICATION_COMMUNICATION_INSTANT
                9,  // NOTIFICATION_COMMUNICATION_DELAYED
                10, // NOTIFICATION_EVENT
                11, // ASSISTANCE_ACCESSIBILITY
                12, // ASSISTANCE_NAVIGATION_GUIDANCE
                13, // ASSISTANCE_SONIFICATION
                14, // GAME
                15, // VIRTUAL_SOURCE (hidden but valid internally)
                16  // ASSISTANT
        };

        int added = 0;
        for (int usage : usages) {
            try {
                AudioAttributes attr = new AudioAttributes.Builder()
                        .setUsage(usage)
                        .build();
                addRule.invoke(ruleBuilder, attr, RULE_MATCH_ATTRIBUTE_USAGE);
                added++;
            } catch (Throwable ignored) {
            }
        }

        // System usages use setSystemUsage rather than setUsage. Add them when the
        // Samsung framework exposes the method; failure here is non-fatal because
        // the normal playback rule set above is already complete and non-empty.
        Method setSystemUsage = findMethod(
                AudioAttributes.Builder.class, "setSystemUsage", 1);
        if (setSystemUsage != null) {
            final int[] systemUsages = new int[]{
                    17,   // CALL_ASSISTANT
                    1000, // EMERGENCY
                    1001, // SAFETY
                    1002, // VEHICLE_STATUS
                    1003  // ANNOUNCEMENT
            };
            for (int usage : systemUsages) {
                try {
                    AudioAttributes.Builder b = new AudioAttributes.Builder();
                    setSystemUsage.invoke(b, usage);
                    AudioAttributes attr = b.build();
                    addRule.invoke(ruleBuilder, attr, RULE_MATCH_ATTRIBUTE_USAGE);
                    added++;
                } catch (Throwable ignored) {
                }
            }
        }

        if (added == 0) {
            throw new IllegalStateException("No playback usage rules could be added");
        }

        Method buildRule = ruleBuilderClass.getMethod("build");
        Object rule = buildRule.invoke(ruleBuilder);

        Class<?> mixClass = Class.forName(
                "android.media.audiopolicy.AudioMix");
        Class<?> mixBuilderClass = Class.forName(
                "android.media.audiopolicy.AudioMix$Builder");

        Constructor<?> ctor = mixBuilderClass.getConstructor(ruleClass);
        Object mixBuilder = ctor.newInstance(rule);

        Method setRouteFlags = findMethod(
                mixBuilderClass, "setRouteFlags", 1);
        if (setRouteFlags == null) {
            throw new IllegalStateException("AudioMix.setRouteFlags unavailable");
        }
        setRouteFlags.invoke(mixBuilder, 1); // ROUTE_FLAG_RENDER

        Method setDevice = null;
        for (Method m : mixBuilderClass.getMethods()) {
            if (!"setDevice".equals(m.getName())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 2 && p[0] == int.class && p[1] == String.class) {
                setDevice = m;
                break;
            }
        }
        if (setDevice == null) {
            for (Method m : mixBuilderClass.getDeclaredMethods()) {
                if (!"setDevice".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == int.class && p[1] == String.class) {
                    try { m.setAccessible(true); } catch (Throwable ignored) {}
                    setDevice = m;
                    break;
                }
            }
        }
        if (setDevice == null) {
            throw new IllegalStateException("AudioMix.setDevice(int,String) unavailable");
        }
        setDevice.invoke(mixBuilder, internalType, address);

        Method buildMix = mixBuilderClass.getMethod("build");
        Object mix = buildMix.invoke(mixBuilder);

        if (!mixClass.isInstance(mix)) {
            throw new IllegalStateException("AudioMix build returned invalid object");
        }
        return mix;
    }

    private static int registerMix(Object mix) throws Exception {
        Class<?> audioSystem = Class.forName("android.media.AudioSystem");
        Method register = audioSystem.getDeclaredMethod(
                "registerPolicyMixes", ArrayList.class, boolean.class);
        register.setAccessible(true);

        ArrayList<Object> mixes = new ArrayList<>();
        mixes.add(mix);
        Object r = register.invoke(null, mixes, true);
        return r instanceof Integer ? (Integer) r : 0;
    }

    private static int unregisterMix(Object mix) throws Exception {
        Class<?> audioSystem = Class.forName("android.media.AudioSystem");
        Method register = audioSystem.getDeclaredMethod(
                "registerPolicyMixes", ArrayList.class, boolean.class);
        register.setAccessible(true);

        ArrayList<Object> mixes = new ArrayList<>();
        mixes.add(mix);
        Object r = register.invoke(null, mixes, false);
        return r instanceof Integer ? (Integer) r : 0;
    }

    private static int setUserAffinity(int internalType, String address) throws Exception {
        Class<?> audioSystem = Class.forName("android.media.AudioSystem");
        Method set = audioSystem.getDeclaredMethod(
                "setUserIdDeviceAffinities",
                int.class, int[].class, String[].class);
        set.setAccessible(true);
        Object r = set.invoke(
                null, 0, new int[]{internalType}, new String[]{address});
        return r instanceof Integer ? (Integer) r : 0;
    }

    private static int removeUserAffinity() throws Exception {
        Class<?> audioSystem = Class.forName("android.media.AudioSystem");
        Method remove = audioSystem.getDeclaredMethod(
                "removeUserIdDeviceAffinities", int.class);
        remove.setAccessible(true);
        Object r = remove.invoke(null, 0);
        return r instanceof Integer ? (Integer) r : 0;
    }

    private static int internalA2dpType() throws Exception {
        Method convert = AudioDeviceInfo.class.getDeclaredMethod(
                "convertDeviceTypeToInternalDevice", int.class);
        convert.setAccessible(true);
        return (Integer) convert.invoke(
                null, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
    }

    private static Method findMethod(Class<?> cls, String name, int count) {
        for (Method m : cls.getMethods()) {
            if (name.equals(m.getName())
                    && m.getParameterTypes().length == count) {
                try { m.setAccessible(true); } catch (Throwable ignored) {}
                return m;
            }
        }
        for (Method m : cls.getDeclaredMethods()) {
            if (name.equals(m.getName())
                    && m.getParameterTypes().length == count) {
                try { m.setAccessible(true); } catch (Throwable ignored) {}
                return m;
            }
        }
        return null;
    }

    private static void exemptHiddenApis() {
        try {
            Class<?> vmRuntime = Class.forName("dalvik.system.VMRuntime");
            Method getRuntime = vmRuntime.getDeclaredMethod("getRuntime");
            Method setHiddenApiExemptions = vmRuntime.getDeclaredMethod(
                    "setHiddenApiExemptions", String[].class);
            getRuntime.setAccessible(true);
            setHiddenApiExemptions.setAccessible(true);
            Object runtime = getRuntime.invoke(null);
            setHiddenApiExemptions.invoke(
                    runtime, (Object) new String[]{"L"});
        } catch (Throwable ignored) {
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        int diff = a.length() ^ b.length();
        int max = Math.max(a.length(), b.length());
        for (int i = 0; i < max; i++) {
            char ca = i < a.length() ? a.charAt(i) : 0;
            char cb = i < b.length() ? b.charAt(i) : 0;
            diff |= ca ^ cb;
        }
        return diff == 0;
    }

    private static String mask(String address) {
        if (address == null || address.length() < 5) return "(empty)";
        return "**:**:**:**:" + address.substring(address.length() - 5);
    }

    private static String rootMessage(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null) x = x.getCause();
        String m = x.getMessage();
        return x.getClass().getSimpleName()
                + (m == null ? "" : ": " + m);
    }
}
