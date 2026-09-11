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
    private final Map<String, UUID> mojangUuidCache = new ConcurrentHashMap<>();
    private static final UUID EMPTY_UUID_SENTINEL = new UUID(0L, 0L);

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

                // 4. Load from world playerdata files
                loadPlayerDataFiles();

                // 5. Add currently online players
                loadOnlinePlayers();

                // 6. Cache visible server advancements count
                refreshAdvancements();

                plugin.getLogger().info("[MineCord] Завантажено " + cachedPlayerNames.size() + " гравців у кеш.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[MineCord] Помилка завантаження кешу гравців: " + t.getMessage());
            }
        });
    }

    /**
     * Checks whether a string is a valid Minecraft username.
     * Java Edition usernames: 3-16 characters [a-zA-Z0-9_].
     * Geyser/Floodgate Bedrock usernames: can start with prefix '.' or '*' followed by 3-16 chars.
     * Filters out server hostnames (like kozlomine.join-server.online), slash characters, bot scanners, etc.
     */
    public static boolean isValidMinecraftUsername(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.length() < 3 || trimmed.length() > 17) return false;
        return trimmed.matches("^[.*]?[a-zA-Z0-9_]{3,16}$");
    }

    /**
     * Add a player name to the cache.
     */
    public void addPlayer(String name) {
        if (isValidMinecraftUsername(name)) {
            cachedPlayerNames.add(name.trim());
        }
    }

    /**
     * Add a player name and UUID to the cache.
     */
    public void addPlayer(String name, UUID uuid) {
        if (isValidMinecraftUsername(name)) {
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
     * Look up known player nickname for a UUID.
     */
    public String getPlayerNameByUuid(UUID uuid) {
        if (uuid == null) return null;
        return uuidToPlayerName.get(uuid);
    }

    /**
     * Parses a 32-character hex string into a UUID with dashes.
     */
    public static UUID parseUuidWithoutDashes(String id) {
        if (id == null || id.length() != 32) return null;
        try {
            return UUID.fromString(
                    id.substring(0, 8) + "-" +
                    id.substring(8, 12) + "-" +
                    id.substring(12, 16) + "-" +
                    id.substring(16, 20) + "-" +
                    id.substring(20, 32)
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Queries the official Mojang API to get the official Online UUID for a licensed player.
     * Returns null if player is not licensed, not found, or on network error.
     * Caches both hits and misses to prevent redundant lookups.
     * Will NOT perform network I/O if called on the main server thread.
     */
    public UUID fetchMojangUuid(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return null;
        String cleanName = playerName.trim();
        String lowerName = cleanName.toLowerCase();

        UUID cached = mojangUuidCache.get(lowerName);
        if (cached != null) {
            return cached.equals(EMPTY_UUID_SENTINEL) ? null : cached;
        }

        // Avoid blocking the main server tick thread with HTTP requests
        if (Bukkit.isPrimaryThread()) {
            return null;
        }

        try {
            java.net.URI uri = java.net.URI.create("https://api.mojang.com/users/profiles/minecraft/" + cleanName);
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .header("User-Agent", "MineCord-Minecraft-Bot")
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                if (obj.has("id")) {
                    String id = obj.get("id").getAsString();
                    UUID mojangUuid = parseUuidWithoutDashes(id);
                    if (mojangUuid != null) {
                        mojangUuidCache.put(lowerName, mojangUuid);
                        return mojangUuid;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Silently handle network errors/timeouts
        }

        mojangUuidCache.put(lowerName, EMPTY_UUID_SENTINEL);
        return null;
    }

    /**
     * Resolves a player nickname for a given UUID across memory cache, online players,
     * Bukkit OfflinePlayer, disk NBT (lastKnownName), and Mojang Session API (async only).
     */
    public String resolvePlayerName(UUID uuid) {
        if (uuid == null) return null;

        // 1. Check in-memory cache
        String cached = uuidToPlayerName.get(uuid);
        if (cached != null && !cached.trim().isEmpty()) {
            return cached.trim();
        }

        // 2. Check currently online player
        try {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getName() != null && !p.getName().trim().isEmpty()) {
                String name = p.getName().trim();
                addPlayer(name, uuid);
                return name;
            }
        } catch (Throwable ignored) {}

        // 3. Check Bukkit OfflinePlayer
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op != null && op.getName() != null && !op.getName().trim().isEmpty()) {
                String name = op.getName().trim();
                addPlayer(name, uuid);
                return name;
            }
        } catch (Throwable ignored) {}

        // 4. Check playerdata file NBT lastKnownName
        try {
            File dat = findExactUserDataFile("playerdata", ".dat", uuid);
            if (dat != null && dat.exists()) {
                byte[] nbt = readDecompressedNbt(dat);
                if (nbt != null) {
                    String lastKnown = findTagString(nbt, "lastKnownName");
                    if (lastKnown != null && !lastKnown.trim().isEmpty()) {
                        String name = lastKnown.trim();
                        addPlayer(name, uuid);
                        return name;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 5. Query Mojang Session API for official player name if on async thread
        if (!Bukkit.isPrimaryThread()) {
            try {
                String noDash = uuid.toString().replace("-", "");
                java.net.URI uri = java.net.URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + noDash);
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(3))
                        .build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(java.time.Duration.ofSeconds(3))
                        .header("User-Agent", "MineCord-Minecraft-Bot")
                        .GET()
                        .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                    JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (obj.has("name")) {
                        String name = obj.get("name").getAsString();
                        if (name != null && !name.trim().isEmpty()) {
                            name = name.trim();
                            addPlayer(name, uuid);
                            return name;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Resolves the OfflinePlayer candidate with the most recent actual data on this server.
     * Evaluates online status, Bukkit statistics, playerdata .dat, stats .json, and advancements .json.
     * Safely handles differences between online-mode (Mojang UUID) and offline-mode (OfflinePlayer hash).
     */
    public OfflinePlayer resolvePlayerWithData(String targetName, List<UUID> preferredUuids) {
        String cleanName = (targetName != null) ? targetName.trim() : null;
        if (cleanName != null && cleanName.isEmpty()) cleanName = null;

        // If targetName is not provided, try to resolve player name from preferred UUIDs
        if (cleanName == null && preferredUuids != null) {
            for (UUID u : preferredUuids) {
                if (u != null) {
                    String resolved = resolvePlayerName(u);
                    if (resolved != null && !resolved.trim().isEmpty()) {
                        cleanName = resolved.trim();
                        break;
                    }
                }
            }
        }

        // 1. Check if player is currently online by nickname
        if (cleanName != null) {
            try {
                Player online = Bukkit.getPlayerExact(cleanName);
                if (online != null) {
                    addPlayer(online.getName(), online.getUniqueId());
                    return online;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Check if player is currently online by preferred UUIDs
        if (preferredUuids != null) {
            for (UUID u : preferredUuids) {
                if (u != null) {
                    try {
                        Player online = Bukkit.getPlayer(u);
                        if (online != null) {
                            addPlayer(online.getName(), online.getUniqueId());
                            return online;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }

        // Collect candidate UUIDs
        LinkedHashSet<UUID> candidates = new LinkedHashSet<>();
        if (preferredUuids != null) {
            for (UUID u : preferredUuids) {
                if (u != null) candidates.add(u);
            }
        }

        UUID mojangUuid = null;
        UUID offlineUuid = null;
        UUID offlineLowerUuid = null;

        if (cleanName != null) {
            UUID cached = playerNameToUuid.get(cleanName.toLowerCase());
            if (cached != null) candidates.add(cached);

            mojangUuid = fetchMojangUuid(cleanName);
            if (mojangUuid != null) candidates.add(mojangUuid);

            offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + cleanName).getBytes(StandardCharsets.UTF_8));
            candidates.add(offlineUuid);

            offlineLowerUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + cleanName.toLowerCase()).getBytes(StandardCharsets.UTF_8));
            candidates.add(offlineLowerUuid);
        }

        // Score each candidate based on evidence on disk or in Bukkit
        UUID bestUuid = null;
        long bestScore = -1;

        for (UUID cand : candidates) {
            if (cand == null) continue;
            long score = 0;

            OfflinePlayer op = Bukkit.getOfflinePlayer(cand);
            if (op.isOnline()) {
                score = Long.MAX_VALUE;
            } else {
                if (op.hasPlayedBefore() || op.getLastPlayed() > 0) {
                    score = Math.max(score, Math.max(1000L, op.getLastPlayed()));
                }

                File dat = findExactUserDataFile("playerdata", ".dat", cand);
                if (dat != null && dat.exists() && dat.length() > 0) {
                    score = Math.max(score, dat.lastModified());
                }

                File json = findExactUserDataFile("stats", ".json", cand);
                if (json != null && json.exists() && json.length() > 0) {
                    score = Math.max(score, json.lastModified());
                }

                File adv = findExactUserDataFile("advancements", ".json", cand);
                if (adv != null && adv.exists() && adv.length() > 0) {
                    score = Math.max(score, adv.lastModified());
                }
            }

            if (score > 0 && score > bestScore) {
                bestScore = score;
                bestUuid = cand;
            }
        }

        // If candidate with actual data found
        if (bestUuid != null) {
            if (cleanName != null) {
                playerNameToUuid.put(cleanName.toLowerCase(), bestUuid);
                uuidToPlayerName.put(bestUuid, cleanName);
                cachedPlayerNames.add(cleanName);
            }
            return Bukkit.getOfflinePlayer(bestUuid);
        }

        // Fallback when no data exists on disk
        UUID fallbackUuid = null;
        boolean isOnlineMode = false;
        try {
            isOnlineMode = Bukkit.getOnlineMode();
        } catch (Throwable ignored) {}

        UUID knownCachedUuid = (cleanName != null) ? playerNameToUuid.get(cleanName.toLowerCase()) : null;
        if (isOnlineMode) {
            if (mojangUuid != null) fallbackUuid = mojangUuid;
            else if (knownCachedUuid != null) fallbackUuid = knownCachedUuid;
            else if (!candidates.isEmpty()) fallbackUuid = candidates.iterator().next();
            else if (offlineUuid != null) fallbackUuid = offlineUuid;
        } else {
            // In offline-mode server, prefer known UUID from whitelist/cache, then offline UUID
            if (knownCachedUuid != null) fallbackUuid = knownCachedUuid;
            else if (offlineUuid != null) fallbackUuid = offlineUuid;
            else if (!candidates.isEmpty()) fallbackUuid = candidates.iterator().next();
        }

        if (fallbackUuid != null) {
            if (cleanName != null) {
                playerNameToUuid.put(cleanName.toLowerCase(), fallbackUuid);
                uuidToPlayerName.put(fallbackUuid, cleanName);
                cachedPlayerNames.add(cleanName);
            }
            return Bukkit.getOfflinePlayer(fallbackUuid);
        }

        if (cleanName != null) {
            return Bukkit.getOfflinePlayer(cleanName);
        }

        return null;
    }

    public OfflinePlayer resolvePlayerWithData(String targetName, UUID preferredUuid) {
        List<UUID> list = (preferredUuid != null) ? Collections.singletonList(preferredUuid) : null;
        return resolvePlayerWithData(targetName, list);
    }

    /**
     * Resolves the actual UUID used by this player on the server.
     * Delegates to resolvePlayerWithData for robust candidate selection.
     */
    public UUID resolveExistingPlayerUuid(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return null;
        OfflinePlayer op = resolvePlayerWithData(playerName, (List<UUID>) null);
        return (op != null) ? op.getUniqueId() : null;
    }

    private List<File> getPossibleWhitelistFiles() {
        List<File> files = new ArrayList<>();
        files.add(new File("whitelist.json"));
        try {
            if (plugin.getServer().getWorldContainer() != null) {
                files.add(new File(plugin.getServer().getWorldContainer(), "whitelist.json"));
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.getDataFolder() != null && plugin.getDataFolder().getParentFile() != null && plugin.getDataFolder().getParentFile().getParentFile() != null) {
                files.add(new File(plugin.getDataFolder().getParentFile().getParentFile(), "whitelist.json"));
            }
        } catch (Throwable ignored) {}
        try {
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                files.add(new File(userDir, "whitelist.json"));
            }
        } catch (Throwable ignored) {}
        return files;
    }

    public Set<String> readWhitelistFile() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (File file : getPossibleWhitelistFiles()) {
            if (file != null && file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed != null && parsed.isJsonArray()) {
                        for (JsonElement el : parsed.getAsJsonArray()) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String n = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString().trim() : null;
                                String u = obj.has("uuid") && !obj.get("uuid").isJsonNull() ? obj.get("uuid").getAsString().trim() : null;
                                if (isValidMinecraftUsername(n)) {
                                    names.add(n);
                                    if (u != null) {
                                        try {
                                            UUID uuid = UUID.fromString(u);
                                            addPlayer(n, uuid);
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }
                        }
                    }
                    if (!names.isEmpty()) {
                        break;
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("[MineCord] Помилка зчитування whitelist.json: " + t.getMessage());
                }
            }
        }
        return names;
    }

    /**
     * Reads the current live whitelist from whitelist.json and Bukkit whitelist.
     * Always returns up-to-date names so removed players and invalid entries are omitted from autocomplete.
     */
    public Set<String> getCurrentWhitelistedPlayers() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        // 1. From whitelist.json directly
        names.addAll(readWhitelistFile());

        // 2. From Bukkit whitelist API
        try {
            for (OfflinePlayer op : Bukkit.getWhitelistedPlayers()) {
                String n = op.getName();
                if (n == null && op.getUniqueId() != null) {
                    n = resolvePlayerName(op.getUniqueId());
                }
                if (isValidMinecraftUsername(n)) {
                    names.add(n.trim());
                    if (op.getUniqueId() != null) {
                        addPlayer(n.trim(), op.getUniqueId());
                    }
                }
            }
        } catch (Throwable ignored) {}

        return names;
    }

    public boolean isWhitelisted(String playerName) {
        if (!isValidMinecraftUsername(playerName)) return false;
        return getCurrentWhitelistedPlayers().contains(playerName.trim());
    }

    /**
     * Returns matching whitelisted player suggestions for Discord /linkadmin autocomplete (max 25).
     * Only whitelisted players are returned, prioritizing currently online players, then alphabetically.
     * Non-whitelisted and invalid names (bots, scanners, hostnames) are strictly excluded.
     */
    public List<String> getMatchingWhitelistedPlayers(String partial) {
        String prefix = (partial == null) ? "" : partial.trim().toLowerCase();
        List<String> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Set<String> liveWhitelist = getCurrentWhitelistedPlayers();

        // 1. First priority: whitelisted players who are currently online
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (isValidMinecraftUsername(name) && (liveWhitelist.isEmpty() || liveWhitelist.contains(name)) && name.toLowerCase().startsWith(prefix)) {
                    results.add(name);
                    seen.add(name.toLowerCase());
                    if (results.size() >= 25) {
                        return results;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Second priority: remaining whitelisted players (sorted alphabetically)
        if (!liveWhitelist.isEmpty()) {
            List<String> sortedWhitelist = new ArrayList<>(liveWhitelist);
            Collections.sort(sortedWhitelist, String.CASE_INSENSITIVE_ORDER);

            for (String name : sortedWhitelist) {
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

        // 3. Fallback only if whitelist is empty / disabled: valid cached players
        List<String> sortedCached = new ArrayList<>();
        for (String name : cachedPlayerNames) {
            if (isValidMinecraftUsername(name)) {
                sortedCached.add(name);
            }
        }
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

    /**
     * Returns matching player suggestions for commands like /stats (max 25).
     * Online players and whitelisted players are prioritized at the top, followed by other valid cached players.
     * Strictly filters out invalid Minecraft usernames.
     */
    public List<String> getMatchingPlayers(String partial) {
        String prefix = (partial == null) ? "" : partial.trim().toLowerCase();
        List<String> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. First priority: currently online players
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (isValidMinecraftUsername(name) && name.toLowerCase().startsWith(prefix)) {
                    results.add(name);
                    seen.add(name.toLowerCase());
                    if (results.size() >= 25) {
                        return results;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Second priority: current live whitelist
        Set<String> liveWhitelist = getCurrentWhitelistedPlayers();
        if (!liveWhitelist.isEmpty()) {
            List<String> sortedWhitelist = new ArrayList<>(liveWhitelist);
            Collections.sort(sortedWhitelist, String.CASE_INSENSITIVE_ORDER);

            for (String name : sortedWhitelist) {
                if (isValidMinecraftUsername(name) && !seen.contains(name.toLowerCase()) && name.toLowerCase().startsWith(prefix)) {
                    results.add(name);
                    seen.add(name.toLowerCase());
                    if (results.size() >= 25) {
                        return results;
                    }
                }
            }
        }

        // 3. Additional players from cache (only valid usernames)
        List<String> sortedCached = new ArrayList<>();
        for (String name : cachedPlayerNames) {
            if (isValidMinecraftUsername(name)) {
                sortedCached.add(name);
            }
        }
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
                if (isValidMinecraftUsername(name)) {
                    UUID uuid = null;
                    if (uuidMatcher.find()) {
                        try {
                            uuid = UUID.fromString(uuidMatcher.group(1).trim());
                        } catch (Exception ignored) {}
                    }
                    addPlayer(name, uuid);
                }
            }
        }
    }

    private void loadWhitelistFile() {
        Set<String> whitelisted = readWhitelistFile();
        for (String name : whitelisted) {
            if (isValidMinecraftUsername(name)) {
                cachedPlayerNames.add(name);
            }
        }
    }

    private void loadBukkitWhitelist() {
        try {
            for (OfflinePlayer op : Bukkit.getWhitelistedPlayers()) {
                String name = op.getName();
                UUID uuid = op.getUniqueId();
                if (isValidMinecraftUsername(name)) {
                    addPlayer(name.trim(), uuid);
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
                if (isValidMinecraftUsername(name)) {
                    addPlayer(name.trim(), uuid);
                    if (uuid != null) {
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
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        if (name == null && uuid != null) {
            name = resolvePlayerName(uuid);
        }
        if (uuid != null) {
            if (findExactUserDataFile("playerdata", ".dat", uuid) != null) return true;
            if (findExactUserDataFile("stats", ".json", uuid) != null) return true;
        }
        if (findPlayerDataFile(uuid, name) != null) {
            return true;
        }
        return findUserDataFile("stats", ".json", uuid, name) != null;
    }

    public File findPlayerDataFile(UUID uuid, String playerName) {
        return findUserDataFile("playerdata", ".dat", uuid, playerName);
    }

    public File findAdvancementsFile(UUID uuid, String playerName) {
        return findUserDataFile("advancements", ".json", uuid, playerName);
    }

    public File findExactUserDataFile(String subDir, String extension, UUID uuid) {
        if (uuid == null) return null;

        List<File> searchDirs = getPossibleWorldDirs(subDir);
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

            File f = new File(dir, uuid.toString() + extension);
            if (f.exists() && f.canRead() && f.length() > 0) {
                return f;
            }
            File fNoDash = new File(dir, uuid.toString().replace("-", "") + extension);
            if (fNoDash.exists() && fNoDash.canRead() && fNoDash.length() > 0) {
                return fNoDash;
            }
        }

        return null;
    }

    public File findUserDataFile(String subDir, String extension, UUID uuid, String playerName) {
        if (uuid == null && playerName == null) return null;

        List<File> searchDirs = getPossibleWorldDirs(subDir);
        List<UUID> candidateUuids = new ArrayList<>();
        if (uuid != null) candidateUuids.add(uuid);
        if (playerName != null && !playerName.trim().isEmpty()) {
            String cleanName = playerName.trim();
            UUID cachedUuid = playerNameToUuid.get(cleanName.toLowerCase());
            if (cachedUuid != null && !candidateUuids.contains(cachedUuid)) {
                candidateUuids.add(cachedUuid);
            }
            UUID mojangUuid = fetchMojangUuid(cleanName);
            if (mojangUuid != null && !candidateUuids.contains(mojangUuid)) {
                candidateUuids.add(mojangUuid);
            }
            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + cleanName).getBytes(StandardCharsets.UTF_8));
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
                    if (playerName != null && !playerName.trim().isEmpty()) {
                        playerNameToUuid.put(playerName.trim().toLowerCase(), u);
                        uuidToPlayerName.put(u, playerName.trim());
                    }
                    return f;
                }
                File fNoDash = new File(dir, u.toString().replace("-", "") + extension);
                if (fNoDash.exists() && fNoDash.canRead() && fNoDash.length() > 0) {
                    if (playerName != null && !playerName.trim().isEmpty()) {
                        playerNameToUuid.put(playerName.trim().toLowerCase(), u);
                        uuidToPlayerName.put(u, playerName.trim());
                    }
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

    private byte[] readDecompressedNbt(File file) {
        if (file == null || !file.exists() || file.length() == 0) return null;
        try {
            byte[] compressed = Files.readAllBytes(file.toPath());
            if (compressed.length == 0) return null;

            // GZIP: 0x1F 0x8B
            if (compressed.length > 2 && (compressed[0] == (byte) 0x1F) && (compressed[1] == (byte) 0x8B)) {
                try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                    return gis.readAllBytes();
                }
            }
            // ZLIB: 0x78
            else if (compressed.length > 2 && compressed[0] == 0x78) {
                try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
                    return iis.readAllBytes();
                }
            }
            // Uncompressed NBT: TAG_Compound = 0x0A
            else if (compressed[0] == 0x0A) {
                return compressed;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void loadPlayerDataFiles() {
        try {
            List<File> playerdataDirs = getPossibleWorldDirs("playerdata");
            Set<String> seen = new HashSet<>();
            for (File dir : playerdataDirs) {
                if (dir == null || !dir.isDirectory()) continue;
                try {
                    String can = dir.getCanonicalPath();
                    if (!seen.add(can)) continue;
                } catch (Exception e) {
                    if (!seen.add(dir.getAbsolutePath())) continue;
                }

                File[] files = dir.listFiles((d, name) -> name.endsWith(".dat") && !name.endsWith(".dat_old"));
                if (files == null) continue;

                for (File f : files) {
                    String fname = f.getName();
                    String rawUuid = fname.substring(0, fname.length() - 4);
                    UUID uuid = null;
                    try {
                        if (rawUuid.contains("-")) {
                            uuid = UUID.fromString(rawUuid);
                        } else if (rawUuid.length() == 32) {
                            uuid = parseUuidWithoutDashes(rawUuid);
                        }
                    } catch (Exception ignored) {}

                    if (uuid == null) continue;

                    String name = null;
                    try {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                        name = op.getName();
                    } catch (Throwable ignored) {}

                    if (name == null || name.isEmpty()) {
                        try {
                            byte[] nbt = readDecompressedNbt(f);
                            if (nbt != null) {
                                name = findTagString(nbt, "lastKnownName");
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (name != null && !name.trim().isEmpty()) {
                        addPlayer(name.trim(), uuid);
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[MineCord] Не вдалося завантажити playerdata файли: " + t.getMessage());
        }
    }

    private String findTagString(byte[] data, String tagName) {
        if (data == null || tagName == null) return null;
        byte[] nameBytes = tagName.getBytes(StandardCharsets.UTF_8);
        byte[] pattern = new byte[3 + nameBytes.length];
        pattern[0] = 8; // TAG_String
        pattern[1] = (byte) ((nameBytes.length >> 8) & 0xFF);
        pattern[2] = (byte) (nameBytes.length & 0xFF);
        System.arraycopy(nameBytes, 0, pattern, 3, nameBytes.length);

        for (int i = 0; i <= data.length - pattern.length - 2; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                int offset = i + pattern.length;
                int strLen = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                if (strLen > 0 && offset + 2 + strLen <= data.length) {
                    return new String(data, offset + 2, strLen, StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private PlayerXpData readXpFromPlayerData(UUID uuid, String playerName) {
        try {
            File playerdataFile = findPlayerDataFile(uuid, playerName);
            if (playerdataFile == null) {
                return new PlayerXpData(0, 0);
            }

            byte[] data = readDecompressedNbt(playerdataFile);
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
