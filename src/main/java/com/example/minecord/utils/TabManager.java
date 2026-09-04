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
            
            String header = plugin.getConfig().getString("tablist.header", "§d§lKozloMine\n§7Ласкаво просимо!\n");
            header = org.bukkit.ChatColor.translateAlternateColorCodes('&', header);
            
            String footerTemplate = plugin.getConfig().getString("tablist.footer", "\n§7ТПС: %tps% §8| §7Аптайм: §e%uptime% §8| §7Пінг: %ping%мс");
            String baseFooter = org.bukkit.ChatColor.translateAlternateColorCodes('&', footerTemplate)
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
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("г ");
        if (minutes > 0) sb.append(minutes).append("хв ");
        sb.append(seconds).append("с");
        return sb.toString();
    }
}
