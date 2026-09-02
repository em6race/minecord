package com.example.minecord;

import com.example.minecord.utils.AccountLinkManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MineCordCommand implements CommandExecutor {

    private final MineCord plugin;

    public MineCordCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("map")) {
            sender.sendMessage(ChatColor.YELLOW + "Веб-мапа сервера:");
            
            // Створюємо клікабельне посилання для зручності
            if (sender instanceof Player) {
                net.md_5.bungee.api.chat.TextComponent link = new net.md_5.bungee.api.chat.TextComponent("§a§lНатисніть тут, щоб відкрити мапу");
                link.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, "http://kozlomine.minecraft.how:27257/"));
                link.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("§7Перейти на сайт мапи")));
                ((Player) sender).spigot().sendMessage(link);
            } else {
                sender.sendMessage(ChatColor.AQUA + "http://kozlomine.minecraft.how:27257/");
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

        if (args.length > 0 && args[0].equalsIgnoreCase("link")) {
            AccountLinkManager linkManager = plugin.getLinkManager();
            
            // Якщо вже прив'язано
            if (linkManager.isLinked(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "Ваш акаунт вже прив'язано до Discord!");
                return true;
            }

            // Генеруємо код і видаємо гравцю
            String code = linkManager.generateCode(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "Ваш код для прив'язки: " + ChatColor.AQUA + ChatColor.BOLD + code);
            player.sendMessage(ChatColor.YELLOW + "Зайдіть на наш Discord сервер і введіть команду: " + ChatColor.WHITE + "/link " + code);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Використання: /discord link");
        return true;
    }
}