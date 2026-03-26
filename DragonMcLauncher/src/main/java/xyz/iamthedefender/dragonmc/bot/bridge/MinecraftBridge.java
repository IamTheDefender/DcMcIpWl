package xyz.iamthedefender.dragonmc.bot.bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class MinecraftBridge {

    private final String base;

    public MinecraftBridge(int apiPort) {
        this.base = "http://localhost:" + apiPort;
    }

    public CompletableFuture<String> sendCommand(String cmd) {
        return post("/command", "{\"command\":\"" + esc(cmd) + "\"}");
    }

    public CompletableFuture<String> start() {
        return post("/start", "");
    }

    public CompletableFuture<String> stop() {
        return post("/stop", "");
    }

    public CompletableFuture<String> restart() {
        return post("/restart", "");
    }

    public CompletableFuture<String> status() {
        return get("/status");
    }

    public CompletableFuture<String> banPlayer(String n, String r) {
        return sendCommand("dragonmc ban " + n + " " + r);
    }

    public CompletableFuture<String> unbanPlayer(String n) {
        return sendCommand("dragonmc unban " + n);
    }

    public CompletableFuture<String> kickPlayer(String n, String r) {
        return sendCommand("dragonmc kick " + n + " " + r);
    }

    public CompletableFuture<String> mutePlayer(String n) {
        return sendCommand("dragonmc mute " + n);
    }

    public CompletableFuture<String> unmutePlayer(String n) {
        return sendCommand("dragonmc unmute " + n);
    }

    public CompletableFuture<String> broadcastToMc(String u, String m) {
        return sendCommand("dragonmc chat [Discord] <" + u + "> " + m);
    }

    private CompletableFuture<String> get(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return read(open(base + path, "GET"));
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private CompletableFuture<String> post(String path, String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection c = open(base + path, "POST");
                if (!body.isEmpty()) {
                    c.setDoOutput(true);
                    c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                }
                return read(c);
            } catch (Exception e) {
                return err(e);
            }
        });
    }

    private HttpURLConnection open(String url, String method) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        return c;
    }

    private String read(HttpURLConnection c) throws IOException {
        InputStream is;
        try {
            is = c.getInputStream();
        } catch (IOException e) {
            is = c.getErrorStream();
        }
        if (is == null) return "{\"ok\":false}";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String err(Exception e) {
        return "{\"ok\":false,\"error\":\"" + esc(e.getMessage()) + "\"}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
