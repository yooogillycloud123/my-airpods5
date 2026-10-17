package com.aaairpods.bridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class RouteDaemonClient {
    private final int port;
    private final String token;

    public RouteDaemonClient(int port, String token) {
        this.port = port;
        this.token = token;
    }

    public String ping() throws Exception {
        return send("PING", null, 1500);
    }

    public String setDirectRender(String address) throws Exception {
        return send("MIX_SET", address, 7000);
    }

    public String resetDirectRender() throws Exception {
        return send("MIX_RESET", null, 5000);
    }

    public String precheckDirectRender(String address) throws Exception {
        return send("PRECHECK", address, 9000);
    }

    public String diagnoseRouting(String address) throws Exception {
        return send("DIAG", address, 20000);
    }

    public String diagnoseCompactRouting(String address) throws Exception {
        return send("DIAG_COMPACT", address, 20000);
    }

    public String runWorker(String apkPath, String op, String arg) throws Exception {
        String payload = apkPath + "\n" + op + "\n" + (arg == null ? "" : arg);
        String b64 = Base64.getEncoder().encodeToString(
                payload.getBytes(StandardCharsets.UTF_8));
        return send("WORKER", b64, 20000);
    }

    public String stop() throws Exception {
        return send("STOP", null, 2000);
    }

    private String send(String cmd, String arg, int timeoutMs) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String line = token + "|" + cmd + (arg == null ? "" : "|" + arg);
            out.println(line);

            StringBuilder sb = new StringBuilder();
            String r;
            while ((r = in.readLine()) != null) {
                sb.append(r).append('\n');
            }
            return sb.toString().trim();
        }
    }
}
