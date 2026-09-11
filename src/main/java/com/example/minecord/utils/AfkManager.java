package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
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

    public AfkManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("afk.enabled", true);
    }

    public void start() {
        stop();
        if (!isEnabled()) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long now = System.currentTimeMillis();
            long timeoutSeconds = plugin.getConfig().getLong("afk.timeout-seconds", 60);
            if (timeoutSeconds <= 0) timeoutSeconds = 60;
            long afkTimeout = timeoutSeconds * 1000L;
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                AfkData data = afkDataMap.computeIfAbsent(player.getUniqueId(), k -> new AfkData(player.getLocation(), now));
                
                Location currentLoc = player.getLocation();
                Location lastLoc = data.lastLocation;
                
                boolean moved = currentLoc.getX() != lastLoc.getX() || 
                                currentLoc.getY() != lastLoc.getY() || 
                                currentLoc.getZ() != lastLoc.getZ() ||
                                currentLoc.getYaw() != lastLoc.getYaw() ||
                                currentLoc.getPitch() != lastLoc.getPitch();
                
                if (moved || player.isSleeping()) {
                    data.lastLocation = currentLoc;
                    data.lastActivityTime = now;
                    if (data.isAfk) {
                        setAfk(player, data, false, now);
                    }
                } else {
                    if (!player.isSleeping() && !data.isAfk && now - data.lastActivityTime >= afkTimeout) {
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
        HandlerList.unregisterAll(this);
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (Map.Entry<UUID, AfkData> entry : afkDataMap.entrySet()) {
            AfkData data = entry.getValue();
            if (data.display != null && data.display.isValid()) {
                data.display.remove();
            }
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && p.isOnline()) {
                p.setCollidable(true);
                if (plugin.getRoleSyncManager() != null) {
                    p.setPlayerListName(plugin.getRoleSyncManager().formatPlayerListName(p, false));
                } else {
                    p.setPlayerListName(p.getName());
                }
            }
        }
        afkDataMap.clear();
    }

    private void setAfk(Player player, AfkData data, boolean afk, long now) {
        data.isAfk = afk;
        if (afk) {
            data.afkStartTime = now;

            // Prevent mobs from physically pushing the AFK player
            if (isMobInvulnerableEnabled()) {
                player.setCollidable(false);
            }

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

            // Reset targets of nearby mobs so they ignore the AFK player
            if (isMobInvulnerableEnabled()) {
                try {
                    for (Entity entity : player.getNearbyEntities(16, 16, 16)) {
                        if (entity instanceof Mob) {
                            Mob mob = (Mob) entity;
                            if (player.equals(mob.getTarget())) {
                                mob.setTarget(null);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Update tab list name
            if (plugin.getRoleSyncManager() != null) {
                player.setPlayerListName(plugin.getRoleSyncManager().formatPlayerListName(player, true));
            } else {
                player.setPlayerListName(ChatColor.GRAY + "[АФК] " + ChatColor.RESET + player.getName());
            }
            
            player.sendMessage(ChatColor.GRAY + "Ви перейшли в режим АФК" + (isMobInvulnerableEnabled() ? " (захист та фіксація позиції увімкнені)." : "."));
        } else {
            player.setCollidable(true);
            if (data.display != null && data.display.isValid()) {
                data.display.remove();
                data.display = null;
            }
            // Restore tab list name
            if (plugin.getRoleSyncManager() != null) {
                player.setPlayerListName(plugin.getRoleSyncManager().formatPlayerListName(player, false));
            } else {
                player.setPlayerListName(player.getName());
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
        if (!isEnabled()) return;
        AfkData data = afkDataMap.get(player.getUniqueId());
        if (data != null) {
            data.lastActivityTime = System.currentTimeMillis();
            if (data.isAfk) {
                setAfk(player, data, false, System.currentTimeMillis());
            }
        }
    }

    public boolean isAfk(Player player) {
        if (!isEnabled()) return false;
        AfkData data = afkDataMap.get(player.getUniqueId());
        return data != null && data.isAfk;
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
        try {
            event.getPlayer().setCollidable(true);
        } catch (Exception ignored) {}
        AfkData data = afkDataMap.remove(event.getPlayer().getUniqueId());
        if (data != null && data.display != null && data.display.isValid()) {
            data.display.remove();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        AfkData data = afkDataMap.get(player.getUniqueId());
        if (data == null || !data.isAfk) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // If player intentionally rotated their head/camera, exit AFK
        if (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
            updateActivity(player);
            return;
        }

        // If coordinates changed without rotation:
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            // If in liquid (water/lava current trying to displace the AFK player), prevent pushing
            if (from.getBlock().isLiquid() || to.getBlock().isLiquid()) {
                event.setTo(from);
                return;
            }

            // Player walked or jumped on solid ground (WASD / Space) -> exit AFK
            updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (isAfk(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onBedLeave(PlayerBedLeaveEvent event) {
        updateActivity(event.getPlayer());
    }

    public boolean isMobInvulnerableEnabled() {
        return isEnabled() && plugin.getConfig().getBoolean("afk.mob-invulnerable", true);
    }

    private boolean isMobDamage(Entity damager) {
        if (damager == null) return false;
        if (damager instanceof Mob) {
            return true;
        }
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return projectile.getShooter() instanceof Mob;
        }
        if (damager instanceof AreaEffectCloud) {
            AreaEffectCloud cloud = (AreaEffectCloud) damager;
            return cloud.getSource() instanceof Mob;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!isMobInvulnerableEnabled()) return;
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (isAfk(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // If a player attacks, remove their AFK status immediately
        if (event.getDamager() instanceof Player) {
            updateActivity((Player) event.getDamager());
        }

        // Protect AFK player from mob attacks and projectiles
        if (isMobInvulnerableEnabled() && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isAfk(player) && isMobDamage(event.getDamager())) {
                event.setCancelled(true);
            }
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
