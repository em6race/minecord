package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkManager implements Listener {

    private final MineCord plugin;
    private final Map<UUID, AfkData> afkDataMap = new HashMap<>();
    private int taskId = -1;
    private final long AFK_TIMEOUT = 60_000; // 1 хвилина

    public AfkManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long now = System.currentTimeMillis();
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                AfkData data = afkDataMap.computeIfAbsent(player.getUniqueId(), k -> new AfkData(player.getLocation(), now));
                
                Location currentLoc = player.getLocation();
                Location lastLoc = data.lastLocation;
                
                boolean moved = currentLoc.getX() != lastLoc.getX() || 
                                currentLoc.getY() != lastLoc.getY() || 
                                currentLoc.getZ() != lastLoc.getZ() ||
                                currentLoc.getYaw() != lastLoc.getYaw() ||
                                currentLoc.getPitch() != lastLoc.getPitch();
                
                if (moved) {
                    data.lastLocation = currentLoc;
                    data.lastActivityTime = now;
                    if (data.isAfk) {
                        setAfk(player, data, false, now);
                    }
                } else {
                    if (!data.isAfk && now - data.lastActivityTime >= AFK_TIMEOUT) {
                        setAfk(player, data, true, now);
                    }
                }
                
                if (data.isAfk && data.display != null && data.display.isValid()) {
                    long afkSeconds = (now - data.afkStartTime) / 1000;
                    String timeString = formatTime(afkSeconds);
                    data.display.setText(ChatColor.GRAY + "АФК: " + ChatColor.YELLOW + timeString);
                }
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (AfkData data : afkDataMap.values()) {
            if (data.display != null && data.display.isValid()) {
                data.display.remove();
            }
        }
        afkDataMap.clear();
    }

    private void setAfk(Player player, AfkData data, boolean afk, long now) {
        data.isAfk = afk;
        if (afk) {
            data.afkStartTime = now;
            TextDisplay display = player.getWorld().spawn(player.getLocation().add(0, 2.3, 0), TextDisplay.class, entity -> {
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setText(ChatColor.GRAY + "АФК: " + ChatColor.YELLOW + "0с");
                entity.setDefaultBackground(true);
                entity.setSeeThrough(false);
                entity.setTransformation(new Transformation(
                        new Vector3f(0, 0.8f, 0),
                        new Quaternionf(),
                        new Vector3f(1.1f, 1.1f, 1.1f),
                        new Quaternionf()
                ));
            });
            player.addPassenger(display);
            data.display = display;
            
            player.sendMessage(ChatColor.GRAY + "Ви перейшли в режим АФК.");
        } else {
            if (data.display != null && data.display.isValid()) {
                data.display.remove();
                data.display = null;
            }
            player.sendMessage(ChatColor.GRAY + "Ви вийшли з режиму АФК.");
        }
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("г ");
        if (m > 0 || h > 0) sb.append(m).append("хв ");
        sb.append(s).append("с");
        
        return sb.toString().trim();
    }

    public void updateActivity(Player player) {
        AfkData data = afkDataMap.get(player.getUniqueId());
        if (data != null) {
            data.lastActivityTime = System.currentTimeMillis();
            if (data.isAfk) {
                setAfk(player, data, false, System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> updateActivity(event.getPlayer()));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AfkData data = afkDataMap.remove(event.getPlayer().getUniqueId());
        if (data != null && data.display != null && data.display.isValid()) {
            data.display.remove();
        }
    }

    private static class AfkData {
        Location lastLocation;
        long lastActivityTime;
        boolean isAfk = false;
        long afkStartTime = 0;
        TextDisplay display = null;

        public AfkData(Location lastLocation, long lastActivityTime) {
            this.lastLocation = lastLocation;
            this.lastActivityTime = lastActivityTime;
        }
    }
}
