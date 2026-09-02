package com.example.minecord.utils;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ConsoleManager {
    private final MineCord plugin;
    private final List<String> logBuffer = new ArrayList<>();
    private int taskId = -1;
    private Handler logHandler;

    public ConsoleManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        // Якщо канал не налаштований, не запускаємо менеджер
        if (consoleChannelId == null || consoleChannelId.equals("000000000000000000") || consoleChannelId.isEmpty()) {
            return;
        }

        // Створюємо перехоплювач логів
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                if (msg != null && !msg.isEmpty()) {
                    synchronized (logBuffer) {
                        logBuffer.add("[" + record.getLevel().getName() + "] " + msg);
                    }
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };
        
        // Підключаємо перехоплювач до головних логерів сервера
        Bukkit.getLogger().addHandler(logHandler);
        java.util.logging.Logger.getLogger("").addHandler(logHandler);

        // Створюємо завдання, яке кожні 1.5 секунди забирає накопичені логи і відправляє їх в Discord
        // (Це необхідно, щоб не перевищити ліміт повідомлень Discord API під час спаму в консоль)
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            List<String> toSend;
            synchronized (logBuffer) {
                if (logBuffer.isEmpty()) return;
                toSend = new ArrayList<>(logBuffer);
                logBuffer.clear(); // Очищаємо буфер після копіювання
            }

            if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
                TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(consoleChannelId);
                if (channel != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("```\n");
                    for (String line : toSend) {
                        String clean = org.bukkit.ChatColor.stripColor(line); // Прибираємо ігрові кольори
                        
                        // Discord має ліміт 2000 символів на повідомлення
                        if (sb.length() + clean.length() > 1900) {
                            sb.append("```");
                            channel.sendMessage(sb.toString()).queue();
                            sb = new StringBuilder();
                            sb.append("```\n");
                        }
                        sb.append(clean).append("\n");
                    }
                    if (sb.length() > 4) { // Якщо є хоча б один символ тексту після ```\n
                        sb.append("```");
                        channel.sendMessage(sb.toString()).queue();
                    }
                }
            }
        }, 30L, 30L); // 30 тіків = 1.5 секунди
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        if (logHandler != null) {
            Bukkit.getLogger().removeHandler(logHandler);
            java.util.logging.Logger.getLogger("").removeHandler(logHandler);
        }
    }
}
