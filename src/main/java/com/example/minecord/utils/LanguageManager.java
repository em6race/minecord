package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages plugin localization across English, Ukrainian, and Slovak,
 * supporting both global server language (for Discord/console) and per-player client language in-game.
 */
public class LanguageManager {
    private final MineCord plugin;
    private String currentLang = "en";
    private boolean perPlayerLanguage = true;
    private final Map<String, FileConfiguration> langConfigs = new HashMap<>();
    private FileConfiguration langConfig;
    private FileConfiguration fallbackConfig;

    private static final String[] SUPPORTED_LANGUAGES = {"en", "uk", "sk"};

    public LanguageManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaultLanguageFiles();

        currentLang = plugin.getConfig().getString("language", "en").toLowerCase().trim();
        if (!isSupported(currentLang)) {
            plugin.getLogger().warning("Unknown language '" + currentLang + "' specified in config.yml! Falling back to 'en'.");
            currentLang = "en";
        }
        perPlayerLanguage = plugin.getConfig().getBoolean("per-player-language", true);

        langConfigs.clear();
        for (String lang : SUPPORTED_LANGUAGES) {
            File langFile = new File(plugin.getDataFolder(), "languages/" + lang + ".yml");
            InputStream bundledStream = plugin.getResource("languages/" + lang + ".yml");
            FileConfiguration bundledConfig = bundledStream != null
                    ? YamlConfiguration.loadConfiguration(new InputStreamReader(bundledStream, StandardCharsets.UTF_8))
                    : null;

            FileConfiguration cfg;
            if (langFile.exists()) {
                cfg = YamlConfiguration.loadConfiguration(langFile);
                if (bundledConfig != null) {
                    cfg.setDefaults(bundledConfig);
                    cfg.options().copyDefaults(true);
                    try {
                        cfg.save(langFile);
                    } catch (Throwable ignored) {}
                }
            } else if (bundledConfig != null) {
                cfg = bundledConfig;
            } else {
                cfg = new YamlConfiguration();
            }
            langConfigs.put(lang, cfg);
        }

        fallbackConfig = langConfigs.getOrDefault("en", new YamlConfiguration());
        langConfig = langConfigs.getOrDefault(currentLang, fallbackConfig);

        plugin.logPink("Localization loaded: [" + currentLang.toUpperCase() + "] (per-player client language: " + (perPlayerLanguage ? "ON" : "OFF") + ")");
    }

    private void saveDefaultLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (String lang : SUPPORTED_LANGUAGES) {
            File target = new File(langDir, lang + ".yml");
            if (!target.exists()) {
                try {
                    plugin.saveResource("languages/" + lang + ".yml", false);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Could not extract default language file languages/" + lang + ".yml: " + t.getMessage());
                }
            }
        }
    }

    public boolean isSupported(String lang) {
        if (lang == null) return false;
        for (String s : SUPPORTED_LANGUAGES) {
            if (s.equalsIgnoreCase(lang)) return true;
        }
        return false;
    }

    /**
     * Resolves the language code ("en", "uk", "sk") for a specific player or sender.
     * If per-player-language is enabled and sender is a Player, reads player.getLocale() (e.g. "uk_ua" -> "uk").
     * If the player's client language is not in the supported list, defaults to "en" (English).
     */
    public String resolvePlayerLang(CommandSender sender) {
        if (perPlayerLanguage && sender instanceof Player player) {
            try {
                String locale = player.getLocale();
                if (locale != null && !locale.isEmpty()) {
                    String lower = locale.toLowerCase().trim();
                    String prefix = lower.contains("_") ? lower.substring(0, lower.indexOf('_')) : lower;
                    if (prefix.equals("uk")) return "uk";
                    if (prefix.equals("sk") || prefix.equals("cs")) return "sk";
                    return "en";
                }
            } catch (Throwable ignored) {}
            return "en";
        }
        return currentLang;
    }

    public String getRawByLang(String lang, String key, Object... args) {
        FileConfiguration targetConfig = (lang != null) ? langConfigs.get(lang.toLowerCase()) : langConfig;
        if (targetConfig == null) {
            targetConfig = langConfig;
        }

        String msg = null;
        if (targetConfig != null) {
            msg = targetConfig.getString(key);
        }
        if (msg == null && fallbackConfig != null) {
            msg = fallbackConfig.getString(key);
        }
        if (msg == null) {
            return key;
        }

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String val = String.valueOf(args[i]);
                msg = msg.replace("{" + i + "}", val);
            }
        }
        return msg;
    }

    public String getByLang(String lang, String key, Object... args) {
        String raw = getRawByLang(lang, key, args);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getRaw(CommandSender sender, String key, Object... args) {
        return getRawByLang(resolvePlayerLang(sender), key, args);
    }

    public String get(CommandSender sender, String key, Object... args) {
        return getByLang(resolvePlayerLang(sender), key, args);
    }

    public String getRaw(String key, Object... args) {
        return getRawByLang(currentLang, key, args);
    }

    public String get(String key, Object... args) {
        return getByLang(currentLang, key, args);
    }

    public List<String> getList(String key) {
        List<String> list = null;
        if (langConfig != null) {
            list = langConfig.getStringList(key);
        }
        if ((list == null || list.isEmpty()) && fallbackConfig != null) {
            list = fallbackConfig.getStringList(key);
        }
        if (list == null) return new ArrayList<>();

        List<String> colored = new ArrayList<>(list.size());
        for (String s : list) {
            colored.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return colored;
    }

    public String getLanguage() {
        return currentLang;
    }

    public boolean isPerPlayerLanguage() {
        return perPlayerLanguage;
    }

    public boolean isEnglish() {
        return "en".equalsIgnoreCase(currentLang);
    }

    public boolean isUkrainian() {
        return "uk".equalsIgnoreCase(currentLang);
    }

    public boolean isSlovak() {
        return "sk".equalsIgnoreCase(currentLang);
    }
}
