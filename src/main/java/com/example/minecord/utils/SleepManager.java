package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

import java.util.List;
import java.util.stream.Collectors;

public class SleepManager implements Listener {
    private final MineCord plugin;
    
    public SleepManager(MineCord plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Disable vanilla sleep skipping by setting the gamerule very high
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setGameRule(org.bukkit.GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);
            }
        }
    }
    
    public void stop() {
        PlayerBedEnterEvent.getHandlerList().unregister(this);
    }
    
    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;
        
        World world = event.getPlayer().getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;
        
        // Wait 10 ticks (half a second) to ensure the player is considered "sleeping"
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkSleep(world, event.getPlayer()), 10L);
    }
    
    private void checkSleep(World world, Player bedEnterer) {
        if (world.getTime() < 12541 && world.getTime() > 23458 && !world.hasStorm()) return;
        
        List<Player> players = world.getPlayers();
        // Ignore players who are AFK using our AfkManager
        List<Player> activePlayers = players.stream()
            .filter(p -> plugin.getAfkManager() == null || !plugin.getAfkManager().isAfk(p))
            .filter(p -> p.getGameMode() != org.bukkit.GameMode.SPECTATOR)
            .collect(Collectors.toList());
            
        int totalActive = activePlayers.size();
        if (totalActive == 0) totalActive = 1;
        
        long sleepingCount = activePlayers.stream().filter(Player::isSleeping).count();
        
        int required = (int) Math.ceil(totalActive / 2.0);
        
        if (sleepingCount >= required) {
            world.setTime(0);
            if (world.hasStorm()) {
                world.setStorm(false);
                world.setThundering(false);
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "🌙 Світло перемогло темряву! Ніч пропущено.");
        } else {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "🛏 " + ChatColor.WHITE + 
                bedEnterer.getName() + " ліг спати. Потрібно ще " + (required - sleepingCount) + " (всього " + required + " з " + totalActive + " активних).");
        }
    }
}
