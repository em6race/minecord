package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class AccountLinkManager {
    private final MineCord plugin;
    // FIX: Змінено HashMap → ConcurrentHashMap, оскільки доступ відбувається
    // з декількох потоків (Discord callbacks, AsyncPlayerPreLoginEvent тощо)
    private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> linkedAccounts = new ConcurrentHashMap<>();
    // FIX: Зберігаємо час генерації коду для автоматичного прострочення через 10 хв
    private final Map<String, Long> codeExpiry = new ConcurrentHashMap<>();
    private static final long CODE_TTL_MS = 10 * 60 * 1000L; // 10 хвилин

    private File linksFile;
    private FileConfiguration linksConfig;

    public AccountLinkManager(MineCord plugin) {
        this.plugin = plugin;
        loadLinks();
    }

    // Завантаження збережених зв'язків з файлу links.yml
    private void loadLinks() {
        linksFile = new File(plugin.getDataFolder(), "links.yml");
        if (!linksFile.exists()) {
            try {
                linksFile.getParentFile().mkdirs(); // FIX: переконуємось що тека існує
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
                    // FIX: Ігноруємо пошкоджені записи з невалідним UUID
                }
            }
        }
    }

    // Збереження зв'язків у файл
    public void saveLinks() {
        linksConfig.set("links", null); // Очищуємо стару секцію
        for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
            linksConfig.set("links." + entry.getKey().toString(), entry.getValue());
        }
        try {
            linksConfig.save(linksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не вдалося зберегти links.yml: " + e.getMessage());
        }
    }

    // Генерація 4-значного коду для гравця
    public String generateCode(UUID playerUUID) {
        // Видаляємо старий код гравця, якщо він робить запит повторно
        pendingCodes.values().remove(playerUUID);

        // FIX: Очищуємо прострочені коди попутно
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
        // FIX: Перевіряємо чи код не прострочений
        Long expiry = codeExpiry.get(code);
        if (expiry != null && System.currentTimeMillis() > expiry) {
            pendingCodes.remove(code);
            codeExpiry.remove(code);
            return null;
        }
        return pendingCodes.get(code);
    }

    // Прив'язка акаунта
    public void linkAccount(String code, String discordId) {
        UUID uuid = pendingCodes.remove(code);
        codeExpiry.remove(code);
        if (uuid != null) {
            linkedAccounts.put(uuid, discordId);
            saveLinks();
        }
    }

    // Пряма прив'язка акаунта
    public void linkAccountDirectly(UUID uuid, String discordId) {
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
        for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
            if (entry.getValue().equals(discordId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
