package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import com.example.minecord.utils.SkinHelper;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final MineCord plugin;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{3,16})(?![a-zA-Z0-9_])");

    public ChatListener(MineCord plugin) {
        this.plugin = plugin;
    }

    private static final Map<String, List<String>> recentMessages = new ConcurrentHashMap<>();

    public static List<String> getRecentMessages(String playerName) {
        if (playerName == null) return Collections.emptyList();
        List<String> list = recentMessages.get(playerName.toLowerCase());
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChatModerationAndFormat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean canBypass = player.isOp() || player.hasPermission("minecord.antispam.bypass");

        // 1. Mute check
        if (!canBypass && plugin.getAntiSpamManager().isMuted(uuid)) {
            event.setCancelled(true);
            long remaining = plugin.getAntiSpamManager().getMuteRemainingSeconds(uuid);
            player.sendMessage("§cВи замучені за спам. Залишилося: " + remaining + " сек.");
            return;
        }

        // 2. Spam check
        if (!canBypass) {
            int spamLevel = plugin.getAntiSpamManager().checkSpamLevel(uuid, event.getMessage());
            if (spamLevel == 1) {
                event.setCancelled(true);
                player.sendMessage("§cЗачекайте перед відправкою наступного повідомлення!");
                return;
            } else if (spamLevel == 2) {
                event.setCancelled(true);
                long muteSec = plugin.getConfig().getLong("antispam.mute-duration-seconds", 60);
                player.sendMessage("§cВас замучено на " + muteSec + " сек. за спам у чаті!");
                return;
            }
        }

        // 3. Process in-game mentions (@Nickname) safely without substring collisions
        String originalMessage = event.getMessage();
        Matcher matcher = MENTION_PATTERN.matcher(originalMessage);
        StringBuilder mcSb = new StringBuilder();
        Set<Player> alertedPlayers = new HashSet<>();

        while (matcher.find()) {
            String targetName = matcher.group(1);
            Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
            if (targetPlayer != null) {
                String replacement = ChatColor.YELLOW + "@" + targetPlayer.getName() + ChatColor.RESET;
                matcher.appendReplacement(mcSb, Matcher.quoteReplacement(replacement));
                alertedPlayers.add(targetPlayer);
            } else {
                matcher.appendReplacement(mcSb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(mcSb);
        event.setMessage(mcSb.toString());

        if (!alertedPlayers.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (Player targetPlayer : alertedPlayers) {
                    if (targetPlayer.isOnline()) {
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                }
            });
        }

        // 4. Role prefix formatting (escape % to prevent UnknownFormatConversionException)
        if (plugin.getConfig().getBoolean("role-sync.chat-prefix", true)) {
            String rolePrefix = (plugin.getRoleSyncManager() != null) ? plugin.getRoleSyncManager().getRolePrefix(player) : "";
            if (rolePrefix != null && !rolePrefix.isEmpty()) {
                String safePrefix = rolePrefix.replace("%", "%%");
                event.setFormat(safePrefix + "%1$s" + ChatColor.GRAY + ": " + ChatColor.WHITE + "%2$s");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChatMonitor(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerUuid = player.getUniqueId();
        String rawMessage = ChatColor.stripColor(event.getMessage());

        // Record message for moderation / report history
        recentMessages.compute(playerName.toLowerCase(), (k, v) -> {
            if (v == null) v = Collections.synchronizedList(new ArrayList<>());
            synchronized (v) {
                v.add(rawMessage);
                if (v.size() > 5) v.remove(0);
            }
            return v;
        });

        // AI Moderator background check
        if (plugin.getConfig().getBoolean("ai-moderator.enabled", false)) {
            plugin.getOpenAIModerator().isMessageToxic(rawMessage).thenAccept(isToxic -> {
                if (!Boolean.TRUE.equals(isToxic)) return;

                plugin.getOpenAIModerator().setLastBlockedMessage(playerUuid, rawMessage);

                // Notify moderators in Discord
                if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
                    String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
                    if (modChannelId != null && !modChannelId.isEmpty() && !modChannelId.equals("000000000000000000")) {
                        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(modChannelId);
                        if (channel != null) {
                            net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                            embed.setTitle("⚠️ Підозра на серйозне порушення чату");
                            embed.setColor(0xFF0000);
                            embed.addField("Гравець", playerName, true);
                            String msgField = rawMessage.length() > 1000 ? rawMessage.substring(0, 1000) + "..." : rawMessage;
                            embed.addField("Повідомлення", msgField, false);
                            embed.setFooter("Автоматично виявлено AI-Модератором");
                            channel.sendMessageEmbeds(embed.build()).queue();
                        }
                    }
                }
            }).exceptionally(e -> {
                plugin.getLogger().warning("Помилка при перевірці чату: " + e.getMessage());
                return null;
            });
        }

        // Convert @Nickname mentions to <@discordId> for Discord
        Matcher matcher = MENTION_PATTERN.matcher(rawMessage);
        StringBuilder dcSb = new StringBuilder();
        while (matcher.find()) {
            String targetName = matcher.group(1);
            Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
            String replacement = matcher.group(0);
            if (targetPlayer != null) {
                String discordId = plugin.getLinkManager().getDiscordId(targetPlayer.getUniqueId());
                if (discordId != null) {
                    replacement = "<@" + discordId + ">";
                }
            }
            matcher.appendReplacement(dcSb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(dcSb);

        // Send to Discord via Webhook
        if (plugin.getBotManager() != null && plugin.getBotManager().getWebhookManager() != null) {
            String avatarUrl = SkinHelper.getAvatarUrl(player);
            plugin.getBotManager().getWebhookManager().sendMessage(playerName, dcSb.toString(), avatarUrl);
        }
    }
}
