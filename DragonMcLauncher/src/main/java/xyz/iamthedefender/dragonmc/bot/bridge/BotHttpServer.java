package xyz.iamthedefender.dragonmc.bot.bridge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import xyz.iamthedefender.dragonmc.bot.commands.LinkCommand;
import xyz.iamthedefender.dragonmc.bot.ip.IpLookup;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class BotHttpServer {

    public static void start(int port) throws IOException {
        HttpServer srv = HttpServer.create(new InetSocketAddress(port), 0);
        srv.setExecutor(Executors.newCachedThreadPool());
        srv.createContext("/mc-chat", BotHttpServer::mcChat);
        srv.createContext("/ip-confirm", BotHttpServer::ipConfirm);
        srv.start();
        System.out.println("[Dragon] Bot HTTP on :" + port);
    }

    private static void mcChat(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String player = field(body, "player");
        String msg = field(body, "message");
        if (player != null && msg != null) ChatBridgeListener.relayToDiscord(player, msg);
        send(ex, 200, "{\"ok\":true}");
    }

    private static void ipConfirm(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "{}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String discordId = field(body, "discordId");
        String mcUser = field(body, "mcUsername");
        String ip = field(body, "ip");
        if (discordId == null || mcUser == null || ip == null) {
            send(ex, 400, "{\"ok\":false}");
            return;
        }
        IpLookup.lookup(ip).thenAccept(loc ->
                LinkCommand.sendIpConfirmPrompt(discordId, mcUser, ip, loc.format())
        );
        send(ex, 200, "{\"ok\":true}");
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private static String field(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return null;
        i += s.length();
        int end = json.indexOf('"', i);
        return end < 0 ? null : json.substring(i, end);
    }
}
