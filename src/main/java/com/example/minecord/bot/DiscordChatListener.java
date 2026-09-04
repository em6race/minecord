package com.example.minecord.bot;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class DiscordChatListener extends ListenerAdapter {

    private final MineCord plugin;

    public DiscordChatListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Ігноруємо повідомлення від самих ботів та вебхуків,
        // щоб не створити нескінченний цикл (луну) повідомлень
        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        // 1. Перевіряємо, чи це канал КОНСОЛІ
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (consoleChannelId != null && event.getChannel().getId().equals(consoleChannelId)) {
            // FIX: Перевірка дозволів - виконувати команди може лише адміністратор сервера
            if (event.getMember() == null || !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                event.getChannel().asTextChannel().sendMessage("❌ Недостатньо прав! Тільки адміністратори можуть виконувати команди.").queue();
                return;
            }

            // Це канал консолі! Читаємо текст як команду
            String command = event.getMessage().getContentRaw();
            plugin.getLogger().info("Виконання команди з Discord: " + command);
            
            // Виконуємо в головному потоці сервера
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            });
            return;
        }

        // 1.5 Перевіряємо, чи це відповідь у гілці (тікеті)
        if (event.getChannelType() == net.dv8tion.jda.api.entities.channel.ChannelType.GUILD_PUBLIC_THREAD) {
            net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel thread = event.getChannel().asThreadChannel();
            String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
            if (modChannelId != null && thread.getParentChannel().getId().equals(modChannelId)) {
                thread.retrieveStartMessage().queue(startMsg -> {
                    if (!startMsg.getEmbeds().isEmpty()) {
                        net.dv8tion.jda.api.entities.MessageEmbed embed = startMsg.getEmbeds().get(0);
                        if (embed.getFooter() != null && embed.getFooter().getText() != null && embed.getFooter().getText().startsWith("UUID: ")) {
                            String uuidStr = embed.getFooter().getText().substring(6);
                            try {
                                java.util.UUID playerUuid = java.util.UUID.fromString(uuidStr);
                                String authorName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
                                String text = event.getMessage().getContentDisplay();
                                
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    org.bukkit.entity.Player p = plugin.getServer().getPlayer(playerUuid);
                                    if (p != null && p.isOnline()) {
                                        p.sendMessage(ChatColor.RED + "🎫 [Підтримка] " + ChatColor.YELLOW + authorName + ": " + ChatColor.WHITE + text);
                                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                                    } else {
                                        String discordId = plugin.getLinkManager().getDiscordId(playerUuid);
                                        if (discordId != null) {
                                            event.getJDA().openPrivateChannelById(discordId).queue(dm -> {
                                                dm.sendMessage("🎫 **Відповідь на ваш тікет від " + authorName + ":**\n" + text).queue();
                                            });
                                        }
                                    }
                                });
                            } catch (Exception ignored) {}
                        }
                    }
                });
                return;
            }
        }

        // 2. Перевіряємо, чи це ІГРОВИЙ чат
        String targetChannelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (targetChannelId == null || !event.getChannel().getId().equals(targetChannelId)) {
            return;
        }

        // Беремо нікнейм на сервері (якщо є), інакше глобальний нік
        String author = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        // Беремо очищений текст (getContentDisplay замінює згадки <@id> на реальні імена)
        String message = event.getMessage().getContentDisplay();
        
        java.util.Set<java.util.UUID> pingedPlayers = new java.util.HashSet<>();

        // 1. Обробляємо справжні Discord-пінги
        for (net.dv8tion.jda.api.entities.Member mentionedMember : event.getMessage().getMentions().getMembers()) {
            java.util.UUID uuid = plugin.getLinkManager().getUUIDFromDiscordId(mentionedMember.getId());
            if (uuid != null) {
                org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    message = message.replace("@" + mentionedMember.getEffectiveName(), org.bukkit.ChatColor.YELLOW + "@" + p.getName() + org.bukkit.ChatColor.GRAY);
                    pingedPlayers.add(uuid);
                }
            }
        }

        // 2. Обробляємо текстові пінги (@Нікнейм)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_]{3,16})");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String targetName = matcher.group(1);
            org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
            if (targetPlayer != null) {
                message = message.replace("@" + targetName, org.bukkit.ChatColor.YELLOW + "@" + targetName + org.bukkit.ChatColor.GRAY);
                pingedPlayers.add(targetPlayer.getUniqueId());
            }
        }

        // 3. Відтворюємо звук для всіх пінгнутих гравців
        for (java.util.UUID uuid : pingedPlayers) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            });
        }

        // Створюємо красиво відформатоване повідомлення для гри
        String formattedMessage = ChatColor.BLUE + "[Discord] " 
                + ChatColor.WHITE + author + ": " 
                + ChatColor.GRAY + message;

        // Якщо користувач відправив картинку або файл, додаємо помітку
        if (!event.getMessage().getAttachments().isEmpty()) {
            formattedMessage += ChatColor.AQUA + " [Вкладення]";
        }

        // Відправляємо сформоване повідомлення всім гравцям на сервері в головному потоці
        String finalMessage = formattedMessage;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().broadcastMessage(finalMessage);
        });
    }
}
