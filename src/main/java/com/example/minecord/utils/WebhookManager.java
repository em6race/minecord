package com.example.minecord.utils;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookManager {
    private final MineCord plugin;
    private String webhookUrl;
    private static final String WEBHOOK_NAME = "MineCord_Chat";

    public WebhookManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void initialize(TextChannel channel) {
        if (channel == null) return;

        // Шукаємо існуючий webhook або створюємо новий
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
        if (webhookUrl == null) return;

        // Відправляємо повідомлення через Discord Webhook API напряму через HTTP
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setDoOutput(true);

                // Екрануємо текст для JSON
                String safeUsername = playerName.replace("\"", "\\\"");
                String safeContent = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                String avatarUrl = "https://mc-heads.net/avatar/" + playerName + "/256";

                String json = "{" +
                        "\"username\":\"" + safeUsername + "\"," +
                        "\"avatar_url\":\"" + avatarUrl + "\"," +
                        "\"content\":\"" + safeContent + "\"" +
                        "}";

                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                con.setRequestProperty("Content-Length", String.valueOf(body.length));

                try (OutputStream os = con.getOutputStream()) {
                    os.write(body);
                }

                // Читаємо відповідь (важливо для завершення запиту)
                int responseCode = con.getResponseCode();
                if (responseCode >= 400) {
                    plugin.getLogger().warning("[Webhook] Discord повернув помилку: " + responseCode);
                }
                con.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[Webhook] Помилка відправки: " + e.getMessage());
            }
        }, "MineCord-Webhook-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void close() {
        this.webhookUrl = null;
    }
}
