package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoRestartManager implements CommandExecutor {
    private final MineCord plugin;
    private final List<String> restartTimes = new ArrayList<>();
    private int taskId = -1;
    private int lastAnnouncedSecond = -1;
    private boolean isPaused = false; // Flag to pause auto-restarts
    private final Set<UUID> ignoredPlayers = new HashSet<>(); // Players who opted out of notifications
    private boolean smartRestartPending = false;
    public AutoRestartManager(MineCord plugin) {
        this.plugin = plugin;
        loadTimes();
        
        if (plugin.getCommand("togglerestart") != null) {
            plugin.getCommand("togglerestart").setExecutor(this);
        }
    }

    private void loadTimes() {
        if (plugin.getConfig().contains("autorestart.times")) {
            restartTimes.addAll(plugin.getConfig().getStringList("autorestart.times"));
        }
    }

    private void saveTimes() {
        plugin.getConfig().set("autorestart.times", restartTimes);
        plugin.saveConfig();
    }

    public void start() {
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (smartRestartPending && plugin.getServer().getOnlinePlayers().isEmpty()) {
                smartRestartPending = false;
                plugin.getLogger().info("Онлайн дорівнює 0: виконую відкладений одноразовий рестарт.");
                List<String> commands = plugin.getConfig().getStringList("autorestart.commands");
                if (commands.isEmpty()) commands.add("restart");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                return;
            }

            java.time.ZonedDateTime nowZoned = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Kyiv"));
            LocalTime now = nowZoned.toLocalTime();
            String currentDay = nowZoned.getDayOfWeek().name();
            int currentSecond = now.getSecond();
            
            if (currentSecond == lastAnnouncedSecond) return;
            lastAnnouncedSecond = currentSecond;
            
            for (String timeStr : new ArrayList<>(restartTimes)) {
                try {
                    String targetDay = "DAILY";
                    String targetTimeStr = timeStr;
                    
                    if (timeStr.contains(";")) {
                        String[] parts = timeStr.split(";");
                        targetDay = parts[0].toUpperCase();
                        targetTimeStr = parts[1];
                    }
                    
                    if (!targetDay.equals("DAILY") && !targetDay.equals(currentDay)) continue;
                    
                    if (targetTimeStr.length() == 4) targetTimeStr = "0" + targetTimeStr;
                    LocalTime restartTime = LocalTime.parse(targetTimeStr);
                    
                    long diff = ChronoUnit.SECONDS.between(now.truncatedTo(ChronoUnit.SECONDS), restartTime);
                    if (diff < 0) diff += 86400;
                    
                    if (isPaused) continue;

                    // Skip scheduled restart if the server was started recently (e.g. within 15 minutes)
                    long uptimeMinutes = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 60000;
                    int minUptimeMinutes = plugin.getConfig().getInt("autorestart.min-uptime-minutes", 15);
                    if (uptimeMinutes < minUptimeMinutes) {
                        if (diff == 0) {
                            plugin.getLogger().info("Плановий рестарт о " + targetTimeStr + " пропущено: сервер працює лише " + uptimeMinutes + " хв (мінімальний необхідний аптайм: " + minUptimeMinutes + " хв).");
                        }
                        continue;
                    }
                    
                    String diffStr = String.valueOf(diff);
                    
                    // Broadcast messages to chat
                    if (plugin.getConfig().contains("autorestart.messages." + diffStr)) {
                        String msg = plugin.getConfig().getString("autorestart.messages." + diffStr);
                        if (msg != null && !msg.isEmpty()) broadcast(msg, diff);
                    }
                    
                    // Send Title and Subtitle
                    if (plugin.getConfig().contains("autorestart.titles." + diffStr) || 
                        plugin.getConfig().contains("autorestart.subtitles." + diffStr)) {
                        
                        String title = plugin.getConfig().getString("autorestart.titles." + diffStr, "");
                        String subtitle = plugin.getConfig().getString("autorestart.subtitles." + diffStr, "");
                        broadcastTitle(title, subtitle);
                    }
                    
                    // Execute commands when timer reaches 0
                    if (diff == 0) {
                        smartRestartPending = false;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            List<String> commands = plugin.getConfig().getStringList("autorestart.commands");
                            if (commands.isEmpty()) commands.add("restart");
                            for (String cmd : commands) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }
                        });
                    }
                } catch (Exception e) {
                    // Ignore parsing errors for individual time entries
                }
            }
        }, 10L, 10L);
    }
    
    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        // FIX: reset state so a reload doesn't skip restart second
        lastAnnouncedSecond = -1;
        taskId = -1;
    }

    private void broadcast(String message, long secondsLeft) {
        // In Minecraft chat, use classic bold red text
        String formattedMessage = ChatColor.RED + "" + ChatColor.BOLD + "[Увага] " + ChatColor.RESET + ChatColor.RED + message;
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!ignoredPlayers.contains(p.getUniqueId())) {
                p.sendMessage(formattedMessage);
            }
        }
        
        // Send scheduled restart notification to Discord
        if (plugin.getBotManager() != null) {
            if (secondsLeft == 300) {
                plugin.getBotManager().sendSystemEmbed("⚠️ Планове перезавантаження сервера через 5 хвилин!", 0xFFA500, null);
            } else if (secondsLeft == 60) {
                plugin.getBotManager().sendSystemEmbed("⚠️ Планове перезавантаження сервера через 1 хвилину!", 0xFFA500, null);
            } else if (secondsLeft == 0) {
                plugin.getBotManager().sendSystemEmbedSync("⏱ Планове перезавантаження сервера почалося!", 0xFFA500, null);
            }
        }
    }

    private void broadcastTitle(String title, String subtitle) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!ignoredPlayers.contains(p.getUniqueId())) {
                p.sendTitle(title, subtitle, 5, 20, 5); // fadeIn, stay, fadeOut
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Цю команду можуть використовувати лише гравці!");
            return true;
        }
        Player p = (Player) sender;
        if (ignoredPlayers.contains(p.getUniqueId())) {
            ignoredPlayers.remove(p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + "✅ Ви увімкнули попередження про авторестарт сервера.");
        } else {
            ignoredPlayers.add(p.getUniqueId());
            p.sendMessage(ChatColor.RED + "❌ Ви вимкнули попередження про авторестарт сервера.");
        }
        return true;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean toggleSmartRestart() {
        smartRestartPending = !smartRestartPending;
        return smartRestartPending;
    }

    public void addTime(String time) {
        if (!restartTimes.contains(time)) {
            restartTimes.add(time);
            saveTimes();
        }
    }

    public boolean removeTime(String time) {
        if (restartTimes.remove(time)) {
            saveTimes();
            return true;
        }
        return false;
    }

    public void clearTimes() {
        restartTimes.clear();
        saveTimes();
    }

    public List<String> getTimes() {
        return new ArrayList<>(restartTimes);
    }
}
