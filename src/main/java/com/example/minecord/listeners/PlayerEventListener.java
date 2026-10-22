package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import com.example.minecord.utils.SkinHelper;
import net.dv8tion.jda.api.JDA;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final MineCord plugin;

    public PlayerEventListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        // Whitelist check (Discord synchronization)
        if (!plugin.getConfig().getBoolean("whitelist.enabled", false)) return;

        UUID uuid = event.getUniqueId();
        
        // 1. Check if the account is linked
        if (!plugin.getLinkManager().isLinked(uuid)) {
            String code = plugin.getLinkManager().generateCode(uuid);
            String kickMsg = plugin.getConfig().getString("whitelist.kick-messages.not-linked", "§cВведіть /link code %code% у Discord!");
            kickMsg = kickMsg.replace("%code%", code);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg);
            return;
        }

        // 2. If a required Discord role is configured, verify it (Fail-Closed for security)
        String requiredRoleId = plugin.getConfig().getString("whitelist.require-discord-role", "");
        if (requiredRoleId != null && !requiredRoleId.trim().isEmpty() && !requiredRoleId.equals("000000000000000000")) {
            requiredRoleId = requiredRoleId.trim();
            String discordId = plugin.getLinkManager().getDiscordId(uuid);
            String guildId = plugin.getConfig().getString("discord.guild-id", "");

            if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) {
                plugin.getLogger().warning("Гравець " + event.getName() + " намагався зайти, але Discord-бот ще не підключений для перевірки ролі вайтліста.");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        "§cDiscord-бот тимчасово недоступний для перевірки вайтліста.\n§7Спробуйте зайти через декілька секунд!");
                return;
            }

            JDA jda = plugin.getBotManager().getJda();
            Guild guild = null;
            if (guildId != null && !guildId.trim().isEmpty() && !guildId.trim().equals("000000000000000000")) {
                guild = jda.getGuildById(guildId.trim());
            } else if (!jda.getGuilds().isEmpty()) {
                guild = jda.getGuilds().get(0);
            }

            if (guild == null) {
                plugin.getLogger().warning("Увімкнено перевірку ролей (require-discord-role), але не вдалося знайти Discord-сервер (перевірте guild-id у config.yml)!");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        "§cПомилка конфігурації Discord-вайтліста (сервер не знайдено).\n§7Зверніться до адміністратора.");
                return;
            }

            try {
                // Retrieve member (blocking call, safe inside AsyncPlayerPreLoginEvent)
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
            } catch (Throwable e) {
                // If user left the guild, JDA throws ErrorResponseException (Unknown Member)
                String kickMsg = plugin.getConfig().getString("whitelist.kick-messages.not-in-guild", "§cВи не на нашому Discord-сервері!");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMsg);
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        // Check if maintenance mode is enabled
        if (plugin.getConfig().getBoolean("maintenance.enabled", false)) {
            Player player = event.getPlayer();
            // If player is not admin, disallow login
            if (!player.isOp() && !player.hasPermission("minecord.maintenance.bypass")) {
                String kickMsg = plugin.getConfig().getString("maintenance.message", "🛠️ Сервер на тестуванні.");
                kickMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&', kickMsg);
                
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();

            // Warm up skin avatar cache on the main thread
            SkinHelper.getAvatarUrl(player);

            if (plugin.getPlayerCacheManager() != null) {
                plugin.getPlayerCacheManager().addPlayer(player.getName(), player.getUniqueId());
                plugin.getPlayerCacheManager().updatePlayerXp(player.getUniqueId(), player.getLevel(), player.getTotalExperience());
                plugin.getPlayerCacheManager().updatePlayerAdvancements(player);
            }

            if (!player.hasPlayedBefore() && plugin.getConfig().getBoolean("events.first-join", true)) {
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    onlinePlayer.sendMessage(plugin.getLanguageManager().get(onlinePlayer, "events.first-join", player.getName()));
                }
                
                if (plugin.getBotManager() != null) {
                    String embedText = plugin.getLanguageManager().getRaw("events.first-join-embed", player.getName());
                    plugin.getBotManager().sendSystemEmbed(embedText, 0xFFA500, player.getName());
                }
            } else if (plugin.getConfig().getBoolean("events.join-leave", true)) {
                if (plugin.getBotManager() != null) {
                    String embedText = plugin.getLanguageManager().getRaw("events.join", player.getName());
                    plugin.getBotManager().sendSystemEmbed(embedText, 0x00FF00, player.getName());
                }
            }

            // Synchronize Discord role, TAB prefix, and nametag
            if (plugin.getRoleSyncManager() != null) {
                plugin.getRoleSyncManager().syncPlayer(player);
            }

            plugin.setLastPlayerSeenOnlineMillis(System.currentTimeMillis());
            plugin.setHadPlayersBeforeShutdown(null);

        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error in onPlayerJoin", e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            if (plugin.getServer().getOnlinePlayers().size() > 1) {
                plugin.setLastPlayerSeenOnlineMillis(System.currentTimeMillis());
            }

            if (plugin.getRoleSyncManager() != null) {
                plugin.getRoleSyncManager().clearPlayer(event.getPlayer());
            }

            if (plugin.getOpenAIModerator() != null) {
                plugin.getOpenAIModerator().clearPlayer(event.getPlayer().getUniqueId());
            }

            if (plugin.getPlayerCacheManager() != null) {
                plugin.getPlayerCacheManager().updatePlayerXp(event.getPlayer().getUniqueId(), event.getPlayer().getLevel(), event.getPlayer().getTotalExperience());
                plugin.getPlayerCacheManager().updatePlayerAdvancements(event.getPlayer());
            }

            if (plugin.getConfig().getBoolean("events.join-leave", true)) {
                if (plugin.getBotManager() != null) {
                    String embedText = plugin.getLanguageManager().getRaw("events.quit", event.getPlayer().getName());
                    plugin.getBotManager().sendSystemEmbed(embedText, 0xFF0000, event.getPlayer().getName());
                }
            }
        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error in onPlayerQuit", e);
        }
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        try {
            if (plugin.getPlayerCacheManager() != null) {
                plugin.getPlayerCacheManager().updatePlayerXp(
                        event.getPlayer().getUniqueId(),
                        event.getNewLevel(),
                        event.getPlayer().getTotalExperience()
                );
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            org.bukkit.entity.Player player = event.getEntity();
            org.bukkit.Location loc = player.getLocation();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            
            String dimensionKey = "events.dimensions.overworld";
            if (worldName.endsWith("_nether")) dimensionKey = "events.dimensions.nether";
            else if (worldName.endsWith("_the_end")) dimensionKey = "events.dimensions.end";
            String playerDimension = plugin.getLanguageManager().getRaw(player, dimensionKey);
            String serverDimension = plugin.getLanguageManager().getRaw(dimensionKey);
            
            // Send coordinates to player in their client language (with clickable map link only if a map plugin is installed)
            String coordsMsg = plugin.getLanguageManager().get(player, "events.death-coords", 
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), playerDimension);
            
            if (plugin.isMapAvailable()) {
                String mapUrl = plugin.getConfig().getString("discord.map-url", "http://localhost:8100/");
                if (mapUrl == null || mapUrl.trim().isEmpty()) {
                    mapUrl = "http://localhost:8100/";
                }
                if (!mapUrl.endsWith("/")) mapUrl += "/";
                
                // Link format for BlueMap (version 4/5+ requires 10 parameters)
                String fullUrl = String.format("%s#%s:%d:%d:%d:30:0:0:0:0:flat", mapUrl, worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                
                String buttonText = plugin.getLanguageManager().getRaw(player, "events.death-coords-map-button");
                String hoverText = plugin.getLanguageManager().getRaw(player, "events.death-coords-map-hover");
                net.md_5.bungee.api.chat.TextComponent msgComponent = new net.md_5.bungee.api.chat.TextComponent(coordsMsg + " ");
                net.md_5.bungee.api.chat.TextComponent linkComponent = new net.md_5.bungee.api.chat.TextComponent(buttonText);
                linkComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, fullUrl));
                linkComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text(hoverText)));
                
                msgComponent.addExtra(linkComponent);
                player.spigot().sendMessage(msgComponent);
            } else {
                player.sendMessage(coordsMsg);
            }
            
            // Log to server console in server language
            plugin.getLogger().info(plugin.getLanguageManager().getRaw("events.death-coords-log", 
                    player.getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), serverDimension));

            if (plugin.getConfig().getBoolean("events.death", true)) {
                String deathMessage = event.getDeathMessage();
                if (deathMessage != null) {
                    // Strip Minecraft color codes from message
                    String cleanMessage = ChatColor.stripColor(deathMessage);
                    if (plugin.getRoleSyncManager() != null) {
                        cleanMessage = plugin.getRoleSyncManager().cleanRoleTags(cleanMessage);
                    } else {
                        cleanMessage = com.example.minecord.utils.RoleSyncManager.stripRoleTags(cleanMessage);
                    }
                    
                    String lang = plugin.getLanguageManager() != null ? plugin.getLanguageManager().getLanguage() : "en";
                    String translatedMessage = cleanMessage;
                    try {
                        translatedMessage = com.example.minecord.utils.DeathTranslator.translate(cleanMessage, lang);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Could not translate death message: " + t.getMessage());
                    }

                    if (plugin.getRoleSyncManager() != null) {
                        translatedMessage = plugin.getRoleSyncManager().cleanRoleTags(translatedMessage);
                    } else {
                        translatedMessage = com.example.minecord.utils.RoleSyncManager.stripRoleTags(translatedMessage);
                    }
                    
                    if (plugin.getBotManager() != null) {
                        plugin.getBotManager().sendSystemEmbed("💀 " + translatedMessage, 0x000000, player.getName());
                    }
                }
            }
        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error in onPlayerDeath", e);
        }
    }

    @EventHandler
    public void onPlayerAdvancement(org.bukkit.event.player.PlayerAdvancementDoneEvent event) {
        try {
            // Ignore recipe/technical advancements
            String advKey = event.getAdvancement().getKey().getKey();
            if (advKey.startsWith("recipes/")) return;
            
            // Ignore root advancements (category milestones like "Minecraft", "Nether", "Adventure")
            // because they are not real player achievements
            if (advKey.endsWith("/root")) return;

            // Safely attempt to get advancement display; ignore hidden technical/datapack advancements
            String fallbackTitle = advKey;
            try {
                org.bukkit.advancement.AdvancementDisplay display = event.getAdvancement().getDisplay();
                if (display == null) {
                    return;
                }
                fallbackTitle = display.getTitle();
            } catch (Throwable ignored) {
                // Fallback if getDisplay() is unsupported on a specific server build
            }

            if (plugin.getPlayerCacheManager() != null) {
                plugin.getPlayerCacheManager().incrementAdvancements(event.getPlayer().getUniqueId());
            }

            if (!plugin.getConfig().getBoolean("events.advancement", true)) return;

            String lang = plugin.getLanguageManager() != null ? plugin.getLanguageManager().getLanguage() : "en";
            String translatedTitle = fallbackTitle;
            try {
                translatedTitle = com.example.minecord.utils.AdvancementTranslator.translate(advKey, fallbackTitle, lang);
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not translate advancement: " + t.getMessage());
            }
            
            // Broadcast achievement to Discord
            if (plugin.getBotManager() != null) {
                String embedMsg = plugin.getLanguageManager().getRaw("advancement.embed", event.getPlayer().getName(), translatedTitle);
                plugin.getBotManager().sendSystemEmbed(embedMsg, 0xFFD700, event.getPlayer().getName());
            }
        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error in onPlayerAdvancement", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim().toLowerCase();
        if (msg.startsWith("/")) msg = msg.substring(1).trim();
        if (isRestartCmd(msg)) {
            if (event.getPlayer().isOp() || event.getPlayer().hasPermission("bukkit.command.restart")) {
                plugin.setRestarting(true);
                plugin.recordPlayerPresenceBeforeShutdown();
            }
        } else if (isStopCmd(msg)) {
            if (event.getPlayer().isOp() || event.getPlayer().hasPermission("bukkit.command.stop")) {
                plugin.recordPlayerPresenceBeforeShutdown();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String cmd = event.getCommand().trim().toLowerCase();
        if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
        if (isRestartCmd(cmd)) {
            plugin.setRestarting(true);
            plugin.recordPlayerPresenceBeforeShutdown();
        } else if (isStopCmd(cmd)) {
            plugin.recordPlayerPresenceBeforeShutdown();
        }
    }

    private boolean isRestartCmd(String cmd) {
        return cmd.equals("restart") || cmd.startsWith("restart ")
                || cmd.equals("spigot:restart") || cmd.startsWith("spigot:restart ")
                || cmd.equals("minecraft:restart") || cmd.startsWith("minecraft:restart ")
                || cmd.equals("queuerestart") || cmd.startsWith("queuerestart ");
    }

    private boolean isStopCmd(String cmd) {
        return cmd.equals("stop") || cmd.startsWith("stop ")
                || cmd.equals("minecraft:stop") || cmd.startsWith("minecraft:stop ")
                || cmd.equals("spigot:stop") || cmd.startsWith("spigot:stop ")
                || cmd.equals("end") || cmd.startsWith("end ");
    }
}
