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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String sentryDsn = getConfig().getString("sentry.dsn");
        if (getConfig().getBoolean("sentry.enabled", false) && sentryDsn != null && !sentryDsn.isEmpty()) {
            io.sentry.Sentry.init(options -> {
                options.setDsn(sentryDsn);
                options.setTracesSampleRate(1.0);
            });
            getLogger().info("Sentry integration enabled!");
        }
        
        // Initialize AI moderator, anti-spam, and performance monitor
        this.openAIModerator = new OpenAIModerator(this);
        this.antiSpamManager = new AntiSpamManager(this);
        this.performanceMonitor = new PerformanceMonitor(this);
        
        linkManager = new AccountLinkManager(this);
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
        }
        if (getCommand("minecord") != null) {
            getCommand("minecord").setExecutor(cmd);
        }
        if (getCommand("map") != null) {
            getCommand("map").setExecutor(cmd);
        }
        if (getCommand("mail") != null) {
            getCommand("mail").setExecutor(cmd);
        }
        if (getCommand("ticket") != null) {
            getCommand("ticket").setExecutor(cmd);
        }
        if (getCommand("unmute") != null) {
            getCommand("unmute").setExecutor(cmd);
        }
        if (getCommand("stats") != null) {
            com.example.minecord.commands.StatsCommand statsCmd = new com.example.minecord.commands.StatsCommand(this);
            getCommand("stats").setExecutor(statsCmd);
            getCommand("stats").setTabCompleter(statsCmd);
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Soft-dependency check
        if (com.example.minecord.utils.SkinHelper.isSkinsRestorerAvailable()) {
            getLogger().info("Плагін SkinsRestorer знайдено — увімкнено підтримку кастомних скінів для Discord.");
        } else {
            getLogger().info("Плагін SkinsRestorer не знайдено — інтеграцію скінів вимкнено (використовується нативний профіль Paper).");
        }

        getLogger().info("MineCord (Модульна версія) успішно завантажено!");
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
        
        if (botManager != null) {
            botManager.stop();
            this.botManager = new BotManager(this);
            this.botManager.start();
        }
        
        // Reload AI moderator
        this.openAIModerator = new OpenAIModerator(this);
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
}