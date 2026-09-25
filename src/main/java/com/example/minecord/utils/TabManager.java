package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.entity.Player;

public class TabManager {
    private final MineCord plugin;
    private int taskId = -1;
    private final long startTime;

    public TabManager(MineCord plugin) {
        this.plugin = plugin;
        this.startTime = System.currentTimeMillis();
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("tablist.enabled", true)) return;

        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            String tpsStr = getTPSString();
            String uptimeStr = getUptimeString();
            
            String header = plugin.getConfig().getString("tablist.header");
            if (header == null || header.trim().isEmpty()) {
                header = (plugin.getLanguageManager() != null)
                        ? plugin.getLanguageManager().get("tablist.default-header")
                        : "&b&lMineCord Server\n&7Welcome to the server!\n";
            }
            header = org.bukkit.ChatColor.translateAlternateColorCodes('&', header);
            
            String footerTemplate = plugin.getConfig().getString("tablist.footer");
            if (footerTemplate == null || footerTemplate.trim().isEmpty()) {
                footerTemplate = (plugin.getLanguageManager() != null)
                        ? plugin.getLanguageManager().get("tablist.default-footer")
                        : "\n&7TPS: %tps% &8| &7Uptime: &e%uptime% &8| &7Ping: %ping%ms";
            }
            footerTemplate = org.bukkit.ChatColor.translateAlternateColorCodes('&', footerTemplate);
            String baseFooter = footerTemplate
                    .replace("%tps%", tpsStr)
                    .replace("%uptime%", uptimeStr);

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                int ping = player.getPing();
                String pingColor = "§a";
                if (ping > 80) pingColor = "§e";
                if (ping > 150) pingColor = "§c";
                
                String playerFooter = baseFooter.replace("%ping%", pingColor + ping);
                player.setPlayerListHeaderFooter(header, playerFooter);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private String getTPSString() {
        try {
            double[] tps = plugin.getServer().getTPS();
            double currentTps = tps[0];
            String color = "§a";
            if (currentTps < 18.0) color = "§e";
            if (currentTps < 15.0) color = "§c";
            return color + String.format("%.1f", Math.min(20.0, currentTps)).replace(",", ".");
        } catch (NoSuchMethodError e) {
            return "20.0";
        }
    }
    
    private String getUptimeString() {
        long diff = System.currentTimeMillis() - startTime;
        long seconds = diff / 1000 % 60;
        long minutes = diff / (60 * 1000) % 60;
        long hours = diff / (60 * 60 * 1000) % 24;
        long days = diff / (24 * 60 * 60 * 1000);
        
        LanguageManager lm = plugin.getLanguageManager();
        String dayUnit = lm != null ? lm.getRaw("tablist.units.days") : "d ";
        String hourUnit = lm != null ? lm.getRaw("tablist.units.hours") : "h ";
        String minUnit = lm != null ? lm.getRaw("tablist.units.minutes") : "m ";
        String secUnit = lm != null ? lm.getRaw("tablist.units.seconds") : "s";

        if (dayUnit == null || dayUnit.equals("tablist.units.days")) dayUnit = "d ";
        if (hourUnit == null || hourUnit.equals("tablist.units.hours")) hourUnit = "h ";
        if (minUnit == null || minUnit.equals("tablist.units.minutes")) minUnit = "m ";
        if (secUnit == null || secUnit.equals("tablist.units.seconds")) secUnit = "s";

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(dayUnit);
        if (hours > 0) sb.append(hours).append(hourUnit);
        if (minutes > 0) sb.append(minutes).append(minUnit);
        sb.append(seconds).append(secUnit);
        return sb.toString();
    }
}
