package com.example.minecord.utils;

import com.example.minecord.MineCord;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamManager {
    
    private final MineCord plugin;
    private final Map<UUID, Long> mutedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> fastMessageCount = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastWarningTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageText = new ConcurrentHashMap<>();
    
    public AntiSpamManager(MineCord plugin) {
        this.plugin = plugin;
    }
    
    public void mute(UUID player, int seconds) {
        mutedUntil.put(player, System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public boolean unmute(UUID player) {
        fastMessageCount.remove(player);
        lastWarningTime.remove(player);
        return mutedUntil.remove(player) != null;
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
        if (!plugin.getConfig().getBoolean("antispam.enabled", true)) {
            return 0; // AntiSpam is disabled
        }

        long now = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(player, 0L);
        String lastMsg = lastMessageText.getOrDefault(player, "");
        
        long cooldownMs = plugin.getConfig().getLong("antispam.cooldown-ms", 700);
        int dupTimeoutSec = plugin.getConfig().getInt("antispam.duplicate-timeout-seconds", 3);
        int maxWarnings = plugin.getConfig().getInt("antispam.max-warnings", 5);
        int muteDuration = plugin.getConfig().getInt("antispam.mute-duration-seconds", 60);
        
        // Decay warnings if more than 15 seconds have passed since the last warning
        long lastWarn = lastWarningTime.getOrDefault(player, 0L);
        if (now - lastWarn > 15000) {
            fastMessageCount.remove(player);
        }

        boolean isSpam = false;
        
        // Check 1: Too fast (cooldown between messages, default 700ms)
        if (lastTime > 0 && (now - lastTime < cooldownMs)) {
            isSpam = true;
        }
        
        // Check 2: Repeated identical message (only if length > 3 to allow "бб", "ок", "+", etc.)
        String trimmed = message.trim();
        if (dupTimeoutSec > 0 && trimmed.length() > 3 && 
            trimmed.equalsIgnoreCase(lastMsg.trim()) && 
            (now - lastTime < dupTimeoutSec * 1000L)) {
            isSpam = true;
        }
        
        if (isSpam) {
            lastWarningTime.put(player, now);
            int count = fastMessageCount.getOrDefault(player, 0) + 1;
            fastMessageCount.put(player, count);
            if (count >= maxWarnings) {
                mute(player, muteDuration);
                fastMessageCount.remove(player);
                return 2; // Muted
            }
            return 1; // Warning
        } else {
            // Only update lastMessageTime and lastMessageText when message was successfully ALLOWED!
            lastMessageTime.put(player, now);
            lastMessageText.put(player, message);
            
            // Decrease spam count slightly if they sent a normal message
            int count = fastMessageCount.getOrDefault(player, 0);
            if (count > 0) fastMessageCount.put(player, count - 1);
        }
        
        return 0; // OK
    }
}
