package xyz.iamthedefender.dragonmc.bot.ip;

import xyz.iamthedefender.dragonmc.Main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class IpLookup {

    public static CompletableFuture<Result> lookup(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(Main.config.getIpApiUrl() + ip).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 429) {
                    System.out.println("[Dragon] IpLookup rate limited.");
                    return new Result(false, null, null, null, null);
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                if (!json.contains("\"status\":\"success\""))
                    return new Result(false, null, null, null, null);
                return new Result(true, ex(json, "country"), ex(json, "regionName"), ex(json, "city"), ex(json, "isp"));
            } catch (Exception e) {
                System.out.println("[Dragon] IpLookup error: " + e.getMessage());
                return new Result(false, null, null, null, null);
            }
        });
    }

    private static String ex(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return "?";
        i += s.length();
        int end = json.indexOf('"', i);
        return end < 0 ? "?" : json.substring(i, end);
    }

    public record Result(boolean success, String country, String region, String city, String isp) {

        public String format() {
                if (!success) return "Location unavailable";
                return city + ", " + region + ", " + country + " -- " + isp;
            }
        }
}
