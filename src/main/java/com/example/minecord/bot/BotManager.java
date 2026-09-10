package com.example.minecord.bot;

import com.example.minecord.MineCord;
import com.example.minecord.utils.WebhookManager;
import com.example.minecord.utils.ConsoleManager;
import com.example.minecord.utils.SkinHelper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.bukkit.Bukkit;

public class BotManager {
    private final MineCord plugin;
    private JDA jda;
    private WebhookManager webhookManager;
    private ConsoleManager consoleManager;
    private int statusTaskId = -1;

    public BotManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String token = plugin.getConfig().getString("discord.token");
        if (token == null || token.equals("YOUR_DISCORD_BOT_TOKEN_HERE") || token.isEmpty()) {
            plugin.getLogger().warning("Будь ласка, вкажіть токен бота в config.yml!");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(new DiscordCommandListener(plugin))
                        .addEventListeners(new DiscordChatListener(plugin))
                        //.addEventListeners(new DiscordTicketListener(plugin))
                        .build();
                
                jda.awaitReady();
                plugin.getLogger().info("Бот підключений як " + jda.getSelfUser().getName());

                // Register slash commands
                jda.updateCommands().addCommands(
                        Commands.slash("help", "Показує список всіх доступних команд бота"),
                        Commands.slash("online", "Список гравців"),

                        Commands.slash("map", "Отримати посилання на веб-мапу сервера"),
                        Commands.slash("link", "Прив'язати акаунт Minecraft до Discord")
                                .addOption(OptionType.STRING, "code", "4-значний код з гри", true),
                        Commands.slash("maintenance", "Увімкнути/вимкнути режим технічних робіт")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                                .addOption(OptionType.BOOLEAN, "enabled", "Увімкнути (True) чи Вимкнути (False)", true),
                        //Commands.slash("ticket", "Ticket settings")
                        //        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        //        .addSubcommands(
                        //                new SubcommandData("setup", "Create a 'Create Ticket' button in this channel")
                        //        ),
                        Commands.slash("autorestart", "Управління авторестартами сервера")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                                .addSubcommands(
                                        new SubcommandData("add", "Додати час (наприклад, 04:00)")
                                                .addOption(OptionType.STRING, "time", "Час у форматі HH:mm", true),
                                        new SubcommandData("remove", "Видалити час")
                                                .addOption(OptionType.STRING, "time", "Час у форматі HH:mm", true),
                                        new SubcommandData("list", "Список авторестартів"),
                                        new SubcommandData("clear", "Очистити всі авторестарти"),
                                        new SubcommandData("toggle", "Призупинити/відновити всі авторестарти")
                                ),

                        Commands.slash("queuerestart", "Одноразовий рестарт у чергу при 0 онлайну (повтор команди скасовує)")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                        Commands.slash("stats", "Статистика сервера або гравця")
                                .addOption(OptionType.STRING, "player", "Нікнейм гравця", false, true),
                        Commands.slash("top", "Рейтинг найкращих гравців сервера")
                                .addOptions(new net.dv8tion.jda.api.interactions.commands.build.OptionData(OptionType.STRING, "category", "Категорія рейтингу", false)
                                        .addChoice("⏱️ Награний час", "time")
                                        .addChoice("⚔️ Вбито мобів", "kills")
                                        .addChoice("💀 Смертей", "deaths")
                                        .addChoice("💎 Добуто алмазів", "diamonds")
                                        .addChoice("⛏️ Зламано блоків", "blocks")),
                        Commands.slash("linkadmin", "Примусово прив'язати гравця до Discord (адміни)")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                                .addOption(OptionType.STRING, "player", "Нікнейм гравця в Minecraft", true, true)
                                .addOption(OptionType.USER, "user", "Користувач Discord", true)
                ).queue();

                // Initialize Webhook for chat bridge
                webhookManager = new WebhookManager(plugin);
                String channelId = plugin.getConfig().getString("discord.chat-channel-id");
                if (channelId != null && !channelId.equals("000000000000000000") && !channelId.isEmpty()) {
                    TextChannel channel = jda.getTextChannelById(channelId);
                    if (channel != null) {
                        webhookManager.initialize(channel);
                    } else {
                        plugin.getLogger().warning("Не знайдено канал чату за ID: " + channelId);
                    }
                }

                // Initialize console logger
                consoleManager = new ConsoleManager(plugin);
                consoleManager.start();

                // Start status updater
                if (plugin.getConfig().getBoolean("status.enabled", true)) {
                    startStatusUpdater();
                }

                // Send startup notification
                String startMsg = plugin.getConfig().getString("events.server-start", "✅ **Сервер успішно запущено! Можна заходити!**");
                if (startMsg != null && !startMsg.isEmpty()) {
                    sendSystemEmbed(startMsg, 0x00FF00, null);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Помилка бота: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        if (statusTaskId != -1) {
            Bukkit.getScheduler().cancelTask(statusTaskId);
            statusTaskId = -1;
        }
        
        // Send shutdown notification (synchronously so it completes before process exits)
        if (jda != null) {
            try {
                String stopMsg = plugin.getConfig().getString("events.server-stop", "🛑 **Сервер вимкнено!**");
                if (stopMsg != null && !stopMsg.isEmpty()) {
                    sendSystemEmbedSync(stopMsg, 0xFF0000, null);
                }
            } catch (Throwable e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to send stop embed", e);
            }
        }

        if (consoleManager != null) {
            consoleManager.stop();
        }
        if (webhookManager != null) {
            webhookManager.close();
        }
        if (jda != null) {
            try {
                jda.shutdown();
            } catch (Throwable e) {
                plugin.getLogger().warning("Помилка під час вимкнення JDA: " + e.getMessage());
            }
        }
    }

    public JDA getJda() { return jda; }
    public WebhookManager getWebhookManager() { return webhookManager; }
    public ConsoleManager getConsoleManager() { return consoleManager; }

    private int statusIndex = 0;
    private String lastStatusText = "";

    private void startStatusUpdater() {
        int interval = plugin.getConfig().getInt("status.update-interval-seconds", 15);
        if (interval < 5) interval = 5;

        statusTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (jda == null) return;
            
            boolean isMaintenance = plugin.getConfig().getBoolean("maintenance.enabled", false);
            String statusText;
            
            if (isMaintenance) {
                statusText = "🛠️ Сервер на тестуванні";
            } else {
                java.util.List<String> messages = plugin.getConfig().getStringList("status.messages");
                String template;
                if (messages != null && !messages.isEmpty()) {
                    if (statusIndex >= messages.size()) statusIndex = 0;
                    template = messages.get(statusIndex);
                    statusIndex = (statusIndex + 1) % messages.size();
                } else {
                    template = plugin.getConfig().getString("status.text", "Грає в Minecraft (%online%/%max%)");
                }

                int online = Bukkit.getOnlinePlayers().size();
                int max = Bukkit.getMaxPlayers();
                double tps = 20.0;
                try {
                    tps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
                } catch (Throwable ignored) {}
                String tpsStr = String.format(java.util.Locale.US, "%.1f", tps);

                long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
                long totalMins = uptimeMs / (60 * 1000L);
                long hours = totalMins / 60;
                long mins = totalMins % 60;
                String uptimeStr = hours > 0 ? (hours + "г " + mins + "хв") : (mins + "хв");

                statusText = template.replace("%online%", String.valueOf(online))
                                     .replace("%max%", String.valueOf(max))
                                     .replace("%tps%", tpsStr)
                                     .replace("%uptime%", uptimeStr);
            }
            
            // Update presence only if changed to avoid Discord rate limits
            if (!statusText.equals(lastStatusText)) {
                jda.getPresence().setActivity(Activity.playing(statusText));
                lastStatusText = statusText;
            }
        }, 0L, interval * 20L); // 20 ticks = 1 second
    }
    
    // Helper method for sending system messages (deaths, joins, etc.)
    public void sendSystemMessage(String message) {
        if (jda == null) return;
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId != null && !channelId.equals("000000000000000000") && !channelId.trim().isEmpty()) {
            try {
                TextChannel channel = jda.getTextChannelById(channelId.trim());
                if (channel != null) {
                    channel.sendMessage(message).queue();
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Невірний формат chat-channel-id: " + channelId);
            }
        }
    }
    
    // Send message as Discord Embed card
    public void sendSystemEmbed(String text, int color, String playerName) {
        if (jda == null) return;
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId != null && !channelId.equals("000000000000000000") && !channelId.trim().isEmpty()) {
            try {
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId.trim());
                if (channel != null) {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setColor(color);
                    
                    if (playerName != null && !playerName.isEmpty()) {
                        String avatarUrl = SkinHelper.getAvatarUrl(playerName);
                        embed.setAuthor(text, null, avatarUrl);
                    } else {
                        embed.setDescription(text);
                    }
                    channel.sendMessageEmbeds(embed.build()).queue();
                }
            } catch (Throwable e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Не вдалося відправити Embed", e);
            }
        }
    }

    // Synchronous delivery to ensure message is delivered before shutdown
    public void sendSystemEmbedSync(String text, int color, String playerName) {
        if (jda == null) return;
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId != null && !channelId.equals("000000000000000000") && !channelId.trim().isEmpty()) {
            try {
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId.trim());
                if (channel != null) {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setColor(color);
                    
                    if (playerName != null && !playerName.isEmpty()) {
                        String avatarUrl = SkinHelper.getAvatarUrl(playerName);
                        embed.setAuthor(text, null, avatarUrl);
                    } else {
                        embed.setDescription(text);
                    }
                    channel.sendMessageEmbeds(embed.build()).complete();
                }
            } catch (Throwable e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Не вдалося відправити Embed синхронно", e);
            }
        }
    }
}
