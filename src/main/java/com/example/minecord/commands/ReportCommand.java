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

        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) {
            player.sendMessage(ChatColor.RED + "❌ Discord-бот наразі офлайн. Спробуйте пізніше!");
            return true;
        }

        String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
        if (modChannelId == null || modChannelId.trim().isEmpty() || modChannelId.trim().equals("000000000000000000")) {
            player.sendMessage(ChatColor.RED + "❌ Канал модераторів не налаштовано на сервері.");
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownSec = plugin.getConfig().getLong("report.cooldown-seconds", 60);
        UUID senderUuid = player.getUniqueId();
        Long lastTime = cooldowns.get(senderUuid);
        if (lastTime != null && (now - lastTime < cooldownSec * 1000L)) {
            long remaining = cooldownSec - ((now - lastTime) / 1000L);
            player.sendMessage(ChatColor.RED + "Зачекайте ще " + remaining + " сек. перед подачею наступної скарги.");
            return true;
        }

        // Capture all Bukkit state on the main thread before going async
        String senderName = player.getName();
        Location sLoc = player.getLocation();
        String sWorld = sLoc.getWorld() != null ? sLoc.getWorld().getName() : "world";
        String senderLocationStr = String.format("X: %d, Y: %d, Z: %d (%s)", sLoc.getBlockX(), sLoc.getBlockY(), sLoc.getBlockZ(), sWorld);

        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        final boolean isTargetOnline = onlineTarget != null && onlineTarget.isOnline();
        final String resolvedOnlineName = isTargetOnline ? onlineTarget.getName() : null;
        final String targetStatus;
        final String targetLocationStr;
        if (isTargetOnline) {
            targetStatus = "🟢 Онлайн (Пінг: " + onlineTarget.getPing() + " ms)";
            Location tLoc = onlineTarget.getLocation();
            String tWorld = tLoc.getWorld() != null ? tLoc.getWorld().getName() : "world";
            targetLocationStr = String.format("X: %d, Y: %d, Z: %d (%s)", tLoc.getBlockX(), tLoc.getBlockY(), tLoc.getBlockZ(), tWorld);
        } else {
            targetStatus = "🔴 Офлайн";
            targetLocationStr = "Офлайн";
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        cooldowns.put(senderUuid, now);

        // Run offline player verification and Discord send asynchronously (avoids main-thread Mojang HTTP lookup)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String finalTargetName = resolvedOnlineName != null ? resolvedOnlineName : targetName;
            if (!isTargetOnline) {
                boolean known = false;
                if (plugin.getPlayerCacheManager() != null) {
                    UUID cachedUuid = plugin.getPlayerCacheManager().getUuidByName(targetName);
                    if (cachedUuid != null) {
                        known = true;
                        String realName = plugin.getPlayerCacheManager().getRealName(cachedUuid, targetName);
                        if (realName != null) finalTargetName = realName;
                    }
                }
                if (!known) {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                    if (target.hasPlayedBefore() || target.isOnline()) {
                        known = true;
                        if (target.getName() != null) finalTargetName = target.getName();
                    }
                }
                if (!known) {
                    cooldowns.remove(senderUuid);
                    if (plugin.isEnabled()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                player.sendMessage(ChatColor.RED + "Гравця " + targetName + " не знайдено на сервері."));
                    }
                    return;
                }
            }

            boolean sent = sendReportToDiscord(senderName, senderUuid, senderLocationStr,
                    finalTargetName, targetStatus, targetLocationStr, reason, modChannelId.trim());
            if (plugin.isEnabled()) {
                final String msgTarget = finalTargetName;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (sent) {
                        player.sendMessage(ChatColor.GREEN + "✅ Вашу скаргу на " + ChatColor.YELLOW + msgTarget + ChatColor.GREEN + " успішно надіслано модераторам у Discord! Дякуємо за допомогу.");
                    } else {
                        cooldowns.remove(senderUuid);
                        player.sendMessage(ChatColor.RED + "❌ Не вдалося надіслати скаргу в Discord. Зверніться до адміністратора.");
                    }
                });
            }
        });

        return true;
    }

    private boolean sendReportToDiscord(String senderName, UUID senderUuid, String senderLocationStr,
                                        String targetName, String targetStatus, String targetLocationStr,
                                        String reason, String modChannelId) {
        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) return false;

        try {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(modChannelId);
            if (channel == null) return false;

            List<String> chatHistory = ChatListener.getRecentMessages(targetName);
            StringBuilder chatSb = new StringBuilder();
            if (chatHistory.isEmpty()) {
                chatSb.append("*Немає останніх повідомлень у чаті*");
            } else {
                for (String msg : chatHistory) {
                    String line = "• `" + msg.replace("`", "'") + "`\n";
                    if (chatSb.length() + line.length() > 1000) {
                        chatSb.append("...");
                        break;
                    }
                    chatSb.append(line);
                }
            }

            String safeReason = reason.length() > 900 ? reason.substring(0, 900) + "..." : reason;

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🚨 СКАРГА НА ГРАВЦЯ");
            embed.setColor(0xFF0000);
            embed.addField("👤 Відправник", senderName + " (`" + senderUuid + "`)", true);
            embed.addField("🎯 Підозрюваний", targetName + "\n" + targetStatus, true);
            embed.addField("📍 Локація відправника", senderLocationStr, false);
            if (!targetLocationStr.equals("Офлайн")) {
                embed.addField("📍 Локація підозрюваного", targetLocationStr, false);
            }
            embed.addField("📝 Причина скарги", "**" + safeReason + "**", false);
            embed.addField("💬 Останні повідомлення підозрюваного", chatSb.toString(), false);
            embed.setFooter("MineCord Moderation System");

            String reportId = UUID.randomUUID().toString().substring(0, 8);
            String safeTargetButton = targetName.length() > 32 ? targetName.substring(0, 32) : targetName;
            channel.sendMessageEmbeds(embed.build())
                    .setActionRow(Button.success("report_done_" + reportId + "_" + safeTargetButton, "✅ Оброблено"))
                    .queue();
            return true;

        } catch (Throwable e) {
            plugin.getLogger().warning("Не вдалося надіслати скаргу в Discord: " + e.getMessage());
            return false;
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
