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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShareCoordsCommand implements CommandExecutor, TabCompleter {

    private final MineCord plugin;

    public ShareCoordsCommand(MineCord plugin) {
        this.plugin = plugin;
    }

    private Integer parseCoord(String raw, int current) {
        if (raw == null) return null;
        String s = raw.trim().replace(",", "").replace(";", "");
        if (s.isEmpty()) return null;

        if (s.equals("~")) {
            return current;
        }
        if (s.startsWith("~")) {
            try {
                return current + Integer.parseInt(s.substring(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Ця команда доступна лише для гравців у грі.");
            return true;
        }

        Location loc = player.getLocation();
        int currentX = loc.getBlockX();
        int currentY = loc.getBlockY();
        int currentZ = loc.getBlockZ();

        int targetX = currentX;
        int targetY = currentY;
        int targetZ = currentZ;
        String comment;

        if (args.length == 0) {
            sendUsageHelp(player);
            return true;
        }

        // Check if user entered manual coordinates
        // Case 1: 3 coordinates: X Y Z (e.g. /sharecoords 100 64 -200 База)
        Integer c0 = parseCoord(args[0], currentX);
        Integer c1 = args.length > 1 ? parseCoord(args[1], currentY) : null;
        Integer c2 = args.length > 2 ? parseCoord(args[2], currentZ) : null;

        if (c0 != null && c1 != null && c2 != null) {
            // User provided 3 coordinates
            if (args.length == 3) {
                player.sendMessage(ChatColor.RED + "❌ Помилка: ви обов'язково маєте вказати опис місця до цих координат!");
                player.sendMessage(ChatColor.GRAY + "Приклад: " + ChatColor.YELLOW + "/" + label + " " + c0 + " " + c1 + " " + c2 + " База в горах");
                return true;
            }
            targetX = c0;
            targetY = c1;
            targetZ = c2;
            comment = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        } else if (c0 != null && c1 != null && c2 == null && args.length >= 2) {
            // Case 2: 2 coordinates: X Z (e.g. /sharecoords 100 -200 База)
            if (args.length == 2) {
                player.sendMessage(ChatColor.RED + "❌ Помилка: ви обов'язково маєте вказати опис місця до цих координат!");
                player.sendMessage(ChatColor.GRAY + "Приклад: " + ChatColor.YELLOW + "/" + label + " " + c0 + " " + c1 + " База в горах");
                return true;
            }
            targetX = c0;
            targetY = currentY;
            targetZ = c1;
            comment = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        } else {
            // Case 3: Automatic player coordinates, entire argument line is the comment
            comment = String.join(" ", args);
        }

        if (comment.trim().isEmpty()) {
            sendUsageHelp(player);
            return true;
        }

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
        String fullUrl = String.format("%s#%s:%d:%d:%d:30:0:0:0:0:flat", 
                mapUrl, worldName, targetX, targetY, targetZ);

        // In-game broadcast components
        String header = ChatColor.GOLD + "📍 Гравець " + ChatColor.YELLOW + player.getName() + ChatColor.GOLD + " поділився координатами: " + ChatColor.WHITE + comment;
        String coordsPart = String.format("§7Координати: §eX: %d, Y: %d, Z: %d §7(%s) ", 
                targetX, targetY, targetZ, dimension);

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
        final int finalX = targetX;
        final int finalY = targetY;
        final int finalZ = targetZ;
        final String finalComment = comment;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sendCoordsToDiscord(player, finalComment, finalX, finalY, finalZ, dimension, fullUrl);
        });

        return true;
    }

    private void sendUsageHelp(Player player) {
        player.sendMessage(ChatColor.RED + "❌ Помилка: ви обов'язково маєте вказати назву або опис місця!");
        player.sendMessage(ChatColor.GRAY + "Способи використання:");
        player.sendMessage(ChatColor.YELLOW + "  • /sharecoords <опис> " + ChatColor.GRAY + "— поділитися поточною позицією");
        player.sendMessage(ChatColor.YELLOW + "  • /sharecoords <X> <Y> <Z> <опис> " + ChatColor.GRAY + "— вказати точні координати");
        player.sendMessage(ChatColor.YELLOW + "  • /sharecoords <X> <Z> <опис> " + ChatColor.GRAY + "— вказати X та Z координати");
        player.sendMessage(ChatColor.GRAY + "Приклад: " + ChatColor.WHITE + "/sharecoords Моя база" + ChatColor.GRAY + " або " + ChatColor.WHITE + "/sharecoords 120 64 -350 База");
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
        if (!(sender instanceof Player player)) return Collections.emptyList();

        Location loc = player.getLocation();
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList(
                    String.valueOf(loc.getBlockX()),
                    "~",
                    "База", "Дім", "Портал", "Спавн", "Шахта", "Скарбниця"
            ));
            return filter(suggestions, args[0]);
        } else if (args.length == 2) {
            Integer c0 = parseCoord(args[0], loc.getBlockX());
            if (c0 != null) {
                List<String> suggestions = new ArrayList<>(Arrays.asList(
                        String.valueOf(loc.getBlockY()),
                        String.valueOf(loc.getBlockZ()),
                        "~"
                ));
                return filter(suggestions, args[1]);
            }
        } else if (args.length == 3) {
            Integer c0 = parseCoord(args[0], loc.getBlockX());
            Integer c1 = parseCoord(args[1], loc.getBlockY());
            if (c0 != null && c1 != null) {
                List<String> suggestions = new ArrayList<>(Arrays.asList(
                        String.valueOf(loc.getBlockZ()),
                        "~",
                        "База", "Дім", "Портал", "Спавн", "Шахта"
                ));
                return filter(suggestions, args[2]);
            }
        } else if (args.length == 4) {
            Integer c0 = parseCoord(args[0], loc.getBlockX());
            Integer c1 = parseCoord(args[1], loc.getBlockY());
            Integer c2 = parseCoord(args[2], loc.getBlockZ());
            if (c0 != null && c1 != null && c2 != null) {
                List<String> suggestions = Arrays.asList("База", "Дім", "Портал", "Шахта", "Скарбниця", "Спавн");
                return filter(suggestions, args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}
