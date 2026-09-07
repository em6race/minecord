package com.example.minecord.utils;

import com.example.minecord.MineCord;
import okhttp3.*;
import org.bukkit.Bukkit;

import java.io.IOException;

public class PterodactylAPI {

    public static void restartServer(MineCord plugin) {
        String panelUrl = plugin.getConfig().getString("pterodactyl.panel-url", "");
        String apiKey = plugin.getConfig().getString("pterodactyl.api-key", "");
        String serverId = plugin.getConfig().getString("pterodactyl.server-id", "");

        if (panelUrl == null || apiKey == null || serverId == null || panelUrl.isEmpty() || apiKey.isEmpty() || serverId.isEmpty()) {
            plugin.getLogger().warning("Pterodactyl API не налаштовано повністю. Виконую стандартний /restart.");
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart"));
            return;
        }

        // Прибираємо слеш в кінці
        if (panelUrl.endsWith("/")) {
            panelUrl = panelUrl.substring(0, panelUrl.length() - 1);
        }

        String url = panelUrl + "/api/client/servers/" + serverId + "/power";
        
        OkHttpClient client = new OkHttpClient();
        
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, "{\"signal\": \"restart\"}");
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.getLogger().severe("Помилка відправки сигналу рестарту в панель Pterodactyl: " + e.getMessage());
                // Резервний варіант
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    plugin.getLogger().info("Сигнал на рестарт успішно відправлено до панелі Pterodactyl!");
                } else {
                    plugin.getLogger().severe("Панель Pterodactyl відхилила сигнал рестарту. Код: " + response.code());
                    // Резервний варіант
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart"));
                }
                response.close();
            }
        });
    }
}
