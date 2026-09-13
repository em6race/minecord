package com.example.minecord.fun;

import com.example.minecord.MineCord;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Розважальний режим: Тягун без дощу.
 * Дозволяє використовувати чари Тягун (Riptide) на тризубці на суші без води та дощу.
 */
public class RiptideMode implements FunMode, Listener {

    private final MineCord plugin;
    private boolean enabled = true;
    private int cooldownTicks = 15;
    private double velocityMultiplier = 1.0;
    private boolean preventFallDamage = true;
    private boolean damageEntities = true;
    private double damageAmount = 8.0;
    private int durabilityCost = 1;

    private final Set<UUID> fallImmunePlayers = ConcurrentHashMap.newKeySet();
    private boolean registered = false;

    public RiptideMode(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "riptide_no_rain";
    }

    @Override
    public String getName() {
        return "Тягун без дощу";
    }

    @Override
    public String getDescription() {
        return "Дозволяє запускатися тризубцем з чарами Тягун на суші без води та дощу.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        reloadConfig();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    @Override
    public void onDisable() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
        fallImmunePlayers.clear();
    }

    @Override
    public void onReload() {
        reloadConfig();
    }

    private void reloadConfig() {
        enabled = plugin.getConfig().getBoolean("fun.modes.riptide_no_rain.enabled", true);
        cooldownTicks = plugin.getConfig().getInt("fun.modes.riptide_no_rain.cooldown_ticks", 15);
        velocityMultiplier = plugin.getConfig().getDouble("fun.modes.riptide_no_rain.velocity_multiplier", 1.0);
        preventFallDamage = plugin.getConfig().getBoolean("fun.modes.riptide_no_rain.prevent_fall_damage", true);
        damageEntities = plugin.getConfig().getBoolean("fun.modes.riptide_no_rain.damage_entities", true);
        damageAmount = plugin.getConfig().getDouble("fun.modes.riptide_no_rain.damage_amount", 8.0);
        durabilityCost = plugin.getConfig().getInt("fun.modes.riptide_no_rain.durability_cost", 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TRIDENT) {
            return;
        }

        int riptideLevel = item.getEnchantmentLevel(Enchantment.RIPTIDE);
        if (riptideLevel <= 0) {
            return;
        }

        Player player = event.getPlayer();

        // Якщо гравець вже у воді або під дощем — ванільний клієнт та сервер самі виконують механіку
        boolean inWater = player.isInWater();
        boolean inRain = player.getWorld().hasStorm() && player.getLocation().getBlock().getLightFromSky() >= 15;
        if (inWater || inRain) {
            return;
        }

        // Перевірка кулдауну
        if (player.hasCooldown(Material.TRIDENT)) {
            return;
        }

        // Скасовуємо стандартну взаємодію, щоб сервер повністю контролював ривок
        event.setCancelled(true);

        // 1. Обчислюємо та задаємо швидкість ривка
        Vector direction = player.getLocation().getDirection().normalize();
        double speed = (1.25 + 0.65 * riptideLevel) * velocityMultiplier;
        player.setVelocity(direction.multiply(speed));

        // 2. Звуковий ефект
        Sound sound = Sound.ITEM_TRIDENT_RIPTIDE_1;
        if (riptideLevel == 2) {
            sound = Sound.ITEM_TRIDENT_RIPTIDE_2;
        } else if (riptideLevel >= 3) {
            sound = Sound.ITEM_TRIDENT_RIPTIDE_3;
        }
        player.getWorld().playSound(player.getLocation(), sound, 1.2f, 1.0f);

        // 3. Частинки вихору
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 2, 0.2, 0.2, 0.2, 0.0);
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, loc, 25, 0.4, 0.4, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 8, 0.3, 0.3, 0.3, 0.05);

        // 4. Витрата міцності предмета
        if (player.getGameMode() != GameMode.CREATIVE && durabilityCost > 0) {
            int unbreaking = item.getEnchantmentLevel(Enchantment.DURABILITY);
            boolean takeDamage = true;
            if (unbreaking > 0) {
                if (ThreadLocalRandom.current().nextInt(unbreaking + 1) != 0) {
                    takeDamage = false;
                }
            }
            if (takeDamage) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof Damageable damageable) {
                    int newDmg = damageable.getDamage() + durabilityCost;
                    if (newDmg >= item.getType().getMaxDurability()) {
                        player.getInventory().setItem(event.getHand(), null);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    } else {
                        damageable.setDamage(newDmg);
                        item.setItemMeta(damageable);
                    }
                }
            }
        }

        // 5. Встановлення кулдауну
        if (cooldownTicks > 0) {
            player.setCooldown(Material.TRIDENT, cooldownTicks);
        }

        // 6. Захист від падіння
        if (preventFallDamage) {
            fallImmunePlayers.add(player.getUniqueId());
        }

        // 7. Таранна шкода сутностям на шляху польоту
        if (damageEntities) {
            startDashCollisionTracker(player);
        }
    }

    private void startDashCollisionTracker(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                ticks++;
                if (!player.isOnline() || ticks > 16 || (ticks > 3 && player.isOnGround())) {
                    cancel();
                    return;
                }

                Location currentLoc = player.getLocation();
                // Частинки сліду під час польоту
                if (ticks % 2 == 0) {
                    player.getWorld().spawnParticle(Particle.WATER_SPLASH, currentLoc.add(0, 0.5, 0), 6, 0.2, 0.2, 0.2, 0.05);
                }

                for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && !(entity instanceof ArmorStand)) {
                        if (target.getUniqueId().equals(player.getUniqueId())) continue;
                        if (hitEntities.add(target.getUniqueId())) {
                            target.damage(damageAmount, player);
                            player.getWorld().playSound(target.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player player) {
            if (fallImmunePlayers.remove(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        fallImmunePlayers.remove(event.getPlayer().getUniqueId());
    }
}
