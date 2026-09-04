package com.example.minecord.utils;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;

public class PerformanceMonitor {
    private final MineCord plugin;
    private int taskId = -1;
    private long lastAlertTime = 0;

    public PerformanceMonitor(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("technical.performance-monitor.enabled", true)) {
            return;
        }

        double tpsThreshold = plugin.getConfig().getDouble("technical.performance-monitor.tps-threshold", 15.0);
        double ramThresholdPercent = plugin.getConfig().getDouble("technical.performance-monitor.ram-threshold-percent", 95.0);
        int cooldownMinutes = plugin.getConfig().getInt("technical.performance-monitor.cooldown-minutes", 5);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            double currentTps = 20.0;
            try {
                currentTps = Bukkit.getServer().getTPS()[0];
            } catch (Exception e) {
                // Fallback
            }

            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double ramPercent = ((double) usedMemory / maxMemory) * 100.0;

            boolean tpsCritical = currentTps < tpsThreshold;
            boolean ramCritical = ramPercent > ramThresholdPercent;

            if (tpsCritical || ramCritical) {
                long now = System.currentTimeMillis();
                if (now - lastAlertTime > cooldownMinutes * 60 * 1000L) {
                    lastAlertTime = now;
                    sendAlert(currentTps, ramPercent, tpsCritical, ramCritical);

                    if (tpsCritical && plugin.getConfig().getBoolean("technical.performance-monitor.auto-spark", false)) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spark profiler start");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spark profiler stop");
                        }, 20 * 60L);
                    }
                }
            }
        }, 20 * 20L, 20 * 20L);
    }

    private void sendAlert(double tps, double ramPercent, boolean tpsCritical, boolean ramCritical) {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null && consoleChannelId != null) {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(consoleChannelId);
            if (channel != null) {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setTitle("🔥 Критичне навантаження на сервер!");
                
                StringBuilder desc = new StringBuilder();
                if (tpsCritical) {
                    desc.append("**TPS впав до:** ").append(String.format("%.1f", tps)).append(" (Критично)\n");
                } else {
                    desc.append("**TPS:** ").append(String.format("%.1f", tps)).append("\n");
                }
                
                if (ramCritical) {
                    desc.append("**Оперативна пам'ять:** ").append(String.format("%.1f%%", ramPercent)).append(" (Критично)\n");
                } else {
                    desc.append("**Оперативна пам'ять:** ").append(String.format("%.1f%%", ramPercent)).append("\n");
                }
                
                embed.setDescription(desc.toString());
                embed.setColor(0xFF0000);
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
