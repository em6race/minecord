package com.example.minecord.utils;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;

public class ConsoleManager {
    private final MineCord plugin;
    private final List<String> logBuffer = new ArrayList<>();
    private int taskId = -1;
    private Handler logHandler;
    private MineCordLog4jAppender log4jAppender;
    private boolean usingLog4j = false;

    public ConsoleManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (consoleChannelId == null || consoleChannelId.equals("000000000000000000") || consoleChannelId.isEmpty()) {
            return;
        }

        try {
            log4jAppender = new MineCordLog4jAppender(this::handleLog4jEvent);
            log4jAppender.start();
            Logger rootLogger = (Logger) LogManager.getRootLogger();
            rootLogger.addAppender(log4jAppender);
            usingLog4j = true;
            plugin.getLogger().info("[ConsoleManager] Успішно підключено Log4j2 перехоплювач (підтримка SLF4J / BlueMap).");
        } catch (Throwable t) {
            plugin.getLogger().warning("[ConsoleManager] Не вдалося підключити Log4j2 (" + t.getMessage() + "), використовуємо fallback JUL.");
            setupJulHandler();
        }

        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::flushLogsAsync, 20L, 20L);
    }

    private void handleLog4jEvent(LogEvent event) {
        String loggerName = event.getLoggerName();
        // Ignore internal JDA verbose logs
        if (loggerName != null && loggerName.startsWith("net.dv8tion.jda")) {
            if (event.getLevel().isLessSpecificThan(org.apache.logging.log4j.Level.WARN)) {
                return;
            }
        }

        String msg = event.getMessage() != null ? event.getMessage().getFormattedMessage() : "";
        if (msg == null || msg.trim().isEmpty()) {
            return;
        }

        String level = event.getLevel() != null ? event.getLevel().name() : "INFO";
        processLogEntry(level, msg, event.getThrown());
    }

    private void setupJulHandler() {
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                if (msg != null && !msg.isEmpty()) {
                    String level = record.getLevel() != null ? record.getLevel().getName() : "INFO";
                    processLogEntry(level, msg, record.getThrown());
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };

        Bukkit.getLogger().addHandler(logHandler);
        java.util.logging.Logger.getLogger("").addHandler(logHandler);
    }

    private void processLogEntry(String level, String msg, Throwable thrown) {
        synchronized (logBuffer) {
            logBuffer.add("[" + level + "] " + msg);
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                thrown.printStackTrace(new PrintWriter(sw));
                String[] lines = sw.toString().split("\n");
                for (int i = 0; i < Math.min(lines.length, 15); i++) {
                    logBuffer.add(lines[i].replace("\r", ""));
                }
                if (lines.length > 15) logBuffer.add("... (" + (lines.length - 15) + " more lines)");
            }
        }

        if (thrown != null && plugin.getConfig().getBoolean("technical.error-catcher.enabled", true)) {
            StringWriter sw = new StringWriter();
            thrown.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String title = level + ": " + (msg != null ? msg : thrown.getMessage());
                if (title.length() > 200) title = title.substring(0, 197) + "...";
                sendErrorEmbed(title, stackTrace);

                if (plugin.getConfig().getBoolean("sentry.enabled", false)) {
                    try {
                        io.sentry.Sentry.captureException(thrown);
                    } catch (Throwable ignored) {
                        // Sentry library not available or ClassLoader conflict — ignore
                    }
                }
            });
        }
    }

    private void flushLogsAsync() {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (consoleChannelId == null || consoleChannelId.equals("000000000000000000")) return;
        
        List<String> toSend;
        synchronized (logBuffer) {
            if (logBuffer.isEmpty()) return;
            toSend = new ArrayList<>(logBuffer);
            logBuffer.clear();
        }

        if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(consoleChannelId);
            if (channel != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("```\n");
                for (String line : toSend) {
                    String clean = org.bukkit.ChatColor.stripColor(line);
                    clean = clean.replaceAll("\u001B\\[[;\\d]*m", "");
                    if (clean.isEmpty()) clean = " ";
                    int i = 0;
                    while (i < clean.length()) {
                        int end = Math.min(i + 1900, clean.length());
                        String part = clean.substring(i, end);
                        if (sb.length() + part.length() > 1900) {
                            sb.append("```");
                            channel.sendMessage(sb.toString()).queue();
                            sb = new StringBuilder("```\n");
                        }
                        sb.append(part);
                        i = end;
                    }
                    sb.append("\n");
                }
                if (sb.length() > 4) {
                    sb.append("```");
                    channel.sendMessage(sb.toString()).queue();
                }
            }
        }
    }

    private void flushLogsSync() {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (consoleChannelId == null || consoleChannelId.equals("000000000000000000")) return;
        
        List<String> toSend;
        synchronized (logBuffer) {
            if (logBuffer.isEmpty()) return;
            toSend = new ArrayList<>(logBuffer);
            logBuffer.clear();
        }

        if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null) {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(consoleChannelId);
            if (channel != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("```\n");
                for (String line : toSend) {
                    String clean = org.bukkit.ChatColor.stripColor(line);
                    clean = clean.replaceAll("\u001B\\[[;\\d]*m", "");
                    if (clean.isEmpty()) clean = " ";
                    int i = 0;
                    while (i < clean.length()) {
                        int end = Math.min(i + 1900, clean.length());
                        String part = clean.substring(i, end);
                        if (sb.length() + part.length() > 1900) {
                            sb.append("```");
                            channel.sendMessage(sb.toString()).complete();
                            sb = new StringBuilder("```\n");
                        }
                        sb.append(part);
                        i = end;
                    }
                    sb.append("\n");
                }
                if (sb.length() > 4) {
                    sb.append("```");
                    channel.sendMessage(sb.toString()).complete();
                }
            }
        }
    }

    private void sendErrorEmbed(String title, String stackTrace) {
        try {
            String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
            if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null && consoleChannelId != null) {
                TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(consoleChannelId);
                if (channel != null) {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                    embed.setTitle("⚠️ Помилка Плагіну!");
                    embed.setColor(0xFF0000);
                    
                    final String safeTitle;
                    if (title != null && !title.isEmpty()) {
                        safeTitle = title.length() > 256 ? title.substring(0, 253) + "..." : title;
                        embed.addField("Опис", safeTitle, false);
                    } else {
                        safeTitle = "Невідома помилка";
                    }
                    
                    if (stackTrace != null && !stackTrace.isEmpty()) {
                        String cleanTrace = org.bukkit.ChatColor.stripColor(stackTrace);
                        if (cleanTrace.length() > 3900) {
                            cleanTrace = cleanTrace.substring(0, 3900) + "\n... (зрізано)";
                        }
                        embed.setDescription("```java\n" + cleanTrace + "\n```");
                    }
                    
                    channel.sendMessageEmbeds(embed.build()).queue(null, (err) -> {
                        channel.sendMessage("⚠️ **Помилка:** " + safeTitle).queue(null, (e) -> {});
                    });
                }
            }
        } catch (Throwable ignored) {
            // Failsafe in case JDA classes or embed construction fail
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        try {
            flushLogsSync();
        } catch (Exception e) {}

        if (usingLog4j && log4jAppender != null) {
            try {
                Logger rootLogger = (Logger) LogManager.getRootLogger();
                rootLogger.removeAppender(log4jAppender);
                log4jAppender.stop();
            } catch (Throwable ignored) {}
        }

        if (logHandler != null) {
            try {
                Bukkit.getLogger().removeHandler(logHandler);
                java.util.logging.Logger.getLogger("").removeHandler(logHandler);
            } catch (Throwable ignored) {}
        }
    }
}
