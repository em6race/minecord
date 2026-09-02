package com.example.minecord.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamManager {
    
    private final Map<UUID, Long> mutedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> fastMessageCount = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageText = new ConcurrentHashMap<>();
    
    public void mute(UUID player, int seconds) {
        mutedUntil.put(player, System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public boolean isMuted(UUID player) {
        if (!mutedUntil.containsKey(player)) return false;
        if (System.currentTimeMillis() > mutedUntil.get(player)) {
            mutedUntil.remove(player);
            return false;
        }
        return true;
    }
    
    public long getMuteRemainingSeconds(UUID player) {
        if (!isMuted(player)) return 0;
        return (mutedUntil.get(player) - System.currentTimeMillis()) / 1000L;
    }
    
    /**
     * Checks if the user is spamming. 
     * @return 0 = OK, 1 = Spam Warning (Block msg), 2 = Muted (Block msg & Mute)
     */
    public int checkSpamLevel(UUID player, String message) {
        long now = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(player, 0L);
        String lastMsg = lastMessageText.getOrDefault(player, "");
        
        lastMessageTime.put(player, now);
        lastMessageText.put(player, message);
        
        boolean isSpam = false;
        
        // Check 1: Too fast (less than 1.5 seconds)
        if (now - lastTime < 1500) {
            isSpam = true;
        }
        
        // Check 2: Repeated message within 10 seconds
        if (message.equalsIgnoreCase(lastMsg) && (now - lastTime < 10000)) {
            isSpam = true;
        }
        
        if (isSpam) {
            int count = fastMessageCount.getOrDefault(player, 0) + 1;
            fastMessageCount.put(player, count);
            if (count >= 3) {
                // Mute for 5 minutes (300 seconds)
                mute(player, 300);
                fastMessageCount.remove(player);
                return 2; // Muted
            }
            return 1; // Warning
        } else {
            // Decrease spam count slightly if they sent a normal message
            int count = fastMessageCount.getOrDefault(player, 0);
            if (count > 0) fastMessageCount.put(player, count - 1);
        }
        
        return 0; // OK
    }
}
