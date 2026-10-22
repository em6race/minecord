package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AccountLinkManager {
    private final MineCord plugin;
    // ConcurrentHashMap since access occurs from multiple threads (Discord callbacks, AsyncPlayerPreLoginEvent, etc.)
    private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> linkedAccounts = new ConcurrentHashMap<>();
    private final Set<UUID> adminLinkedAccounts = ConcurrentHashMap.newKeySet();
    // Store code creation time for automatic expiration after 10 mins
    private final Map<String, Long> codeExpiry = new ConcurrentHashMap<>();
    private static final long CODE_TTL_MS = 10 * 60 * 1000L; // 10 minutes

    private final Object saveLock = new Object();
    private File linksFile;
    private FileConfiguration linksConfig;

    public AccountLinkManager(MineCord plugin) {
        this.plugin = plugin;
        loadLinks();
    }

    public void reload() {
        synchronized (saveLock) {
            linkedAccounts.clear();
            adminLinkedAccounts.clear();
            loadLinks();
        }
    }

    // Load saved links from links.yml
    private void loadLinks() {
        synchronized (saveLock) {
            linksFile = new File(plugin.getDataFolder(), "links.yml");
            if (!linksFile.exists()) {
                try {
                    if (linksFile.getParentFile() != null) {
                        linksFile.getParentFile().mkdirs();
                    }
                    linksFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Не вдалося створити links.yml: " + e.getMessage());
                }
            }
            linksConfig = YamlConfiguration.loadConfiguration(linksFile);

            ConfigurationSection linksSection = linksConfig.getConfigurationSection("links");
            if (linksSection != null) {
                for (String uuidStr : linksSection.getKeys(false)) {
                    try {
                        linkedAccounts.put(UUID.fromString(uuidStr), linksConfig.getString("links." + uuidStr));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore corrupted records with invalid UUID
                    }
                }
            }

            if (linksConfig.contains("admin-links")) {
                for (String uuidStr : linksConfig.getStringList("admin-links")) {
                    try {
                        adminLinkedAccounts.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    // Save links to file (thread-safe)
    public void saveLinks() {
        synchronized (saveLock) {
            linksConfig.set("links", null); // Clear old section
            for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
                linksConfig.set("links." + entry.getKey().toString(), entry.getValue());
            }
            List<String> adminList = new ArrayList<>();
            for (UUID u : adminLinkedAccounts) {
                adminList.add(u.toString());
            }
            linksConfig.set("admin-links", adminList);
            try {
                linksConfig.save(linksFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Не вдалося зберегти links.yml: " + e.getMessage());
            }
        }
    }

    // Generate a 4-digit code for player
    public String generateCode(UUID playerUUID) {
        // Remove old player code if requested again from both maps
        pendingCodes.entrySet().removeIf(entry -> {
            if (playerUUID.equals(entry.getValue())) {
                codeExpiry.remove(entry.getKey());
                return true;
            }
            return false;
        });

        // Clean up expired codes atomically from both maps
        long now = System.currentTimeMillis();
        codeExpiry.entrySet().removeIf(e -> {
            if (now > e.getValue()) {
                pendingCodes.remove(e.getKey());
                return true;
            }
            return false;
        });

        String code;
        int attempts = 0;
        do {
            code = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            attempts++;
        } while (pendingCodes.containsKey(code) && attempts < 25);

        pendingCodes.put(code, playerUUID);
        codeExpiry.put(code, now + CODE_TTL_MS);
        return code;
    }

    public UUID getUUIDFromCode(String code) {
        if (code == null) return null;
        // Verify code is not expired
        Long expiry = codeExpiry.get(code);
        if (expiry == null || System.currentTimeMillis() > expiry) {
            pendingCodes.remove(code);
            codeExpiry.remove(code);
            return null;
        }
        return pendingCodes.get(code);
    }

    // Link account
    public boolean linkAccount(String code, String discordId) {
        UUID uuid = getUUIDFromCode(code);
        if (uuid == null || discordId == null) return false;
        if (adminLinkedAccounts.contains(uuid)) {
            return false;
        }
        pendingCodes.remove(code);
        codeExpiry.remove(code);
        linkedAccounts.entrySet().removeIf(entry -> discordId.equals(entry.getValue()) && !entry.getKey().equals(uuid));
        linkedAccounts.put(uuid, discordId);
        saveLinks();
        return true;
    }

    // Direct account linking
    public void linkAccountDirectly(UUID uuid, String discordId) {
        if (uuid == null || discordId == null) return;
        linkedAccounts.entrySet().removeIf(entry -> discordId.equals(entry.getValue()) && !entry.getKey().equals(uuid));
        linkedAccounts.put(uuid, discordId);
        adminLinkedAccounts.add(uuid);
        saveLinks();
    }

    public boolean isAdminLinked(UUID uuid) {
        return uuid != null && adminLinkedAccounts.contains(uuid);
    }

    public boolean isLinkedTo(UUID uuid, String discordId) {
        if (uuid == null || discordId == null) return false;
        return discordId.equals(linkedAccounts.get(uuid));
    }

    public Map<UUID, String> getAllLinks() {
        return java.util.Collections.unmodifiableMap(linkedAccounts);
    }

    public String getDiscordId(UUID uuid) {
        return linkedAccounts.get(uuid);
    }

    public boolean isLinked(UUID uuid) {
        return linkedAccounts.containsKey(uuid);
    }

    public boolean unlinkAccount(UUID uuid) {
        if (uuid == null) return false;
        if (adminLinkedAccounts.contains(uuid)) {
            return false;
        }
        boolean removed = linkedAccounts.remove(uuid) != null;
        if (removed) {
            saveLinks();
        }
        return removed;
    }

    public boolean unlinkAccountForced(UUID uuid) {
        if (uuid == null) return false;
        adminLinkedAccounts.remove(uuid);
        boolean removed = linkedAccounts.remove(uuid) != null;
        saveLinks();
        return removed;
    }

    public UUID getUUIDFromDiscordId(String discordId) {
        if (discordId == null) return null;
        for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
            if (discordId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<UUID> getAllUUIDsFromDiscordId(String discordId) {
        List<UUID> list = new ArrayList<>();
        if (discordId == null) return list;
        for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
            if (discordId.equals(entry.getValue())) {
                list.add(entry.getKey());
            }
        }
        return list;
    }
}
