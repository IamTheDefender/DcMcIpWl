package xyz.iamthedefender.dragonmc.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpamUtil {

    private static final Map<String, Long> spamMap = new ConcurrentHashMap<>();

    public static boolean getAndSet(String id, long cooldownMs) {
        if (spamMap.get(id) != null) {
            long cooldown = spamMap.get(id);

            if (System.currentTimeMillis() > cooldown) {
                spamMap.put(id, System.currentTimeMillis() + cooldownMs);
                return false;
            }

            return true;
        }

        spamMap.put(id, System.currentTimeMillis() + cooldownMs);
        return false;
    }

}
