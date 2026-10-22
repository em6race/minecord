package com.example.minecord.utils;

import com.example.minecord.MineCord;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Action Bar HUD: coordinates, compass direction and world day above the hotbar.
 * In the Nether it also shows the matching Overworld coordinates (X*8, Z*8).
 * Players can toggle it for themselves with /hud (stored in hud-disabled.yml).
 */
public class HudManager {
    private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private final MineCord plugin;
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();
    private final File dataFile;
    private int taskId = -1;

    public HudManager(MineCord plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "hud-disabled.yml");
        loadDisabled();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("hud.enabled", false);
    }

    public void start() {
        stop();
        if (!isEnabled()) return;

        long interval = Math.max(1, plugin.getConfig().getLong("hud.update-ticks", 5L));
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (disabled.contains(player.getUniqueId())) continue;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(build(player)));
            }
        }, 20L, interval);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /** Toggles HUD for a player. Returns true if the HUD is now ON. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean nowOn;
        if (disabled.remove(id)) {
            nowOn = true;
        } else {
            disabled.add(id);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            nowOn = false;
        }
        saveDisabled();
        return nowOn;
    }

    private long currentDay() {
        String name = plugin.getConfig().getString("hud.overworld-world", "world");
        World ow = plugin.getServer().getWorld(name);
        if (ow == null) ow = plugin.getServer().getWorlds().get(0);
        return ow.getFullTime() / 24000L;
    }

    private static String compass(float yaw) {
        float y = ((yaw % 360) + 360) % 360;
        return COMPASS[Math.round(y / 45f) % 8];
    }

    private String build(Player p) {
        Location l = p.getLocation();
        String coords = l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
        String sep = ChatColor.DARK_GRAY + " | ";
        String day = ChatColor.YELLOW + "Day " + currentDay();

        World.Environment env = p.getWorld().getEnvironment();
        if (env == World.Environment.NETHER) {
            double scale = plugin.getConfig().getDouble("hud.nether-scale", 8.0);
            long ox = Math.round(l.getX() * scale);
            long oz = Math.round(l.getZ() * scale);
            return ChatColor.RED + "Nether: " + ChatColor.WHITE + coords
                    + ChatColor.GRAY + " (Overworld: " + ChatColor.GREEN + ox + " " + oz + ChatColor.GRAY + ")"
                    + sep + day;
        }
        boolean end = env == World.Environment.THE_END;
        return (end ? ChatColor.LIGHT_PURPLE + "End: " : ChatColor.GREEN + "XYZ: ")
                + ChatColor.WHITE + coords
                + ChatColor.AQUA + " [" + compass(l.getYaw()) + "]"
                + sep + day;
    }

    private void loadDisabled() {
        if (!dataFile.exists()) return;
        List<String> list = YamlConfiguration.loadConfiguration(dataFile).getStringList("disabled");
        for (String s : list) {
            try {
                disabled.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private synchronized void saveDisabled() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("disabled", disabled.stream().map(UUID::toString).toList());
        try {
            plugin.getDataFolder().mkdirs();
            y.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save hud-disabled.yml: " + e.getMessage());
        }
    }
}
