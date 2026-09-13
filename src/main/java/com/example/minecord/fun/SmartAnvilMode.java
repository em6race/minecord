package com.example.minecord.fun;

import com.example.minecord.MineCord;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Розважальний та покращувальний режим: Розумне ковадло (Smart Anvil).
 * Знімає ванільне обмеження 40 рівнів ("Занадто дорого!"), дозволяючи
 * досвіду чесно прогресувати та наростати (42, 45, 50, 65, 130... рівнів).
 */
public class SmartAnvilMode implements FunMode, Listener {

    private final MineCord plugin;
    private boolean enabled = true;
    private int maxRepairCost = Integer.MAX_VALUE;
    private boolean showActionBarInfo = true;
    private boolean registered = false;

    public SmartAnvilMode(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "smart_anvil";
    }

    @Override
    public String getName() {
        return "Розумне ковадло";
    }

    @Override
    public String getDescription() {
        return "Знімає ліміт 'Занадто дорого' у ковадлі, дозволяючи вартості ремонту зростати понад 40 рівнів.";
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
    }

    @Override
    public void onReload() {
        reloadConfig();
    }

    private void reloadConfig() {
        enabled = plugin.getConfig().getBoolean("fun.modes.smart_anvil.enabled", true);
        maxRepairCost = plugin.getConfig().getInt("fun.modes.smart_anvil.max_repair_cost", Integer.MAX_VALUE);
        if (maxRepairCost <= 0) {
            maxRepairCost = Integer.MAX_VALUE;
        }
        showActionBarInfo = plugin.getConfig().getBoolean("fun.modes.smart_anvil.show_actionbar_info", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!enabled) return;
        if (event.getInventory() instanceof AnvilInventory anvil) {
            anvil.setMaximumRepairCost(maxRepairCost);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!enabled) return;

        AnvilInventory anvil = event.getInventory();
        anvil.setMaximumRepairCost(maxRepairCost);

        int cost = anvil.getRepairCost();
        if (cost <= 0) return;

        if (showActionBarInfo && event.getView().getPlayer() instanceof Player player) {
            if (cost >= 40) {
                if (player.getLevel() >= cost) {
                    player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            "§6Ковадло: §fВартість §a" + cost + " §fрівнів досвіду §7(знято ліміт 40)"
                    ));
                } else {
                    player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            "§cНедостатньо досвіду! Потрібно §e" + cost + " §cрівнів §7(у вас " + player.getLevel() + ")"
                    ));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        int cost = anvil.getRepairCost();
        if (cost <= 0) return;

        if (player.getGameMode() != GameMode.CREATIVE) {
            if (player.getLevel() < cost) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                if (showActionBarInfo) {
                    player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            "§cНедостатньо досвіду! Потрібно §e" + cost + " §cрівнів §7(у вас " + player.getLevel() + ")"
                    ));
                }
                return;
            }

            // Успішне завершення ремонту/об'єднання
            if (showActionBarInfo) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                "§aУспішно використано §e" + cost + " §aрівнів досвіду!"
                        ));
                    }
                });
            }
        }
    }
}
