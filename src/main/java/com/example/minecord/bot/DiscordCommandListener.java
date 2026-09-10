package com.example.minecord.bot;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import java.util.List;
import java.util.ArrayList;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordCommandListener extends ListenerAdapter {

    private final MineCord plugin;
    private static final org.bukkit.Material[] BLOCK_MATERIALS = java.util.Arrays.stream(org.bukkit.Material.values())
            .filter(org.bukkit.Material::isBlock)
            .toArray(org.bukkit.Material[]::new);
    private static final org.bukkit.Material[] ITEM_MATERIALS = java.util.Arrays.stream(org.bukkit.Material.values())
            .filter(org.bukkit.Material::isItem)
            .toArray(org.bukkit.Material[]::new);

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
                              "🔹 `/stats [гравець]` — Показати свою статистику або статистику гравця\n" +
                              "🔹 `/serverinfo` — Інформація та стан сервера (TPS, RAM, онлайн)\n" +
                              "🔹 `/top [категорія]` — Топ-10 гравців (час, вбивства, смерті, алмази, блоки)\n\n" +
                              "👑 **Команди адміністратора:**\n" +
                              "🔸 `/maintenance <увімкнути>` — Увімкнути/вимкнути режим технічних робіт\n" +
                              "🔸 `/autorestart <add|remove|list|clear|toggle>` — Управління авторестартами сервера\n" +
                              "🔸 `/consolefilter <info>` — Увімкнути/вимкнути INFO логи в каналі консолі\n" +
                              "🔸 `/linkadmin <гравець> <користувач>` — Примусово прив'язати гравця до Discord";
            embed.setDescription(commands);

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
        else if (event.getName().equals("online")) {
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

            event.replyEmbeds(embed.build()).queue();
        }
        else if (event.getName().equals("map")) {
            String mapUrl = plugin.getConfig().getString("discord.map-url", "http://localhost:8123/");
            if (!mapUrl.endsWith("/")) mapUrl += "/";
            String fullUrl = mapUrl.contains("#") ? mapUrl : (mapUrl + "#world:0:0:0:1500:0:0:0:0:flat");
            event.reply("🗺️ **Веб-мапа сервера (2D Flat):**\n[Натисніть тут, щоб відкрити мапу](" + fullUrl + ")").setEphemeral(true).queue();
        }
        else if (event.getName().equals("link")) {
            String code = event.getOption("code").getAsString();
            java.util.UUID uuid = plugin.getLinkManager().getUUIDFromCode(code);

            if (uuid == null) {
                event.reply("❌ Невірний або застарілий код! Введіть `/discord link` у грі ще раз.").setEphemeral(true).queue();
            } else {
                plugin.getLinkManager().linkAccount(code, event.getUser().getId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Get player name (even if offline)
                    org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
                    String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Гравця";

                    event.reply("✅ Успіх! Ваш Discord акаунт успішно прив'язано до Minecraft-акаунта **" + playerName + "**.").setEphemeral(true).queue();

                    // Notify player directly in-game if online
                    Player onlinePlayer = plugin.getServer().getPlayer(uuid);
                    if (onlinePlayer != null) {
                        onlinePlayer.sendMessage(org.bukkit.ChatColor.GREEN + "✅ Ваш акаунт успішно прив'язано до Discord (" + event.getUser().getName() + ")!");
                    }
                });
            }
        }
        else if (event.getName().equals("maintenance")) {
            boolean enable = event.getOption("enabled").getAsBoolean();

            // Perform all server state changes on the main Minecraft thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getConfig().set("maintenance.enabled", enable);
                plugin.saveConfig();

                if (enable) {
                    String kickMsg = plugin.getConfig().getString("maintenance.message", "🛠️ Сервер на тестуванні.");
                    kickMsg = org.bukkit.ChatColor.translateAlternateColorCodes('&', kickMsg);

                    int kickedCount = 0;
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        // Kick players who are not OP and lack bypass permission
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
        else if (event.getName().equals("queuerestart")) {
            if (plugin.getAutoRestartManager() == null) {
                event.reply("❌ Менеджер авторестартів не активний.").setEphemeral(true).queue();
                return;
            }
            boolean isPending = plugin.getAutoRestartManager().toggleSmartRestart();
            if (isPending) {
                if (plugin.getServer().getOnlinePlayers().isEmpty()) {
                    event.reply("✅ На сервері немає гравців. Одноразовий рестарт розпочнеться за мить!").queue();
                } else {
                    event.reply("⏳ **Одноразовий** розумний рестарт додано в чергу.\n" +
                            "Він спрацює лише 1 раз, щойно онлайн опуститься до 0 гравців.\n" +
                            "*(Повторний виклик команди скасує чергу)*").queue();
                }
            } else {
                event.reply("❌ Одноразовий рестарт із черги **СКАСОВАНО**.").queue();
            }
        }

        else if (event.getName().equals("serverinfo")) {
            event.deferReply().queue();
            double currentTps = 20.0;
            try {
                currentTps = plugin.getServer().getTPS()[0];
            } catch (Exception ignored) {}

            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double ramPercent = ((double) usedMemory / maxMemory) * 100.0;

            int onlineCount = plugin.getServer().getOnlinePlayers().size();
            int maxPlayers = plugin.getServer().getMaxPlayers();

            net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
            embed.setTitle("🖥️ Інформація та стан сервера");
            embed.setColor(0x00FF00);

            String tpsStatus = currentTps >= 18.5 ? "🟢 Стабільний" : (currentTps >= 15.0 ? "🟡 Невелике навантаження" : "🔴 Лаги");
            embed.addField("📊 TPS", String.format("%.2f (%s)", currentTps, tpsStatus), true);
            embed.addField("👥 Онлайн", onlineCount + " / " + maxPlayers, true);
            embed.addField("💾 RAM (Використано)", String.format("%.1f%% (%.0f MB)", ramPercent, usedMemory / 1024.0 / 1024.0), true);
            embed.addField("📦 RAM (Виділено)", String.format("%.0f MB", maxMemory / 1024.0 / 1024.0), true);
            embed.addField("⚙️ Ядро", plugin.getServer().getVersion(), false);

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        }

        else if (event.getName().equals("stats")) {
            event.deferReply().queue();
            net.dv8tion.jda.api.interactions.commands.OptionMapping playerOpt = event.getOption("player");

            String resolvedPlayerName = null;
            java.util.UUID resolvedUuid = null;

            if (playerOpt != null) {
                resolvedPlayerName = playerOpt.getAsString();
            } else {
                java.util.UUID linkedUuid = plugin.getLinkManager().getUUIDFromDiscordId(event.getUser().getId());
                if (linkedUuid == null) {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setTitle("🔗 Прив'яжіть свій акаунт Minecraft");
                    embed.setColor(0x5865F2);
                    embed.setDescription("Щоб переглядати **власну статистику** без введення нікнейма, прив'яжіть свій Minecraft акаунт до Discord.\n");
                    embed.addField("🎮 Спосіб 1: Самостійно через гру",
                            "1. Зайдіть на сервер у Minecraft та введіть: `/discord link`\n" +
                            "2. Отримайте 4-значний код\n" +
                            "3. Введіть тут команду: `/link code: <ваш_код>`", false);
                    embed.addField("👑 Спосіб 2: Через адміністратора",
                            "Зверніться до адміністратора, щоб він прив'язав ваш акаунт командою `/linkadmin`.", false);
                    embed.addField("💡 Статистика іншого гравця",
                            "Ви також можете переглянути статистику будь-якого гравця за ніком:\n`/stats player: <нікнейм>`\n\n*Для інформації про стан сервера використовуйте:* `/serverinfo`", false);
                    embed.setFooter("MineCord • Статистика гравців");
                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                    return;
                }
                resolvedUuid = linkedUuid;
            }

            final String targetName = resolvedPlayerName;
            final java.util.UUID targetUuid = resolvedUuid;

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    org.bukkit.OfflinePlayer offlinePlayer;
                    if (targetUuid != null) {
                        offlinePlayer = plugin.getServer().getOfflinePlayer(targetUuid);
                    } else {
                        offlinePlayer = plugin.getServer().getOfflinePlayer(targetName);
                    }

                    if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                        String nameToShow = targetName != null ? targetName : (offlinePlayer.getName() != null ? offlinePlayer.getName() : "невідомий");
                        event.getHook().sendMessage("❌ Гравця з ніком **" + nameToShow + "** не знайдено на сервері (або він ніколи не заходив).").setEphemeral(true).queue();
                        return;
                    }

                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    String displayName = offlinePlayer.getName() != null ? offlinePlayer.getName() : (targetName != null ? targetName : "Гравець");
                    embed.setTitle("📊 Статистика гравця " + displayName);
                    embed.setThumbnail(com.example.minecord.utils.SkinHelper.getAvatarUrl(displayName));
                        
                    boolean isOnline = offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null;
                    org.bukkit.entity.Player onlineP = isOnline ? offlinePlayer.getPlayer() : null;

                    int playerLevel = 0;
                    if (plugin.getPlayerCacheManager() != null) {
                        playerLevel = plugin.getPlayerCacheManager().getPlayerLevel(offlinePlayer);
                    } else if (isOnline) {
                        playerLevel = onlineP.getLevel();
                    }

                    if (isOnline) {
                        embed.setColor(0x00FF00); // Green
                        embed.setDescription("🟢 **Статус:** Онлайн");
                        embed.addField("📶 Пінг", onlineP.getPing() + " ms", true);
                    } else {
                        embed.setColor(0x5865F2); // Blurple
                        embed.setDescription("🔴 **Статус:** Офлайн");
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Kyiv"));
                        String lastSeen = offlinePlayer.getLastPlayed() > 0 ? sdf.format(new java.util.Date(offlinePlayer.getLastPlayed())) : "Невідомо";
                        embed.addField("🕒 Останній вхід", lastSeen, true);
                    }

                    int deaths = 0;
                    int mobKills = 0;
                    int playerKills = 0;
                    long playtimeTicks = 0;

                    if (isOnline) {
                        try { deaths = onlineP.getStatistic(org.bukkit.Statistic.DEATHS); } catch (Exception ignored) {}
                        try { mobKills = onlineP.getStatistic(org.bukkit.Statistic.MOB_KILLS); } catch (Exception ignored) {}
                        try { playerKills = onlineP.getStatistic(org.bukkit.Statistic.PLAYER_KILLS); } catch (Exception ignored) {}
                        try { playtimeTicks = onlineP.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE); } catch (Exception ignored) {}
                    } else {
                        try { deaths = offlinePlayer.getStatistic(org.bukkit.Statistic.DEATHS); } catch (Exception ignored) {}
                        try { mobKills = offlinePlayer.getStatistic(org.bukkit.Statistic.MOB_KILLS); } catch (Exception ignored) {}
                        try { playerKills = offlinePlayer.getStatistic(org.bukkit.Statistic.PLAYER_KILLS); } catch (Exception ignored) {}
                        try { playtimeTicks = offlinePlayer.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE); } catch (Exception ignored) {}
                    }

                    long playtimeHours = playtimeTicks / (20 * 60 * 60);
                    long playtimeMins = (playtimeTicks / (20 * 60)) % 60;

                    embed.addField("🌟 Рівень", playerLevel + " lvl", true);
                    embed.addField("☠️ Смертей", String.valueOf(deaths), true);
                    embed.addField("⚔️ Вбивств (Мобів/Гравців)", mobKills + " / " + playerKills, true);
                    embed.addField("⏱️ Награний час", playtimeHours + " год. " + playtimeMins + " хв.", true);

                    int blocksBroken = 0;
                    int blocksPlaced = 0;
                    int itemsPickedUp = 0;

                    if (isOnline) {
                        for (org.bukkit.Material mat : BLOCK_MATERIALS) {
                            try { blocksBroken += onlineP.getStatistic(org.bukkit.Statistic.MINE_BLOCK, mat); } catch (Exception ignored) {}
                            try { blocksPlaced += onlineP.getStatistic(org.bukkit.Statistic.USE_ITEM, mat); } catch (Exception ignored) {}
                        }
                        for (org.bukkit.Material mat : ITEM_MATERIALS) {
                            try { itemsPickedUp += onlineP.getStatistic(org.bukkit.Statistic.PICKUP, mat); } catch (Exception ignored) {}
                        }
                    } else {
                        for (org.bukkit.Material mat : BLOCK_MATERIALS) {
                            try { blocksBroken += offlinePlayer.getStatistic(org.bukkit.Statistic.MINE_BLOCK, mat); } catch (Exception ignored) {}
                            try { blocksPlaced += offlinePlayer.getStatistic(org.bukkit.Statistic.USE_ITEM, mat); } catch (Exception ignored) {}
                        }
                        for (org.bukkit.Material mat : ITEM_MATERIALS) {
                            try { itemsPickedUp += offlinePlayer.getStatistic(org.bukkit.Statistic.PICKUP, mat); } catch (Exception ignored) {}
                        }
                    }

                    long distanceCm = 0;
                    org.bukkit.Statistic[] distStats = {
                            org.bukkit.Statistic.WALK_ONE_CM, org.bukkit.Statistic.SPRINT_ONE_CM, org.bukkit.Statistic.SWIM_ONE_CM,
                            org.bukkit.Statistic.FLY_ONE_CM, org.bukkit.Statistic.MINECART_ONE_CM, org.bukkit.Statistic.HORSE_ONE_CM,
                            org.bukkit.Statistic.PIG_ONE_CM, org.bukkit.Statistic.BOAT_ONE_CM, org.bukkit.Statistic.AVIATE_ONE_CM,
                            org.bukkit.Statistic.CLIMB_ONE_CM, org.bukkit.Statistic.FALL_ONE_CM, org.bukkit.Statistic.WALK_ON_WATER_ONE_CM,
                            org.bukkit.Statistic.WALK_UNDER_WATER_ONE_CM, org.bukkit.Statistic.CROUCH_ONE_CM
                    };
                    if (isOnline) {
                        for (org.bukkit.Statistic s : distStats) {
                            try { distanceCm += onlineP.getStatistic(s); } catch (Exception ignored) {}
                        }
                    } else {
                        for (org.bukkit.Statistic s : distStats) {
                            try { distanceCm += offlinePlayer.getStatistic(s); } catch (Exception ignored) {}
                        }
                    }
                    long distanceBlocks = distanceCm / 100;
                    long distanceKm = distanceBlocks / 1000;

                    embed.addField("⛏️ Зламано блоків", String.valueOf(blocksBroken), true);
                    embed.addField("🧱 Поставлено блоків", String.valueOf(blocksPlaced), true);
                    embed.addField("🎒 Підібрано предметів", String.valueOf(itemsPickedUp), true);
                    embed.addField("🏃 Подолано відстані", distanceKm + " км (" + distanceBlocks + " блоків)", false);

                    if (!isOnline && offlinePlayer.getFirstPlayed() > 0) {
                        java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd.MM.yyyy");
                        sdfDate.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Kyiv"));
                        embed.setFooter("MineCord • Перший вхід: " + sdfDate.format(new java.util.Date(offlinePlayer.getFirstPlayed())));
                    }

                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                } catch (Throwable t) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Помилка обробки /stats: " + t.getMessage(), t);
                    event.getHook().sendMessage("❌ Не вдалося отримати статистику гравця. Перевірте консоль.").setEphemeral(true).queue();
                }
            });
        }
        else if (event.getName().equals("linkadmin")) {
            event.deferReply(true).queue();
            String playerName = event.getOption("player").getAsString();
            net.dv8tion.jda.api.entities.User discordUser = event.getOption("user").getAsUser();
            
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
                    if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                        event.getHook().sendMessage("❌ Гравця **" + playerName + "** не знайдено на сервері.").setEphemeral(true).queue();
                        return;
                    }
                    
                    plugin.getLinkManager().linkAccountDirectly(offlinePlayer.getUniqueId(), discordUser.getId());
                    String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerName;
                    event.getHook().sendMessage("✅ Акаунт Minecraft **" + name + "** успішно прив'язано до Discord " + discordUser.getAsMention() + "!").queue();
                } catch (Throwable t) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Помилка linkadmin: " + t.getMessage(), t);
                    event.getHook().sendMessage("❌ Помилка прив'язки акаунта.").setEphemeral(true).queue();
                }
            });
        }
        else if (event.getName().equals("top")) {
            event.deferReply().queue();
            String category = event.getOption("category") != null ? event.getOption("category").getAsString() : "time";
            String normCat = plugin.getLeaderboardManager().normalizeCategory(category);
            String catTitle = plugin.getLeaderboardManager().getCategoryTitle(normCat);

            plugin.getLeaderboardManager().getTopAsync(normCat, 10, entries -> {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setTitle("🏆 Топ-10: " + catTitle);
                embed.setColor(0xFFD700);

                if (entries == null || entries.isEmpty()) {
                    embed.setDescription("*Дані для цього рейтингу поки що відсутні.*");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < entries.size(); i++) {
                        com.example.minecord.utils.LeaderboardManager.TopEntry entry = entries.get(i);
                        String medal = switch (i) {
                            case 0 -> "🥇";
                            case 1 -> "🥈";
                            case 2 -> "🥉";
                            default -> "`" + (i + 1) + ".`";
                        };
                        sb.append(medal).append(" **").append(entry.getName()).append("** — `").append(entry.getFormattedValue()).append("`\n");
                    }
                    embed.setDescription(sb.toString());

                    // Top 1 avatar thumbnail
                    String top1 = entries.get(0).getName();
                    embed.setThumbnail(com.example.minecord.utils.SkinHelper.getAvatarUrl(top1));
                }

                embed.setFooter("MineCord Leaderboards • Оновлюється кожні 3 хвилини");
                event.getHook().sendMessageEmbeds(embed.build()).queue();
            });
        }
        else {
            event.reply("❌ Невідома команда: /" + event.getName()).setEphemeral(true).queue();
        }
        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Сталася непередбачувана помилка при виконанні команди /" + event.getName(), e);
            event.reply("❌ Внутрішня помилка бота при виконанні команди. Перевірте консоль.").setEphemeral(true).queue(
                success -> {},
                error -> {
                    // If deferReply was already called, reply() throws an error, so send via hook instead
                    event.getHook().sendMessage("❌ Внутрішня помилка бота при виконанні команди. Перевірте консоль.").setEphemeral(true).queue();
                }
            );
        }
    }

    @Override
    public void onButtonInteraction(@NotNull net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        try {
            if (event.getComponentId().startsWith("report_done_")) {
                if (event.getMessage().getEmbeds().isEmpty()) return;

                net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder(event.getMessage().getEmbeds().get(0));
                eb.setColor(0x00FF00);
                eb.addField("Статус", "✅ Оброблено модератором " + event.getUser().getAsMention(), false);

                event.editMessageEmbeds(eb.build()).setComponents().queue();
            }
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[MineCord] Помилка обробки кнопки: " + t.getMessage(), t);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        try {
            if (event.getName().equals("stats") || event.getName().equals("linkadmin")) {
                if (event.getFocusedOption().getName().equals("player")) {
                    String partialName = event.getFocusedOption().getValue();
                    List<String> matches;
                    if (plugin.getPlayerCacheManager() != null) {
                        matches = plugin.getPlayerCacheManager().getMatchingPlayers(partialName);
                    } else {
                        matches = new ArrayList<>();
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(partialName.toLowerCase())) {
                                matches.add(p.getName());
                                if (matches.size() >= 25) break;
                            }
                        }
                    }

                    List<Choice> choices = new ArrayList<>();
                    for (String name : matches) {
                        choices.add(new Choice(name, name));
                    }
                    event.replyChoices(choices).queue();
                    return;
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "[MineCord] Помилка автодоповнення: " + t.getMessage(), t);
        }

        try {
            event.replyChoices(java.util.Collections.emptyList()).queue();
        } catch (Throwable ignored) {}
    }
}
