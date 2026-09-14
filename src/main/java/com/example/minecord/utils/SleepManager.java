package com.example.minecord.utils;

import com.example.minecord.MineCord;
import com.example.minecord.fun.BloodmoonMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.stream.Collectors;

public class SleepManager implements Listener {
    private final MineCord plugin;
    private int checkTaskId = -1;
    private long lastNightSkipTime = 0;
    
    public SleepManager(MineCord plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Disable vanilla sleep skipping by setting the gamerule very high so our custom system has full control
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setGameRule(org.bukkit.GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);
            }
        }

        // Repeating task: check every second if anyone is sleeping to handle AFK transitions or time changes smoothly
        checkTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    boolean hasSleeping = world.getPlayers().stream().anyMatch(Player::isSleeping);
                    if (hasSleeping) {
                        checkSleep(world, null);
                    }
                }
            }
        }, 20L, 20L);
    }
    
    public void stop() {
        HandlerList.unregisterAll(this);
        if (checkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(checkTaskId);
            checkTaskId = -1;
        }
    }

    private boolean isBloodmoonPreventingSleep(World world) {
        if (plugin.getFunManager() == null) return false;
        BloodmoonMode bm = (BloodmoonMode) plugin.getFunManager().getMode("bloodmoon");
        if (bm == null) return false;

        // Check dusk roll in case Bloodmoon should start tonight
        bm.checkDuskRoll(world);

        return bm.isBloodmoonActive() && (bm.getActiveWorld() == null || bm.getActiveWorld().equals(world));
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        
        World world = event.getPlayer().getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        if (isBloodmoonPreventingSleep(world)) {
            event.setCancelled(true);
            event.getPlayer().wakeup(false);
            return;
        }
        
        // Wait 10 ticks (half a second) to ensure the player is registered as "sleeping" by Bukkit
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkSleep(world, event.getPlayer()), 10L);
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        World world = event.getPlayer().getWorld();
        if (world.getEnvironment() == World.Environment.NORMAL) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (world.getPlayers().stream().anyMatch(Player::isSleeping)) {
                    checkSleep(world, null);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    if (world.getPlayers().stream().anyMatch(Player::isSleeping)) {
                        checkSleep(world, null);
                    }
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        World fromWorld = event.getFrom();
        if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (fromWorld.getPlayers().stream().anyMatch(Player::isSleeping)) {
                    checkSleep(fromWorld, null);
                }
            }, 5L);
        }
    }

    /**
     * Called whenever a player enters or exits AFK status so sleep voting updates immediately.
     */
    public void onAfkStateChanged(Player player) {
        if (player == null) return;
        World world = player.getWorld();
        if (world != null && world.getEnvironment() == World.Environment.NORMAL) {
            if (world.getPlayers().stream().anyMatch(Player::isSleeping)) {
                checkSleep(world, null);
            }
        }
    }

    public void checkSleep(World world, Player bedEnterer) {
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) return;

        if (isBloodmoonPreventingSleep(world)) {
            for (Player p : world.getPlayers()) {
                if (p.isSleeping()) {
                    p.wakeup(false);
                }
            }
            return;
        }

        long time = world.getTime();
        boolean isNight = time >= 12541 && time <= 23458;
        if (!isNight && !world.hasStorm()) return;

        List<Player> sleepingPlayers = world.getPlayers().stream()
            .filter(p -> p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
            .filter(Player::isSleeping)
            .collect(Collectors.toList());

        long sleepingCount = sleepingPlayers.size();
        if (sleepingCount == 0 && bedEnterer == null) return;

        // Active players in this world:
        // Ignore spectators and AFK players.
        // A player currently sleeping is ALWAYS counted as active!
        boolean ignoreAfk = plugin.getConfig().getBoolean("sleep.ignore-afk", true);
        List<Player> activePlayers = world.getPlayers().stream()
            .filter(p -> p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
            .filter(p -> p.isSleeping() || !ignoreAfk || plugin.getAfkManager() == null || !plugin.getAfkManager().isAfk(p))
            .collect(Collectors.toList());

        int totalActive = activePlayers.size();
        if (totalActive <= 0) totalActive = 1;

        int percent = plugin.getConfig().getInt("sleep.percentage", 50);
        if (percent <= 0) percent = 50;
        if (percent > 100) percent = 100;

        int required = Math.max(1, (int) Math.ceil(totalActive * (percent / 100.0)));

        if (sleepingCount >= required) {
            long now = System.currentTimeMillis();
            if (now - lastNightSkipTime < 4000) return;
            lastNightSkipTime = now;

            world.setTime(0);
            if (world.hasStorm()) {
                world.setStorm(false);
                world.setThundering(false);
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "🌙 Світло перемогло темряву! Ніч пропущено.");

            if (plugin.getConfig().getBoolean("events.night-skip", true) && plugin.getBotManager() != null) {
                List<String> sleepingNames = sleepingPlayers.stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());

                String headPlayer = null;
                String text;
                if (!sleepingNames.isEmpty()) {
                    if (sleepingNames.size() == 1) {
                        headPlayer = sleepingNames.get(0);
                        text = "☀️ " + headPlayer + " пропустив(ла) ніч. Доброго ранку!";
                    } else {
                        text = "☀️ " + String.join(", ", sleepingNames) + " пропустили ніч. Доброго ранку!";
                    }
                } else {
                    text = "☀️ Ніч було пропущено. Доброго ранку!";
                }

                plugin.getBotManager().sendSystemEmbed(text, 0xFFD700, headPlayer);
            }
        } else if (bedEnterer != null) {
            int remaining = required - (int) sleepingCount;
            String poolLabel = totalActive == 1 ? "активного гравця" : "активних гравців";
            Bukkit.broadcastMessage(ChatColor.YELLOW + "🛏 " + ChatColor.WHITE + 
                bedEnterer.getName() + " ліг спати. Потрібно ще " + remaining + " (всього " + required + " з " + totalActive + " " + poolLabel + ").");
        }
    }
}
