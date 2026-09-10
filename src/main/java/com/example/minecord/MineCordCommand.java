package com.example.minecord;

import com.example.minecord.utils.AccountLinkManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class MineCordCommand implements CommandExecutor {

    private final MineCord plugin;

    public MineCordCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("map")) {
            String mapUrl = plugin.getConfig().getString("discord.map-url", "http://localhost:8123/");
            if (!mapUrl.endsWith("/")) mapUrl += "/";

            String fullUrl;
            if (sender instanceof Player player) {
                Location loc = player.getLocation();
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
                fullUrl = String.format("%s#%s:%d:%d:%d:500:0:0:0:0:flat", 
                        mapUrl, worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            } else {
                fullUrl = mapUrl.contains("#") ? mapUrl : (mapUrl + "#world:0:0:0:1500:0:0:0:0:flat");
            }

            sender.sendMessage(ChatColor.YELLOW + "Веб-мапа сервера (2D Flat):");
            
            // Create clickable link for convenience
            if (sender instanceof Player player) {
                net.md_5.bungee.api.chat.TextComponent link = new net.md_5.bungee.api.chat.TextComponent("§a§lНатисніть тут, щоб відкрити мапу");
                link.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, fullUrl));
                link.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("§7Перейти на 2D-мапу сервера")));
                player.spigot().sendMessage(link);
            } else {
                sender.sendMessage(ChatColor.AQUA + fullUrl);
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("minecord.admin") || sender.isOp()) {
                sender.sendMessage(ChatColor.YELLOW + "Перезавантаження конфігурації та бота MineCord...");
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "MineCord успішно перезавантажено!");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "У вас немає прав для цієї команди.");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("unmute") || (args.length > 0 && args[0].equalsIgnoreCase("unmute"))) {
            if (!sender.hasPermission("minecord.admin") && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "У вас немає прав для цієї команди.");
                return true;
            }
            String targetName = command.getName().equalsIgnoreCase("unmute") ? (args.length > 0 ? args[0] : null) : (args.length > 1 ? args[1] : null);
            if (targetName == null) {
                sender.sendMessage(ChatColor.RED + "Використання: /" + (command.getName().equalsIgnoreCase("unmute") ? "unmute" : "minecord unmute") + " <гравець>");
                return true;
            }
            org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
            if (plugin.getAntiSpamManager().unmute(target.getUniqueId())) {
                sender.sendMessage(ChatColor.GREEN + "Гравця " + targetName + " успішно розмучено!");
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(ChatColor.GREEN + "Вас було розмучено адміністратором.");
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Гравець " + targetName + " не був у муті або його термін уже закінчився.");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Ця команда доступна лише в грі!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("appeal")) {
            String blockedMsg = plugin.getOpenAIModerator().getAndClearLastBlockedMessage(player.getUniqueId());
            if (blockedMsg == null) {
                player.sendMessage(ChatColor.RED + "У вас немає заблокованих повідомлень або час оскарження вийшов.");
                return true;
            }

            String appealChannelId = plugin.getConfig().getString("ai-moderator.appeal-channel-id");
            if (appealChannelId != null && !appealChannelId.isEmpty()) {
            if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) {
                    player.sendMessage(ChatColor.RED + "Бот ще не підключений. Спробуйте пізніше.");
                    return true;
                }
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(appealChannelId);
                if (channel != null) {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setTitle("⚠️ Апеляція модерації чату");
                    embed.addField("Гравець", player.getName(), true);
                    embed.addField("Повідомлення", blockedMsg, false);
                    embed.setColor(0xFFA500);
                    channel.sendMessageEmbeds(embed.build()).queue();
                    
                    player.sendMessage(ChatColor.GREEN + "Вашу апеляцію надіслано адміністрації! Дякуємо.");
                } else {
                    player.sendMessage(ChatColor.RED + "Помилка відправки (канал не знайдено).");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Апеляції наразі вимкнені.");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "=== Команди MineCord ===");
            player.sendMessage(ChatColor.YELLOW + "/discord link" + ChatColor.WHITE + " - Прив'язати акаунт до Discord");
            player.sendMessage(ChatColor.YELLOW + "/stats [гравець]" + ChatColor.WHITE + " - Статистика гравця");
            player.sendMessage(ChatColor.YELLOW + "/top [категорія]" + ChatColor.WHITE + " - Топ гравців сервера");
            player.sendMessage(ChatColor.YELLOW + "/sharecoords <опис>" + ChatColor.WHITE + " - Поділитися координатами з мапою");
            player.sendMessage(ChatColor.YELLOW + "/report <гравець> <причина>" + ChatColor.WHITE + " - Скарга на порушника");
            player.sendMessage(ChatColor.YELLOW + "/map" + ChatColor.WHITE + " - Посилання на веб-мапу");
            player.sendMessage(ChatColor.YELLOW + "/mail send <гравець> <текст>" + ChatColor.WHITE + " - Надіслати офлайн-повідомлення");
            player.sendMessage(ChatColor.YELLOW + "/ticket create <текст>" + ChatColor.WHITE + " - Зв'язок з адміністрацією");
            player.sendMessage(ChatColor.YELLOW + "/togglerestart" + ChatColor.WHITE + " - Увімкнути/вимкнути сповіщення авторестарту");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("link")) {
            AccountLinkManager linkManager = plugin.getLinkManager();
            
            // If already linked
            if (linkManager.isLinked(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "Ваш акаунт вже прив'язано до Discord!");
                return true;
            }

            // Generate code and send to player
            String code = linkManager.generateCode(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "Ваш код для прив'язки: " + ChatColor.AQUA + ChatColor.BOLD + code);
            player.sendMessage(ChatColor.YELLOW + "Зайдіть на наш Discord сервер і введіть команду: " + ChatColor.WHITE + "/link " + code);
            return true;
        }

        if (command.getName().equalsIgnoreCase("mail")) {
            if (!(sender instanceof Player)) return true;
            if (args.length < 3 || !args[0].equalsIgnoreCase("send")) {
                sender.sendMessage(ChatColor.RED + "Використання: /mail send <гравець> <повідомлення>");
                return true;
            }
            String targetName = args[1];
            String message = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            
            // Execute asynchronously to avoid blocking on getOfflinePlayer lookup
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(targetName);
                if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                    sender.sendMessage(ChatColor.RED + "Гравця не знайдено на сервері.");
                    return;
                }
                String discordId = plugin.getLinkManager().getDiscordId(target.getUniqueId());
                if (discordId == null) {
                    sender.sendMessage(ChatColor.RED + "Гравець не прив'язав свій Discord-акаунт.");
                    return;
                }
                
                if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
                    plugin.getBotManager().getJda().openPrivateChannelById(discordId).queue(channel -> {
                        net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                        embed.setTitle("📩 Новий лист у грі!");
                        embed.setDescription("**Від:** " + sender.getName() + "\n**Повідомлення:** " + message);
                        embed.setColor(0x00FF00);
                        channel.sendMessageEmbeds(embed.build()).queue(
                            success -> sender.sendMessage(ChatColor.GREEN + "Лист успішно надіслано в Discord гравцю " + targetName + "!"),
                            error -> sender.sendMessage(ChatColor.RED + "Не вдалося надіслати повідомлення (можливо в гравця закриті приватні повідомлення).")
                        );
                    }, error -> sender.sendMessage(ChatColor.RED + "Не вдалося знайти користувача Discord."));
                }
            });
            return true;
        }

        if (command.getName().equalsIgnoreCase("ticket")) {
            if (!(sender instanceof Player)) return true;
            if (args.length < 2 || !args[0].equalsIgnoreCase("create")) {
                sender.sendMessage(ChatColor.RED + "Використання: /ticket create <повідомлення>");
                return true;
            }
            String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            
            String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
            if (modChannelId == null || modChannelId.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Канал модераторів не налаштовано.");
                return true;
            }
            
            if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel modChannel = plugin.getBotManager().getJda().getTextChannelById(modChannelId);
                if (modChannel != null) {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setTitle("🎫 Новий тікет від " + sender.getName());
                    embed.setDescription(message);
                    embed.setFooter("UUID: " + ((Player)sender).getUniqueId().toString());
                    embed.setColor(0xFF0000);
                    
                    modChannel.sendMessageEmbeds(embed.build()).queue(msg -> {
                        msg.createThreadChannel("Тікет-" + sender.getName()).queue(thread -> {
                            thread.sendMessage("Модератори скоро вам відповідять. (Щоб відповісти гравцю, пишіть повідомлення прямо сюди)").queue();
                        });
                        sender.sendMessage(ChatColor.GREEN + "Ваш тікет успішно створено! Відповідь від адміністрації прийде сюди.");
                    });
                }
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Використання: /discord link");
        return true;
    }
}