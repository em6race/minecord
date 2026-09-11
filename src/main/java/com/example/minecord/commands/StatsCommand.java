package com.example.minecord.commands;

import com.example.minecord.MineCord;
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
            if (sender instanceof Player) {
                showStats(sender, (OfflinePlayer) sender);
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
                sender.sendMessage(ChatColor.RED + "Гравця " + targetName + " не знайдено на сервері та у вайтлісті.");
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
        String cachedName = plugin.getPlayerCacheManager() != null ? plugin.getPlayerCacheManager().resolvePlayerName(target.getUniqueId()) : null;
        String displayName = (target.getName() != null) ? target.getName() : (cachedName != null ? cachedName : "Гравець");
        viewer.sendMessage(ChatColor.YELLOW + "=== Статистика гравця " + ChatColor.GOLD + displayName + ChatColor.YELLOW + " ===");

        // Unified Status & Ping
        if (target.isOnline() && target.getPlayer() != null) {
            Player p = target.getPlayer();
            viewer.sendMessage(ChatColor.WHITE + "Статус: " + ChatColor.GREEN + "Онлайн " + ChatColor.GRAY + "(Пінг: " + p.getPing() + " ms)");
            viewer.sendMessage(ChatColor.WHITE + "Рівень: " + p.getLevel() + " lvl");
        } else {
            if (!target.hasPlayedBefore() && target.getLastPlayed() <= 0) {
                viewer.sendMessage(ChatColor.WHITE + "Статус: " + ChatColor.GRAY + "У вайтлісті (ще не заходив на сервер)");
            } else {
                viewer.sendMessage(ChatColor.WHITE + "Статус: " + ChatColor.RED + "Офлайн");
            }
            if (plugin.getPlayerCacheManager() != null) {
                com.example.minecord.utils.PlayerCacheManager.PlayerXpData xp = plugin.getPlayerCacheManager().getPlayerXp(target);
                if (xp.totalExp > 0) {
                    viewer.sendMessage(ChatColor.WHITE + "Рівень: " + xp.level + " lvl (" + String.format(java.util.Locale.US, "%,d", xp.totalExp) + " XP)");
                } else if (xp.level > 0) {
                    viewer.sendMessage(ChatColor.WHITE + "Рівень: " + xp.level + " lvl");
                }
            }
        }

        // Advancements
        if (plugin.getPlayerCacheManager() != null) {
            int doneAdv = plugin.getPlayerCacheManager().getPlayerAdvancements(target);
            int totalAdv = plugin.getPlayerCacheManager().getTotalAdvancements();
            viewer.sendMessage(ChatColor.WHITE + "Досягнення: " + doneAdv + " / " + totalAdv);
        }

        // First and last login dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Kyiv"));
        long firstPlayed = target.getFirstPlayed();
        long lastPlayed = target.getLastPlayed();

        if (firstPlayed > 0) {
            viewer.sendMessage(ChatColor.WHITE + "Перший вхід: " + sdf.format(new Date(firstPlayed)));
        }
        if (target.isOnline()) {
            viewer.sendMessage(ChatColor.WHITE + "Останній вхід: " + ChatColor.GREEN + "Зараз у грі");
        } else if (lastPlayed > 0) {
            viewer.sendMessage(ChatColor.WHITE + "Останній вхід: " + sdf.format(new Date(lastPlayed)));
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

        viewer.sendMessage(ChatColor.WHITE + "Вбито мобів/гравців: " + mobKills + " / " + playerKills);
        viewer.sendMessage(ChatColor.WHITE + "Смертей: " + deaths);

        long playtimeHours = playtimeTicks / (20 * 60 * 60);
        long playtimeMins = (playtimeTicks / (20 * 60)) % 60;
        viewer.sendMessage(ChatColor.WHITE + "Награний час: " + playtimeHours + " год. " + playtimeMins + " хв.");

        // Calculate blocks and items
        int blocksBroken = 0;
        int blocksPlaced = 0;
        int itemsPickedUp = 0;

        for (Material mat : Material.values()) {
            if (mat.isBlock()) {
                try { blocksBroken += target.getStatistic(Statistic.MINE_BLOCK, mat); } catch (Exception ignored) {}
                try { blocksPlaced += target.getStatistic(Statistic.USE_ITEM, mat); } catch (Exception ignored) {}
            }
            if (mat.isItem()) {
                try { itemsPickedUp += target.getStatistic(Statistic.PICKUP, mat); } catch (Exception ignored) {}
            }
        }

        long distanceCm = 0;
        Statistic[] distStats = {
                Statistic.WALK_ONE_CM, Statistic.SPRINT_ONE_CM, Statistic.SWIM_ONE_CM,
                Statistic.FLY_ONE_CM, Statistic.MINECART_ONE_CM, Statistic.HORSE_ONE_CM,
                Statistic.PIG_ONE_CM, Statistic.BOAT_ONE_CM, Statistic.AVIATE_ONE_CM,
                Statistic.CLIMB_ONE_CM, Statistic.FALL_ONE_CM, Statistic.WALK_ON_WATER_ONE_CM,
                Statistic.WALK_UNDER_WATER_ONE_CM, Statistic.CROUCH_ONE_CM
        };
        for (Statistic s : distStats) {
            try { distanceCm += target.getStatistic(s); } catch (Exception ignored) {}
        }
        long distanceBlocks = distanceCm / 100;
        long distanceKm = distanceBlocks / 1000;

        viewer.sendMessage(ChatColor.WHITE + "Зламано блоків: " + blocksBroken);
        viewer.sendMessage(ChatColor.WHITE + "Поставлено блоків: " + blocksPlaced);
        viewer.sendMessage(ChatColor.WHITE + "Підібрано предметів: " + itemsPickedUp);
        viewer.sendMessage(ChatColor.WHITE + "Подолано відстані: " + distanceKm + " км (" + distanceBlocks + " блоків)");
    }
}
