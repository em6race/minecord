package com.example.minecord.commands;

import com.example.minecord.MineCord;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

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
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Гравця " + targetName + " не знайдено на сервері.");
            return true;
        }

        showStats(sender, target);
        return true;
    }

    private void showStats(CommandSender viewer, OfflinePlayer target) {
        viewer.sendMessage(ChatColor.YELLOW + "=== Статистика гравця " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " ===");

        if (target.isOnline()) {
            Player p = target.getPlayer();
            viewer.sendMessage(ChatColor.GREEN + "Статус: Онлайн");
            viewer.sendMessage(ChatColor.WHITE + "Пінг: " + p.getPing() + " ms");
            viewer.sendMessage(ChatColor.WHITE + "Рівень: " + p.getLevel() + " lvl");

            int deaths = p.getStatistic(Statistic.DEATHS);
            int mobKills = p.getStatistic(Statistic.MOB_KILLS);
            int playerKills = p.getStatistic(Statistic.PLAYER_KILLS);
            viewer.sendMessage(ChatColor.WHITE + "Вбито мобів/гравців: " + mobKills + " / " + playerKills);
            viewer.sendMessage(ChatColor.WHITE + "Смертей: " + deaths);

            long playtimeTicks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long playtimeHours = playtimeTicks / (20 * 60 * 60);
            long playtimeMins = (playtimeTicks / (20 * 60)) % 60;
            viewer.sendMessage(ChatColor.WHITE + "Награний час: " + playtimeHours + " год. " + playtimeMins + " хв.");

            // Calculate blocks and distance
            int blocksBroken = 0;
            int blocksPlaced = 0;
            int itemsPickedUp = 0;

            for (Material mat : Material.values()) {
                if (mat.isBlock()) {
                    try { blocksBroken += p.getStatistic(Statistic.MINE_BLOCK, mat); } catch (Exception ignored) {}
                    try { blocksPlaced += p.getStatistic(Statistic.USE_ITEM, mat); } catch (Exception ignored) {}
                }
                if (mat.isItem()) {
                    try { itemsPickedUp += p.getStatistic(Statistic.PICKUP, mat); } catch (Exception ignored) {}
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
                try { distanceCm += p.getStatistic(s); } catch (Exception ignored) {}
            }
            long distanceBlocks = distanceCm / 100;
            long distanceKm = distanceBlocks / 1000;

            viewer.sendMessage(ChatColor.WHITE + "Зламано блоків: " + blocksBroken);
            viewer.sendMessage(ChatColor.WHITE + "Поставлено блоків: " + blocksPlaced);
            viewer.sendMessage(ChatColor.WHITE + "Підібрано предметів: " + itemsPickedUp);
            viewer.sendMessage(ChatColor.WHITE + "Подолано відстані: " + distanceKm + " км (" + distanceBlocks + " блоків)");

        } else {
            viewer.sendMessage(ChatColor.RED + "Статус: Офлайн");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Kyiv"));
            viewer.sendMessage(ChatColor.WHITE + "Останній вхід: " + sdf.format(new java.util.Date(target.getLastPlayed())));
            viewer.sendMessage(ChatColor.WHITE + "Перший вхід: " + sdf.format(new java.util.Date(target.getFirstPlayed())));
        }
    }
}
