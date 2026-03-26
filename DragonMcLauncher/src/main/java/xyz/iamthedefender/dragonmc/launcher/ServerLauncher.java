package xyz.iamthedefender.dragonmc.launcher;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import xyz.iamthedefender.dragonmc.bot.BotConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

public class ServerLauncher {

    private static final int LOG_CAP = 200;
    private static final String[] logBuf = new String[LOG_CAP];
    private static volatile int logHead = 0;
    private static volatile int logSize = 0;
    private final BotConfig config;
    private final AtomicReference<Process> process = new AtomicReference<>(null);
    private final AtomicReference<Instant> startedAt = new AtomicReference<>(null);
    private final AtomicLong pid = new AtomicLong(-1);
    private final ConcurrentHashMap<String, StringBuilder> commandResults = new ConcurrentHashMap<>();

    public ServerLauncher(BotConfig config) throws IOException {
        this.config = config;
        startApi(config.getApiPort());
        startConsole();
    }

    private static synchronized void log(String line) {
        logBuf[logHead] = line;
        logHead = (logHead + 1) % LOG_CAP;
        if (logSize < LOG_CAP) logSize++;
    }

    private void startApi(int port) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        http.setExecutor(Executors.newCachedThreadPool());
        http.createContext("/status", this::status);
        http.createContext("/start", this::start);
        http.createContext("/stop", this::stop);
        http.createContext("/restart", this::restart);
        http.createContext("/command", this::command);
       // http.createContext("/logs", this::logs);
        http.start();
        System.out.println("[Dragon] Launcher API on :" + port);
    }

    private void status(HttpExchange ex) throws IOException {
        if (cors(ex)) return;
        Process p = process.get();
        boolean up = p != null && p.isAlive();
        long uptime = 0;
        if (up && startedAt.get() != null)
            uptime = Instant.now().getEpochSecond() - startedAt.get().getEpochSecond();
        send(ex, 200, String.format(
                "{\"running\":%b,\"pid\":%d,\"uptimeSec\":%d,\"jar\":\"%s\",\"ramMb\":%d}",
                up, pid.get(), uptime, esc(config.getMcJarPath()), config.getMcRamMb()
        ));
    }

    private void start(HttpExchange ex) throws IOException {
        if (cors(ex)) return;
        if (!post(ex)) return;
        synchronized (process) {
            Process p = process.get();
            if (p != null && p.isAlive()) {
                send(ex, 409, err("Already running"));
                return;
            }
            try {
                Process np = launch();
                process.set(np);
                startedAt.set(Instant.now());
                pid.set(np.pid());
                pipe(np);
                send(ex, 200, ok("Started (pid=" + np.pid() + ")"));
            } catch (Exception e) {
                send(ex, 500, err(e.getMessage()));
            }
        }
    }

    private void stop(HttpExchange ex) throws IOException {
        if (cors(ex)) return;
        if (!post(ex)) return;
        synchronized (process) {
            Process p = process.get();
            if (p == null || !p.isAlive()) {
                send(ex, 409, err("Not running"));
                return;
            }
            write(p, "stop");
        }
        send(ex, 200, ok("stop sent"));
    }

    private void restart(HttpExchange ex) throws IOException {
        if (cors(ex)) return;
        if (!post(ex)) return;
        new Thread(() -> {
            synchronized (process) {
                Process p = process.get();
                if (p != null && p.isAlive()) {
                    write(p, "stop");
                    try {
                        p.waitFor();
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    Process np = launch();
                    process.set(np);
                    startedAt.set(Instant.now());
                    pid.set(np.pid());
                    pipe(np);
                } catch (Exception e) {
                    log("[Dragon] Restart failed: " + e.getMessage());
                }
            }
        }, "restart").start();
        send(ex, 200, ok("Restarting..."));
    }

    private void command(HttpExchange ex) throws IOException {
        if (cors(ex)) return;
        if (!post(ex)) return;
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        String cmd = body.startsWith("{") ? jfield(body, "command") : body;
        if (cmd == null || cmd.isBlank()) {
            send(ex, 400, err("Missing command"));
            return;
        }
        Process p = process.get();
        if (p == null || !p.isAlive()) {
            send(ex, 409, err("Not running"));
            return;
        }

        // Create a unique ID for this command
        String cmdId = "cmd_" + System.nanoTime();
        StringBuilder result = new StringBuilder();
        commandResults.put(cmdId, result);

        // Send command with marker

        System.out.println(cmd);
        write(p, cmd);

        // Wait for result with timeout (5 seconds)
        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        while (System.currentTimeMillis() - startTime < timeout && result.isEmpty()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        String output = result.toString();
        commandResults.remove(cmdId);

        send(ex, 200, "{\"ok\":true,\"message\":\"Sent\",\"result\":\"" + esc(output) + "\"}");
    }

    private Process launch() throws IOException {
        File jar = new File(config.getMcJarPath());
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-Xms512M", "-Xmx" + config.getMcRamMb() + "M",
                "-jar", jar.getAbsolutePath(), "--nogui"
        );
        pb.directory(jar.getParentFile() != null ? jar.getParentFile() : new File("."));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (p.isAlive()) {
                p.destroy();
                try {
                    if (!p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS))
                        p.destroyForcibly();
                } catch (InterruptedException e) {
                    p.destroyForcibly();
                }
            }
        }, "child-task-killer-" + p.pid()));


        return p;
    }

    private void startConsole() {
        Thread t = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Process p = process.get();
                    if (p != null && p.isAlive()) {
                        write(p, line);

                    }
                    else {
                        System.out.println("[Dragon] Server not running, command ignored.");
                    }
                }
            } catch (IOException ignored) {}
        }, "console-passthrough");
        t.setDaemon(true);
        t.start();
    }

    private void pipe(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    log(line);

                    // Check if any command is waiting for results
                    for (StringBuilder result : commandResults.values()) {
                        if (result.length() == 0) {
                            result.append(line).append("\n");
                            break;
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }, "mc-out-" + p.pid());
        t.setDaemon(true);
        t.start();
    }

    private void write(Process p, String cmd) {
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
            pw.println(cmd);
            log(cmd);
        } catch (Exception e) {
            log("[Dragon] Write error: " + e.getMessage());
        }
    }

    public void sendCommand(String cmd) {
        Process p = process.get();
        if (p != null && p.isAlive()) write(p, cmd);
    }

    public boolean isRunning() {
        Process p = process.get();
        return p != null && p.isAlive();
    }

    private boolean cors(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private boolean post(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, err("POST only"));
            return false;
        }
        return true;
    }

    private void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private String ok(String m) {
        return "{\"ok\":true,\"message\":\"" + esc(m) + "\"}";
    }

    private String err(String m) {
        return "{\"ok\":false,\"error\":\"" + esc(m) + "\"}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String jfield(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return null;
        i += s.length();
        int e = json.indexOf('"', i);
        return e < 0 ? null : json.substring(i, e);
    }
}