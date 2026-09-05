package com.example.minecord.bot;

import com.example.minecord.MineCord;
import com.example.minecord.utils.WebhookManager;
import com.example.minecord.utils.ConsoleManager;
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

                // Реєстрація команд
                jda.updateCommands().addCommands(
                        Commands.slash("help", "Показує список всіх доступних команд бота"),
                        Commands.slash("online", "Список гравців"),

                        Commands.slash("map", "Отримати посилання на веб-мапу сервера"),
                        Commands.slash("link", "Прив'язати акаунт Minecraft до Discord")
                                .addOption(OptionType.STRING, "code", "4-значний код з гри", true),
                        Commands.slash("maintenance", "Увімкнути/вимкнути режим технічних робіт")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                                .addOption(OptionType.BOOLEAN, "enabled", "Увімкнути (True) чи Вимкнути (False)", true),
                        //Commands.slash("ticket", "Налаштування заявок")
                        //        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        //        .addSubcommands(
                        //                new SubcommandData("setup", "Створити кнопку 'Подати заявку' в цьому каналі")
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
                        Commands.slash("consolefilter", "Фільтр логів консолі")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                                .addOption(OptionType.BOOLEAN, "info", "Показувати INFO логи", true),
                        Commands.slash("stats", "Статистика сервера або гравця")
                                .addOption(OptionType.STRING, "player", "Нікнейм гравця", false),
                        Commands.slash("linkadmin", "Примусово прив'язати гравця до Discord (адміни)")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                                .addOption(OptionType.STRING, "player", "Нікнейм гравця в Minecraft", true)
                                .addOption(OptionType.USER, "user", "Користувач Discord", true)
                ).queue();

                // Ініціалізація Webhook для чату
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

                // Ініціалізація консолі
                consoleManager = new ConsoleManager(plugin);
                consoleManager.start();

                // Запуск оновлення статусу
                if (plugin.getConfig().getBoolean("status.enabled", true)) {
                    startStatusUpdater();
                }

                // Відправка повідомлення про запуск
                String startMsg = plugin.getConfig().getString("events.server-start");
                if (startMsg != null && !startMsg.isEmpty()) {
                    sendSystemEmbed(startMsg, 0x00FF00, null);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Помилка бота: " + e.getMessage());
            }
        });
    }

    public void stop() {
        if (statusTaskId != -1) {
            Bukkit.getScheduler().cancelTask(statusTaskId);
        }
        
        // Відправка повідомлення про вимкнення (синхронно, щоб встигло дійти)
        if (jda != null) {
            String stopMsg = plugin.getConfig().getString("events.server-stop");
            if (stopMsg != null && !stopMsg.isEmpty()) {
                sendSystemEmbedSync(stopMsg, 0xFF0000, null);
            }
        }

        if (consoleManager != null) {
            consoleManager.stop();
        }
        if (webhookManager != null) {
            webhookManager.close();
        }
        if (jda != null) {
            jda.shutdown();
        }
    }

    public JDA getJda() { return jda; }
    public WebhookManager getWebhookManager() { return webhookManager; }
    public ConsoleManager getConsoleManager() { return consoleManager; }

    private String lastStatusText = "";

    private void startStatusUpdater() {
        int interval = plugin.getConfig().getInt("status.update-interval-seconds", 60);
        String format = plugin.getConfig().getString("status.text", "Грає в Minecraft (%online%/%max%)");

        statusTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (jda == null) return;
            
            boolean isMaintenance = plugin.getConfig().getBoolean("maintenance.enabled", false);
            String statusText;
            
            if (isMaintenance) {
                statusText = "🛠️ Сервер на тестуванні";
            } else {
                int online = Bukkit.getOnlinePlayers().size();
                int max = Bukkit.getMaxPlayers();
                statusText = format.replace("%online%", String.valueOf(online))
                                          .replace("%max%", String.valueOf(max));
            }
            
            // Оновлюємо статус лише якщо він змінився (щоб уникнути лімітів Discord API)
            if (!statusText.equals(lastStatusText)) {
                jda.getPresence().setActivity(Activity.playing(statusText));
                lastStatusText = statusText;
            }
        }, 0L, interval * 20L); // 20 ticks = 1 second
    }
    
    // Допоміжний метод для відправки системних повідомлень (смерть, вхід тощо)
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
    
    // Відправка повідомлення у вигляді красивого Embed (картки)
    public void sendSystemEmbed(String text, int color, String playerName) {
        if (jda == null) return;
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId != null && !channelId.equals("000000000000000000") && !channelId.trim().isEmpty()) {
            try {
                TextChannel channel = jda.getTextChannelById(channelId.trim());
                if (channel != null) {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setColor(color);
                
                if (playerName != null && !playerName.isEmpty()) {
                    String avatarUrl = "https://mc-heads.net/avatar/" + playerName + "/256";
                    embed.setAuthor(text, null, avatarUrl);
                } else {
                    embed.setDescription(text);
                }
                try {
                    channel.sendMessageEmbeds(embed.build()).queue();
                } catch (net.dv8tion.jda.api.exceptions.InsufficientPermissionException e) {
                    plugin.getLogger().warning("Бот не має прав для відправки Embed повідомлень у канал " + channelId);
                } catch (Exception e) {
                    plugin.getLogger().warning("Помилка відправки системного повідомлення: " + e.getMessage());
                }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Невірний формат chat-channel-id: " + channelId);
            }
        }
    }

    // Синхронна відправка, щоб гарантовано доставити повідомлення перед вимкненням
    private void sendSystemEmbedSync(String text, int color, String playerName) {
        if (jda == null) return;
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId != null && !channelId.equals("000000000000000000") && !channelId.trim().isEmpty()) {
            try {
                TextChannel channel = jda.getTextChannelById(channelId.trim());
                if (channel != null) {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setColor(color);
                
                if (playerName != null && !playerName.isEmpty()) {
                    String avatarUrl = "https://mc-heads.net/avatar/" + playerName + "/256";
                    embed.setAuthor(text, null, avatarUrl);
                } else {
                    embed.setDescription(text);
                }
                
                try {
                    channel.sendMessageEmbeds(embed.build()).complete();
                } catch (Exception ignored) {
                }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Невірний формат chat-channel-id: " + channelId);
            }
        }
    }
}
