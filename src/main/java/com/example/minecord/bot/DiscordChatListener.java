package com.example.minecord.bot;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

public class DiscordChatListener extends ListenerAdapter {

    private final MineCord plugin;

    public DiscordChatListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Ignore bot and webhook messages to prevent an infinite echo loop
        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        // 1. Check if message is in the remote console channel
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (consoleChannelId != null && event.getChannel().getId().equals(consoleChannelId)) {
            // Permission check: only server administrators can execute console commands
            if (event.getMember() == null || !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                event.getChannel().asTextChannel().sendMessage("❌ Недостатньо прав! Тільки адміністратори можуть виконувати команди.").queue();
                return;
            }

            // In console channel: read text as server command
            String command = event.getMessage().getContentRaw().trim();
            if (command.startsWith("/")) {
                command = command.substring(1).trim();
            }
            
            // If message starts with Cyrillic characters, treat as chat and ignore
            if (command.matches("^[а-яА-ЯіІїЇєЄґҐ].*")) {
                return;
            }

            plugin.getLogger().info("[Discord] Користувач " + event.getAuthor().getName() + " виконав команду в консолі: " + command);
            
            // Execute on the main server thread
            final String finalCommand = command;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                List<String> outputLines = new ArrayList<>();
                ConsoleCommandSender wrappedSender = createWrappedConsoleSender(
                        plugin.getServer().getConsoleSender(),
                        line -> {
                            if (line != null) {
                                String clean = ChatColor.stripColor(line);
                                if (!clean.trim().isEmpty()) {
                                    outputLines.add(clean);
                                }
                            }
                        }
                );

                boolean success = plugin.getServer().dispatchCommand(wrappedSender, finalCommand);
                if (success) {
                    event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✅")).queue();
                } else {
                    event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❌")).queue();
                }

                if (!outputLines.isEmpty()) {
                    StringBuilder sb = new StringBuilder("```\n");
                    int messagesSent = 0;
                    for (String line : outputLines) {
                        int i = 0;
                        while (i < line.length()) {
                            int end = Math.min(i + 1900, line.length());
                            String part = line.substring(i, end);
                            if (sb.length() + part.length() + 5 > 1950) {
                                sb.append("```");
                                if (messagesSent < 5) {
                                    event.getMessage().reply(sb.toString()).queue();
                                    messagesSent++;
                                }
                                sb = new StringBuilder("```\n");
                            }
                            sb.append(part);
                            i = end;
                        }
                        sb.append("\n");
                    }
                    if (sb.length() > 4 && messagesSent < 5) {
                        sb.append("```");
                        event.getMessage().reply(sb.toString()).queue();
                    }
                } else if (!success) {
                    event.getMessage().reply("❌ Команда не знайдена або введена неправильно!").queue();
                }
            });
            return;
        }

        // 1.5 Check if message is a reply in a ticket thread
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

        // 2. Check if message is in the game chat bridge channel
        String targetChannelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (targetChannelId == null || !event.getChannel().getId().equals(targetChannelId)) {
            return;
        }

        // Use effective guild nickname if available, else global username
        String author = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        // Use display text (replaces <@id> mentions with names)
        String message = event.getMessage().getContentDisplay();
        
        java.util.Set<java.util.UUID> pingedPlayers = new java.util.HashSet<>();

        // 1. Process genuine Discord mentions
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

        // 2. Process text-based mentions (@PlayerName)
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

        // 3. Play notification sound for all mentioned players
        for (java.util.UUID uuid : pingedPlayers) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            });
        }

        // Construct formatted message for Minecraft chat
        String formattedMessage = ChatColor.BLUE + "[Discord] " 
                + ChatColor.WHITE + author + ": " 
                + ChatColor.GRAY + message;

        // Append attachment indicator if attachments are present
        if (!event.getMessage().getAttachments().isEmpty()) {
            formattedMessage += ChatColor.AQUA + " [Вкладення]";
        }

        // Broadcast to all online players on the main thread
        String finalMessage = formattedMessage;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().broadcastMessage(finalMessage);
        });
    }

    private ConsoleCommandSender createWrappedConsoleSender(
            ConsoleCommandSender realConsole,
            java.util.function.Consumer<String> lineConsumer) {
        return (ConsoleCommandSender) java.lang.reflect.Proxy.newProxyInstance(
                realConsole.getClass().getClassLoader(),
                new Class<?>[]{ ConsoleCommandSender.class },
                (proxy, method, methodArgs) -> {
                    String name = method.getName();
                    if (name.equals("sendMessage") || name.equals("sendRawMessage")) {
                        if (methodArgs != null && methodArgs.length > 0) {
                            for (Object arg : methodArgs) {
                                if (arg instanceof String s) {
                                    lineConsumer.accept(s);
                                } else if (arg instanceof String[] arr) {
                                    for (String s : arr) lineConsumer.accept(s);
                                } else if (arg instanceof net.md_5.bungee.api.chat.BaseComponent[] arr) {
                                    lineConsumer.accept(net.md_5.bungee.api.chat.TextComponent.toPlainText(arr));
                                } else if (arg instanceof net.md_5.bungee.api.chat.BaseComponent comp) {
                                    lineConsumer.accept(comp.toPlainText());
                                } else if (arg != null && !(arg instanceof java.util.UUID)) {
                                    try {
                                        if (arg instanceof net.kyori.adventure.text.Component comp) {
                                            lineConsumer.accept(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp));
                                        } else {
                                            lineConsumer.accept(arg.toString());
                                        }
                                    } catch (Throwable t) {
                                        lineConsumer.accept(arg.toString());
                                    }
                                }
                            }
                        }
                    }
                    try {
                        return method.invoke(realConsole, methodArgs);
                    } catch (java.lang.reflect.InvocationTargetException ite) {
                        throw ite.getCause();
                    }
                }
        );
    }
}
