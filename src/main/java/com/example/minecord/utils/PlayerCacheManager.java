package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerCacheManager {

    private final MineCord plugin;
    private final Set<String> cachedPlayerNames = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> playerLevels = new ConcurrentHashMap<>();

    public PlayerCacheManager(MineCord plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes player cache from whitelist and online players.
     */
    public void init() {
        refreshCache();
    }

    /**
     * Refreshes cached player names from whitelist, usercache, and online players.
     */
    public void refreshCache() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Load from whitelist.json file directly (fast and safe)
                loadWhitelistFile();

                // 2. Load from Bukkit whitelist API
                loadBukkitWhitelist();

                // 3. Load from usercache.json as fallback if whitelist is empty
                if (cachedPlayerNames.isEmpty()) {
                    loadUsercacheFile();
                }

                // 4. Add currently online players
                loadOnlinePlayers();

                plugin.getLogger().info("[MineCord] Завантажено " + cachedPlayerNames.size() + " ніків гравців для автодоповнення.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[MineCord] Помилка завантаження кешу гравців: " + t.getMessage());
            }
        });
    }

    /**
     * Add a player name to the cache (e.g. on PlayerJoinEvent).
     */
    public void addPlayer(String name) {
        if (name != null && !name.trim().isEmpty()) {
            cachedPlayerNames.add(name.trim());
        }
    }

    /**
     * Returns matching player suggestions for Discord autocomplete (max 25).
     * Online players are prioritized at the top of the list.
     */
    public List<String> getMatchingPlayers(String partial) {
        String prefix = (partial == null) ? "" : partial.trim().toLowerCase();
        List<String> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. First priority: currently online players
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    results.add(name);
                    seen.add(name.toLowerCase());
                    if (results.size() >= 25) {
                        return results;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Second priority: whitelisted / cached players
        List<String> sortedCached = new ArrayList<>(cachedPlayerNames);
        Collections.sort(sortedCached, String.CASE_INSENSITIVE_ORDER);

        for (String name : sortedCached) {
            if (!seen.contains(name.toLowerCase()) && name.toLowerCase().startsWith(prefix)) {
                results.add(name);
                seen.add(name.toLowerCase());
                if (results.size() >= 25) {
                    break;
                }
            }
        }

        return results;
    }

    private void loadWhitelistFile() {
        File[] possibleFiles = new File[] {
                new File("whitelist.json"),
                new File(plugin.getServer().getWorldContainer(), "whitelist.json")
        };
        for (File file : possibleFiles) {
            if (file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(sb.toString());
                    while (matcher.find()) {
                        String name = matcher.group(1);
                        if (name != null && !name.trim().isEmpty()) {
                            cachedPlayerNames.add(name.trim());
                        }
                    }
                    break;
                } catch (Exception e) {
                    plugin.getLogger().warning("[MineCord] Не вдалося зчитати whitelist.json: " + e.getMessage());
                }
            }
        }
    }

    private void loadBukkitWhitelist() {
        try {
            for (OfflinePlayer op : Bukkit.getWhitelistedPlayers()) {
                String name = op.getName();
                if (name != null && !name.trim().isEmpty()) {
                    cachedPlayerNames.add(name.trim());
                }
            }
        } catch (Throwable ignored) {}
    }

    private void loadUsercacheFile() {
        File usercache = new File("usercache.json");
        if (usercache.exists() && usercache.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(usercache, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(sb.toString());
                while (matcher.find()) {
                    String name = matcher.group(1);
                    if (name != null && !name.trim().isEmpty()) {
                        cachedPlayerNames.add(name.trim());
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void loadOnlinePlayers() {
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && !name.trim().isEmpty()) {
                    cachedPlayerNames.add(name.trim());
                }
            }
        } catch (Throwable ignored) {}
    }

    public void updatePlayerLevel(UUID uuid, int level) {
        if (uuid != null) {
            playerLevels.put(uuid, level);
        }
    }

    public int getPlayerLevel(OfflinePlayer player) {
        if (player == null) return 0;
        if (player.isOnline() && player.getPlayer() != null) {
            int lvl = player.getPlayer().getLevel();
            playerLevels.put(player.getUniqueId(), lvl);
            return lvl;
        }

        Integer cached = playerLevels.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }

        int lvl = readLevelFromPlayerData(player.getUniqueId());
        playerLevels.put(player.getUniqueId(), lvl);
        return lvl;
    }

    private int readLevelFromPlayerData(UUID uuid) {
        try {
            File worldFolder = null;
            if (!Bukkit.getWorlds().isEmpty()) {
                worldFolder = Bukkit.getWorlds().get(0).getWorldFolder();
            } else {
                worldFolder = new File("world");
            }
            File playerdataFile = new File(new File(worldFolder, "playerdata"), uuid + ".dat");
            if (playerdataFile.exists() && playerdataFile.canRead()) {
                try (java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(playerdataFile))) {
                    byte[] data = gis.readAllBytes();
                    byte[] target = "XpLevel".getBytes(StandardCharsets.UTF_8);
                    for (int i = 0; i <= data.length - target.length - 4; i++) {
                        boolean match = true;
                        for (int j = 0; j < target.length; j++) {
                            if (data[i + j] != target[j]) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            int offset = i + target.length;
                            return ((data[offset] & 0xFF) << 24) |
                                   ((data[offset + 1] & 0xFF) << 16) |
                                   ((data[offset + 2] & 0xFF) << 8) |
                                   (data[offset + 3] & 0xFF);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public Set<String> getCachedPlayerNames() {
        return Collections.unmodifiableSet(cachedPlayerNames);
    }
}
