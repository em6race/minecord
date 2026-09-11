package com.example.minecord;

import com.example.minecord.bot.BotManager;
import com.example.minecord.listeners.ChatListener;
import com.example.minecord.listeners.PlayerEventListener;
import org.bukkit.plugin.java.JavaPlugin;

import com.example.minecord.utils.AccountLinkManager;
import com.example.minecord.utils.AutoRestartManager;
import com.example.minecord.utils.OpenAIModerator;
import com.example.minecord.utils.AntiSpamManager;
import com.example.minecord.utils.PerformanceMonitor;
import com.example.minecord.utils.AfkManager;
import com.example.minecord.utils.BlueMapManager;
import com.example.minecord.utils.SleepManager;

public final class MineCord extends JavaPlugin {

    private BotManager botManager;
    private AccountLinkManager linkManager;
    private AutoRestartManager autoRestartManager;
    private OpenAIModerator openAIModerator;
    private AntiSpamManager antiSpamManager;
    private PerformanceMonitor performanceMonitor;
    private com.example.minecord.utils.TabManager tabManager;
    private AfkManager afkManager;
    private SleepManager sleepManager;
    private BlueMapManager blueMapManager;
    private com.example.minecord.utils.LeaderboardManager leaderboardManager;
    private com.example.minecord.utils.PlayerCacheManager playerCacheManager;
    private com.example.minecord.utils.PlayerTipManager playerTipManager;
    private com.example.minecord.utils.RoleSyncManager roleSyncManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String sentryDsn = getConfig().getString("sentry.dsn");
        if (getConfig().getBoolean("sentry.enabled", false) && sentryDsn != null && !sentryDsn.isEmpty()) {
            io.sentry.Sentry.init(options -> {
                options.setDsn(sentryDsn);
                options.setTracesSampleRate(1.0);
            });
            logPink("Sentry integration enabled!");
        }
        
        // Initialize AI moderator, anti-spam, and performance monitor
        this.openAIModerator = new OpenAIModerator(this);
        this.antiSpamManager = new AntiSpamManager(this);
        this.performanceMonitor = new PerformanceMonitor(this);
        
        linkManager = new AccountLinkManager(this);
        this.playerCacheManager = new com.example.minecord.utils.PlayerCacheManager(this);
        this.playerCacheManager.init();
        this.playerTipManager = new com.example.minecord.utils.PlayerTipManager(this);
        this.playerTipManager.start();
        botManager = new BotManager(this);
        botManager.start();
        
        // Initialize auto-restart scheduler
        this.autoRestartManager = new AutoRestartManager(this);
        this.autoRestartManager.start();
        
        // Start performance monitoring
        this.performanceMonitor.start();
        
        // Initialize tab list manager
        this.tabManager = new com.example.minecord.utils.TabManager(this);
        this.tabManager.start();
        
        // Initialize Discord role sync manager
        this.roleSyncManager = new com.example.minecord.utils.RoleSyncManager(this);
        this.roleSyncManager.start();
        
        // Initialize AFK manager
        this.afkManager = new AfkManager(this);
        this.afkManager.start();
        
        // Initialize sleep manager
        this.sleepManager = new SleepManager(this);
        this.sleepManager.start();

        // Initialize BlueMap auto-reload manager
        this.blueMapManager = new BlueMapManager(this);
        this.blueMapManager.start();

        // Register commands
        MineCordCommand cmd = new MineCordCommand(this);
        if (getCommand("discord") != null) {
            getCommand("discord").setExecutor(cmd);
            getCommand("discord").setTabCompleter(cmd);
        }
        if (getCommand("minecord") != null) {
            getCommand("minecord").setExecutor(cmd);
            getCommand("minecord").setTabCompleter(cmd);
        }
        if (getCommand("map") != null) {
            getCommand("map").setExecutor(cmd);
        }
        if (getCommand("mail") != null) {
            getCommand("mail").setExecutor(cmd);
            getCommand("mail").setTabCompleter(cmd);
        }
        if (getCommand("ticket") != null) {
            getCommand("ticket").setExecutor(cmd);
            getCommand("ticket").setTabCompleter(cmd);
        }
        if (getCommand("unmute") != null) {
            getCommand("unmute").setExecutor(cmd);
        }
        // Initialize leaderboard manager
        this.leaderboardManager = new com.example.minecord.utils.LeaderboardManager(this);

        if (getCommand("stats") != null) {
            com.example.minecord.commands.StatsCommand statsCmd = new com.example.minecord.commands.StatsCommand(this);
            getCommand("stats").setExecutor(statsCmd);
            getCommand("stats").setTabCompleter(statsCmd);
        }
        if (getCommand("top") != null) {
            com.example.minecord.commands.TopCommand topCmd = new com.example.minecord.commands.TopCommand(this);
            getCommand("top").setExecutor(topCmd);
            getCommand("top").setTabCompleter(topCmd);
        }
        if (getCommand("report") != null) {
            com.example.minecord.commands.ReportCommand reportCmd = new com.example.minecord.commands.ReportCommand(this);
            getCommand("report").setExecutor(reportCmd);
            getCommand("report").setTabCompleter(reportCmd);
        }
        if (getCommand("sharecoords") != null) {
            com.example.minecord.commands.ShareCoordsCommand shareCoordsCmd = new com.example.minecord.commands.ShareCoordsCommand(this);
            getCommand("sharecoords").setExecutor(shareCoordsCmd);
            getCommand("sharecoords").setTabCompleter(shareCoordsCmd);
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.example.minecord.listeners.DragonEventListener(this), this);
        
        // Warm up and eagerly pre-load translator classes into JVM RAM
        try {
            com.example.minecord.utils.DeathTranslator.translate("");
            com.example.minecord.utils.AdvancementTranslator.translate("", "");
        } catch (Throwable ignored) {}

        // Soft-dependency check
        if (com.example.minecord.utils.SkinHelper.isSkinsRestorerAvailable()) {
            logPink("Плагін SkinsRestorer знайдено — увімкнено підтримку кастомних скінів для Discord.");
        } else {
            logPink("Плагін SkinsRestorer не знайдено — інтеграцію скінів вимкнено (використовується нативний профіль Paper).");
        }

        logPink("📦 Збірка плагіна: коміт " + com.example.minecord.utils.GitVersion.getCommitHash() + " (" + com.example.minecord.utils.GitVersion.getBuildTime() + ")");
        logPink("📝 Зміни: " + com.example.minecord.utils.GitVersion.getCommitMessage());
        
        String ins = com.example.minecord.utils.GitVersion.getDiffInsertions();
        String del = com.example.minecord.utils.GitVersion.getDiffDeletions();
        String files = com.example.minecord.utils.GitVersion.getDiffFiles();
        if (!ins.equals("0") || !del.equals("0") || !files.equals("0")) {
            getServer().getConsoleSender().sendMessage(
                    org.bukkit.ChatColor.LIGHT_PURPLE + "[MineCord] " +
                    org.bukkit.ChatColor.LIGHT_PURPLE + "📊 Диф коду: " +
                    org.bukkit.ChatColor.GREEN + "+" + ins + " " +
                    org.bukkit.ChatColor.RED + "-" + del + " " +
                    org.bukkit.ChatColor.GRAY + "(" + files + " " + (files.equals("1") ? "файл" : "файлів") + ")"
            );
        }
        logPink("MineCord (Модульна версія) успішно завантажено!");
    }

    public void logPink(String message) {
        getServer().getConsoleSender().sendMessage(org.bukkit.ChatColor.LIGHT_PURPLE + "[MineCord] " + message);
    }

    @Override
    public void onDisable() {
        if (getConfig().getBoolean("sentry.enabled", false)) {
            io.sentry.Sentry.close();
        }
        if (botManager != null) {
            botManager.stop();
        }
        if (autoRestartManager != null) {
            autoRestartManager.stop();
        }
        if (tabManager != null) {
            tabManager.stop();
        }
        if (afkManager != null) {
            afkManager.stop();
        }
        if (sleepManager != null) {
            sleepManager.stop();
        }
        if (performanceMonitor != null) {
            performanceMonitor.stop();
        }
        if (blueMapManager != null) {
            blueMapManager.stop();
        }
        if (playerTipManager != null) {
            playerTipManager.stop();
        }
        if (roleSyncManager != null) {
            roleSyncManager.stop();
        }
    }
    
    public void reloadPlugin() {
        reloadConfig();
        
        if (autoRestartManager != null) {
            autoRestartManager.stop();
            autoRestartManager.start();
        }
        
        if (tabManager != null) {
            tabManager.stop();
            this.tabManager = new com.example.minecord.utils.TabManager(this);
            this.tabManager.start();
        }
        
        if (roleSyncManager != null) {
            roleSyncManager.reloadConfig();
            roleSyncManager.syncAllOnlinePlayers();
        }
        
        if (afkManager != null) {
            afkManager.stop();
            this.afkManager = new AfkManager(this);
            this.afkManager.start();
        }
        
        if (sleepManager != null) {
            sleepManager.stop();
            this.sleepManager = new SleepManager(this);
            this.sleepManager.start();
        }

        if (performanceMonitor != null) {
            performanceMonitor.stop();
            this.performanceMonitor = new PerformanceMonitor(this);
            this.performanceMonitor.start();
        }

        if (blueMapManager != null) {
            blueMapManager.stop();
            this.blueMapManager = new BlueMapManager(this);
            this.blueMapManager.start();
        }
        
        if (playerTipManager != null) {
            playerTipManager.stop();
            this.playerTipManager = new com.example.minecord.utils.PlayerTipManager(this);
            this.playerTipManager.start();
        }
        
        if (botManager != null) {
            botManager.stop();
            this.botManager = new BotManager(this);
            this.botManager.start();
        }
        
        // Reload AI moderator
        this.openAIModerator = new OpenAIModerator(this);

        if (playerCacheManager != null) {
            playerCacheManager.refreshCache();
        }
    }

    public BotManager getBotManager() {
        return botManager;
    }

    public AccountLinkManager getLinkManager() {
        return linkManager;
    }

    public AutoRestartManager getAutoRestartManager() {
        return autoRestartManager;
    }

    public OpenAIModerator getOpenAIModerator() {
        return openAIModerator;
    }

    public AntiSpamManager getAntiSpamManager() {
        return antiSpamManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public AfkManager getAfkManager() {
        return afkManager;
    }

    public BlueMapManager getBlueMapManager() {
        return blueMapManager;
    }

    public com.example.minecord.utils.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public com.example.minecord.utils.PlayerCacheManager getPlayerCacheManager() {
        return playerCacheManager;
    }

    public com.example.minecord.utils.PlayerTipManager getPlayerTipManager() {
        return playerTipManager;
    }

    public com.example.minecord.utils.RoleSyncManager getRoleSyncManager() {
        return roleSyncManager;
    }
}