package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import com.example.minecord.utils.SkinHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class DragonEventListener implements Listener {

    private final MineCord plugin;
    private long lastDragonSpawnAlert = 0;

    public DragonEventListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;
        if (!plugin.getConfig().getBoolean("events.dragon", true)) return;

        World world = event.getLocation().getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) return;

        // Debounce to prevent multiple triggers if multiple dragon components register
        long now = System.currentTimeMillis();
        if (now - lastDragonSpawnAlert < 30_000) {
            return;
        }
        lastDragonSpawnAlert = now;

        // In-game broadcast
        plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "[MineCord] " + ChatColor.DARK_PURPLE + "🐉 У Краї з'явився Ендер Дракон! Готуйтеся до битви!");

        // Discord embed
        sendDiscordEmbed("🐉 Ендер Дракон прокинувся!",
                "У Краї з'явився **Ендер Дракон**! Гравці готуються до битви за Край!",
                0x9B59B6,
                null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (!plugin.getConfig().getBoolean("events.dragon", true)) return;

        Player killer = dragon.getKiller();
        if (killer == null) {
            EntityDamageEvent lastDamage = dragon.getLastDamageCause();
            if (lastDamage instanceof EntityDamageByEntityEvent edbe) {
                if (edbe.getDamager() instanceof Player p) {
                    killer = p;
                } else if (edbe.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                    killer = p;
                } else if (edbe.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
                    killer = p;
                }
            }
        }

        if (killer != null) {
            String killerName = killer.getName();
            plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "[MineCord] " + ChatColor.GOLD + "🎉 Гравець " + ChatColor.YELLOW + killerName + ChatColor.GOLD + " завдав останнього смертельного удару Ендер Дракону!");

            String avatar = SkinHelper.getAvatarUrl(killer);
            sendDiscordEmbed("🐉 Ендер Дракон переможений!",
                    "Гравець **" + killerName + "** завдав останнього смертельного удару Ендер Дракону в Краю! Вітаємо героїв!",
                    0xF1C40F,
                    avatar);
        } else {
            plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "[MineCord] " + ChatColor.GOLD + "🎉 Ендер Дракона було подолано хоробрими воїнами!");

            sendDiscordEmbed("🐉 Ендер Дракон переможений!",
                    "Ендер Дракон був подоланий хоробрими воїнами сервера!",
                    0xF1C40F,
                    null);
        }
    }

    private void sendDiscordEmbed(String title, String description, int color, String thumbnailUrl) {
        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) return;

        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId == null || channelId.isEmpty() || channelId.equals("000000000000000000")) return;

        try {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(channelId.trim());
            if (channel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(title);
                embed.setDescription(description);
                embed.setColor(color);
                if (thumbnailUrl != null) {
                    embed.setThumbnail(thumbnailUrl);
                }
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("Не вдалося відправити сповіщення про Дракона: " + e.getMessage());
        }
    }
}
