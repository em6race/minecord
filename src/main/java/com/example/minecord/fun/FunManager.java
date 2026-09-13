package com.example.minecord.fun;

import com.example.minecord.MineCord;

import java.util.*;

public class FunManager {

    private final MineCord plugin;
    private final Map<String, FunMode> registeredModes = new LinkedHashMap<>();
    private boolean funEnabled = true;

    public FunManager(MineCord plugin) {
        this.plugin = plugin;
        registerMode(new RiptideMode(plugin));
        registerMode(new SpearHungerMode(plugin));
        registerMode(new SmartAnvilMode(plugin));
    }

    /**
     * Реєстрація нового розважального режиму.
     */
    public void registerMode(FunMode mode) {
        if (mode == null) return;
        registeredModes.put(mode.getId().toLowerCase(Locale.ROOT), mode);
    }

    public FunMode getMode(String id) {
        if (id == null) return null;
        return registeredModes.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<FunMode> getAllModes() {
        return Collections.unmodifiableCollection(registeredModes.values());
    }

    public boolean isFunEnabled() {
        return funEnabled;
    }

    public void initialize() {
        loadConfig();
        for (FunMode mode : registeredModes.values()) {
            try {
                if (funEnabled && mode.isEnabled()) {
                    mode.onEnable();
                    plugin.getLogger().info("[FunManager] Режим '" + mode.getName() + "' активовано.");
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("[FunManager] Не вдалося активувати режим '" + mode.getName() + "': " + t.getMessage());
            }
        }
    }

    public void reload() {
        loadConfig();
        for (FunMode mode : registeredModes.values()) {
            try {
                mode.onReload();
                if (funEnabled && mode.isEnabled()) {
                    mode.onEnable();
                } else {
                    mode.onDisable();
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("[FunManager] Помилка перезавантаження режиму '" + mode.getName() + "': " + t.getMessage());
            }
        }
    }

    public void shutdown() {
        for (FunMode mode : registeredModes.values()) {
            try {
                mode.onDisable();
            } catch (Throwable t) {
                plugin.getLogger().severe("[FunManager] Помилка вимкнення режиму '" + mode.getName() + "': " + t.getMessage());
            }
        }
        registeredModes.clear();
    }

    private void loadConfig() {
        funEnabled = plugin.getConfig().getBoolean("fun.enabled", true);
    }
}