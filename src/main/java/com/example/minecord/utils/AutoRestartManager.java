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
    private boolean isPaused = false; // Змінна для паузи авторестартів
    private final Set<UUID> ignoredPlayers = new HashSet<>(); // Гравці, які вимкнули сповіщення

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
                    
                    String diffStr = String.valueOf(diff);
                    
                    // Відправка повідомлень в чат
                    if (plugin.getConfig().contains("autorestart.messages." + diffStr)) {
                        String msg = plugin.getConfig().getString("autorestart.messages." + diffStr);
                        if (msg != null && !msg.isEmpty()) broadcast(msg, diff);
                    }
                    
                    // Відправка Title та Subtitle
                    if (plugin.getConfig().contains("autorestart.titles." + diffStr) || 
                        plugin.getConfig().contains("autorestart.subtitles." + diffStr)) {
                        
                        String title = plugin.getConfig().getString("autorestart.titles." + diffStr, "");
                        String subtitle = plugin.getConfig().getString("autorestart.subtitles." + diffStr, "");
                        broadcastTitle(title, subtitle);
                    }
                    
                    // Виконання команд при досягненні 0
                    if (diff == 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            List<String> commands = plugin.getConfig().getStringList("autorestart.commands");
                            if (commands.isEmpty()) commands.add("restart"); // fallback
                            for (String cmd : commands) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }
                        });
                    }
                } catch (Exception e) {
                    // Ігноруємо помилки парсингу конкретного часу
                }
            }
        }, 10L, 10L);
    }
    
    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        // FIX: скидаємо стан, щоб після reload не пропустити секунду рестарту
        lastAnnouncedSecond = -1;
        taskId = -1;
    }

    private void broadcast(String message, long secondsLeft) {
        // У Minecraft чаті використовуємо класичний жирний червоний текст
        String formattedMessage = ChatColor.RED + "" + ChatColor.BOLD + "[Увага] " + ChatColor.RESET + ChatColor.RED + message;
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!ignoredPlayers.contains(p.getUniqueId())) {
                p.sendMessage(formattedMessage);
            }
        }
        
        // В Discord відправляємо тільки фінальне повідомлення про рестарт (коли secondsLeft == 0)
        if (secondsLeft == 0 && plugin.getBotManager() != null) {
            plugin.getBotManager().sendSystemEmbed("⏰ Планове перезавантаження сервера", 0xFFA500, null);
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
