package com.example.minecord.commands;

import com.example.minecord.MineCord;
import com.example.minecord.bot.DiscordCommandListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final MineCord plugin;

    public StatsCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> showStats(sender, player));
            } else {
                sender.sendMessage(ChatColor.RED + "Використання: /stats <гравець>");
            }
            return true;
        }

        String targetName = args[0];
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = plugin.getPlayerCacheManager() != null 
                    ? plugin.getPlayerCacheManager().resolvePlayerWithData(targetName, (java.util.List<java.util.UUID>) null) 
                    : plugin.getServer().getOfflinePlayer(targetName);

            boolean isWhitelisted = (target != null && target.isWhitelisted()) ||
                    (plugin.getPlayerCacheManager() != null && plugin.getPlayerCacheManager().isWhitelisted(targetName));
            boolean hasPlayed = target != null && (target.hasPlayedBefore() || target.isOnline() || target.getLastPlayed() > 0);
            if (!hasPlayed && target != null && plugin.getPlayerCacheManager() != null) {
                hasPlayed = plugin.getPlayerCacheManager().hasPlayerData(target);
            }

            if (target == null || (!hasPlayed && !isWhitelisted)) {
                if (plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.RED + "Гравця " + targetName + " не знайдено на сервері та у вайтлісті."));
                }
                return;
            }

            showStats(sender, target);
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if (plugin.getPlayerCacheManager() != null) {
                return plugin.getPlayerCacheManager().getMatchingPlayers(prefix);
            }
            List<String> suggestions = new ArrayList<>();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(p.getName());
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    private void showStats(CommandSender viewer, OfflinePlayer target) {
        List<String> lines = new ArrayList<>();
        String cachedName = plugin.getPlayerCacheManager() != null ? plugin.getPlayerCacheManager().resolvePlayerName(target.getUniqueId()) : null;
        String displayName = (target.getName() != null) ? target.getName() : (cachedName != null ? cachedName : "Гравець");
        lines.add(ChatColor.YELLOW + "=== Статистика гравця " + ChatColor.GOLD + displayName + ChatColor.YELLOW + " ===");

        // Unified Status & Ping
        if (target.isOnline() && target.getPlayer() != null) {
            Player p = target.getPlayer();
            lines.add(ChatColor.WHITE + "Статус: " + ChatColor.GREEN + "Онлайн " + ChatColor.GRAY + "(Пінг: " + p.getPing() + " ms)");
            lines.add(ChatColor.WHITE + "Рівень: " + p.getLevel() + " lvl");
        } else {
            if (!target.hasPlayedBefore() && target.getLastPlayed() <= 0) {
                lines.add(ChatColor.WHITE + "Статус: " + ChatColor.GRAY + "У вайтлісті (ще не заходив на сервер)");
            } else {
                lines.add(ChatColor.WHITE + "Статус: " + ChatColor.RED + "Офлайн");
            }
            if (plugin.getPlayerCacheManager() != null) {
                com.example.minecord.utils.PlayerCacheManager.PlayerXpData xp = plugin.getPlayerCacheManager().getPlayerXp(target);
                if (xp.totalExp > 0) {
                    lines.add(ChatColor.WHITE + "Рівень: " + xp.level + " lvl (" + String.format(java.util.Locale.US, "%,d", xp.totalExp) + " XP)");
                } else if (xp.level > 0) {
                    lines.add(ChatColor.WHITE + "Рівень: " + xp.level + " lvl");
                }
            }
        }

        // Advancements
        if (plugin.getPlayerCacheManager() != null) {
            int doneAdv = plugin.getPlayerCacheManager().getPlayerAdvancements(target);
            int totalAdv = plugin.getPlayerCacheManager().getTotalAdvancements();
            lines.add(ChatColor.WHITE + "Досягнення: " + doneAdv + " / " + totalAdv);
        }

        // First and last login dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Kyiv"));
        long firstPlayed = target.getFirstPlayed();
        long lastPlayed = target.getLastPlayed();

        if (firstPlayed > 0) {
            lines.add(ChatColor.WHITE + "Перший вхід: " + sdf.format(new Date(firstPlayed)));
        }
        if (target.isOnline()) {
            lines.add(ChatColor.WHITE + "Останній вхід: " + ChatColor.GREEN + "Зараз у грі");
        } else if (lastPlayed > 0) {
            lines.add(ChatColor.WHITE + "Останній вхід: " + sdf.format(new Date(lastPlayed)));
        }

        // Statistics (available for both online and offline players)
        int deaths = 0;
        int mobKills = 0;
        int playerKills = 0;
        long playtimeTicks = 0;
        try { deaths = target.getStatistic(Statistic.DEATHS); } catch (Exception ignored) {}
        try { mobKills = target.getStatistic(Statistic.MOB_KILLS); } catch (Exception ignored) {}
        try { playerKills = target.getStatistic(Statistic.PLAYER_KILLS); } catch (Exception ignored) {}
        try { playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE); } catch (Exception ignored) {}

        lines.add(ChatColor.WHITE + "Вбито мобів/гравців: " + mobKills + " / " + playerKills);
        lines.add(ChatColor.WHITE + "Смертей: " + deaths);

        long playtimeHours = playtimeTicks / (20 * 60 * 60);
        long playtimeMins = (playtimeTicks / (20 * 60)) % 60;
        lines.add(ChatColor.WHITE + "Награний час: " + playtimeHours + " год. " + playtimeMins + " хв.");

        // Calculate blocks and items using pre-filtered non-legacy Material arrays
        int blocksBroken = 0;
        int blocksPlaced = 0;
        int itemsPickedUp = 0;

        for (Material mat : DiscordCommandListener.BLOCK_MATERIALS) {
            try { blocksBroken += target.getStatistic(Statistic.MINE_BLOCK, mat); } catch (Exception ignored) {}
        }
        for (Material mat : DiscordCommandListener.PLACEABLE_BLOCK_MATERIALS) {
            try { blocksPlaced += target.getStatistic(Statistic.USE_ITEM, mat); } catch (Exception ignored) {}
        }
        for (Material mat : DiscordCommandListener.ITEM_MATERIALS) {
            try { itemsPickedUp += target.getStatistic(Statistic.PICKUP, mat); } catch (Exception ignored) {}
        }

        long distanceCm = 0;
        for (Statistic s : DiscordCommandListener.DIST_STATS) {
            try { distanceCm += target.getStatistic(s); } catch (Exception ignored) {}
        }
        long distanceBlocks = distanceCm / 100;
        long distanceKm = distanceBlocks / 1000;

        lines.add(ChatColor.WHITE + "Зламано блоків: " + blocksBroken);
        lines.add(ChatColor.WHITE + "Поставлено блоків: " + blocksPlaced);
        lines.add(ChatColor.WHITE + "Підібрано предметів: " + itemsPickedUp);
        lines.add(ChatColor.WHITE + "Подолано відстані: " + distanceKm + " км (" + distanceBlocks + " блоків)");

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String line : lines) {
                    viewer.sendMessage(line);
                }
            });
        }
    }
}
