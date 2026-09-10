package com.example.minecord.commands;

import com.example.minecord.MineCord;
import com.example.minecord.utils.SkinHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShareCoordsCommand implements CommandExecutor, TabCompleter {

    private final MineCord plugin;

    public ShareCoordsCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Ця команда доступна лише для гравців у грі.");
            return true;
        }

        // Mandatory comment check as requested by user
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "❌ Помилка: ви обов'язково маєте вказати назву або опис місця!");
            player.sendMessage(ChatColor.GRAY + "Використання: " + ChatColor.YELLOW + "/sharecoords <опис> " + 
                    ChatColor.GRAY + "(наприклад: " + ChatColor.WHITE + "/sharecoords База в горах" + ChatColor.GRAY + " або " + 
                    ChatColor.WHITE + "/sharecoords Портал у Незер" + ChatColor.GRAY + ")");
            return true;
        }

        String comment = String.join(" ", args);
        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";

        final String dimension;
        if (worldName.endsWith("_nether")) {
            dimension = "Незер";
        } else if (worldName.endsWith("_the_end")) {
            dimension = "Енд";
        } else {
            dimension = "Верхній світ";
        }

        String mapUrl = plugin.getConfig().getString("discord.map-url", "http://localhost:8123/");
        if (!mapUrl.endsWith("/")) mapUrl += "/";

        // BlueMap URL format
        String fullUrl = String.format("%s#%s:%d:%d:%d:30:0:0:0:0:perspective", 
                mapUrl, worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // In-game broadcast components
        String header = ChatColor.GOLD + "📍 Гравець " + ChatColor.YELLOW + player.getName() + ChatColor.GOLD + " поділився координатами: " + ChatColor.WHITE + comment;
        String coordsPart = String.format("§7Координати: §eX: %d, Y: %d, Z: %d §7(%s) ", 
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dimension);

        TextComponent line2 = new TextComponent(coordsPart);
        TextComponent linkComp = new TextComponent("§b§n[🗺️ Відкрити на мапі]");
        linkComp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, fullUrl));
        linkComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aНатисніть, щоб відкрити мітку на 3D-мапі сервера")));
        line2.addExtra(linkComp);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(header);
            p.spigot().sendMessage(line2);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
        }

        // Send to Discord
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sendCoordsToDiscord(player, comment, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dimension, fullUrl);
        });

        return true;
    }

    private void sendCoordsToDiscord(Player player, String comment, int x, int y, int z, String dimension, String mapUrl) {
        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) return;

        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId == null || channelId.isEmpty() || channelId.equals("000000000000000000")) return;

        try {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(channelId.trim());
            if (channel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("📍 Мітка на мапі: " + comment);
                embed.setColor(0x3498DB);
                embed.setDescription(String.format("Гравець **%s** поділився координатами:\n**X: %d, Y: %d, Z: %d** (`%s`)\n\n[🗺️ Відкрити на інтерактивній мапі](%s)",
                        player.getName(), x, y, z, dimension, mapUrl));
                embed.setThumbnail(SkinHelper.getAvatarUrl(player));
                embed.setFooter("MineCord Map Integration");
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("Не вдалося відправити координати в Discord: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("База", "Дім", "Портал", "Спавн", "Шахта", "Скарбниця");
        }
        return Collections.emptyList();
    }
}
