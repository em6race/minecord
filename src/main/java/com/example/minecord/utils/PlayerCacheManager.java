package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PlayerCacheManager {

    public static class PlayerXpData {
        public final int level;
        public final int totalExp;

        public PlayerXpData(int level, int totalExp) {
            this.level = level;
            this.totalExp = totalExp;
        }
    }

    private final MineCord plugin;
    private final Set<String> cachedPlayerNames = ConcurrentHashMap.newKeySet();
    private final Map<String, UUID> playerNameToUuid = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidToPlayerName = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerXpData> playerXpCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerAdvancementsCache = new ConcurrentHashMap<>();
    private final Set<String> visibleAdvancementKeys = ConcurrentHashMap.newKeySet();
    private int totalAdvancementsCount = -1;

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
     * Refreshes cached player names and UUIDs from whitelist, usercache, and online players.
     */
    public void refreshCache() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Load from whitelist.json file directly (fast and safe)
                loadWhitelistFile();

                // 2. Load from Bukkit whitelist API
                loadBukkitWhitelist();

                // 3. Load from usercache.json
                loadUsercacheFile();

                // 4. Add currently online players
                loadOnlinePlayers();

                // 5. Cache visible server advancements count
                refreshAdvancements();

                plugin.getLogger().info("[MineCord] Завантажено " + cachedPlayerNames.size() + " гравців у кеш.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[MineCord] Помилка завантаження кешу гравців: " + t.getMessage());
            }
        });
    }

    /**
     * Add a player name to the cache.
     */
    public void addPlayer(String name) {
        if (name != null && !name.trim().isEmpty()) {
            cachedPlayerNames.add(name.trim());
        }
    }

    /**
     * Add a player name and UUID to the cache.
     */
    public void addPlayer(String name, UUID uuid) {
        if (name != null && !name.trim().isEmpty()) {
            String trimmed = name.trim();
            cachedPlayerNames.add(trimmed);
            if (uuid != null) {
                playerNameToUuid.put(trimmed.toLowerCase(), uuid);
                uuidToPlayerName.put(uuid, trimmed);
            }
        }
    }

    /**
     * Look up known UUID for a player nickname.
     */
    public UUID getUuidByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return playerNameToUuid.get(name.trim().toLowerCase());
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

    private void parseJsonForPlayers(String content) {
        if (content == null || content.isEmpty()) return;
        Matcher objMatcher = Pattern.compile("\\{[^{}]*\\}").matcher(content);
        while (objMatcher.find()) {
            String obj = objMatcher.group();
            Matcher nameMatcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(obj);
            Matcher uuidMatcher = Pattern.compile("\"uuid\"\\s*:\\s*\"([^\"]+)\"").matcher(obj);
            if (nameMatcher.find()) {
                String name = nameMatcher.group(1).trim();
                if (!name.isEmpty()) {
                    cachedPlayerNames.add(name);
                    if (uuidMatcher.find()) {
                        try {
                            UUID uuid = UUID.fromString(uuidMatcher.group(1).trim());
                            playerNameToUuid.put(name.toLowerCase(), uuid);
                            uuidToPlayerName.put(uuid, name);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
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
                    parseJsonForPlayers(sb.toString());
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
                UUID uuid = op.getUniqueId();
                if (name != null && !name.trim().isEmpty()) {
                    cachedPlayerNames.add(name.trim());
                    if (uuid != null) {
                        playerNameToUuid.put(name.trim().toLowerCase(), uuid);
                        uuidToPlayerName.put(uuid, name.trim());
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void loadUsercacheFile() {
        File[] possibleFiles = new File[] {
                new File("usercache.json"),
                new File(plugin.getServer().getWorldContainer(), "usercache.json")
        };
        for (File usercache : possibleFiles) {
            if (usercache.exists() && usercache.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(usercache, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    parseJsonForPlayers(sb.toString());
                    break;
                } catch (Exception ignored) {}
            }
        }
    }

    private void loadOnlinePlayers() {
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                UUID uuid = p.getUniqueId();
                if (name != null && !name.trim().isEmpty()) {
                    cachedPlayerNames.add(name.trim());
                    if (uuid != null) {
                        playerNameToUuid.put(name.trim().toLowerCase(), uuid);
                        uuidToPlayerName.put(uuid, name.trim());
                        playerXpCache.put(uuid, new PlayerXpData(p.getLevel(), p.getTotalExperience()));
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void updatePlayerLevel(UUID uuid, int level) {
        if (uuid != null) {
            PlayerXpData existing = playerXpCache.get(uuid);
            int total = existing != null ? existing.totalExp : 0;
            playerXpCache.put(uuid, new PlayerXpData(level, total));
        }
    }

    public void updatePlayerXp(UUID uuid, int level, int totalExp) {
        if (uuid != null) {
            playerXpCache.put(uuid, new PlayerXpData(level, totalExp));
        }
    }

    public PlayerXpData getPlayerXp(OfflinePlayer player) {
        if (player == null) return new PlayerXpData(0, 0);

        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (player.isOnline() && player.getPlayer() != null) {
            Player p = player.getPlayer();
            PlayerXpData data = new PlayerXpData(p.getLevel(), p.getTotalExperience());
            if (uuid != null) {
                playerXpCache.put(uuid, data);
            }
            return data;
        }

        if (uuid != null) {
            PlayerXpData cached = playerXpCache.get(uuid);
            if (cached != null) {
                return cached;
            }
        }

        PlayerXpData loaded = readXpFromPlayerData(uuid, name);
        if (uuid != null) {
            playerXpCache.put(uuid, loaded);
        }
        return loaded;
    }

    public int getPlayerLevel(OfflinePlayer player) {
        return getPlayerXp(player).level;
    }

    public int getPlayerTotalExp(OfflinePlayer player) {
        return getPlayerXp(player).totalExp;
    }

    public boolean hasPlayerData(OfflinePlayer player) {
        if (player == null) return false;
        if (player.isOnline() || player.hasPlayedBefore() || player.getLastPlayed() > 0) return true;
        return findPlayerDataFile(player.getUniqueId(), player.getName()) != null;
    }

    public File findPlayerDataFile(UUID uuid, String playerName) {
        return findUserDataFile("playerdata", ".dat", uuid, playerName);
    }

    public File findAdvancementsFile(UUID uuid, String playerName) {
        return findUserDataFile("advancements", ".json", uuid, playerName);
    }

    public File findUserDataFile(String subDir, String extension, UUID uuid, String playerName) {
        if (uuid == null && playerName == null) return null;

        List<File> searchDirs = getPossibleWorldDirs(subDir);
        List<UUID> candidateUuids = new ArrayList<>();
        if (uuid != null) candidateUuids.add(uuid);
        if (playerName != null && !playerName.trim().isEmpty()) {
            UUID cachedUuid = playerNameToUuid.get(playerName.trim().toLowerCase());
            if (cachedUuid != null && !candidateUuids.contains(cachedUuid)) {
                candidateUuids.add(cachedUuid);
            }
            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName.trim()).getBytes(StandardCharsets.UTF_8));
            if (!candidateUuids.contains(offlineUuid)) {
                candidateUuids.add(offlineUuid);
            }
        }

        Set<String> seenDirs = new HashSet<>();
        for (File dir : searchDirs) {
            if (dir == null) continue;
            try {
                String canonical = dir.getCanonicalPath();
                if (!seenDirs.add(canonical)) continue;
            } catch (Exception e) {
                if (!seenDirs.add(dir.getAbsolutePath())) continue;
            }

            if (!dir.exists() || !dir.isDirectory()) continue;

            for (UUID u : candidateUuids) {
                File f = new File(dir, u.toString() + extension);
                if (f.exists() && f.canRead() && f.length() > 0) {
                    return f;
                }
                File fNoDash = new File(dir, u.toString().replace("-", "") + extension);
                if (fNoDash.exists() && fNoDash.canRead() && fNoDash.length() > 0) {
                    return fNoDash;
                }
            }
        }

        return null;
    }

    private List<File> getPossibleWorldDirs(String subDirName) {
        List<File> searchDirs = new ArrayList<>();

        // 1. All worlds known to Bukkit
        try {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                if (w != null && w.getWorldFolder() != null) {
                    searchDirs.add(new File(w.getWorldFolder(), subDirName));
                }
            }
        } catch (Throwable ignored) {}

        // 2. Primary world from server.properties
        try {
            File props = new File("server.properties");
            if (props.exists() && props.canRead()) {
                try (BufferedReader br = new BufferedReader(new FileReader(props, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("level-name=")) {
                            String levelName = line.substring("level-name=".length()).trim();
                            if (!levelName.isEmpty()) {
                                searchDirs.add(new File(levelName, subDirName));
                                searchDirs.add(new File(Bukkit.getWorldContainer(), levelName + "/" + subDirName));
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. World container and its direct children
        try {
            File worldContainer = Bukkit.getWorldContainer();
            if (worldContainer != null && worldContainer.exists()) {
                searchDirs.add(new File(worldContainer, subDirName));
                searchDirs.add(new File(worldContainer, "world/" + subDirName));
                File[] subdirs = worldContainer.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File sub : subdirs) {
                        searchDirs.add(new File(sub, subDirName));
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 4. Default root locations
        searchDirs.add(new File("world/" + subDirName));
        searchDirs.add(new File(subDirName));

        return searchDirs;
    }

    private PlayerXpData readXpFromPlayerData(UUID uuid, String playerName) {
        try {
            File playerdataFile = findPlayerDataFile(uuid, playerName);
            if (playerdataFile == null) {
                return new PlayerXpData(0, 0);
            }

            byte[] compressed = Files.readAllBytes(playerdataFile.toPath());
            if (compressed == null || compressed.length == 0) {
                return new PlayerXpData(0, 0);
            }

            byte[] data = null;
            // GZIP: 0x1F 0x8B
            if (compressed.length > 2 && (compressed[0] == (byte) 0x1F) && (compressed[1] == (byte) 0x8B)) {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                    data = gis.readAllBytes();
                }
            }
            // ZLIB: 0x78
            else if (compressed.length > 2 && compressed[0] == 0x78) {
                try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
                    data = iis.readAllBytes();
                }
            }
            // Uncompressed NBT: TAG_Compound = 0x0A
            else if (compressed.length > 0 && compressed[0] == 0x0A) {
                data = compressed;
            }

            if (data == null || data.length < 10) {
                return new PlayerXpData(0, 0);
            }

            int level = findTagInt(data, "XpLevel");
            int totalXp = findTagInt(data, "XpTotal");

            return new PlayerXpData(Math.max(0, level), Math.max(0, totalXp));
        } catch (Throwable t) {
            plugin.getLogger().warning("[MineCord] Не вдалося зчитати playerdata для " + (uuid != null ? uuid : playerName) + ": " + t.getMessage());
            return new PlayerXpData(0, 0);
        }
    }

    private int findTagInt(byte[] data, String tagName) {
        byte[] nameBytes = tagName.getBytes(StandardCharsets.UTF_8);
        byte[] pattern = new byte[3 + nameBytes.length];
        pattern[0] = 3; // TAG_Int
        pattern[1] = (byte) ((nameBytes.length >> 8) & 0xFF);
        pattern[2] = (byte) (nameBytes.length & 0xFF);
        System.arraycopy(nameBytes, 0, pattern, 3, nameBytes.length);

        // 1. Exact match with tagId=3 and length
        for (int i = 0; i <= data.length - pattern.length - 4; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                int offset = i + pattern.length;
                return ((data[offset] & 0xFF) << 24) |
                       ((data[offset + 1] & 0xFF) << 16) |
                       ((data[offset + 2] & 0xFF) << 8) |
                       (data[offset + 3] & 0xFF);
            }
        }

        // 2. Fallback: match tagName only
        for (int i = 0; i <= data.length - nameBytes.length - 4; i++) {
            boolean match = true;
            for (int j = 0; j < nameBytes.length; j++) {
                if (data[i + j] != nameBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                int offset = i + nameBytes.length;
                int val = ((data[offset] & 0xFF) << 24) |
                          ((data[offset + 1] & 0xFF) << 16) |
                          ((data[offset + 2] & 0xFF) << 8) |
                          (data[offset + 3] & 0xFF);
                if (val >= 0 && val < 2000000000) {
                    return val;
                }
            }
        }

        return 0;
    }

    public int getTotalAdvancements() {
        if (totalAdvancementsCount > 0) {
            return totalAdvancementsCount;
        }
        refreshAdvancements();
        return Math.max(totalAdvancementsCount, 1);
    }

    public synchronized void refreshAdvancements() {
        int count = 0;
        try {
            Iterator<org.bukkit.advancement.Advancement> it = Bukkit.advancementIterator();
            while (it.hasNext()) {
                org.bukkit.advancement.Advancement adv = it.next();
                String key = adv.getKey().toString();
                String subKey = adv.getKey().getKey();
                if (subKey.startsWith("recipes/") || subKey.endsWith("/root")) {
                    continue;
                }
                boolean hasDisplay = false;
                try {
                    hasDisplay = (adv.getDisplay() != null);
                } catch (Throwable ignored) {
                    hasDisplay = true;
                }
                if (hasDisplay) {
                    visibleAdvancementKeys.add(key);
                    count++;
                }
            }
        } catch (Throwable ignored) {}

        if (count > 0) {
            totalAdvancementsCount = count;
        } else if (totalAdvancementsCount <= 0) {
            totalAdvancementsCount = 110;
        }
    }

    public int getPlayerAdvancements(OfflinePlayer player) {
        if (player == null) return 0;
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (player.isOnline() && player.getPlayer() != null) {
            int done = countOnlinePlayerAdvancements(player.getPlayer());
            if (uuid != null) {
                playerAdvancementsCache.put(uuid, done);
            }
            return done;
        }

        if (uuid != null) {
            Integer cached = playerAdvancementsCache.get(uuid);
            if (cached != null) {
                return cached;
            }
        }

        int done = readCompletedAdvancements(uuid, name);
        if (uuid != null) {
            playerAdvancementsCache.put(uuid, done);
        }
        return done;
    }

    public int countOnlinePlayerAdvancements(Player player) {
        if (player == null) return 0;
        if (visibleAdvancementKeys.isEmpty()) {
            refreshAdvancements();
        }

        int done = 0;
        for (String keyStr : visibleAdvancementKeys) {
            try {
                org.bukkit.NamespacedKey nsk = org.bukkit.NamespacedKey.fromString(keyStr);
                if (nsk != null) {
                    org.bukkit.advancement.Advancement adv = Bukkit.getAdvancement(nsk);
                    if (adv != null && player.getAdvancementProgress(adv).isDone()) {
                        done++;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return done;
    }

    public void updatePlayerAdvancements(Player player) {
        if (player != null && player.getUniqueId() != null) {
            int count = countOnlinePlayerAdvancements(player);
            playerAdvancementsCache.put(player.getUniqueId(), count);
        }
    }

    public void incrementAdvancements(UUID uuid) {
        if (uuid != null) {
            playerAdvancementsCache.compute(uuid, (k, v) -> (v == null ? 1 : v + 1));
        }
    }

    private int readCompletedAdvancements(UUID uuid, String playerName) {
        try {
            File advFile = findAdvancementsFile(uuid, playerName);
            if (advFile == null || !advFile.exists()) {
                return 0;
            }

            String content = Files.readString(advFile.toPath(), StandardCharsets.UTF_8);
            if (content == null || content.isEmpty()) return 0;

            if (visibleAdvancementKeys.isEmpty()) {
                refreshAdvancements();
            }

            int count = 0;
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.startsWith("minecraft:recipes/") || key.endsWith("/root") || key.equals("DataVersion")) {
                    continue;
                }

                if (!visibleAdvancementKeys.isEmpty() && !visibleAdvancementKeys.contains(key)) {
                    continue;
                }

                if (entry.getValue().isJsonObject()) {
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    if (obj.has("done") && obj.get("done").getAsBoolean()) {
                        count++;
                    }
                }
            }
            return count;
        } catch (Throwable t) {
            plugin.getLogger().warning("[MineCord] Не вдалося зчитати advancements для " + (uuid != null ? uuid : playerName) + ": " + t.getMessage());
            return 0;
        }
    }

    public Set<String> getCachedPlayerNames() {
        return Collections.unmodifiableSet(cachedPlayerNames);
    }
}
