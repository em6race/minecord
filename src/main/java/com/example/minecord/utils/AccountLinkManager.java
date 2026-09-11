package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AccountLinkManager {
    private final MineCord plugin;
    // FIX: Changed HashMap → ConcurrentHashMap since access occurs
    // from multiple threads (Discord callbacks, AsyncPlayerPreLoginEvent, etc.)
    private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> linkedAccounts = new ConcurrentHashMap<>();
    // FIX: Store code creation time for automatic expiration after 10 mins
    private final Map<String, Long> codeExpiry = new ConcurrentHashMap<>();
    private static final long CODE_TTL_MS = 10 * 60 * 1000L; // 10 minutes

    private File linksFile;
    private FileConfiguration linksConfig;

    public AccountLinkManager(MineCord plugin) {
        this.plugin = plugin;
        loadLinks();
    }

    // Load saved links from links.yml
    private void loadLinks() {
        linksFile = new File(plugin.getDataFolder(), "links.yml");
        if (!linksFile.exists()) {
            try {
                linksFile.getParentFile().mkdirs(); // FIX: ensure parent directory exists
                linksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не вдалося створити links.yml: " + e.getMessage());
            }
        }
        linksConfig = YamlConfiguration.loadConfiguration(linksFile);

        if (linksConfig.contains("links")) {
            for (String uuidStr : linksConfig.getConfigurationSection("links").getKeys(false)) {
                try {
                    linkedAccounts.put(UUID.fromString(uuidStr), linksConfig.getString("links." + uuidStr));
                } catch (IllegalArgumentException ignored) {
                    // FIX: Ignore corrupted records with invalid UUID
                }
            }
        }
    }

    // Save links to file
    public void saveLinks() {
        linksConfig.set("links", null); // Clear old section
        for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
            linksConfig.set("links." + entry.getKey().toString(), entry.getValue());
        }
        try {
            linksConfig.save(linksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не вдалося зберегти links.yml: " + e.getMessage());
        }
    }

    // Generate a 4-digit code for player
    public String generateCode(UUID playerUUID) {
        // Remove old player code if requested again
        pendingCodes.values().remove(playerUUID);

        // FIX: Clean up expired codes concurrently
        long now = System.currentTimeMillis();
        codeExpiry.entrySet().removeIf(e -> now > e.getValue());
        codeExpiry.forEach((code, expiry) -> {
            if (now > expiry) pendingCodes.remove(code);
        });

        String code = String.format("%04d", new Random().nextInt(10000));
        pendingCodes.put(code, playerUUID);
        codeExpiry.put(code, now + CODE_TTL_MS);
        return code;
    }

    public UUID getUUIDFromCode(String code) {
        // FIX: Verify code is not expired
        Long expiry = codeExpiry.get(code);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            pendingCodes.remove(code);
            codeExpiry.remove(code);
            return null;
        }
        return pendingCodes.get(code);
    }

    // Link account
    public void linkAccount(String code, String discordId) {
        UUID uuid = pendingCodes.remove(code);
        codeExpiry.remove(code);
        if (uuid != null && discordId != null) {
            linkedAccounts.entrySet().removeIf(entry -> discordId.equals(entry.getValue()) && !entry.getKey().equals(uuid));
            linkedAccounts.put(uuid, discordId);
            saveLinks();
        }
    }

    // Direct account linking
    public void linkAccountDirectly(UUID uuid, String discordId) {
        if (uuid == null || discordId == null) return;
        linkedAccounts.entrySet().removeIf(entry -> discordId.equals(entry.getValue()) && !entry.getKey().equals(uuid));
        linkedAccounts.put(uuid, discordId);
        saveLinks();
    }

    public String getDiscordId(UUID uuid) {
        return linkedAccounts.get(uuid);
    }

    public boolean isLinked(UUID uuid) {
        return linkedAccounts.containsKey(uuid);
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
