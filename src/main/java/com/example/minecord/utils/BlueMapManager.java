package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class BlueMapManager {
    private final MineCord plugin;
    private int repeatingTaskId = -1;
    private int startupTaskId = -1;
    private int lastReloadMinute = -1;

    public BlueMapManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        if (!plugin.getConfig().getBoolean("bluemap.auto-reload.enabled", true)) {
            return;
        }

        // Check if BlueMap plugin is installed and active on the server
        Plugin blueMap = Bukkit.getPluginManager().getPlugin("BlueMap");
        if (blueMap == null || !blueMap.isEnabled()) {
            plugin.getLogger().info("Плагін BlueMap не знайдено на сервері. Автоматичне оновлення карти вимкнено.");
            return;
        }

        String rawCmd = plugin.getConfig().getString("bluemap.auto-reload.command", "bluemap reload");
        final String command = (rawCmd != null && rawCmd.startsWith("/")) ? rawCmd.substring(1) : (rawCmd != null ? rawCmd : "bluemap reload");

        // 1. Оновлення невдовзі після запуску сервера (наприклад, через 2 хв)
        boolean reloadOnStartup = plugin.getConfig().getBoolean("bluemap.auto-reload.reload-on-startup", true);
        long startupDelaySeconds = plugin.getConfig().getLong("bluemap.auto-reload.startup-delay-seconds", 120);
        if (reloadOnStartup) {
            startupTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                executeReload(command, "після запуску сервера");
            }, Math.max(20L, startupDelaySeconds * 20L));
        }

        // 2. Оновлення у фіксований час (наприклад, щодня о 16:00 за Києвом)
        List<String> rawTimes = plugin.getConfig().getStringList("bluemap.auto-reload.times");
        List<LocalTime> targetTimes = new ArrayList<>();
        if (rawTimes != null && !rawTimes.isEmpty()) {
            for (String t : rawTimes) {
                try {
                    String clean = t.trim();
                    if (clean.length() == 4) clean = "0" + clean;
                    targetTimes.add(LocalTime.parse(clean));
                } catch (DateTimeParseException e) {
                    plugin.getLogger().warning("Невірний формат часу в bluemap.auto-reload.times: " + t);
                }
            }
        } else {
            targetTimes.add(LocalTime.of(16, 0));
        }

        plugin.logPink(String.format("Планувальник BlueMap активовано: після старту (+%dс) та о %s (за Києвом).",
                startupDelaySeconds, targetTimes));

        // Перевірка щосекунди за київським часом
        repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            ZonedDateTime nowZoned = ZonedDateTime.now(ZoneId.of("Europe/Kyiv"));
            int currentMinute = nowZoned.getMinute();
            int currentSecond = nowZoned.getSecond();

            if (currentSecond != 0) return;
            if (currentMinute == lastReloadMinute) return;

            LocalTime nowTime = nowZoned.toLocalTime().withSecond(0).withNano(0);
            for (LocalTime target : targetTimes) {
                if (nowTime.equals(target)) {
                    lastReloadMinute = currentMinute;
                    executeReload(command, "за розкладом о " + target);
                    break;
                }
            }
        }, 20L, 20L);
    }

    private void executeReload(String command, String reason) {
        Plugin bm = Bukkit.getPluginManager().getPlugin("BlueMap");
        if (bm == null || !bm.isEnabled()) {
            plugin.getLogger().warning("Плагін BlueMap було вимкнено або видалено. Пропускаю оновлення.");
            return;
        }

        try {
            plugin.getLogger().info("Виконую оновлення BlueMap (" + reason + "): /" + command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            plugin.getLogger().warning("Помилка під час оновлення BlueMap: " + e.getMessage());
        }
    }

    public void stop() {
        if (repeatingTaskId != -1) {
            Bukkit.getScheduler().cancelTask(repeatingTaskId);
            repeatingTaskId = -1;
        }
        if (startupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(startupTaskId);
            startupTaskId = -1;
        }
        lastReloadMinute = -1;
    }
}
