package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final MineCord plugin;

    public ChatListener(MineCord plugin) {
        this.plugin = plugin;
    }

    // FIX: Use MONITOR with ignoreCancelled=false in order to:
    // - Intercept message AFTER all other plugins (anti-spam, etc.)
    // - Still run our checks (mute, spam, AI) even if already cancelled by someone else
    // Moderation (mute/spam/AI) cancels the event itself, so we need to see it
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // If the message was already cancelled by another plugin, do not send to Discord, and skip AI check
        boolean alreadyCancelled = event.isCancelled();

        boolean canBypass = event.getPlayer().isOp() || event.getPlayer().hasPermission("minecord.antispam.bypass");

        // 1. Mute check
        if (!canBypass && plugin.getAntiSpamManager().isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            long remaining = plugin.getAntiSpamManager().getMuteRemainingSeconds(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage("§cВи замучені за спам. Залишилося: " + remaining + " сек.");
            return;
        }

        // 2. Spam check
        if (!alreadyCancelled && !canBypass) {
            int spamLevel = plugin.getAntiSpamManager().checkSpamLevel(event.getPlayer().getUniqueId(), event.getMessage());
            if (spamLevel == 1) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cЗачекайте перед відправкою наступного повідомлення!");
                return;
            } else if (spamLevel == 2) {
                event.setCancelled(true);
                long muteSec = plugin.getConfig().getLong("antispam.mute-duration-seconds", 60);
                event.getPlayer().sendMessage("§cВас замучено на " + muteSec + " сек. за спам у чаті!");
                return;
            }
        }

        // 3. If the message is already cancelled by another plugin, do not send to Discord
        if (alreadyCancelled) return;

        if (plugin.getConfig().getBoolean("ai-moderator.enabled", false)) {
            String message = event.getMessage();
            org.bukkit.entity.Player player = event.getPlayer();

            // Run AI check completely in the background to prevent input lag
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    boolean isToxic = plugin.getOpenAIModerator().isMessageToxic(message).join();
                    if (isToxic) {
                        // Notify moderators in Discord instead of automatic mute
                        if (plugin.getBotManager() != null) {
                            String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
                            if (modChannelId != null && !modChannelId.isEmpty()) {
                                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(modChannelId);
                                if (channel != null) {
                                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                                    embed.setTitle("⚠️ Підозра на серйозне порушення чату");
                                    embed.setColor(0xFF0000);
                                    embed.addField("Гравець", player.getName(), true);
                                    embed.addField("Повідомлення", message, false);
                                    embed.setFooter("Автоматично виявлено AI-Модератором");
                                    channel.sendMessageEmbeds(embed.build()).queue();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Помилка при перевірці чату: " + e.getMessage());
                }
            });
        }

        // If all checks pass, prepare the message
        String playerName = event.getPlayer().getName();
        String originalMessage = event.getMessage();
        String discordMessage = originalMessage;

        // Look for mentions in message (format @Nickname)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_]{3,16})");
        java.util.regex.Matcher matcher = pattern.matcher(originalMessage);
        
        String minecraftMessage = originalMessage;
        
        while (matcher.find()) {
            String targetName = matcher.group(1);
            org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
            
            if (targetPlayer != null) {
                // Highlight in Minecraft (current match only)
                minecraftMessage = minecraftMessage.replace("@" + targetName, org.bukkit.ChatColor.YELLOW + "@" + targetName + org.bukkit.ChatColor.RESET);
                
                // Play sound (using runTask because we are in async)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    targetPlayer.playSound(targetPlayer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                });
                
                // Convert to Discord ping if the player has linked their account
                String discordId = plugin.getLinkManager().getDiscordId(targetPlayer.getUniqueId());
                if (discordId != null) {
                    discordMessage = discordMessage.replace("@" + targetName, "<@" + discordId + ">");
                }
            }
        }
        
        event.setMessage(minecraftMessage);

        // Send to Discord
        if (plugin.getBotManager() != null && plugin.getBotManager().getWebhookManager() != null) {
            plugin.getBotManager().getWebhookManager().sendMessage(playerName, discordMessage);
        }
    }
}
