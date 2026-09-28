package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages plugin localization across English, Ukrainian, and Slovak.
 */
public class LanguageManager {
    private final MineCord plugin;
    private String currentLang = "en";
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

        File langFile = new File(plugin.getDataFolder(), "languages/" + currentLang + ".yml");
        InputStream bundledLangStream = plugin.getResource("languages/" + currentLang + ".yml");
        FileConfiguration bundledLangConfig = bundledLangStream != null
                ? YamlConfiguration.loadConfiguration(new InputStreamReader(bundledLangStream, StandardCharsets.UTF_8))
                : null;

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
            if (bundledLangConfig != null) {
                langConfig.setDefaults(bundledLangConfig);
                langConfig.options().copyDefaults(true);
                try {
                    langConfig.save(langFile);
                } catch (Throwable ignored) {}
            }
        } else if (bundledLangConfig != null) {
            langConfig = bundledLangConfig;
        } else {
            langConfig = new YamlConfiguration();
        }

        // Always keep 'en' as fallback
        File fallbackFile = new File(plugin.getDataFolder(), "languages/en.yml");
        InputStream bundledEnStream = plugin.getResource("languages/en.yml");
        FileConfiguration bundledEnConfig = bundledEnStream != null
                ? YamlConfiguration.loadConfiguration(new InputStreamReader(bundledEnStream, StandardCharsets.UTF_8))
                : null;

        if (fallbackFile.exists()) {
            fallbackConfig = YamlConfiguration.loadConfiguration(fallbackFile);
            if (bundledEnConfig != null) {
                fallbackConfig.setDefaults(bundledEnConfig);
                fallbackConfig.options().copyDefaults(true);
            }
        } else if (bundledEnConfig != null) {
            fallbackConfig = bundledEnConfig;
        } else {
            fallbackConfig = new YamlConfiguration();
        }

        plugin.logPink("Localization loaded: [" + currentLang.toUpperCase() + "]");
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

    public String getRaw(String key, Object... args) {
        String msg = null;
        if (langConfig != null) {
            msg = langConfig.getString(key);
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

    public String get(String key, Object... args) {
        String raw = getRaw(key, args);
        return ChatColor.translateAlternateColorCodes('&', raw);
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
