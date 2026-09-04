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

public class ConsoleManager {
    private final MineCord plugin;
    private final List<String> logBuffer = new ArrayList<>();
    private int taskId = -1;
    private Handler logHandler;
    
    private boolean showInfo = true;
    private boolean showWarn = true;

    public ConsoleManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void setShowInfo(boolean showInfo) {
        this.showInfo = showInfo;
    }

    public boolean isShowInfo() {
        return showInfo;
    }

    public void start() {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (consoleChannelId == null || consoleChannelId.equals("000000000000000000") || consoleChannelId.isEmpty()) {
            return;
        }

        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().getName().equals("INFO") && !showInfo) return;
                if (record.getLevel().getName().equals("WARNING") && !showWarn) return;
                
                String msg = record.getMessage();
                if (msg != null && !msg.isEmpty()) {
                    synchronized (logBuffer) {
                        logBuffer.add("[" + record.getLevel().getName() + "] " + msg);
                    }
                }
                
                if (record.getThrown() != null && plugin.getConfig().getBoolean("technical.error-catcher.enabled", true)) {
                    StringWriter sw = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(sw));
                    String stackTrace = sw.toString();
                    
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String title = record.getLevel().getName() + ": " + (msg != null ? msg : record.getThrown().getMessage());
                        if (title.length() > 200) title = title.substring(0, 197) + "...";
                        uploadError(title, stackTrace);
                    });
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        };
        
        Bukkit.getLogger().addHandler(logHandler);
        java.util.logging.Logger.getLogger("").addHandler(logHandler);

        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
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
                        if (sb.length() + clean.length() > 1900) {
                            sb.append("```");
                            channel.sendMessage(sb.toString()).queue();
                            sb = new StringBuilder();
                            sb.append("```\n");
                        }
                        sb.append(clean).append("\n");
                    }
                    if (sb.length() > 4) {
                        sb.append("```");
                        channel.sendMessage(sb.toString()).queue();
                    }
                }
            }
        }, 30L, 30L);
    }

    private void uploadError(String title, String stackTrace) {
        try {
            String pasteUrlConfig = plugin.getConfig().getString("technical.error-catcher.paste-service", "https://paste.md-5.net/documents");
            URL url = new URL(pasteUrlConfig);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", "MineCord/1.0");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(stackTrace.getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() == 200) {
                try (Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                    String response = s.hasNext() ? s.next() : "";
                    Matcher m = Pattern.compile("\"key\":\"([^\"]+)\"").matcher(response);
                    if (m.find()) {
                        String baseUrl = pasteUrlConfig.replace("/documents", "");
                        String finalUrl = baseUrl + "/" + m.group(1);
                        sendErrorEmbed(title, finalUrl);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail to avoid console spam loops
        }
    }
    
    private void sendErrorEmbed(String title, String pasteUrl) {
        String consoleChannelId = plugin.getConfig().getString("discord.console-channel-id");
        if (plugin.getBotManager() != null && plugin.getBotManager().getJda() != null && consoleChannelId != null) {
            TextChannel channel = plugin.getBotManager().getJda().getTextChannelById(consoleChannelId);
            if (channel != null) {
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                embed.setTitle("⚠️ Виявлено помилку (Stacktrace)");
                embed.setDescription("**" + title + "**\n\n[Переглянути повний лог помилки](" + pasteUrl + ")");
                embed.setColor(0xFF0000);
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        }
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
