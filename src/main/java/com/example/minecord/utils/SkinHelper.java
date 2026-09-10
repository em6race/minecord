package com.example.minecord.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinHelper {

    // 60-second cache to update quickly when skin is changed via /skin
    private static final Map<UUID, String> avatarCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> avatarTimeCache = new ConcurrentHashMap<>();
    private static final Map<String, String> nameCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> nameTimeCache = new ConcurrentHashMap<>();

    public static String getAvatarUrl(Player player) {
        if (player == null) {
            return "https://mc-heads.net/avatar/steve/256";
        }

        UUID uuid = player.getUniqueId();
        String cached = avatarCache.get(uuid);
        Long cachedTime = avatarTimeCache.get(uuid);
        if (cached != null && cachedTime != null && (System.currentTimeMillis() - cachedTime < 60000)) {
            return cached;
        }

        String url = resolveAvatarUrl(player);
        long now = System.currentTimeMillis();
        avatarCache.put(uuid, url);
        avatarTimeCache.put(uuid, now);
        if (player.getName() != null) {
            String lowerName = player.getName().toLowerCase();
            nameCache.put(lowerName, url);
            nameTimeCache.put(lowerName, now);
        }
        return url;
    }

    public static String getAvatarUrl(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return "https://mc-heads.net/avatar/steve/256";
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return getAvatarUrl(player);
        }

        String lowerName = playerName.toLowerCase();
        String cached = nameCache.get(lowerName);
        Long cachedTime = nameTimeCache.get(lowerName);
        if (cached != null && cachedTime != null && (System.currentTimeMillis() - cachedTime < 300000)) {
            return cached;
        }

        // Attempt to get skin from SkinsRestorer for offline/recently disconnected player (only if plugin is present)
        if (isSkinsRestorerAvailable()) {
            try {
                Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                Object srApi = providerClass.getMethod("get").invoke(null);
                Object playerStorage = srApi.getClass().getMethod("getPlayerStorage").invoke(srApi);
                java.lang.reflect.Method getSkinMethod = playerStorage.getClass().getMethod("getSkinForPlayer", UUID.class, String.class);
                UUID offlineUuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                Object optSkin = getSkinMethod.invoke(playerStorage, offlineUuid, playerName);

                if (optSkin instanceof java.util.Optional) {
                    java.util.Optional<?> opt = (java.util.Optional<?>) optSkin;
                    if (opt.isPresent()) {
                        Object skinProperty = opt.get();
                        String base64Value = (String) skinProperty.getClass().getMethod("getValue").invoke(skinProperty);
                        if (base64Value != null && !base64Value.isEmpty()) {
                            String decodedJson = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
                            String hash = extractHashFromJson(decodedJson);
                            if (hash != null && !hash.isEmpty()) {
                                String url = "https://mc-heads.net/avatar/" + hash + "/256";
                                nameCache.put(lowerName, url);
                                nameTimeCache.put(lowerName, System.currentTimeMillis());
                                return url;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return "https://mc-heads.net/avatar/" + playerName + "/256";
    }

    public static boolean isSkinsRestorerAvailable() {
        try {
            org.bukkit.plugin.Plugin sr = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
            return sr != null && sr.isEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clearCache(UUID uuid) {
        avatarCache.remove(uuid);
        avatarTimeCache.remove(uuid);
    }

    private static String resolveAvatarUrl(Player player) {
        // 1. Attempt via Paper PlayerProfile textures (native Paper method)
        try {
            org.bukkit.profile.PlayerTextures textures = player.getPlayerProfile().getTextures();
            URL skinUrl = textures.getSkin();
            if (skinUrl != null) {
                String hash = extractHash(skinUrl.toString());
                if (hash != null && !hash.isEmpty()) {
                    return "https://mc-heads.net/avatar/" + hash + "/256";
                }
            }
        } catch (Throwable ignored) {}

        // 2. Attempt via SkinsRestorer API (only if plugin is installed and active)
        if (isSkinsRestorerAvailable()) {
            try {
                Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                Object srApi = providerClass.getMethod("get").invoke(null);
                Object playerStorage = srApi.getClass().getMethod("getPlayerStorage").invoke(srApi);
                java.lang.reflect.Method getSkinMethod = playerStorage.getClass().getMethod("getSkinForPlayer", UUID.class, String.class);
                Object optSkin = getSkinMethod.invoke(playerStorage, player.getUniqueId(), player.getName());

                if (optSkin instanceof java.util.Optional) {
                    java.util.Optional<?> opt = (java.util.Optional<?>) optSkin;
                    if (opt.isPresent()) {
                        Object skinProperty = opt.get();
                        String base64Value = (String) skinProperty.getClass().getMethod("getValue").invoke(skinProperty);
                        if (base64Value != null && !base64Value.isEmpty()) {
                            String decodedJson = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
                            String hash = extractHashFromJson(decodedJson);
                            if (hash != null && !hash.isEmpty()) {
                                return "https://mc-heads.net/avatar/" + hash + "/256";
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback: query by player name
        return "https://mc-heads.net/avatar/" + player.getName() + "/256";
    }

    private static String extractHash(String url) {
        if (url == null || url.isEmpty()) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1).trim();
        }
        return null;
    }

    private static String extractHashFromJson(String json) {
        // Extract URL from JSON: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/4b429074..."}}}
        int idx = json.indexOf("/texture/");
        if (idx != -1) {
            int start = idx + 9;
            int end = json.indexOf('"', start);
            if (end != -1) {
                return json.substring(start, end).trim();
            }
        }
        return null;
    }
}
