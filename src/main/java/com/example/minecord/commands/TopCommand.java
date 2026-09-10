package com.example.minecord.commands;

import com.example.minecord.MineCord;
import com.example.minecord.utils.LeaderboardManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TopCommand implements CommandExecutor, TabCompleter {

    private final MineCord plugin;
    private static final List<String> CATEGORIES = Arrays.asList("time", "kills", "deaths", "diamonds", "blocks");

    public TopCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendMenu(sender);
            return true;
        }

        String rawCat = args[0].toLowerCase();
        String normalized = plugin.getLeaderboardManager().normalizeCategory(rawCat);
        String title = plugin.getLeaderboardManager().getCategoryTitle(normalized);

        sender.sendMessage(ChatColor.YELLOW + "⏳ Завантаження рейтингу " + title + "...");

        plugin.getLeaderboardManager().getTopAsync(normalized, 10, entries -> {
            sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "🏆 Топ-10: " + title + ChatColor.GOLD + " ===");

            if (entries == null || entries.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Дані для цього рейтингу поки що відсутні.");
                return;
            }

            for (int i = 0; i < entries.size(); i++) {
                LeaderboardManager.TopEntry entry = entries.get(i);
                String prefix = switch (i) {
                    case 0 -> "§e🥇 1.";
                    case 1 -> "§f🥈 2.";
                    case 2 -> "§6🥉 3.";
                    default -> "§7   " + (i + 1) + ".";
                };

                sender.sendMessage(prefix + " §b" + entry.getName() + " §8— §f" + entry.getFormattedValue());
            }
            sender.sendMessage(ChatColor.DARK_GRAY + "Натисніть /top, щоб відкрити список інших категорій.");
        });

        return true;
    }

    private void sendMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "🏆 Топи гравців сервера MineCord" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "Оберіть категорію для перегляду (клікніть по рядку):");

        sendClickableOption(sender, "⏱️ Награний час", "/top time", "Переглянути топ гравців за часом у грі");
        sendClickableOption(sender, "⚔️ Вбито мобів", "/top kills", "Переглянути топ за кількістю вбивств");
        sendClickableOption(sender, "💀 Смертей", "/top deaths", "Переглянути топ за кількістю смертей");
        sendClickableOption(sender, "💎 Добуто алмазів", "/top diamonds", "Переглянути топ за видобутою алмазною рудою");
        sendClickableOption(sender, "⛏️ Зламано блоків", "/top blocks", "Переглянути топ за кількістю зламаних блоків");
    }

    private void sendClickableOption(CommandSender sender, String title, String cmd, String tooltip) {
        if (sender instanceof Player player) {
            TextComponent comp = new TextComponent("§e• §b" + title + " §7(натисніть)");
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§a" + tooltip + "\n§7Команда: " + cmd)));
            player.spigot().sendMessage(comp);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "• " + ChatColor.AQUA + title + ChatColor.GRAY + " (" + cmd + ")");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String cat : CATEGORIES) {
                if (cat.startsWith(prefix)) {
                    result.add(cat);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
