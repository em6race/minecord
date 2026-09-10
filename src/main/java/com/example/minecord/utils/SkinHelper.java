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

    private static class CacheEntry {
        final String url;
        final long timestamp;

        CacheEntry(String url) {
            this.url = url;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Кеш на 60 секунд, щоб при зміні скіна через /skin він швидко оновлювався
    private static final Map<UUID, CacheEntry> avatarCache = new ConcurrentHashMap<>();
    private static final Map<String, CacheEntry> nameCache = new ConcurrentHashMap<>();

    public static String getAvatarUrl(Player player) {
        if (player == null) {
            return "https://mc-heads.net/avatar/steve/256";
        }

        UUID uuid = player.getUniqueId();
        CacheEntry cached = avatarCache.get(uuid);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp < 60000)) {
            return cached.url;
        }

        String url = resolveAvatarUrl(player);
        CacheEntry entry = new CacheEntry(url);
        avatarCache.put(uuid, entry);
        if (player.getName() != null) {
            nameCache.put(player.getName().toLowerCase(), entry);
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

        CacheEntry cached = nameCache.get(playerName.toLowerCase());
        if (cached != null && (System.currentTimeMillis() - cached.timestamp < 300000)) {
            return cached.url;
        }

        // Спроба отримати з SkinsRestorer для офлайн/щойно вийшовшого гравця
        try {
            org.bukkit.plugin.Plugin sr = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
            if (sr != null && sr.isEnabled()) {
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
                                nameCache.put(playerName.toLowerCase(), new CacheEntry(url));
                                return url;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return "https://mc-heads.net/avatar/" + playerName + "/256";
    }

    public static void clearCache(UUID uuid) {
        avatarCache.remove(uuid);
    }

    private static String resolveAvatarUrl(Player player) {
        // 1. Спроба через Paper PlayerProfile Textures (нативний спосіб Paper)
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

        // 2. Спроба через SkinsRestorer API (працює з будь-якими скінами, встановленими через /skin)
        try {
            org.bukkit.plugin.Plugin sr = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
            if (sr != null && sr.isEnabled()) {
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
            }
        } catch (Throwable ignored) {}

        // 3. Запасний варіант: за нікнеймом гравця
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
        // Шукаємо посилання в JSON: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/4b429074..."}}}
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
