package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerTipManager {

    private final MineCord plugin;
    private final Map<UUID, BukkitTask> playerTasks = new ConcurrentHashMap<>();

    public PlayerTipManager(MineCord plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts tip scheduling for all currently online players.
     */
    public void start() {
        if (!isEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleNextTip(player);
        }
    }

    /**
     * Stops all active tip tasks.
     */
    public void stop() {
        for (BukkitTask task : playerTasks.values()) {
            if (task != null) {
                try {
                    task.cancel();
                } catch (Throwable ignored) {}
            }
        }
        playerTasks.clear();
    }

    /**
     * Starts scheduling tips for a player who just joined or became active.
     */
    public void startForPlayer(Player player) {
        if (!isEnabled() || player == null || !player.isOnline()) return;
        scheduleNextTip(player);
    }

    /**
     * Cancels any pending tip task for a disconnected player.
     */
    public void stopForPlayer(UUID uuid) {
        if (uuid == null) return;
        BukkitTask task = playerTasks.remove(uuid);
        if (task != null) {
            try {
                task.cancel();
            } catch (Throwable ignored) {}
        }
    }

    public boolean isEnabled() {
        if (plugin.getConfig().contains("tips.enabled")) {
            return plugin.getConfig().getBoolean("tips.enabled", true);
        }
        return plugin.getConfig().getBoolean("join-tips.enabled", true);
    }

    public List<String> getTipMessages() {
        List<String> list = plugin.getConfig().getStringList("tips.messages");
        if (list == null || list.isEmpty()) {
            list = plugin.getConfig().getStringList("join-tips.messages");
        }
        return list;
    }

    public int getMinIntervalMinutes() {
        int min = plugin.getConfig().getInt("tips.min-interval-minutes", 60);
        return Math.max(1, min);
    }

    public int getMaxIntervalMinutes() {
        int max = plugin.getConfig().getInt("tips.max-interval-minutes", 120);
        return Math.max(getMinIntervalMinutes(), max);
    }

    private void scheduleNextTip(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        // Cancel previous task if one exists
        stopForPlayer(uuid);

        if (!isEnabled()) return;

        int minMin = getMinIntervalMinutes();
        int maxMin = getMaxIntervalMinutes();

        long minTicks = (long) minMin * 60L * 20L;
        long maxTicks = (long) maxMin * 60L * 20L;

        long delayTicks;
        if (maxTicks <= minTicks) {
            delayTicks = minTicks;
        } else {
            delayTicks = ThreadLocalRandom.current().nextLong(minTicks, maxTicks + 1);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                playerTasks.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    sendRandomTip(p);
                    // Schedule next tip for this player
                    scheduleNextTip(p);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[MineCord] Помилка надсилання підказки гравцю: " + t.getMessage());
            }
        }, delayTicks);

        playerTasks.put(uuid, task);
    }

    private void sendRandomTip(Player player) {
        List<String> tips = getTipMessages();
        if (tips == null || tips.isEmpty()) return;

        String randomTip = tips.get(ThreadLocalRandom.current().nextInt(tips.size()));
        String formatted = ChatColor.translateAlternateColorCodes('&', randomTip);
        player.sendMessage(formatted);
    }
}
