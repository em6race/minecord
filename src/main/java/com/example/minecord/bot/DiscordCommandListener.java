package com.example.minecord.bot;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordCommandListener extends ListenerAdapter {

    private final MineCord plugin;

    public DiscordCommandListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            plugin.getLogger().info("[Discord] Користувач " + event.getUser().getName() + " використав команду: /" + event.getName());
            
            if (event.getName().equals("help")) {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setTitle("📚 Доступні команди бота MineCord:");
                embed.setColor(0x5865F2); // Discord blurple

            String commands = "🔹 `/online` — Показує список гравців на сервері\n" +
                              "🔹 `/map` — Отримати посилання на веб-мапу сервера\n" +
                              "🔹 `/link <code>` — Прив'язати акаунт Minecraft до Discord\n" +
                              "🔹 `/help` — Показує це повідомлення\n" +
                              "🔹 `/stats [гравець]` — Показати статистику сервера або гравця\n\n" +
                              "👑 **Команди адміністратора:**\n" +
                              "🔸 `/maintenance <увімкнути>` — Увімкнути/вимкнути режим технічних робіт\n" +
                              "🔸 `/autorestart <add|remove|list|clear|toggle>` — Управління авторестартами сервера\n" +
                              "🔸 `/consolefilter <info>` — Увімкнути/вимкнути INFO логи в каналі консолі\n" +
                              "🔸 `/linkadmin <гравець> <користувач>` — Примусово прив'язати гравця до Discord";
            embed.setDescription(commands);

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
        else if (event.getName().equals("online")) {
            event.deferReply().queue();

            int onlineCount = plugin.getServer().getOnlinePlayers().size();
            int maxPlayers = plugin.getServer().getMaxPlayers();

            net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();

            if (onlineCount == 0) {
                embed.setTitle("🔴 Наразі на сервері немає гравців (0/" + maxPlayers + ")");
                embed.setColor(0xFF0000);
            } else {
                embed.setTitle("🟢 Онлайн (" + onlineCount + "/" + maxPlayers + "):");
                embed.setColor(0x00FF00);

                StringBuilder playersList = new StringBuilder();
                for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                    playersList.append("`").append(player.getName()).append("` ");
                }
                embed.setDescription(playersList.toString());
            }

            event.getHook().editOriginalEmbeds(embed.build()).queue();
        }
        else if (event.getName().equals("map")) {
            event.reply("🗺️ **Веб-мапа сервера:**\n[Натисніть тут, щоб відкрити мапу](http://kozlomine.minecraft.how:27257/)").setEphemeral(true).queue();
        }
        else if (event.getName().equals("link")) {
            String code = event.getOption("code").getAsString();
            java.util.UUID uuid = plugin.getLinkManager().getUUIDFromCode(code);

            if (uuid == null) {
                event.reply("❌ Невірний або застарілий код! Введіть `/discord link` у грі ще раз.").setEphemeral(true).queue();
            } else {
                plugin.getLinkManager().linkAccount(code, event.getUser().getId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Отримуємо ім'я гравця (навіть якщо він вийшов з гри)
                    org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
                    String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Гравця";

                    event.reply("✅ Успіх! Ваш Discord акаунт успішно прив'язано до Minecraft-акаунта **" + playerName + "**.").setEphemeral(true).queue();

                    // Сповіщаємо гравця безпосередньо у грі, якщо він онлайн
                    Player onlinePlayer = plugin.getServer().getPlayer(uuid);
                    if (onlinePlayer != null) {
                        onlinePlayer.sendMessage(org.bukkit.ChatColor.GREEN + "✅ Ваш акаунт успішно прив'язано до Discord (" + event.getUser().getName() + ")!");
                    }
                });
            }
        }
        else if (event.getName().equals("maintenance")) {
            boolean enable = event.getOption("enabled").getAsBoolean();

            // Всі зміни стану сервера робимо в головному потоці Minecraft
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getConfig().set("maintenance.enabled", enable);
                plugin.saveConfig();

                if (enable) {
                    String kickMsg = plugin.getConfig().getString("maintenance.message", "🛠️ Сервер на тестуванні.");
                    kickMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&', kickMsg);

                    int kickedCount = 0;
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        // Якщо гравець не адмін (OP) і не має спеціального дозволу - кікаємо
                        if (!p.isOp() && !p.hasPermission("minecord.maintenance.bypass")) {
                            p.kickPlayer(kickMsg);
                            kickedCount++;
                        }
                    }
                    event.reply("🚧 Режим технічних робіт **УВІМКНЕНО**. Збережено в конфіг. Кікнуто звичайних гравців: " + kickedCount).queue();
                } else {
                    event.reply("✅ Режим технічних робіт **ВИМКНЕНО**. Збережено в конфіг. Сервер відкритий для всіх!").queue();
                }
            });
        }
        else if (event.getName().equals("autorestart")) {
            String sub = event.getSubcommandName();
            if (sub == null) return;

            com.example.minecord.utils.AutoRestartManager manager = plugin.getAutoRestartManager();

            if (sub.equals("add")) {
                String time = event.getOption("time").getAsString();
                if (time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    manager.addTime(time);
                    event.reply("✅ Час `" + time + "` успішно додано до авторестартів.").queue();
                } else {
                    event.reply("❌ Невірний формат часу! Використовуйте HH:mm (наприклад, 04:00 або 15:30)").setEphemeral(true).queue();
                }
            }
            else if (sub.equals("remove")) {
                String time = event.getOption("time").getAsString();
                if (manager.removeTime(time)) {
                    event.reply("✅ Час `" + time + "` видалено з розкладу.").queue();
                } else {
                    event.reply("❌ Такого часу немає в списку авторестартів.").setEphemeral(true).queue();
                }
            }
            else if (sub.equals("list")) {
                java.util.List<String> times = manager.getTimes();
                if (times.isEmpty()) {
                    event.reply("ℹ️ Список авторестартів порожній.").queue();
                } else {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setTitle("⏰ Заплановані авторестарти");
                    embed.setColor(0x00FFFF);
                    StringBuilder sb = new StringBuilder();
                    for (String t : times) {
                        sb.append("• `").append(t).append("`\n");
                    }
                    embed.setDescription(sb.toString());
                    embed.setFooter("Час вказано за Києвом");

                    event.replyEmbeds(embed.build()).queue();
                }
            }
            else if (sub.equals("clear")) {
                manager.clearTimes();
                event.reply("🗑️ Всі авторестарти повністю видалено.").queue();
            }
            else if (sub.equals("toggle")) {
                boolean isPaused = !manager.isPaused();
                manager.setPaused(isPaused);
                if (isPaused) {
                    event.reply("⏸️ **Авторестарти ПРИЗУПИНЕНО.** Сервер більше не буде автоматично перезавантажуватись.").queue();
                } else {
                    event.reply("▶️ **Авторестарти ВІДНОВЛЕНО.** Розклад знову працює.").queue();
                }
            }
        }
        else if (event.getName().equals("consolefilter")) {
            boolean showInfo = event.getOption("info").getAsBoolean();
            if (plugin.getBotManager().getConsoleManager() != null) {
                plugin.getBotManager().getConsoleManager().setShowInfo(showInfo);
                if (showInfo) {
                    event.reply("✅ Фільтр оновлено: тепер INFO логи **показуються**.").queue();
                } else {
                    event.reply("✅ Фільтр оновлено: тепер INFO логи **приховані**.").queue();
                }
            } else {
                event.reply("❌ Менеджер консолі не активний.").setEphemeral(true).queue();
            }
        }
        else if (event.getName().equals("stats")) {
            net.dv8tion.jda.api.interactions.commands.OptionMapping playerOpt = event.getOption("player");
            if (playerOpt == null) {
                // Server stats
                double currentTps = 20.0;
                try {
                    currentTps = plugin.getServer().getTPS()[0];
                } catch (Exception ignored) {}

                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                double ramPercent = ((double) usedMemory / maxMemory) * 100.0;
                
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setTitle("📊 Статистика сервера");
                embed.setColor(0x00FF00);
                embed.addField("TPS", String.format("%.2f", currentTps), true);
                embed.addField("RAM (Використано)", String.format("%.2f%% (%.0f MB)", ramPercent, usedMemory / 1024.0 / 1024.0), true);
                embed.addField("RAM (Виділено)", String.format("%.0f MB", maxMemory / 1024.0 / 1024.0), true);
                
                event.replyEmbeds(embed.build()).queue();
            } else {
                // Player stats
                String playerName = playerOpt.getAsString();
                org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
                
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    event.reply("❌ Гравця з ніком **" + playerName + "** не знайдено на сервері (або він ніколи не заходив).").setEphemeral(true).queue();
                    return;
                }
                
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setTitle("📊 Статистика гравця " + offlinePlayer.getName());
                
                if (offlinePlayer.isOnline()) {
                    org.bukkit.entity.Player p = offlinePlayer.getPlayer();
                    embed.setColor(0x00FF00); // Green
                    embed.setDescription("🟢 **Статус:** Онлайн");
                    
                    int ping = p.getPing();
                    double health = p.getHealth();
                    int food = p.getFoodLevel();
                    int level = p.getLevel();
                    
                    int deaths = p.getStatistic(org.bukkit.Statistic.DEATHS);
                    int mobKills = p.getStatistic(org.bukkit.Statistic.MOB_KILLS);
                    int playerKills = p.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                    
                    long playtimeTicks = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                    long playtimeHours = playtimeTicks / (20 * 60 * 60);
                    long playtimeMins = (playtimeTicks / (20 * 60)) % 60;
                    
                    embed.addField("📶 Пінг", ping + " ms", true);
                    embed.addField("❤️ Здоров'я", String.format("%.1f/20", health), true);
                    embed.addField("🍗 Ситість", food + "/20", true);
                    
                    embed.addField("🌟 Рівень", level + " lvl", true);
                    embed.addField("☠️ Смертей", String.valueOf(deaths), true);
                    embed.addField("⚔️ Вбивств (Мобів/Гравців)", mobKills + " / " + playerKills, true);
                    
                    embed.addField("⏱️ Награний час", playtimeHours + " год. " + playtimeMins + " хв.", true);
                    
                    org.bukkit.Location loc = p.getLocation();
                    embed.addField("🗺️ Локація", loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")", false);
                } else {
                    embed.setColor(0xFF0000); // Red
                    embed.setDescription("🔴 **Статус:** Офлайн");
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    embed.addField("🕒 Останній вхід", sdf.format(new java.util.Date(offlinePlayer.getLastPlayed())), false);
                    embed.addField("📅 Перший вхід", sdf.format(new java.util.Date(offlinePlayer.getFirstPlayed())), false);
                }
                
                event.replyEmbeds(embed.build()).queue();
            }
        }
        else if (event.getName().equals("linkadmin")) {
            String playerName = event.getOption("player").getAsString();
            net.dv8tion.jda.api.entities.User discordUser = event.getOption("user").getAsUser();
            
            org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                event.reply("❌ Гравця **" + playerName + "** не знайдено на сервері.").setEphemeral(true).queue();
                return;
            }
            
            plugin.getLinkManager().linkAccountDirectly(offlinePlayer.getUniqueId(), discordUser.getId());
            event.reply("✅ Акаунт Minecraft **" + offlinePlayer.getName() + "** успішно прив'язано до Discord " + discordUser.getAsMention() + "!").queue();
        }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Сталася непередбачувана помилка при виконанні команди /" + event.getName(), e);
            event.reply("❌ Внутрішня помилка бота при виконанні команди. Перевірте консоль.").setEphemeral(true).queue(
                success -> {},
                error -> {
                    // Якщо deferReply вже було викликано, reply() видасть помилку, тому оновлюємо оригінальне повідомлення
                    event.getHook().sendMessage("❌ Внутрішня помилка бота при виконанні команди. Перевірте консоль.").setEphemeral(true).queue();
                }
            );
        }
    }
}
