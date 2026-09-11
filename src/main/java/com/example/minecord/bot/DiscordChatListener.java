package com.example.minecord.bot;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

        try {
            // Use effective guild nickname if available, else global username
            String author = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
            // Use display text (replaces <@id> mentions with names)
            String message = event.getMessage().getContentDisplay();

            List<String> pingedNames = new ArrayList<>();
            List<String> mentionedDiscordUserIds = new ArrayList<>();

            // 1. Process genuine Discord mentions
            for (net.dv8tion.jda.api.entities.Member mentionedMember : event.getMessage().getMentions().getMembers()) {
                mentionedDiscordUserIds.add(mentionedMember.getId());
                String effectiveName = mentionedMember.getEffectiveName();
                java.util.UUID uuid = plugin.getLinkManager().getUUIDFromDiscordId(mentionedMember.getId());
                String mcName = (uuid != null && plugin.getPlayerCacheManager() != null) ? plugin.getPlayerCacheManager().resolvePlayerName(uuid) : null;
                String replaceWith = (mcName != null) ? mcName : effectiveName;
                message = message.replace("@" + effectiveName, org.bukkit.ChatColor.YELLOW + "@" + replaceWith + org.bukkit.ChatColor.GRAY);
            }

            // 2. Process text-based mentions (@PlayerName)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_]{3,16})");
            java.util.regex.Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String targetName = matcher.group(1);
                pingedNames.add(targetName);
                message = message.replace("@" + targetName, org.bukkit.ChatColor.YELLOW + "@" + targetName + org.bukkit.ChatColor.GRAY);
            }

            // 3. Check if message is a reply to another message
            net.dv8tion.jda.api.entities.Message refMsg = event.getMessage().getReferencedMessage();
            TextComponent replyComponent = null;

            if (refMsg != null) {
                String refAuthor;
                if (refMsg.isWebhookMessage()) {
                    refAuthor = refMsg.getAuthor().getName();
                    pingedNames.add(refAuthor);
                } else if (refMsg.getMember() != null) {
                    refAuthor = refMsg.getMember().getEffectiveName();
                    mentionedDiscordUserIds.add(refMsg.getAuthor().getId());
                } else if (refMsg.getAuthor() != null) {
                    refAuthor = refMsg.getAuthor().getName();
                    mentionedDiscordUserIds.add(refMsg.getAuthor().getId());
                } else {
                    refAuthor = "Хтось";
                }

                String refContent = refMsg.getContentDisplay();
                if (refContent == null || refContent.trim().isEmpty()) {
                    if (!refMsg.getAttachments().isEmpty()) {
                        refContent = "[Вкладення]";
                    } else if (!refMsg.getEmbeds().isEmpty()) {
                        refContent = "[Вбудоване повідомлення]";
                    } else {
                        refContent = "...";
                    }
                }
                if (refContent.length() > 150) {
                    refContent = refContent.substring(0, 147) + "...";
                }

                replyComponent = new TextComponent(" §8[§b↩ §7" + refAuthor + "§8]");
                String hoverText = "§eВідповідь на повідомлення від §b@" + refAuthor + "§7:\n§f" + refContent;
                try {
                    String jumpUrl = refMsg.getJumpUrl();
                    replyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, jumpUrl));
                    hoverText += "\n\n§8(Клікніть, щоб відкрити в Discord)";
                } catch (Throwable ignored) {}

                try {
                    replyComponent.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            TextComponent.fromLegacyText(hoverText)
                    ));
                } catch (Throwable ignored) {}
            }

            // Role prefix for the Discord author
            String rolePrefix = "";
            if (plugin.getConfig().getBoolean("role-sync.chat-prefix", true) && plugin.getRoleSyncManager() != null) {
                if (event.getMember() != null) {
                    rolePrefix = plugin.getRoleSyncManager().getRolePrefixForMember(event.getMember());
                }
            }

            // Construct interactive component message for Minecraft chat
            TextComponent rootComponent = new TextComponent("§9[Discord] " + (rolePrefix != null ? rolePrefix : "") + "§f" + author);
            if (replyComponent != null) {
                rootComponent.addExtra(replyComponent);
            }
            rootComponent.addExtra(new TextComponent("§f: §7" + message));

            // Append attachment indicator if attachments are present
            if (!event.getMessage().getAttachments().isEmpty()) {
                TextComponent attachComp = new TextComponent(" §b[Вкладення]");
                try {
                    String attachUrl = event.getMessage().getAttachments().get(0).getUrl();
                    attachComp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachUrl));
                    attachComp.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            TextComponent.fromLegacyText("§eНатисніть, щоб відкрити вкладення в браузері")
                    ));
                } catch (Throwable ignored) {}
                rootComponent.addExtra(attachComp);
            }

            // Broadcast to all online players and play sounds safely on the main thread
            final TextComponent finalRoot = rootComponent;
            final List<String> finalPingNames = pingedNames;
            final List<String> finalDiscordIds = mentionedDiscordUserIds;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    // Play notification sound for mentioned / replied players
                    java.util.Set<Player> alertedPlayers = new java.util.HashSet<>();
                    for (String name : finalPingNames) {
                        Player p = plugin.getServer().getPlayerExact(name);
                        if (p != null && p.isOnline()) {
                            alertedPlayers.add(p);
                        }
                    }
                    for (String dId : finalDiscordIds) {
                        java.util.UUID uuid = plugin.getLinkManager().getUUIDFromDiscordId(dId);
                        if (uuid != null) {
                            Player p = plugin.getServer().getPlayer(uuid);
                            if (p != null && p.isOnline()) {
                                alertedPlayers.add(p);
                            }
                        }
                    }
                    for (Player p : alertedPlayers) {
                        try {
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        } catch (Throwable ignored) {}
                    }

                    // Send message to all players
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        try {
                            p.spigot().sendMessage(finalRoot);
                        } catch (Throwable t) {
                            // Fallback to legacy text if Spigot component fails
                            p.sendMessage(finalRoot.toLegacyText());
                        }
                    }
                    plugin.getServer().getConsoleSender().sendMessage(finalRoot.toLegacyText());
                } catch (Throwable t) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Помилка доставки Discord-повідомлення в чат: " + t.getMessage(), t);
                }
            });
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Помилка обробки повідомлення Discord -> Minecraft: " + t.getMessage(), t);
        }
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
