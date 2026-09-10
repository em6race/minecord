package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;

public class BlueMapManager {
    private final MineCord plugin;
    private int taskId = -1;

    public BlueMapManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        if (!plugin.getConfig().getBoolean("bluemap.auto-reload.enabled", true)) {
            return;
        }

        long hours = plugin.getConfig().getLong("bluemap.auto-reload.interval-hours", 2);
        if (hours <= 0) {
            hours = 2;
        }

        // Convert hours to ticks: hours * 3600 seconds * 20 ticks
        long intervalTicks = hours * 3600L * 20L;
        String rawCmd = plugin.getConfig().getString("bluemap.auto-reload.command", "bluemap reload");
        final String command = (rawCmd != null && rawCmd.startsWith("/")) ? rawCmd.substring(1) : (rawCmd != null ? rawCmd : "bluemap reload");

        plugin.getLogger().info(String.format("Планувальник BlueMap активовано: виконання '/%s' кожні %d год.", command, hours));

        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            try {
                plugin.getLogger().info("Виконую автоматичне оновлення BlueMap: /" + command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                plugin.getLogger().warning("Помилка під час автоматичного оновлення BlueMap: " + e.getMessage());
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
