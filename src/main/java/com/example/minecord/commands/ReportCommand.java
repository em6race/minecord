package com.example.minecord.commands;

import com.example.minecord.MineCord;
import com.example.minecord.listeners.ChatListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final MineCord plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public ReportCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Ця команда доступна лише для гравців у грі.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "❌ Помилка: вкажіть нікнейм гравця та причину скарги!");
            player.sendMessage(ChatColor.GRAY + "Використання: " + ChatColor.YELLOW + "/report <гравець> <причина> " + ChatColor.GRAY + "(наприклад: " + ChatColor.WHITE + "/report Steve Чіти / спам у чаті" + ChatColor.GRAY + ")");
            return true;
        }

        String targetName = args[0];
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(ChatColor.RED + "❌ Ви не можете подати скаргу на самого себе!");
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownSec = plugin.getConfig().getLong("report.cooldown-seconds", 60);
        Long lastTime = cooldowns.get(player.getUniqueId());
        if (lastTime != null && (now - lastTime < cooldownSec * 1000L)) {
            long remaining = cooldownSec - ((now - lastTime) / 1000L);
            player.sendMessage(ChatColor.RED + "Зачекайте ще " + remaining + " сек. перед подачею наступної скарги.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Гравця " + targetName + " не знайдено на сервері.");
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        cooldowns.put(player.getUniqueId(), now);

        // Run Discord send asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sendReportToDiscord(player, target, targetName, reason);
        });

        player.sendMessage(ChatColor.GREEN + "✅ Вашу скаргу на " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " успішно надіслано модераторам у Discord! Дякуємо за допомогу.");
        return true;
    }

    private void sendReportToDiscord(Player sender, OfflinePlayer target, String targetName, String reason) {
        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) return;

        String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
        if (modChannelId == null || modChannelId.isEmpty() || modChannelId.equals("000000000000000000")) return;

        try {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(modChannelId.trim());
            if (channel == null) return;

            Location sLoc = sender.getLocation();
            String sWorld = sLoc.getWorld() != null ? sLoc.getWorld().getName() : "world";
            String senderLocationStr = String.format("X: %d, Y: %d, Z: %d (%s)", sLoc.getBlockX(), sLoc.getBlockY(), sLoc.getBlockZ(), sWorld);

            String targetStatus;
            String targetLocationStr = "Офлайн";
            if (target.isOnline() && target.getPlayer() != null) {
                Player tPlayer = target.getPlayer();
                targetStatus = "🟢 Онлайн (Пінг: " + tPlayer.getPing() + " ms)";
                Location tLoc = tPlayer.getLocation();
                String tWorld = tLoc.getWorld() != null ? tLoc.getWorld().getName() : "world";
                targetLocationStr = String.format("X: %d, Y: %d, Z: %d (%s)", tLoc.getBlockX(), tLoc.getBlockY(), tLoc.getBlockZ(), tWorld);
            } else {
                targetStatus = "🔴 Офлайн";
            }

            List<String> chatHistory = ChatListener.getRecentMessages(targetName);
            StringBuilder chatSb = new StringBuilder();
            if (chatHistory.isEmpty()) {
                chatSb.append("*Немає останніх повідомлень у чаті*");
            } else {
                for (String msg : chatHistory) {
                    chatSb.append("• `").append(msg.replace("`", "'")).append("`\n");
                }
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🚨 СКАРГА НА ГРАВЦЯ");
            embed.setColor(0xFF0000);
            embed.addField("👤 Відправник", sender.getName() + " (`" + sender.getUniqueId() + "`)", true);
            embed.addField("🎯 Підозрюваний", targetName + "\n" + targetStatus, true);
            embed.addField("📍 Локація відправника", senderLocationStr, false);
            if (!targetLocationStr.equals("Офлайн")) {
                embed.addField("📍 Локація підозрюваного", targetLocationStr, false);
            }
            embed.addField("📝 Причина скарги", "**" + reason + "**", false);
            embed.addField("💬 Останні повідомлення підозрюваного", chatSb.toString(), false);
            embed.setFooter("MineCord Moderation System");

            String reportId = UUID.randomUUID().toString().substring(0, 8);
            channel.sendMessageEmbeds(embed.build())
                    .setActionRow(Button.success("report_done_" + reportId + "_" + targetName, "✅ Оброблено"))
                    .queue();

        } catch (Throwable e) {
            plugin.getLogger().warning("Не вдалося надіслати скаргу в Discord: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(p.getName());
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
