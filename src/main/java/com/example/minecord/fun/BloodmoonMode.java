package com.example.minecord.fun;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Режим: Кривавий Місяць (Bloodmoon Mode).
 * Включає 3 рівні загрози, збільшений спавнрейт, орди, міні-босів,
 * ванільне червоне затемнення/віньєтку без ресурспаків, збереження стану
 * при рестарті та інтеграцію з Discord.
 */
public class BloodmoonMode implements FunMode, Listener {

    private final MineCord plugin;
    private final NamespacedKey mobKey;
    private final NamespacedKey bossKey;

    private boolean enabled = true;
    private double chancePercent = 5.0;
    private final List<String> targetWorldNames = new ArrayList<>();
    private boolean blockBeds = true;
    private boolean redSkyEffects = true;
    private boolean hordesEnabled = true;
    private int hordeIntervalSeconds = 120;
    private boolean discordAnnouncements = true;
    private int durationMinutes = 10;
    private int durationSeconds = 600;
    private int elapsedSeconds = 0;

    private final Map<Integer, BloodmoonTier> tiers = new HashMap<>();

    // Стан активного Кривавого Місяця
    private boolean active = false;
    private BloodmoonTier currentTier = null;
    private World activeWorld = null;
    private BossBar bossBar = null;
    private int hordesSpawned = 0;
    private int mobsKilled = 0;
    private long lastCheckedDay = -1;

    // Фоновий цикл перевірки часу та ефектів
    private BukkitTask mainCycleTask = null;
    private BukkitTask hordeTask = null;
    private BukkitTask particleTask = null;

    private final Set<UUID> activeBorderPlayers = ConcurrentHashMap.newKeySet();
    private boolean registered = false;

    private Particle redParticle;
    private Particle.DustOptions redDustOptions;

    public BloodmoonMode(MineCord plugin) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, "bloodmoon_mob");
        this.bossKey = new NamespacedKey(plugin, "bloodmoon_boss");

        initParticle();
        registerDefaultTiers();
    }

    private void initParticle() {
        try {
            this.redParticle = Particle.valueOf("DUST");
        } catch (IllegalArgumentException e) {
            this.redParticle = Particle.REDSTONE;
        }
        this.redDustOptions = new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.2f);
    }

    private void registerDefaultTiers() {
        tiers.put(1, new BloodmoonTier(
                1, "tier_1", "Кривавий Місяць", 40,
                2.5, 1, 1, 1, 70,
                2.5, 30.0, 6, 12,
                "§5§lКривавий Жнець", BarColor.RED, 0xDD0000
        ));
        tiers.put(2, new BloodmoonTier(
                2, "tier_2", "Кривавий Армагеддон", 35,
                4.5, 2, 2, 1, 85,
                4.0, 50.0, 10, 18,
                "§4§l☠ Володар Безодні ☠", BarColor.RED, 0xAA0000
        ));
        tiers.put(3, new BloodmoonTier(
                3, "tier_3", "Пекельний Катаклізм", 18,
                6.5, 2, 3, 2, 95,
                6.0, 65.0, 16, 26,
                "§4§l☠ Архідемон Смерті ☠", BarColor.PURPLE, 0x660033
        ));
        tiers.put(4, new BloodmoonTier(
                4, "tier_4", "☠ Судний День (Раґнарок) ☠", 7,
                10.0, 3, 4, 2, 100,
                10.0, 80.0, 22, 36,
                "§0§l☠ §4§lТИТАН ХАОСУ §0§l☠", BarColor.PURPLE, 0x2A0000
        ));
    }

    @Override
    public String getId() {
        return "bloodmoon";
    }

    @Override
    public String getName() {
        return "Кривавий Місяць";
    }

    @Override
    public String getDescription() {
        return "Періодичний нічний івент зі зростаючими складностями, ордами, босами та червоним небом.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        reloadConfig();
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }

        startMainCycle();
        checkAndResumeState();
    }

    @Override
    public void onDisable() {
        if (active) {
            saveState();
            cleanUpVisuals();
        }
        stopMainCycle();
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    @Override
    public void onReload() {
        reloadConfig();
    }

    private void reloadConfig() {
        enabled = plugin.getConfig().getBoolean("fun.modes.bloodmoon.enabled", true);
        chancePercent = plugin.getConfig().getDouble("fun.modes.bloodmoon.chance_percent", 5.0);
        durationMinutes = plugin.getConfig().getInt("fun.modes.bloodmoon.duration_minutes", 10);
        durationSeconds = Math.max(60, durationMinutes * 60);
        blockBeds = plugin.getConfig().getBoolean("fun.modes.bloodmoon.block_beds", true);
        redSkyEffects = plugin.getConfig().getBoolean("fun.modes.bloodmoon.red_sky_effects", true);
        hordesEnabled = plugin.getConfig().getBoolean("fun.modes.bloodmoon.hordes_enabled", true);
        hordeIntervalSeconds = plugin.getConfig().getInt("fun.modes.bloodmoon.horde_interval_seconds", 120);
        discordAnnouncements = plugin.getConfig().getBoolean("fun.modes.bloodmoon.discord_announcements", true);

        targetWorldNames.clear();
        List<String> wList = plugin.getConfig().getStringList("fun.modes.bloodmoon.worlds");
        if (wList != null && !wList.isEmpty()) {
            targetWorldNames.addAll(wList);
        } else {
            targetWorldNames.add("world");
        }

        // Завантаження конфігурації пресетів Tiers
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("fun.modes.bloodmoon.tiers");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection t = sec.getConfigurationSection(key);
                if (t == null) continue;
                int lvl = t.getInt("level", 1);
                String name = t.getString("name", "Кривавий Місяць");
                int weight = t.getInt("weight", 33);
                double hp = t.getDouble("health_multiplier", 1.5);
                int spd = t.getInt("speed_level", 0);
                int str = t.getInt("strength_level", 0);
                int res = t.getInt("resistance_level", 0);
                int armorChance = t.getInt("armor_chance", 30);
                double exp = t.getDouble("exp_multiplier", 2.0);
                double bossChance = t.getDouble("boss_chance", 10.0);
                int hordeMin = t.getInt("horde_min", 3);
                int hordeMax = t.getInt("horde_max", 6);
                String bossName = t.getString("boss_name", "§4§lКривавий Лицар");
                String bColorStr = t.getString("bar_color", "RED");
                BarColor bColor;
                try {
                    bColor = BarColor.valueOf(bColorStr.toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    bColor = BarColor.RED;
                }
                int hex = t.getInt("discord_color", 0xDD0000);

                tiers.put(lvl, new BloodmoonTier(
                        lvl, key, name, weight, hp, spd, str, res, armorChance,
                        exp, bossChance, hordeMin, hordeMax, bossName, bColor, hex
                ));
            }
        }
    }

    private void startMainCycle() {
        stopMainCycle();
        mainCycleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;
                tickCycle();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void stopMainCycle() {
        if (mainCycleTask != null) {
            mainCycleTask.cancel();
            mainCycleTask = null;
        }
        stopHordes();
        stopParticleTask();
    }

    /**
     * Основний щосекундний тік перевірки настання або завершення ночі.
     */
    private void tickCycle() {
        World world = getPrimaryWorld();
        if (world == null) return;

        long time = world.getTime();

        if (!active) {
            checkDuskRoll(world);
        } else {
            elapsedSeconds++;
            if (elapsedSeconds >= durationSeconds) {
                stopBloodmoon(true);
            } else {
                double progress = (double) elapsedSeconds / durationSeconds;
                long targetTime = 13000L + (long) (progress * 10000L);
                if (activeWorld != null) {
                    activeWorld.setTime(targetTime);
                }
                updateBossBar(durationSeconds - elapsedSeconds, progress);

                // Захист від сну: якщо будь-який гравець спить у ліжку, негайно вибити його
                if (blockBeds && activeWorld != null) {
                    for (Player p : activeWorld.getPlayers()) {
                        if (p.isSleeping()) {
                            p.wakeup(false);
                            p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                    "§4§l[!] §cКривавий Місяць не дає вам спати!"
                            ));
                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }
    }

    public void checkDuskRoll(World world) {
        if (!enabled || active || world == null) return;
        long time = world.getTime();
        // Перевірка заходу сонця (між 12541 та 13150 тіків)
        if (time >= 12541 && time <= 13150) {
            long currentDay = world.getFullTime() / 24000L;
            if (currentDay != lastCheckedDay) {
                lastCheckedDay = currentDay;
                double roll = ThreadLocalRandom.current().nextDouble(100.0);
                if (roll < chancePercent) {
                    BloodmoonTier tier = rollTier();
                    startBloodmoon(world, tier, false);
                }
            }
        }
    }

    private World getPrimaryWorld() {
        for (String wName : targetWorldNames) {
            World w = Bukkit.getWorld(wName);
            if (w != null) return w;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private BloodmoonTier rollTier() {
        int totalWeight = 0;
        for (BloodmoonTier t : tiers.values()) {
            totalWeight += Math.max(1, t.getWeight());
        }
        if (totalWeight <= 0) return tiers.getOrDefault(2, tiers.get(1));

        int rand = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;
        for (BloodmoonTier t : tiers.values()) {
            current += Math.max(1, t.getWeight());
            if (rand < current) {
                return t;
            }
        }
        return tiers.getOrDefault(2, tiers.get(1));
    }

    public boolean startBloodmoon(World world, int tierLevel, boolean manual) {
        BloodmoonTier tier = tiers.get(tierLevel);
        if (tier == null) {
            tier = tiers.getOrDefault(2, tiers.get(1));
        }
        if (tier == null) return false;
        startBloodmoon(world, tier, manual);
        return true;
    }

    public void startBloodmoon(World world, BloodmoonTier tier, boolean manual) {
        if (world == null || tier == null) return;

        if (world.getTime() < 13000 || world.getTime() >= 23000) {
            world.setTime(13000L);
        }

        this.active = true;
        this.activeWorld = world;
        this.currentTier = tier;
        this.hordesSpawned = 0;
        this.mobsKilled = 0;
        this.elapsedSeconds = 0;

        saveState();

        // Створення BossBar
        if (bossBar != null) {
            bossBar.removeAll();
        }
        String formattedTime = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60);
        bossBar = Bukkit.createBossBar(
                "§4§l🩸 КРИВАВИЙ МІСЯЦЬ §c[" + tier.getName() + "] §f[До світанку: §e" + formattedTime + "§f] §4§l🩸",
                tier.getBarColor(),
                BarStyle.SOLID
        );
        bossBar.setProgress(0.0);
        bossBar.setVisible(true);

        // Накладання візуалу та звуків для всіх гравців + примусовий викид із ліжок
        for (Player p : world.getPlayers()) {
            bossBar.addPlayer(p);
            applyVisualsToPlayer(p);

            if (p.isSleeping()) {
                p.wakeup(false);
                p.sendTitle(
                        "§4§lНЕ ЧАС ДЛЯ СНУ!",
                        "§cКривавий Місяць викинув вас із ліжка!",
                        10, 70, 20
                );
                p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                        "§4§l[!] §cКривавий Місяць зриває ваші сни! Прокидайтеся!"
                ));
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.9f);
            } else if (tier.getLevel() >= 4) {
                p.sendTitle(
                        "§4§l☠ СУДНИЙ ДЕНЬ (РАҐНАРОК) ☠",
                        "§cРівень загрози: §4§l" + tier.getName() + " (MAX)",
                        10, 100, 30
                );
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
                p.getWorld().strikeLightningEffect(p.getLocation());
            } else if (tier.getLevel() == 3) {
                p.sendTitle(
                        "§c§l🔥 ПЕКЕЛЬНИЙ КАТАКЛІЗМ 🔥",
                        "§cРівень загрози: §e" + tier.getName(),
                        10, 90, 25
                );
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.6f);
                p.getWorld().strikeLightningEffect(p.getLocation());
            } else if (tier.getLevel() == 2) {
                p.sendTitle(
                        "§4§l☠ КРИВАВИЙ АРМАГЕДДОН ☠",
                        "§cРівень загрози: §e" + tier.getName(),
                        10, 85, 25
                );
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
            } else {
                p.sendTitle(
                        "§4§l🩸 КРИВАВИЙ МІСЯЦЬ 🩸",
                        "§cРівень загрози: §f" + tier.getName(),
                        10, 80, 20
                );
            }

            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.7f);
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }

        // Повідомлення в ігровий чат
        String chatMsg;
        if (tier.getLevel() >= 4) {
            chatMsg = "\n§4§l☠━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☠\n" +
                    "  §4§l☠ НАСТАВ СУДНИЙ ДЕНЬ (РАҐНАРОК) — РІВЕНЬ IV! ☠\n" +
                    "  §cТИХИЙ ЖАХ ТА ТИТАНИ ХАОСУ ПРИЙШЛИ ЗА ВАШИМИ ДУШАМИ!\n" +
                    "  §c10x HP, повний незерит, смертельні орди до 36 мобів!\n" +
                    "  §4§lНЕМАЄ КУДИ ТІКАТИ — БИЙТЕСЯ ДО ОСТАННЬОЇ КРАПЛІ КРОВІ!\n" +
                    "§4§l☠━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☠\n";
        } else if (tier.getLevel() == 3) {
            chatMsg = "\n§c§l🔥━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━🔥\n" +
                    "  §c§l🔥 НАСТАВ ПЕКЕЛЬНИЙ КАТАКЛІЗМ (РІВЕНЬ III)! 🔥\n" +
                    "  §cАРХІДЕМОНИ СМЕРТІ ВЕДУТЬ ВІЙСЬКА ПЕКЛА!\n" +
                    "  §c6.5x HP, незеритова броня, гігантські орди та заряджені кріпери!\n" +
                    "  §4§lТРИМАЙТЕ ОБОРОНУ БАЗ ТА ГОТУЙТЕСЯ ДО БОЮ!\n" +
                    "§c§l🔥━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━🔥\n";
        } else if (tier.getLevel() == 2) {
            chatMsg = "\n§4§l☠━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☠\n" +
                    "  §4§l☠ НАСТАВ КРИВАВИЙ АРМАГЕДДОН (РІВЕНЬ II)! ☠\n" +
                    "  §cВОЛОДАРІ БЕЗОДНІ ТА ЗАХИСНІ ЧАРИ ЗАХОПИЛИ СВІТ!\n" +
                    "  §c4.5x HP, діамантове спорядження, орди до 18 монстрів!\n" +
                    "  §4§lТРИМАЙТЕ ОБОРОНУ ДО СВІТАНКУ!\n" +
                    "§4§l☠━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☠\n";
        } else {
            chatMsg = "\n§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "  §4§l🩸 КРИВАВИЙ МІСЯЦЬ ЗІЙШОВ НАД СВІТОМ (РІВЕНЬ I)! 🩸\n" +
                    "  §cКривавий Жнець та орди кровожерливих мерців вийшли на полювання!\n" +
                    "  §c2.5x HP, діамантова броня! Сон у ліжках заблоковано!\n" +
                    "  §4§lТРИМАЙТЕ ОБОРОНУ БАЗ ТА ГОТУЙТЕ ЗБРОЮ!\n" +
                    "§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        }
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(chatMsg));

        // Відправка повідомлення в Discord
        sendDiscordStartEmbed(tier);

        // Запуск тасків орд та червоного попелу
        startHordes();
        startParticleTask();

        plugin.getLogger().info("[BloodmoonMode] Кривавий Місяць активовано! Рівень: " + tier.getName() + " (" + tier.getLevel() + ")");
    }

    public void stopBloodmoon(boolean naturallyEnded) {
        if (!active) return;

        this.active = false;
        clearStateFile();

        this.elapsedSeconds = 0;

        cleanUpVisuals();
        stopHordes();
        stopParticleTask();

        if (naturallyEnded && activeWorld != null) {
            activeWorld.setTime(0L);
            if (activeWorld.hasStorm()) {
                activeWorld.setStorm(false);
                activeWorld.setThundering(false);
            }

            for (Player p : activeWorld.getPlayers()) {
                p.sendTitle(
                        "§6§lСВІТАНОК НАСТАВ",
                        "§aКривавий Місяць відступив. Ви вижили!",
                        10, 70, 20
                );
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                    "\n§2§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "  §a§l🌅 СВІТАНОК НАСТАВ! КРИВАВИЙ МІСЯЦЬ ВІДСТУПИВ! 🌅\n" +
                    "  §7Сервер успішно пережив ніч кошмару!\n" +
                    "  §7Відбито хвиль орд: §e" + hordesSpawned + "§7 | Знищено монстрів: §e" + mobsKilled + "\n" +
                    "§2§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
            ));

            sendDiscordEndEmbed();
        }

        plugin.getLogger().info("[BloodmoonMode] Кривавий Місяць завершено.");
        this.activeWorld = null;
        this.currentTier = null;
    }

    private void updateBossBar(long remainingSeconds, double progress) {
        if (bossBar == null) return;

        long safeRemaining = Math.max(0, remainingSeconds);
        String formatted = String.format("%02d:%02d", safeRemaining / 60, safeRemaining % 60);

        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bossBar.setTitle("§4§l🩸 КРИВАВИЙ МІСЯЦЬ §c[" + (currentTier != null ? currentTier.getName() : "") + "] §f[До світанку: §e" + formatted + "§f] §4§l🩸");
    }

    private void applyVisualsToPlayer(Player player) {
        if (!redSkyEffects) return;

        // 1. Червона імла/віньєтка через персональний WorldBorder
        try {
            WorldBorder border = Bukkit.createWorldBorder();
            border.setCenter(player.getLocation());
            border.setSize(1000.0);
            border.setWarningDistance(1000);
            player.setWorldBorder(border);
            activeBorderPlayers.add(player.getUniqueId());
        } catch (Throwable ignored) {}

        // 2. Темна штормова погода
        player.setPlayerWeather(WeatherType.DOWNFALL);
    }

    private void removeVisualsFromPlayer(Player player) {
        try {
            player.setWorldBorder(null);
        } catch (Throwable ignored) {}
        player.resetPlayerWeather();
        activeBorderPlayers.remove(player.getUniqueId());
    }

    private void cleanUpVisuals() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        for (UUID uuid : activeBorderPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                removeVisualsFromPlayer(p);
            }
        }
        activeBorderPlayers.clear();
    }

    private void startParticleTask() {
        stopParticleTask();
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || activeWorld == null || !redSkyEffects) {
                    cancel();
                    return;
                }

                for (Player p : activeWorld.getPlayers()) {
                    Location pLoc = p.getLocation();
                    for (int i = 0; i < 5; i++) {
                        double dx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 12.0;
                        double dy = ThreadLocalRandom.current().nextDouble() * 4.0;
                        double dz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 12.0;
                        p.spawnParticle(redParticle, pLoc.clone().add(dx, dy, dz), 1, redDustOptions);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void stopParticleTask() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    private void startHordes() {
        stopHordes();
        if (!hordesEnabled) return;

        hordeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || activeWorld == null || currentTier == null) {
                    cancel();
                    return;
                }

                // Захист від лагів: якщо TPS < 18.0, пропускаємо хвилю
                double currentTps = 20.0;
                try {
                    currentTps = Bukkit.getServer().getTPS()[0];
                } catch (Throwable ignored) {}
                if (currentTps < 18.0) {
                    return;
                }

                List<Player> players = new ArrayList<>(activeWorld.getPlayers());
                if (players.isEmpty()) return;

                Player target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                spawnHordeNearPlayer(target);
            }
        }.runTaskTimer(plugin, hordeIntervalSeconds * 20L, hordeIntervalSeconds * 20L);
    }

    private void stopHordes() {
        if (hordeTask != null) {
            hordeTask.cancel();
            hordeTask = null;
        }
    }

    /**
     * Знаходить безпечну точку для спавну мобів/боса на рівні гравця (в кімнаті, печері або на поверхні).
     */
    private Location findSafeSpawnLocation(Location center, double minRadius, double maxRadius, int heightRequired) {
        World world = center.getWorld();
        if (world == null) return null;

        int playerY = center.getBlockY();

        // 1. Пошук підлоги на рівні гравця (+3 до -6 блоків)
        for (int attempt = 0; attempt < 25; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            double dist = ThreadLocalRandom.current().nextDouble(minRadius, maxRadius);
            int x = center.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * dist);

            for (int dy = 3; dy >= -6; dy--) {
                int y = playerY + dy;
                org.bukkit.block.Block floor = world.getBlockAt(x, y - 1, z);

                if (!floor.getType().isSolid() || floor.isLiquid()) {
                    continue;
                }

                boolean spaceOk = true;
                for (int h = 0; h < heightRequired; h++) {
                    org.bukkit.block.Block space = world.getBlockAt(x, y + h, z);
                    if (space.getType().isSolid() || space.isLiquid()) {
                        spaceOk = false;
                        break;
                    }
                }

                if (spaceOk) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
            }
        }

        // 2. Якщо відкрита поверхня з перепадами рельєфу
        int highestY = world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ());
        if (Math.abs(highestY - playerY) <= 8) {
            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
                double dist = ThreadLocalRandom.current().nextDouble(minRadius, maxRadius);
                int x = center.getBlockX() + (int) (Math.cos(angle) * dist);
                int z = center.getBlockZ() + (int) (Math.sin(angle) * dist);
                int hY = world.getHighestBlockYAt(x, z);
                if (Math.abs(hY - playerY) <= 8) {
                    org.bukkit.block.Block floor = world.getBlockAt(x, hY - 1, z);
                    if (floor.getType().isSolid() && !floor.isLiquid()) {
                        return new Location(world, x + 0.5, hY, z + 0.5);
                    }
                }
            }
        }

        // 3. Fallback: безпосередньо позаду гравця
        org.bukkit.util.Vector dir = center.getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) {
            dir = new org.bukkit.util.Vector(1, 0, 0);
        } else {
            dir.normalize();
        }
        return center.clone().add(dir.multiply(-3.5));
    }

    private void spawnHordeNearPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        Location pLoc = player.getLocation();
        int count = ThreadLocalRandom.current().nextInt(currentTier.getHordeMin(), currentTier.getHordeMax() + 1);

        hordesSpawned++;
        saveState();

        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize("§4§l[!] §cОрда Кривавого Місяця атакує вас! §4§l[!]"));
        player.playSound(pLoc, Sound.EVENT_RAID_HORN, 1.0f, 0.8f);

        EntityType[] possibleTypes = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER};

        for (int i = 0; i < count; i++) {
            Location spawnLoc = findSafeSpawnLocation(pLoc, 6.0, 14.0, 2);
            if (spawnLoc == null) {
                spawnLoc = pLoc.clone().add(ThreadLocalRandom.current().nextDouble(-3, 3), 0, ThreadLocalRandom.current().nextDouble(-3, 3));
            }

            EntityType type = possibleTypes[ThreadLocalRandom.current().nextInt(possibleTypes.length)];
            Entity entity = player.getWorld().spawnEntity(spawnLoc, type, CreatureSpawnEvent.SpawnReason.CUSTOM);
            if (entity instanceof Monster monster) {
                buffMonster(monster, currentTier);
                monster.setTarget(player);
                AttributeInstance followAttr = monster.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
                if (followAttr != null) {
                    followAttr.setBaseValue(40.0);
                }
            }
        }

        // Перевірка на спавн міні-боса
        if (currentTier.getBossChancePercent() > 0 && ThreadLocalRandom.current().nextDouble(100.0) < currentTier.getBossChancePercent()) {
            spawnBossNearPlayer(player);
        }
    }

    private void spawnBossNearPlayer(Player player) {
        Location pLoc = player.getLocation();
        Location spawnLoc = findSafeSpawnLocation(pLoc, 5.0, 10.0, 3);
        if (spawnLoc == null) {
            spawnLoc = pLoc.clone().add(pLoc.getDirection().multiply(-4).setY(0));
        }

        EntityType bossType = (currentTier.getLevel() >= 3) ? EntityType.WITHER_SKELETON : EntityType.ZOMBIE;
        Entity entity = player.getWorld().spawnEntity(spawnLoc, bossType, CreatureSpawnEvent.SpawnReason.CUSTOM);

        if (entity instanceof Monster boss) {
            PersistentDataContainer pdc = boss.getPersistentDataContainer();
            pdc.set(mobKey, PersistentDataType.BYTE, (byte) currentTier.getLevel());
            pdc.set(bossKey, PersistentDataType.BYTE, (byte) 1);

            boss.setCustomName(currentTier.getBossName());
            boss.setCustomNameVisible(true);
            boss.setGlowing(true);
            boss.setRemoveWhenFarAway(false);
            boss.setTarget(player);

            AttributeInstance followAttr = boss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
            if (followAttr != null) {
                followAttr.setBaseValue(64.0);
            }

            double bossHp;
            if (currentTier.getLevel() >= 4) {
                bossHp = 1400.0;
            } else if (currentTier.getLevel() == 3) {
                bossHp = 850.0;
            } else if (currentTier.getLevel() == 2) {
                bossHp = 550.0;
            } else {
                bossHp = 300.0;
            }
            AttributeInstance hpAttr = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.setBaseValue(bossHp);
                boss.setHealth(bossHp);
            }

            int speedAmp = (currentTier.getLevel() >= 4) ? 3 : (currentTier.getLevel() >= 2 ? 2 : 1);
            int strAmp = (currentTier.getLevel() >= 4) ? 3 : (currentTier.getLevel() >= 3 ? 2 : 1);
            int resAmp = (currentTier.getLevel() >= 3) ? 2 : 1;

            int bDuration = (durationMinutes + 5) * 60 * 20;
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, bDuration, speedAmp, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, bDuration, strAmp, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, bDuration, resAmp, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, bDuration, 0, false, false));
            if (currentTier.getLevel() >= 3) {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, bDuration, 1, false, false));
            }

            EntityEquipment eq = boss.getEquipment();
            if (eq != null) {
                eq.setHelmetDropChance(0.0f);
                eq.setChestplateDropChance(0.0f);
                eq.setLeggingsDropChance(0.0f);
                eq.setBootsDropChance(0.0f);
                eq.setItemInMainHandDropChance(0.0f);
                eq.setItemInOffHandDropChance(0.0f);

                if (currentTier.getLevel() >= 4) {
                    ItemStack helm = new ItemStack(Material.NETHERITE_HELMET);
                    helm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    eq.setHelmet(helm);

                    ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                    chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    chest.addEnchantment(Enchantment.THORNS, 3);
                    eq.setChestplate(chest);

                    ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                    legs.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    eq.setLeggings(legs);

                    ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
                    boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    eq.setBoots(boots);

                    ItemStack weapon = new ItemStack(Material.NETHERITE_SWORD);
                    weapon.addEnchantment(Enchantment.DAMAGE_ALL, 5);
                    weapon.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                    weapon.addEnchantment(Enchantment.KNOCKBACK, 2);
                    eq.setItemInMainHand(weapon);
                } else if (currentTier.getLevel() == 3) {
                    ItemStack helm = new ItemStack(Material.NETHERITE_HELMET);
                    helm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    eq.setHelmet(helm);

                    ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                    chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    chest.addEnchantment(Enchantment.THORNS, 2);
                    eq.setChestplate(chest);

                    ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                    legs.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    eq.setLeggings(legs);

                    ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
                    boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                    eq.setBoots(boots);

                    ItemStack weapon = new ItemStack(Material.NETHERITE_SWORD);
                    weapon.addEnchantment(Enchantment.DAMAGE_ALL, 5);
                    weapon.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                    eq.setItemInMainHand(weapon);
                } else if (currentTier.getLevel() == 2) {
                    ItemStack helm = new ItemStack(Material.NETHERITE_HELMET);
                    helm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
                    eq.setHelmet(helm);

                    ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
                    chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
                    eq.setChestplate(chest);

                    ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
                    legs.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
                    eq.setLeggings(legs);

                    ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
                    boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
                    eq.setBoots(boots);

                    ItemStack weapon = new ItemStack(Material.NETHERITE_SWORD);
                    weapon.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                    eq.setItemInMainHand(weapon);
                } else {
                    eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                    eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                    ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);
                    weapon.addEnchantment(Enchantment.FIRE_ASPECT, 1);
                    eq.setItemInMainHand(weapon);
                }
            }

            // Візуальні ефекти та звук появи прямо біля гравця
            player.getWorld().strikeLightningEffect(spawnLoc);
            player.getWorld().spawnParticle(Particle.FLAME, spawnLoc.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);

            int distInt = (int) Math.round(player.getLocation().distance(spawnLoc));
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                    "§4§l☠ БОС " + currentTier.getBossName() + " §4§lПОВСТАВ ПОРУЧ! (" + distInt + "м) ☠"
            ));

            if (currentTier.getLevel() >= 4) {
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        "§0§l☠§4§l☠§0§l☠ [СУДНИЙ ДЕНЬ] ТИТАН ХАОСУ ПОВСТАВ! §r" + currentTier.getBossName() + "§4§l біля гравця §e" + player.getName() + "§4§l! ХАЙ ДОПОМОЖУТЬ ВАМ БОГИ! ☠"
                ));
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
            } else if (currentTier.getLevel() == 3) {
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        "§4§l☠ [КАТАКЛІЗМ] ПОВСТАВ АРХІДЕМОН СМЕРТІ! §r" + currentTier.getBossName() + "§4§l біля гравця §e" + player.getName() + "§4§l! ☠"
                ));
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.7f);
            } else if (currentTier.getLevel() == 2) {
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        "§4§l☠ [АРМАГЕДДОН] ВОЛОДАР БЕЗОДНІ ПОВСТАВ! §r" + currentTier.getBossName() + "§4§l біля гравця §e" + player.getName() + "§4§l! ☠"
                ));
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.8f);
            } else {
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        "§4§l[!] §cСходження Боса: §r" + currentTier.getBossName() + "§c біля гравця §e" + player.getName() + "§c!"
                ));
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 1.2f, 1.0f);
            }
        }
    }

    private void buffMonster(Monster monster, BloodmoonTier tier) {
        if (monster == null || tier == null) return;

        PersistentDataContainer pdc = monster.getPersistentDataContainer();
        pdc.set(mobKey, PersistentDataType.BYTE, (byte) tier.getLevel());

        AttributeInstance hp = monster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) {
            double newHp = hp.getBaseValue() * tier.getHealthMultiplier();
            hp.setBaseValue(newHp);
            monster.setHealth(newHp);
        }

        // Кріпери НЕ повинні отримувати PotionEffect!
        // У ванільному Майнкрафті, якщо кріпер із зіллям вибухає, він створює
        // AreaEffectCloud (хмару осідання зілля) з цими ефектами, і гравці отримують бафи на роки.
        if (monster instanceof Creeper creeper) {
            if (tier.getLevel() >= 2) {
                creeper.setPowered(true);
            }
            if (tier.getSpeedLevel() > 0) {
                AttributeInstance spd = creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (spd != null) {
                    spd.setBaseValue(spd.getBaseValue() * (1.0 + 0.18 * tier.getSpeedLevel()));
                }
            }
            if (tier.getLevel() >= 2) {
                creeper.setGlowing(true);
            }
            return;
        }

        int effectDurationTicks = (durationMinutes + 5) * 60 * 20;

        if (tier.getSpeedLevel() > 0) {
            monster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, effectDurationTicks, tier.getSpeedLevel() - 1, false, false));
        }
        if (tier.getStrengthLevel() > 0) {
            monster.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, effectDurationTicks, tier.getStrengthLevel() - 1, false, false));
        }
        if (tier.getResistanceLevel() > 0) {
            monster.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, effectDurationTicks, tier.getResistanceLevel() - 1, false, false));
        }

        if (tier.getLevel() >= 2) {
            monster.setGlowing(true);
        }

        // Екіпірування бронею (тільки людиноподібні моби: зомбі, скелети, пігліни)
        boolean canWearArmor = (monster instanceof Zombie || monster instanceof Skeleton || monster instanceof PiglinAbstract);
        if (canWearArmor && ThreadLocalRandom.current().nextInt(100) < tier.getArmorChancePercent()) {
            EntityEquipment eq = monster.getEquipment();
            if (eq != null) {
                // ОБОВ'ЯЗКОВО 0.0f шанс випадання надітої броні та зброї!
                // Броня мобів призначена тільки для захисту мобів у бою, а не для дюпу алмазних/незеритових сетів!
                eq.setHelmetDropChance(0.0f);
                eq.setChestplateDropChance(0.0f);
                eq.setLeggingsDropChance(0.0f);
                eq.setBootsDropChance(0.0f);
                eq.setItemInMainHandDropChance(0.0f);
                eq.setItemInOffHandDropChance(0.0f);

                if (tier.getLevel() >= 4) {
                    // Рівень 4 (Раґнарок): Повний незерит, максимальні чари
                    eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    eq.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                    if (monster instanceof Skeleton) {
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
                        bow.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
                        eq.setItemInMainHand(bow);
                    } else {
                        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                        sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
                        sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                        sword.addEnchantment(Enchantment.KNOCKBACK, 2);
                        eq.setItemInMainHand(sword);
                    }
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, effectDurationTicks, 0, false, false));
                } else if (tier.getLevel() == 3) {
                    // Рівень 3 (Катаклізм): Повний незерит, потужні чари
                    eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    eq.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                    if (monster instanceof Skeleton) {
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 4);
                        eq.setItemInMainHand(bow);
                    } else {
                        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                        sword.addEnchantment(Enchantment.DAMAGE_ALL, 4);
                        sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
                        eq.setItemInMainHand(sword);
                    }
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, effectDurationTicks, 0, false, false));
                } else if (tier.getLevel() == 2) {
                    // Рівень 2 (Армагеддон): Діамант + Незерит
                    eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                    if (monster instanceof Skeleton) {
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
                        eq.setItemInMainHand(bow);
                    } else {
                        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                        sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
                        sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
                        eq.setItemInMainHand(sword);
                    }
                } else {
                    // Рівень 1 (Кривавий Місяць): Повний діамантовий сет
                    eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                    eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                    if (monster instanceof Skeleton) {
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        eq.setItemInMainHand(bow);
                    } else {
                        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                        sword.addEnchantment(Enchantment.DAMAGE_ALL, 2);
                        eq.setItemInMainHand(sword);
                    }
                }
            }
        }
    }

    // ==========================================
    // Збереження та відновлення стану (Рестарт)
    // ==========================================

    private File getStateFile() {
        return new File(plugin.getDataFolder(), "bloodmoon_state.yml");
    }

    private void saveState() {
        File file = getStateFile();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("active", active);
        if (active && activeWorld != null && currentTier != null) {
            yaml.set("world", activeWorld.getName());
            yaml.set("tier_level", currentTier.getLevel());
            yaml.set("hordes_spawned", hordesSpawned);
            yaml.set("mobs_killed", mobsKilled);
            yaml.set("elapsed_seconds", elapsedSeconds);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[BloodmoonMode] Не вдалося зберегти bloodmoon_state.yml: " + e.getMessage());
        }
    }

    private void clearStateFile() {
        File file = getStateFile();
        if (file.exists()) {
            file.delete();
        }
    }

    private void checkAndResumeState() {
        File file = getStateFile();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.getBoolean("active", false)) {
            clearStateFile();
            return;
        }

        String wName = yaml.getString("world");
        World world = (wName != null) ? Bukkit.getWorld(wName) : null;
        if (world == null) {
            clearStateFile();
            return;
        }

        int savedElapsed = yaml.getInt("elapsed_seconds", 0);
        if (savedElapsed < durationSeconds) {
            int tierLvl = yaml.getInt("tier_level", 2);
            BloodmoonTier tier = tiers.getOrDefault(tierLvl, tiers.get(2));
            this.hordesSpawned = yaml.getInt("hordes_spawned", 0);
            this.mobsKilled = yaml.getInt("mobs_killed", 0);

            startBloodmoon(world, tier, false);
            this.elapsedSeconds = savedElapsed;
            double progress = (double) elapsedSeconds / durationSeconds;
            long targetTime = 13000L + (long) (progress * 10000L);
            world.setTime(targetTime);
            updateBossBar(durationSeconds - elapsedSeconds, progress);
            plugin.getLogger().info("[BloodmoonMode] Стан Кривавого Місяця відновлено після рестарту! Світ: " + world.getName() + " (Пройшло: " + savedElapsed + "с / " + durationSeconds + "с)");
        } else {
            clearStateFile();
        }
    }

    // ==========================================
    // Інтеграція з Discord
    // ==========================================

    private TextChannel getDiscordChatChannel() {
        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) return null;
        String channelId = plugin.getConfig().getString("discord.chat-channel-id");
        if (channelId == null || channelId.equals("000000000000000000") || channelId.trim().isEmpty()) return null;
        try {
            return plugin.getBotManager().getJda().getTextChannelById(channelId.trim());
        } catch (Throwable t) {
            return null;
        }
    }

    private void sendDiscordStartEmbed(BloodmoonTier tier) {
        if (!discordAnnouncements) return;

        try {
            TextChannel channel = getDiscordChatChannel();
            if (channel == null) return;

            EmbedBuilder eb = new EmbedBuilder();
            if (tier.getLevel() >= 4) {
                eb.setTitle("☠️ [РАҐНАРОК] СУДНИЙ ДЕНЬ — РІВЕНЬ IV! ☠️");
            } else if (tier.getLevel() == 3) {
                eb.setTitle("🔥 [КАТАКЛІЗМ] ПЕКЕЛЬНИЙ КАТАКЛІЗМ — РІВЕНЬ III! 🔥");
            } else if (tier.getLevel() == 2) {
                eb.setTitle("☠️ [АРМАГЕДДОН] КРИВАВИЙ АРМАГЕДДОН — РІВЕНЬ II! ☠️");
            } else {
                eb.setTitle("🩸 КРИВАВИЙ МІСЯЦЬ ЗІЙШОВ НАД СВІТОМ! 🩸");
            }
            eb.setColor(tier.getDiscordColorHex());
            String desc;
            if (tier.getLevel() >= 4) {
                desc = "**Рівень загрози:** `☠ " + tier.getName() + " ☠` (МАКСИМАЛЬНИЙ РІВЕНЬ 4)\n\n" +
                        "🔥 **ТИХИЙ ЖАХ І ТИТАНИ ХАОСУ ПОГЛИНУЛИ СВІТ:**\n" +
                        "• Сон у ліжках повністю унеможливлено!\n" +
                        "• Монстри посилені у **10.0 разів** (10x HP, Сила IV, Опір II, Швидкість III)!\n" +
                        "• 100% захист: повні комплекти зачарованого незериту (Захист IV, Шипи III)!\n" +
                        "• Гігантські орди до 36 монстрів та швидкісні заряджені кріпери!\n" +
                        "• Повстає **☠ ТИТАН ХАОСУ ☠** (1400 HP)!\n" +
                        "• За перемогу над босом: **3-4 Зірки Незеру, Блок Незериту, 3-4 Тотеми, Блоки Діамантів, Яблука Нотча та 10000 EXP**!\n\n" +
                        "⚰️ *Шанси пережити цю ніч мізерні. Бийтеся до останнього подиху!*";
            } else if (tier.getLevel() == 3) {
                desc = "**Рівень загрози:** `🔥 " + tier.getName() + " 🔥` (РІВЕНЬ 3)\n\n" +
                        "⚔️ **ПЕКЕЛЬНІ ЛЕГІОНИ ТА АРХІДЕМОНИ СМЕРТІ:**\n" +
                        "• Сон у ліжках заблоковано до настання світанку!\n" +
                        "• Монстри посилені у **6.5 разів** (6.5x HP, Сила III, Опір II)!\n" +
                        "• 95% монстрів у зачарованому незериті з гострими мечами!\n" +
                        "• Заряджені кріпери та невпинні орди до 26 монстрів!\n" +
                        "• Повстає **☠ Архідемон Смерті ☠** (850 HP)!\n" +
                        "• За перемогу над босом: **2 Зірки Незеру, 2-4 незеритові зливки, 2-3 Тотеми, Блоки Діамантів та 6000 EXP**!\n\n" +
                        "🛡️ *Збирайтеся у фортецях та тримайте оборону!*";
            } else if (tier.getLevel() == 2) {
                desc = "**Рівень загрози:** `☠ " + tier.getName() + " ☠` (РІВЕНЬ 2)\n\n" +
                        "⚡ **АРМАГЕДДОН ПРИЙШОВ НА СЕРВЕР:**\n" +
                        "• Сон у ліжках заблоковано!\n" +
                        "• Монстри посилені у **4.5 рази** (4.5x HP, Сила II, Опір I)!\n" +
                        "• Незеритова й діамантова броня, заряджені кріпери та орди до 18 монстрів!\n" +
                        "• Повстає **Володар Безодні** (550 HP)!\n" +
                        "• За перемогу над босом: **1 Зірка Незеру, 2-3 незеритові зливки, 2 Тотеми, Блоки Діамантів та 4000 EXP**!\n\n" +
                        "⚔️ *Приготуйтеся до важкої битви!*";
            } else {
                desc = "**Рівень загрози:** `" + tier.getName() + "` (РІВЕНЬ 1)\n\n" +
                        "⚠️ **Увага всім гравцям на сервері:**\n" +
                        "• Сон у ліжках заблоковано до світанку!\n" +
                        "• Монстри отримали **2.5x здоров'я**, бафи Швидкості та Сили!\n" +
                        "• Орди монстрів до 12 створінь у діамантовому спорядженні!\n" +
                        "• Повстає бос **«Кривавий Жнець»** (300 HP)!\n" +
                        "• За перемогу над босом: **100% Тотем Безсмертя, Незеритовий зливок, Зачароване Золоте Яблуко, Блок Діамантів та 2000 EXP**!\n\n" +
                        "🛡️ *Тримайте оборону баз та готуйте зброю!*";
            }
            eb.setDescription(desc);
            eb.setTimestamp(Instant.now());
            eb.setFooter("MineCord Bloodmoon Event", null);

            channel.sendMessageEmbeds(eb.build()).queue(null, (err) -> {});
        } catch (Throwable t) {
            plugin.getLogger().warning("[BloodmoonMode] Помилка надсилання ембеду початку в Discord: " + t.getMessage());
        }
    }

    private void sendDiscordEndEmbed() {
        if (!discordAnnouncements) return;

        try {
            TextChannel channel = getDiscordChatChannel();
            if (channel == null) return;

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("🌅 СВІТАНОК НАСТАВ — КРИВАВИЙ МІСЯЦЬ ВІДСТУПИВ 🌅");
            eb.setColor(0x2ECC71);
            eb.setDescription(
                    "Ніч жаху завершилась. Гравці сервера успішно пережили випробування!\n\n" +
                    "📊 **Статистика ночі:**\n" +
                    "• Відбито атак орд: `" + hordesSpawned + "`\n" +
                    "• Знищено кривавих монстрів: `" + mobsKilled + "`"
            );
            eb.setTimestamp(Instant.now());
            eb.setFooter("MineCord Bloodmoon Event", null);

            channel.sendMessageEmbeds(eb.build()).queue(null, (err) -> {});
        } catch (Throwable t) {
            plugin.getLogger().warning("[BloodmoonMode] Помилка надсилання ембеду світанку в Discord: " + t.getMessage());
        }
    }

    // ==========================================
    // Event Handlers
    // ==========================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!active || !blockBeds) return;
        if (activeWorld != null && event.getPlayer().getWorld().equals(activeWorld)) {
            event.setCancelled(true);
            Player p = event.getPlayer();
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                    "§4§l[!] §cКривавий Місяць не дає вам спати! Монстри оточують вас..."
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!active || activeWorld == null || currentTier == null) return;
        if (!event.getLocation().getWorld().equals(activeWorld)) return;

        // Захист від ферм мобів: спавнери, розмноження чешуйниць, поділ слаймів НЕ беруть участі у Кривавому Місяці!
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || reason == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT
                || reason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_WITHER
                || reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }

        // Чешуйниці, ендерміти, слайми не беруть участі
        LivingEntity entity = event.getEntity();
        if (entity instanceof Silverfish || entity instanceof Endermite || entity instanceof Slime) {
            return;
        }

        if (event.getEntity() instanceof Monster monster) {
            buffMonster(monster, currentTier);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof Creeper creeper) {
            // Очищаємо будь-які ефекти зілля перед вибухом, щоб не утворювалась хмара AreaEffectCloud
            for (PotionEffect effect : creeper.getActivePotionEffects()) {
                creeper.removePotionEffect(effect.getType());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAreaCloudApply(AreaEffectCloudApplyEvent event) {
        // Якщо хмара містить надто тривалі ефекти (> 3 хвилин), блокуємо їх накладання на гравців
        boolean tooLong = false;
        for (PotionEffect effect : event.getEntity().getCustomEffects()) {
            if (effect.getDuration() > 20 * 60 * 3) {
                tooLong = true;
                break;
            }
        }
        if (tooLong) {
            event.getAffectedEntities().removeIf(e -> e instanceof Player);
        }
    }

    private void spawnBossVictoryFirework(Location loc) {
        try {
            World w = loc.getWorld();
            if (w == null) return;
            Firework fw = w.spawn(loc.clone().add(0, 1, 0), Firework.class);
            FireworkMeta fwm = fw.getFireworkMeta();
            fwm.addEffect(FireworkEffect.builder()
                    .withColor(Color.RED, Color.ORANGE, Color.YELLOW)
                    .withFade(Color.PURPLE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withTrail()
                    .withFlicker()
                    .build());
            fwm.setPower(1);
            fw.setFireworkMeta(fwm);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!active || currentTier == null) return;

        LivingEntity entity = event.getEntity();
        if (entity instanceof Silverfish || entity instanceof Endermite || entity instanceof Slime) {
            return;
        }

        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        if (pdc.has(mobKey, PersistentDataType.BYTE)) {
            // Захист від автоферм (за принципом BloodmoonReloaded): кастомний дроп тільки якщо моба вбив гравець
            if (entity.getKiller() == null) {
                return;
            }

            mobsKilled++;

            // Збільшення досвіду
            event.setDroppedExp((int) (event.getDroppedExp() * currentTier.getExpMultiplier()));

            boolean isBoss = pdc.has(bossKey, PersistentDataType.BYTE);
            Location loc = entity.getLocation();
            World w = loc.getWorld();
            if (w == null) return;

            if (isBoss) {
                // Додатковий досвід за боса
                w.spawn(loc, ExperienceOrb.class).setExperience(currentTier.getLevel() * 500);

                if (currentTier.getLevel() >= 4) {
                    // Рівень 4: Судний День (Раґнарок) — Божественний дроп
                    w.spawn(loc, ExperienceOrb.class).setExperience(10000);
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHER_STAR, ThreadLocalRandom.current().nextInt(3, 5)));
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_BLOCK, 1));
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, ThreadLocalRandom.current().nextInt(4, 7)));
                    w.dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING, ThreadLocalRandom.current().nextInt(3, 5)));
                    w.dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(4, 7)));
                    w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND_BLOCK, ThreadLocalRandom.current().nextInt(4, 7)));
                    w.dropItemNaturally(loc, new ItemStack(Material.GOLD_BLOCK, ThreadLocalRandom.current().nextInt(8, 13)));
                    w.dropItemNaturally(loc, new ItemStack(Material.IRON_BLOCK, ThreadLocalRandom.current().nextInt(10, 17)));
                    w.dropItemNaturally(loc, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
                } else if (currentTier.getLevel() == 3) {
                    // Рівень 3: Пекельний Катаклізм — Міфічний дроп
                    w.spawn(loc, ExperienceOrb.class).setExperience(6000);
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHER_STAR, 2));
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, ThreadLocalRandom.current().nextInt(2, 5)));
                    w.dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING, ThreadLocalRandom.current().nextInt(2, 4)));
                    w.dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(2, 5)));
                    w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND_BLOCK, 3));
                    w.dropItemNaturally(loc, new ItemStack(Material.GOLD_BLOCK, ThreadLocalRandom.current().nextInt(5, 9)));
                    w.dropItemNaturally(loc, new ItemStack(Material.IRON_BLOCK, ThreadLocalRandom.current().nextInt(6, 11)));
                    w.dropItemNaturally(loc, new ItemStack(Material.EXPERIENCE_BOTTLE, 48));
                } else if (currentTier.getLevel() == 2) {
                    // Рівень 2: Кривавий Армагеддон — Легендарний дроп
                    w.spawn(loc, ExperienceOrb.class).setExperience(4000);
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHER_STAR, 1));
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, ThreadLocalRandom.current().nextInt(2, 4)));
                    w.dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING, 2));
                    w.dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(2, 4)));
                    w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND_BLOCK, 2));
                    w.dropItemNaturally(loc, new ItemStack(Material.GOLD_BLOCK, ThreadLocalRandom.current().nextInt(3, 7)));
                    w.dropItemNaturally(loc, new ItemStack(Material.IRON_BLOCK, ThreadLocalRandom.current().nextInt(4, 9)));
                    w.dropItemNaturally(loc, new ItemStack(Material.EXPERIENCE_BOTTLE, 32));
                } else {
                    // Рівень 1: Кривавий Місяць — Епічний гарантований дроп
                    w.spawn(loc, ExperienceOrb.class).setExperience(2000);
                    w.dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, 1));
                    w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, ThreadLocalRandom.current().nextInt(2, 5)));
                    w.dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                    w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND_BLOCK, 1));
                    w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(12, 21)));
                    w.dropItemNaturally(loc, new ItemStack(Material.GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(4, 9)));
                    w.dropItemNaturally(loc, new ItemStack(Material.GOLD_BLOCK, ThreadLocalRandom.current().nextInt(2, 5)));
                    w.dropItemNaturally(loc, new ItemStack(Material.IRON_BLOCK, ThreadLocalRandom.current().nextInt(3, 7)));
                    w.dropItemNaturally(loc, new ItemStack(Material.EXPERIENCE_BOTTLE, 24));
                }

                // Святковий феєрверк тріумфу при падінні боса
                spawnBossVictoryFirework(loc);

                Player killer = entity.getKiller();
                String killerName = (killer != null) ? killer.getName() : "Герої";
                if (currentTier.getLevel() >= 4) {
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§4§l☠ [БОЖЕСТВЕННА ЛЕГЕНДА] Гравець §e" + killerName + " §4§lздолав ТИТАНА ХАОСУ §r" + entity.getCustomName() + "§4§l! 4 Зірки Незеру, Блок Незериту та тотеми випали на землю! ☠"
                    ));
                } else if (currentTier.getLevel() == 3) {
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§c§l☠ [МІФІЧНИЙ ПОДВИГ] Гравець §e" + killerName + " §c§lвигнав АРХІДЕМОНА СМЕРТІ §r" + entity.getCustomName() + "§c§l! 2 Зірки Незеру, незерит та тотеми випали на землю! ☠"
                    ));
                } else if (currentTier.getLevel() == 2) {
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§4§l☠ [ЛЕГЕНДА] Гравець §e" + killerName + " §4§lздолав ВОЛОДАРЯ БЕЗОДНІ §r" + entity.getCustomName() + "§4§l! Зірка Незеру та незерит випали на землю! ☠"
                    ));
                } else {
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§5§l☠ [ЕПОС] Гравець §e" + killerName + " §5§lпереміг КРИВАВОГО ЖНЕЦЯ §r" + entity.getCustomName() + "§5§l! Тотем, незерит та скарби випали на землю! ☠"
                    ));
                }
            } else {
                // Дроп зі звичайних мобів (за принципом BloodmoonReloaded: 1-5 пачок луту залежно від рівня)
                int lvl = currentTier.getLevel();
                int rolls = (lvl >= 4) ? ThreadLocalRandom.current().nextInt(2, 6)
                        : (lvl == 3) ? ThreadLocalRandom.current().nextInt(1, 5)
                        : (lvl == 2) ? ThreadLocalRandom.current().nextInt(1, 4)
                        : ThreadLocalRandom.current().nextInt(1, 3);

                for (int i = 0; i < rolls; i++) {
                    int roll = ThreadLocalRandom.current().nextInt(100);
                    if (lvl >= 4) {
                        if (roll < 12) {
                            w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, 1));
                        } else if (roll < 28) {
                            w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(2, 5)));
                        } else if (roll < 48) {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLD_BLOCK, ThreadLocalRandom.current().nextInt(1, 3)));
                        } else if (roll < 70) {
                            w.dropItemNaturally(loc, new ItemStack(Material.IRON_BLOCK, ThreadLocalRandom.current().nextInt(1, 4)));
                        } else if (roll < 88) {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(1, 3)));
                        } else {
                            w.dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                        }
                    } else if (lvl == 3) {
                        if (roll < 10) {
                            w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, 1));
                        } else if (roll < 28) {
                            w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(1, 4)));
                        } else if (roll < 52) {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(4, 9)));
                        } else if (roll < 76) {
                            w.dropItemNaturally(loc, new ItemStack(Material.IRON_BLOCK, 1));
                        } else if (roll < 90) {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLDEN_APPLE, 1));
                        } else {
                            w.dropItemNaturally(loc, new ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(2, 6)));
                        }
                    } else if (lvl == 2) {
                        if (roll < 7) {
                            w.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, 1));
                        } else if (roll < 25) {
                            w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(1, 3)));
                        } else if (roll < 55) {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(3, 7)));
                        } else if (roll < 80) {
                            w.dropItemNaturally(loc, new ItemStack(Material.IRON_INGOT, ThreadLocalRandom.current().nextInt(4, 10)));
                        } else {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLDEN_APPLE, 1));
                        }
                    } else {
                        if (roll < 15) {
                            w.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 1));
                        } else if (roll < 45) {
                            w.dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, ThreadLocalRandom.current().nextInt(2, 6)));
                        } else if (roll < 80) {
                            w.dropItemNaturally(loc, new ItemStack(Material.IRON_INGOT, ThreadLocalRandom.current().nextInt(3, 8)));
                        } else {
                            w.dropItemNaturally(loc, new ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(1, 4)));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!active || activeWorld == null || currentTier == null) return;
        Player p = event.getPlayer();
        if (p.getWorld().equals(activeWorld)) {
            if (bossBar != null) {
                bossBar.addPlayer(p);
            }
            applyVisualsToPlayer(p);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (bossBar != null) {
            bossBar.removePlayer(p);
        }
        removeVisualsFromPlayer(p);
    }

    // ==========================================
    // Геттери для зовнішнього керування (команди)
    // ==========================================

    public boolean isBloodmoonActive() {
        return active;
    }

    public World getActiveWorld() {
        return activeWorld;
    }

    public boolean isActive() {
        return active;
    }

    public int getMobsKilledTonight() {
        return mobsKilled;
    }

    public int getHordesSpawnedTonight() {
        return hordesSpawned;
    }

    public BloodmoonTier getCurrentTier() {
        return currentTier;
    }

    public BloodmoonTier getTier(int level) {
        return tiers.get(level);
    }

    public Map<Integer, BloodmoonTier> getAllTiers() {
        return Collections.unmodifiableMap(tiers);
    }
}
