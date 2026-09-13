package com.example.minecord.fun;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Розважальний режим: Спис без витрати ситості.
 * Дозволяє використовувати списи (Spears) без втрати голоду, насичення та виснаження.
 */
public class SpearHungerMode implements FunMode, Listener {

    private final MineCord plugin;
    private boolean enabled = true;
    private boolean onlyOnAttack = false;
    private final Set<String> customSpearMaterials = new HashSet<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private boolean registered = false;

    public SpearHungerMode(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "spear_no_hunger";
    }

    @Override
    public String getName() {
        return "Спис без витрати ситості";
    }

    @Override
    public String getDescription() {
        return "Запобігає втраті голоду та виснаження при утриманні або атаках списами.";
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
        lastAttackTime.clear();
    }

    @Override
    public void onReload() {
        reloadConfig();
    }

    private void reloadConfig() {
        enabled = plugin.getConfig().getBoolean("fun.modes.spear_no_hunger.enabled", true);
        onlyOnAttack = plugin.getConfig().getBoolean("fun.modes.spear_no_hunger.only_on_attack", false);

        customSpearMaterials.clear();
        List<String> list = plugin.getConfig().getStringList("fun.modes.spear_no_hunger.materials");
        if (list != null && !list.isEmpty()) {
            for (String s : list) {
                if (s != null) customSpearMaterials.add(s.trim().toUpperCase(Locale.ROOT));
            }
        }
    }

    private boolean isSpear(ItemStack item) {
        if (item == null) return false;
        String typeName = item.getType().name().toUpperCase(Locale.ROOT);
        if (typeName.endsWith("_SPEAR")) {
            return true;
        }
        return customSpearMaterials.contains(typeName);
    }

    private boolean isSpearHeld(Player player) {
        return isSpear(player.getInventory().getItemInMainHand()) || isSpear(player.getInventory().getItemInOffHand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (isSpear(player.getInventory().getItemInMainHand()) || isSpear(player.getInventory().getItemInOffHand())) {
                lastAttackTime.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExhaustion(EntityExhaustionEvent event) {
        if (!enabled) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (!isSpearHeld(player)) return;

        if (onlyOnAttack) {
            Long last = lastAttackTime.get(player.getUniqueId());
            boolean recentAttack = last != null && (System.currentTimeMillis() - last) <= 1500;
            if (recentAttack || event.getExhaustionReason() == EntityExhaustionEvent.ExhaustionReason.ATTACK) {
                event.setExhaustion(0.0f);
                event.setCancelled(true);
            }
        } else {
            event.setExhaustion(0.0f);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!enabled) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (!isSpearHeld(player)) return;

        if (onlyOnAttack) {
            Long last = lastAttackTime.get(player.getUniqueId());
            boolean recentAttack = last != null && (System.currentTimeMillis() - last) <= 1500;
            if (recentAttack && event.getFoodLevel() < player.getFoodLevel()) {
                event.setCancelled(true);
            }
        } else {
            if (event.getFoodLevel() < player.getFoodLevel()) {
                event.setCancelled(true);
            }
        }
    }
}
