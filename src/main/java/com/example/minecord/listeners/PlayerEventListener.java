package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final MineCord plugin;

    public PlayerEventListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        // Перевірка Whitelist (синхронізація з Discord)
        if (!plugin.getConfig().getBoolean("whitelist.enabled", false)) return;

        UUID uuid = event.getUniqueId();
        
        // 1. Перевірка чи акаунт взагалі прив'язаний
        if (!plugin.getLinkManager().isLinked(uuid)) {
            String code = plugin.getLinkManager().generateCode(uuid);
            String kickMsg = plugin.getConfig().getString("whitelist.kick-messages.not-linked", "§cВведіть /link code %code% у Discord!");
            kickMsg = kickMsg.replace("%code%", code);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg);
            return;
        }

        // 2. Якщо вказана обов'язкова роль у Discord - перевіряємо її
        String requiredRoleId = plugin.getConfig().getString("whitelist.require-discord-role", "");
        if (requiredRoleId != null && !requiredRoleId.isEmpty()) {
            String discordId = plugin.getLinkManager().getDiscordId(uuid);
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            
            if (guildId.isEmpty() || guildId.equals("000000000000000000")) {
                plugin.getLogger().warning("Увімкнено перевірку ролей (require-discord-role), але не вказано guild-id у config.yml!");
                return;
            }

            if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
                Guild guild = plugin.getBotManager().getJda().getGuildById(guildId);
                if (guild != null) {
                    try {
                        // Отримуємо учасника (це блокуючий виклик, але ми в Async-події, тому це безпечно)
                        Member member = guild.retrieveMemberById(discordId).complete();
                        
                        if (member == null) {
                            String kickMsg = plugin.getConfig().getString("whitelist.kick-messages.not-in-guild", "§cВи не на нашому Discord-сервері!");
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg);
                            return;
                        }

                        boolean hasRole = false;
                        for (Role role : member.getRoles()) {
                            if (role.getId().equals(requiredRoleId)) {
                                hasRole = true;
                                break;
                            }
                        }

                        if (!hasRole) {
                            String kickMsg = plugin.getConfig().getString("whitelist.kick-messages.no-role", "§cУ вас немає ролі!");
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg);
                        }
                    } catch (Exception e) {
                        // Якщо користувач вийшов з сервера, JDA кине виняток ErrorResponseException (Unknown Member)
                        String kickMsg = plugin.getConfig().getString("whitelist.kick-messages.not-in-guild", "§cВи не на нашому Discord-сервері!");
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        // Перевіряємо, чи увімкнений режим технічних робіт
        if (plugin.getConfig().getBoolean("maintenance.enabled", false)) {
            Player player = event.getPlayer();
            // Якщо гравець не адмін, забороняємо вхід
            if (!player.isOp() && !player.hasPermission("minecord.maintenance.bypass")) {
                String kickMsg = plugin.getConfig().getString("maintenance.message", "🛠️ Сервер на тестуванні.");
                kickMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&', kickMsg);
                
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore() && plugin.getConfig().getBoolean("events.first-join", true)) {
            String welcomeMessage = ChatColor.GOLD + "🎉 Вітаємо нового гравця " + ChatColor.YELLOW + player.getName() + ChatColor.GOLD + " на сервері!";
            plugin.getServer().broadcastMessage(welcomeMessage);
            
            if (plugin.getBotManager() != null) {
                plugin.getBotManager().sendSystemEmbed(player.getName() + " вперше приєднався до сервера! Бажаємо гарної гри!", 0xFFA500, player.getName());
            }
        } else if (plugin.getConfig().getBoolean("events.join-leave", true)) {
            if (plugin.getBotManager() != null) {
                plugin.getBotManager().sendSystemEmbed(player.getName() + " зайшов на сервер.", 0x00FF00, player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getConfig().getBoolean("events.join-leave", true)) {
            if (plugin.getBotManager() != null) {
                plugin.getBotManager().sendSystemEmbed(event.getPlayer().getName() + " вийшов із сервера.", 0xFF0000, event.getPlayer().getName());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        org.bukkit.entity.Player player = event.getEntity();
        org.bukkit.Location loc = player.getLocation();
        String worldName = loc.getWorld().getName();
        
        String dimension = "Верхній світ";
        if (worldName.endsWith("_nether")) dimension = "Незер";
        else if (worldName.endsWith("_the_end")) dimension = "Енд";
        
        // Відправляємо координати гравцю з клікабельним посиланням на мапу
        String coordsMsg = String.format("§c📍 Ви померли на координатах: §eX: %d, Y: %d, Z: %d §7(%s)", 
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dimension);
        
        String mapUrl = plugin.getConfig().getString("discord.map-url", "http://kozlomine.minecraft.how:27257/");
        if (!mapUrl.endsWith("/")) mapUrl += "/";
        
        // Формат посилання для BlueMap
        String fullUrl = String.format("%s#%s:%d:%d:%d:30:0:0:0:0", mapUrl, worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        
        net.md_5.bungee.api.chat.TextComponent msgComponent = new net.md_5.bungee.api.chat.TextComponent(coordsMsg + " ");
        net.md_5.bungee.api.chat.TextComponent linkComponent = new net.md_5.bungee.api.chat.TextComponent("§b§n[🗺️ Відкрити на мапі]");
        linkComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, fullUrl));
        linkComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("Натисніть, щоб відкрити місце смерті в браузері")));
        
        msgComponent.addExtra(linkComponent);
        player.spigot().sendMessage(msgComponent);
        
        // Записуємо в консоль сервера
        plugin.getLogger().info(String.format("Гравець %s помер на координатах: X: %d, Y: %d, Z: %d (%s)", 
                player.getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dimension));

        if (plugin.getConfig().getBoolean("events.death", true)) {
            String deathMessage = event.getDeathMessage();
            if (deathMessage != null) {
                // Очищаємо повідомлення від кольорів Minecraft
                String cleanMessage = ChatColor.stripColor(deathMessage);
                
                // Перекладаємо повідомлення на українську
                String translatedMessage = com.example.minecord.utils.DeathTranslator.translate(cleanMessage);
                
                if (plugin.getBotManager() != null) {
                    plugin.getBotManager().sendSystemEmbed("💀 " + translatedMessage, 0x000000, player.getName());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAdvancement(org.bukkit.event.player.PlayerAdvancementDoneEvent event) {
        if (!plugin.getConfig().getBoolean("events.advancement", true)) return;
        
        // Ігноруємо технічні досягнення (наприклад, відкриття рецептів)
        String advKey = event.getAdvancement().getKey().getKey();
        if (advKey.startsWith("recipes/")) return;
        
        // Ігноруємо кореневі досягнення (відкриття категорії типу "Minecraft", "Nether", "Adventure"),
        // бо вони не є повноцінними досягненнями
        if (advKey.endsWith("/root")) return;

        // В Bukkit складно дістати точну назву здобутку для всіх мов, тому перевіряємо чи це справжнє досягнення
        try {
            Object display = event.getAdvancement().getDisplay();
            if (display != null) {
                String title = ((org.bukkit.advancement.AdvancementDisplay) display).getTitle();
                String translatedTitle = com.example.minecord.utils.AdvancementTranslator.translate(advKey, title);
                if (plugin.getBotManager() != null) {
                    plugin.getBotManager().sendSystemEmbed("🏆 " + event.getPlayer().getName() + " виконав здобуток: " + translatedTitle, 0xFFD700, event.getPlayer().getName());
                }
            }
        } catch (Exception ignored) {
            // У старих версіях Bukkit getDisplay може не бути
        }
    }
}