package com.example.minecord.utils;

import com.example.minecord.MineCord;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebhookManager {
    private final MineCord plugin;
    private volatile String webhookUrl;
    private static final String WEBHOOK_NAME = "MineCord_Chat";
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MineCord-Webhook-Thread");
        t.setDaemon(true);
        return t;
    });

    public WebhookManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void initialize(TextChannel channel) {
        if (channel == null) return;

        // Find existing webhook or create a new one
        channel.retrieveWebhooks().queue(webhooks -> {
            Webhook targetWebhook = null;
            for (Webhook wh : webhooks) {
                if (wh.getName().equals(WEBHOOK_NAME)) {
                    targetWebhook = wh;
                    break;
                }
            }

            if (targetWebhook == null) {
                channel.createWebhook(WEBHOOK_NAME).queue(newWebhook -> {
                    this.webhookUrl = newWebhook.getUrl();
                    plugin.getLogger().info("Створено новий Webhook для чату!");
                }, error -> {
                    plugin.getLogger().warning("Не вдалося створити Webhook. Перевірте, чи має бот права 'Manage Webhooks'.");
                });
            } else {
                this.webhookUrl = targetWebhook.getUrl();
            }
        });
    }

    public void sendMessage(String playerName, String message) {
        sendMessage(playerName, message, null);
    }

    public void sendMessage(String playerName, String message, String resolvedAvatarUrl) {
        final String currentUrl = this.webhookUrl;
        if (currentUrl == null || executor.isShutdown()) return;

        executor.submit(() -> {
            try {
                URL url = new URL(currentUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setDoOutput(true);

                String avatarUrl = (resolvedAvatarUrl != null && !resolvedAvatarUrl.isEmpty())
                        ? resolvedAvatarUrl
                        : SkinHelper.getAvatarUrl(playerName);

                JsonObject payload = new JsonObject();
                payload.addProperty("username", playerName);
                payload.addProperty("avatar_url", avatarUrl);
                payload.addProperty("content", message);

                // Prevent @everyone, @here, and @role mass-ping exploits while allowing explicit user mentions
                JsonObject allowedMentions = new JsonObject();
                JsonArray parseArr = new JsonArray();
                parseArr.add("users");
                allowedMentions.add("parse", parseArr);
                payload.add("allowed_mentions", allowedMentions);

                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                con.setRequestProperty("Content-Length", String.valueOf(body.length));

                try (OutputStream os = con.getOutputStream()) {
                    os.write(body);
                }

                // Read response code (important for completing request)
                int responseCode = con.getResponseCode();
                if (responseCode >= 400) {
                    plugin.getLogger().warning("[Webhook] Discord повернув помилку: " + responseCode);
                }
                try (java.io.InputStream is = (responseCode >= 400 ? con.getErrorStream() : con.getInputStream())) {
                    if (is != null) {
                        is.transferTo(java.io.OutputStream.nullOutputStream());
                    }
                } catch (Exception ignored) {}
                con.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[Webhook] Помилка відправки: " + e.getMessage());
            }
        });
    }

    public void close() {
        this.webhookUrl = null;
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
