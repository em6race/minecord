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

            // AsyncPlayerChatEvent вже викликається асинхронно від головного потоку,
            // тому .join() блокує лише цей async-потік, а не сервер
            boolean isToxic = false;
            try {
                isToxic = plugin.getOpenAIModerator().isMessageToxic(message).join();
            } catch (Exception e) {
                plugin.getLogger().warning("Помилка при перевірці чату: " + e.getMessage());
            }

            if (isToxic) {
                event.setCancelled(true);

                // Зберігаємо заблоковане повідомлення для можливої апеляції
                plugin.getOpenAIModerator().setLastBlockedMessage(event.getPlayer().getUniqueId(), message);

                String warnMsg = plugin.getConfig().getString("ai-moderator.warn-message", "§cВаше повідомлення видалено модератором!");
                event.getPlayer().sendMessage(warnMsg);
                event.getPlayer().sendMessage("§7Твоє повідомлення: §c" + message);

                // Клікабельна кнопка апеляції
                net.md_5.bungee.api.chat.TextComponent appealBtn = new net.md_5.bungee.api.chat.TextComponent("§e§n[Натисніть тут, якщо повідомлення заблоковано помилково]");
                appealBtn.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/minecord appeal"));
                appealBtn.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("Відправити адміністрації на перевірку")));
                event.getPlayer().spigot().sendMessage(appealBtn);

                if (plugin.getConfig().getBoolean("ai-moderator.kick-player", false)) {
                    String kickMsg = plugin.getConfig().getString("ai-moderator.kick-message", "§cПорушення правил чату!");
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        event.getPlayer().kickPlayer(kickMsg);
                    });
                }

                return;
            }
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
