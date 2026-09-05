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

public final class MineCord extends JavaPlugin {

    private BotManager botManager;
    private AccountLinkManager linkManager;
    private AutoRestartManager autoRestartManager;
    private OpenAIModerator openAIModerator;
    private AntiSpamManager antiSpamManager;
    private PerformanceMonitor performanceMonitor;
    private com.example.minecord.utils.TabManager tabManager;

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
        
        // Ініціалізація AI модератора та анти-спаму
        this.openAIModerator = new OpenAIModerator(this);
        this.antiSpamManager = new AntiSpamManager();
        this.performanceMonitor = new PerformanceMonitor(this);
        
        // Ініціалізація бази прив'язки акаунтів
        this.linkManager = new AccountLinkManager(this);
        
        // Ініціалізація авторестартів
        this.autoRestartManager = new AutoRestartManager(this);
        this.autoRestartManager.start();
        
        // Запуск моніторингу
        this.performanceMonitor.start();
        
        // Ініціалізація TAB-листа
        this.tabManager = new com.example.minecord.utils.TabManager(this);
        this.tabManager.start();
        
        // Реєстрація команд
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
        if (getCommand("stats") != null) {
            getCommand("stats").setExecutor(new com.example.minecord.commands.StatsCommand(this));
        }

        // Ініціалізація головного менеджера бота
        this.botManager = new BotManager(this);
        this.botManager.start();

        // Реєстрація слухачів подій Minecraft
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
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
        
        if (botManager != null) {
            botManager.stop();
            this.botManager = new BotManager(this);
            this.botManager.start();
        }
        
        // Перезавантажуємо AI
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
}