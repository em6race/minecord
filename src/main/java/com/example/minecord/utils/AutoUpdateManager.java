package com.example.minecord.utils;

import com.example.minecord.MineCord;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Automatically checks GitHub Releases (Latest Build) exclusively during server restart
 * (scheduled/technical or manual) and stages the new artifact into Paper's plugins/update/
 * directory so Paper applies it immediately as the server starts back up.
 */
public class AutoUpdateManager {

    private final MineCord plugin;
    private String currentJarSha256;
    private final AtomicBoolean checking = new AtomicBoolean(false);
    private volatile Thread activeUpdateThread = null;

    public AutoUpdateManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!isEnabled()) {
            return;
        }
        File currentJar = plugin.getPluginJarFile();
        if (currentJar != null && currentJar.exists()) {
            this.currentJarSha256 = computeSha256(currentJar);
        }
    }

    public void stop() {
        // No background polling tasks to cancel; updates run only during restart.
    }

    /**
     * Triggered a few seconds before a scheduled/technical restart in AutoRestartManager
     * so the download starts ahead of shutdown.
     */
    public void prepareBeforeRestartAsync() {
        if (!isEnabled()) {
            return;
        }
        startUpdateThread(false);
    }

    /**
     * Starts the update check in a background thread right at the beginning of onDisable()
     * so it runs in parallel while JDA/Discord shutdown completes.
     */
    public void startShutdownCheck() {
        if (!isEnabled()) {
            return;
        }
        startUpdateThread(true);
    }

    /**
     * Waits for any in-flight update check/download to finish before onDisable() returns.
     */
    public void awaitShutdownCheck() {
        Thread thread = this.activeUpdateThread;
        if (thread != null && thread.isAlive()) {
            try {
                thread.join(20000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void startUpdateThread(boolean isShutdown) {
        Thread existing = this.activeUpdateThread;
        if (existing != null && existing.isAlive()) {
            return;
        }
        Thread thread = new Thread(() -> checkAndDownload(isShutdown, null), "MineCord-RestartUpdater");
        thread.setDaemon(false);
        this.activeUpdateThread = thread;
        thread.start();
    }

    /**
     * Triggered manually via /minecord update
     */
    public void triggerManualUpdate(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "[MineCord] ⏳ Перевірка наявності оновлень на GitHub (Latest Build)...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> checkAndDownload(false, sender));
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("auto-update.enabled", false);
    }

    private boolean checkAndDownload(boolean isShutdown, CommandSender notifySender) {
        if (!checking.compareAndSet(false, true)) {
            if (notifySender != null) {
                notifySender.sendMessage(ChatColor.YELLOW + "[MineCord] Перевірка оновлень вже виконується...");
            }
            return false;
        }

        try {
            String repo = plugin.getConfig().getString("auto-update.repository", "em6race/minecord").trim();
            String tag = plugin.getConfig().getString("auto-update.release-tag", "latest").trim();
            if (repo.isEmpty() || tag.isEmpty()) {
                return false;
            }

            File currentJar = plugin.getPluginJarFile();
            if (currentJar == null || !currentJar.exists()) {
                return false;
            }

            if (currentJarSha256 == null) {
                currentJarSha256 = computeSha256(currentJar);
            }

            File updateFolder = Bukkit.getUpdateFolderFile();
            File stagedFile = new File(updateFolder, currentJar.getName());
            String stagedSha256 = (stagedFile.exists() && stagedFile.length() > 0)
                    ? computeSha256(stagedFile)
                    : null;

            String apiUrl = "https://api.github.com/repos/" + repo + "/releases/tags/" + tag;
            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(isShutdown ? 4000 : 7000);
            conn.setReadTimeout(isShutdown ? 6000 : 10000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "MineCord-AutoUpdater/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                if (notifySender != null) {
                    notifySender.sendMessage(ChatColor.RED + "[MineCord] Не вдалося отримати інформацію з GitHub API (HTTP " + status + ").");
                }
                return false;
            }

            StringBuilder jsonSb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonSb.append(line);
                }
            }

            JsonObject releaseObj = JsonParser.parseString(jsonSb.toString()).getAsJsonObject();
            if (!releaseObj.has("assets") || !releaseObj.get("assets").isJsonArray()) {
                return false;
            }

            JsonArray assets = releaseObj.getAsJsonArray("assets");
            JsonObject targetAsset = null;
            for (JsonElement el : assets) {
                if (!el.isJsonObject()) continue;
                JsonObject asset = el.getAsJsonObject();
                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                if (name.endsWith("-all.jar") || name.endsWith(".jar")) {
                    targetAsset = asset;
                    break;
                }
            }

            if (targetAsset == null) {
                return false;
            }

            String downloadUrl = targetAsset.get("browser_download_url").getAsString();
            long expectedSize = targetAsset.has("size") ? targetAsset.get("size").getAsLong() : -1L;
            String remoteSha256 = null;
            if (targetAsset.has("digest") && !targetAsset.get("digest").isJsonNull()) {
                String digestStr = targetAsset.get("digest").getAsString();
                if (digestStr.startsWith("sha256:")) {
                    remoteSha256 = digestStr.substring("sha256:".length()).trim().toLowerCase();
                }
            }

            // Compare SHA-256 with currently running JAR
            if (remoteSha256 != null && currentJarSha256 != null && remoteSha256.equalsIgnoreCase(currentJarSha256)) {
                if (notifySender != null) {
                    notifySender.sendMessage(ChatColor.GREEN + "[MineCord] ✅ Встановлено найновішу версію плагіна (коміт "
                            + ChatColor.AQUA + GitVersion.getCommitHash() + ChatColor.GREEN + "). Оновлення не потрібне.");
                }
                return false;
            }

            // Compare SHA-256 with already staged JAR in plugins/update/
            if (remoteSha256 != null && stagedSha256 != null && remoteSha256.equalsIgnoreCase(stagedSha256)) {
                if (notifySender != null) {
                    notifySender.sendMessage(ChatColor.GREEN + "[MineCord] ✅ Найновіша збірка вже завантажена в "
                            + ChatColor.YELLOW + "plugins/update/" + ChatColor.GREEN + " і очікує перезапуску сервера!");
                }
                return false;
            }

            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            File tempFile = new File(updateFolder, currentJar.getName() + ".tmp");
            plugin.logPink("⬇️ Знайдено новий реліз на GitHub (Latest Build)! Завантаження оновлення перед рестартом...");

            HttpURLConnection dlConn = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            dlConn.setInstanceFollowRedirects(true);
            dlConn.setConnectTimeout(isShutdown ? 5000 : 10000);
            dlConn.setReadTimeout(isShutdown ? 15000 : 30000);
            dlConn.setRequestProperty("User-Agent", "MineCord-AutoUpdater/1.0");

            if (dlConn.getResponseCode() != 200) {
                plugin.getLogger().warning("[AutoUpdate] Не вдалося завантажити JAR з GitHub: HTTP " + dlConn.getResponseCode());
                return false;
            }

            try (InputStream in = dlConn.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            if (expectedSize > 0 && tempFile.length() != expectedSize) {
                plugin.getLogger().warning("[AutoUpdate] Розмір завантаженого файлу не співпадає (" + tempFile.length() + " != " + expectedSize + "). Скасовано.");
                tempFile.delete();
                return false;
            }

            String downloadedSha = computeSha256(tempFile);
            if (remoteSha256 != null && downloadedSha != null && !remoteSha256.equalsIgnoreCase(downloadedSha)) {
                plugin.getLogger().warning("[AutoUpdate] Контрольна сума SHA-256 завантаженого JAR не співпадає! Скасовано.");
                tempFile.delete();
                return false;
            }

            // Read git.properties from the downloaded JAR to verify integrity and get commit info
            String newCommitHash = "unknown";
            String newCommitMsg = "";
            String newBuildTime = "";
            try (JarFile jar = new JarFile(tempFile)) {
                JarEntry entry = jar.getJarEntry("git.properties");
                if (entry != null) {
                    Properties props = new Properties();
                    try (InputStream propIn = jar.getInputStream(entry)) {
                        props.load(propIn);
                        newCommitHash = props.getProperty("git.commit.id.abbrev", "unknown");
                        newCommitMsg = props.getProperty("git.commit.message.short", "");
                        newBuildTime = props.getProperty("git.build.time", "");
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[AutoUpdate] Помилка перевірки архіву JAR: " + t.getMessage());
                tempFile.delete();
                return false;
            }

            // If remoteSha256 wasn't provided by GitHub API, check if commit hash + build time are identical
            if (remoteSha256 == null && newCommitHash.equalsIgnoreCase(GitVersion.getCommitHash())
                    && newBuildTime.equals(GitVersion.getBuildTime())) {
                tempFile.delete();
                return false;
            }

            Files.move(tempFile.toPath(), stagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            String infoMsg = "✅ Оновлення успішно завантажено в plugins/update/" + stagedFile.getName()
                    + " [коміт " + newCommitHash + (newCommitMsg.isEmpty() ? "" : ": " + newCommitMsg) + "]! "
                    + "Воно автоматично застосується під час запуску сервера.";
            plugin.logPink(infoMsg);

            if (notifySender != null) {
                notifySender.sendMessage(ChatColor.GREEN + "[MineCord] " + infoMsg);
            }

            return true;
        } catch (Throwable t) {
            if (notifySender != null) {
                notifySender.sendMessage(ChatColor.RED + "[MineCord] Помилка під час оновлення: " + t.getMessage());
            }
            return false;
        } finally {
            checking.set(false);
        }
    }

    private static String computeSha256(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
