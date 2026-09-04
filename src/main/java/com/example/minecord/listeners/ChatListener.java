package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final MineCord plugin;

    public ChatListener(MineCord plugin) {
        this.plugin = plugin;
    }

    // FIX: Використовуємо MONITOR з ignoreCancelled=false, щоб:
    // - Перехопити повідомлення ПІСЛЯ всіх інших плагінів (антиспам тощо)
    // - Все одно виконати нашу перевірку (мут, спам, ШІ) навіть якщо хтось вже скасував
    // Модерація (мут/спам/ШІ) відміняє подію сама, тому нам потрібно її бачити
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Якщо повідомлення вже скасовано іншим плагіном — не відправляємо в Discord, але ШІ-перевірку все одно пропускаємо
        boolean alreadyCancelled = event.isCancelled();

        // 1. Перевірка на мут
        if (plugin.getAntiSpamManager().isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            long remaining = plugin.getAntiSpamManager().getMuteRemainingSeconds(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage("§cВи замучені за спам. Залишилося: " + remaining + " сек.");
            return;
        }

        // 2. Перевірка на спам
        if (!alreadyCancelled) {
            int spamLevel = plugin.getAntiSpamManager().checkSpamLevel(event.getPlayer().getUniqueId(), event.getMessage());
            if (spamLevel == 1) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cЗачекайте перед відправкою наступного повідомлення (або не повторюйтесь)!");
                return;
            } else if (spamLevel == 2) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cВас замучено на 5 хвилин за спам у чаті!");
                return;
            }
        }

        // 3. Якщо повідомлення вже скасовано іншим плагіном — не відправляємо в Discord
        if (alreadyCancelled) return;

        if (plugin.getConfig().getBoolean("ai-moderator.enabled", true)) {
            String message = event.getMessage();
            org.bukkit.entity.Player player = event.getPlayer();

            // Запускаємо перевірку ШІ повністю в фоні, щоб не було інпут лагу
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    boolean isToxic = plugin.getOpenAIModerator().isMessageToxic(message).join();
                    if (isToxic) {
                        // Повідомляємо модераторів у Discord замість автоматичного муту
                        if (plugin.getBotManager() != null) {
                            String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id");
                            if (modChannelId != null && !modChannelId.isEmpty()) {
                                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(modChannelId);
                                if (channel != null) {
                                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                                    embed.setTitle("⚠️ Підозра на серйозне порушення чату");
                                    embed.setColor(0xFF0000);
                                    embed.addField("Гравець", player.getName(), true);
                                    embed.addField("Повідомлення", message, false);
                                    embed.setFooter("Автоматично виявлено AI-Модератором");
                                    channel.sendMessageEmbeds(embed.build()).queue();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Помилка при перевірці чату: " + e.getMessage());
                }
            });
        }

        // Якщо все добре — готуємо повідомлення
        String playerName = event.getPlayer().getName();
        String originalMessage = event.getMessage();
        String discordMessage = originalMessage;

        // Шукаємо пінги в повідомленні (формат @Нікнейм)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_]{3,16})");
        java.util.regex.Matcher matcher = pattern.matcher(originalMessage);
        
        String minecraftMessage = originalMessage;
        
        while (matcher.find()) {
            String targetName = matcher.group(1);
            org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayerExact(targetName);
            
            if (targetPlayer != null) {
                // Підсвічуємо в Minecraft (тільки поточне співпадіння)
                minecraftMessage = minecraftMessage.replace("@" + targetName, org.bukkit.ChatColor.YELLOW + "@" + targetName + org.bukkit.ChatColor.RESET);
                
                // Відтворюємо звук (використовуємо runTask бо ми в async)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    targetPlayer.playSound(targetPlayer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                });
                
                // Перетворюємо на Discord пінг, якщо гравець прив'язав акаунт
                String discordId = plugin.getLinkManager().getDiscordId(targetPlayer.getUniqueId());
                if (discordId != null) {
                    discordMessage = discordMessage.replace("@" + targetName, "<@" + discordId + ">");
                }
            }
        }
        
        event.setMessage(minecraftMessage);

        // Відправляємо в Discord
        if (plugin.getBotManager() != null && plugin.getBotManager().getWebhookManager() != null) {
            plugin.getBotManager().getWebhookManager().sendMessage(playerName, discordMessage);
        }
    }
}
