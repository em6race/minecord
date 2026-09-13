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
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
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
                1, "tier_1", "Багряний Сутінок", 25,
                1.3, 0, 0, 0, 20,
                1.5, 0.0, 2, 4,
                "§c§lБагряний Страж", BarColor.YELLOW, 0xFFA500
        ));
        tiers.put(2, new BloodmoonTier(
                2, "tier_2", "Кривавий Місяць", 50,
                1.7, 1, 1, 0, 45,
                2.0, 15.0, 4, 6,
                "§4§lКривавий Лицар", BarColor.RED, 0xDD0000
        ));
        tiers.put(3, new BloodmoonTier(
                3, "tier_3", "Затемнення Апокаліпсису", 20,
                2.5, 2, 2, 1, 75,
                3.0, 40.0, 6, 10,
                "§5§lКривавий Жнець", BarColor.PURPLE, 0x660033
        ));
        tiers.put(4, new BloodmoonTier(
                4, "tier_4", "Кривавий Армагеддон", 5,
                4.0, 2, 3, 2, 95,
                5.0, 60.0, 8, 14,
                "§4§l☠ Володар Безодні ☠", BarColor.RED, 0x2A0000
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
            // Кривавий Місяць активний: перевірка чи настав світанок
            if (time >= 23000 || time < 12541) {
                stopBloodmoon(true);
            } else {
                updateBossBar(time);
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

        saveState();

        // Створення BossBar
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(
                "§4§l🩸 КРИВАВИЙ МІСЯЦЬ §c[" + tier.getName() + "] §4§l🩸",
                tier.getBarColor(),
                BarStyle.SOLID
        );
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
                        "§4§l☠ КРИВАВИЙ АРМАГЕДДОН ☠",
                        "§cРівень загрози: §4§l" + tier.getName() + " (MAX)",
                        10, 100, 30
                );
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
                p.getWorld().strikeLightningEffect(p.getLocation());
            } else {
                p.sendTitle(
                        "§4§lКРИВАВИЙ МІСЯЦЬ",
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
                    "  §4§l☠ НАСТАВ КРИВАВИЙ АРМАГЕДДОН (РІВЕНЬ IV)! ☠\n" +
                    "  §cБЕЗОДНЯ ПОГЛИНУЛА СВІТ! ПОВСТАВ ВОЛОДАР СМЕРТІ!\n" +
                    "  §cМонстри смертоносні, орди невблаганні, сон заблоковано!\n" +
                    "  §4§lНЕМАЄ КУДИ ТІКАТИ — БИЙТЕСЯ ЗА ВИЖИВАННЯ!\n" +
                    "§4§l☠━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━☠\n";
        } else {
            chatMsg = "\n§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "  §4§l🩸 КРИВАВИЙ МІСЯЦЬ ЗІЙШОВ НАД СВІТОМ! 🩸\n" +
                    "  §cРівень загрози: §e" + tier.getName() + " §7(Рівень " + tier.getLevel() + ")\n" +
                    "  §cМонстри посилені! Сон у ліжках заблоковано!\n" +
                    "  §cТримайте оборону баз до світанку!\n" +
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

        cleanUpVisuals();
        stopHordes();
        stopParticleTask();

        if (naturallyEnded && activeWorld != null) {
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

    private void updateBossBar(long time) {
        if (bossBar == null) return;

        long remainingTicks = Math.max(0, 23000 - time);
        long remainingSeconds = remainingTicks / 20;
        String formatted = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);

        double progress = Math.max(0.0, Math.min(1.0, (double) (time - 13000) / 10000.0));
        bossBar.setProgress(progress);
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
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            double dist = ThreadLocalRandom.current().nextDouble(10.0, 16.0);
            int x = pLoc.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = pLoc.getBlockZ() + (int) (Math.sin(angle) * dist);
            int y = player.getWorld().getHighestBlockYAt(x, z) + 1;

            Location spawnLoc = new Location(player.getWorld(), x + 0.5, y, z + 0.5);
            EntityType type = possibleTypes[ThreadLocalRandom.current().nextInt(possibleTypes.length)];

            Entity entity = player.getWorld().spawnEntity(spawnLoc, type, CreatureSpawnEvent.SpawnReason.CUSTOM);
            if (entity instanceof Monster monster) {
                buffMonster(monster, currentTier);
            }
        }

        // Перевірка на спавн міні-боса
        if (currentTier.getBossChancePercent() > 0 && ThreadLocalRandom.current().nextDouble(100.0) < currentTier.getBossChancePercent()) {
            spawnBossNearPlayer(player);
        }
    }

    private void spawnBossNearPlayer(Player player) {
        Location pLoc = player.getLocation();
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        double dist = ThreadLocalRandom.current().nextDouble(12.0, 18.0);
        int x = pLoc.getBlockX() + (int) (Math.cos(angle) * dist);
        int z = pLoc.getBlockZ() + (int) (Math.sin(angle) * dist);
        int y = player.getWorld().getHighestBlockYAt(x, z) + 1;
        Location spawnLoc = new Location(player.getWorld(), x + 0.5, y, z + 0.5);

        EntityType bossType = (currentTier.getLevel() >= 3) ? EntityType.WITHER_SKELETON : EntityType.ZOMBIE;
        Entity entity = player.getWorld().spawnEntity(spawnLoc, bossType, CreatureSpawnEvent.SpawnReason.CUSTOM);

        if (entity instanceof Monster boss) {
            PersistentDataContainer pdc = boss.getPersistentDataContainer();
            pdc.set(mobKey, PersistentDataType.BYTE, (byte) currentTier.getLevel());
            pdc.set(bossKey, PersistentDataType.BYTE, (byte) 1);

            boss.setCustomName(currentTier.getBossName());
            boss.setCustomNameVisible(true);
            boss.setGlowing(true);

            double bossHp = (currentTier.getLevel() >= 4) ? 500.0 : ((currentTier.getLevel() >= 3) ? 300.0 : 150.0);
            AttributeInstance hpAttr = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.setBaseValue(bossHp);
                boss.setHealth(bossHp);
            }

            int speedAmp = (currentTier.getLevel() >= 4) ? 2 : 1;
            int strAmp = (currentTier.getLevel() >= 4) ? 2 : 1;
            int resAmp = (currentTier.getLevel() >= 4) ? 2 : 1;

            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmp, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, strAmp, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, resAmp, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            if (currentTier.getLevel() >= 4) {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, false, false));
            }

            EntityEquipment eq = boss.getEquipment();
            if (eq != null) {
                if (currentTier.getLevel() >= 4) {
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
                    weapon.addEnchantment(Enchantment.KNOCKBACK, 2);
                    eq.setItemInMainHand(weapon);
                } else if (currentTier.getLevel() >= 3) {
                    eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    eq.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
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
                eq.setItemInMainHandDropChance(0.0f);
            }

            if (currentTier.getLevel() >= 4) {
                player.getWorld().strikeLightningEffect(spawnLoc);
                Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                        "§4§l☠☠☠ [АРМАГЕДДОН] ВОЛОДАР БЕЗОДНІ ПОВСТАВ! §r" + currentTier.getBossName() + "§4§l біля гравця §e" + player.getName() + "§4§l! ☠☠☠"
                ));
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.7f);
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

        if (tier.getSpeedLevel() > 0) {
            monster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, tier.getSpeedLevel() - 1, false, false));
        }
        if (tier.getStrengthLevel() > 0) {
            monster.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, tier.getStrengthLevel() - 1, false, false));
        }
        if (tier.getResistanceLevel() > 0) {
            monster.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, tier.getResistanceLevel() - 1, false, false));
        }

        if (tier.getLevel() >= 4 && monster instanceof Creeper creeper) {
            creeper.setPowered(true);
        }

        if (tier.getLevel() >= 3) {
            monster.setGlowing(true);
        }

        // Екіпірування бронею
        if (ThreadLocalRandom.current().nextInt(100) < tier.getArmorChancePercent()) {
            EntityEquipment eq = monster.getEquipment();
            if (eq != null) {
                if (tier.getLevel() >= 4) {
                    eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    eq.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                    if (monster instanceof Skeleton) {
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
                        eq.setItemInMainHand(bow);
                    } else {
                        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                        sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
                        sword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
                        eq.setItemInMainHand(sword);
                    }
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                } else if (tier.getLevel() >= 3) {
                    eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                    eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                    if (monster instanceof Skeleton) {
                        ItemStack bow = new ItemStack(Material.BOW);
                        bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        eq.setItemInMainHand(bow);
                    }
                } else if (tier.getLevel() == 2) {
                    eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                    eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                    eq.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                    eq.setBoots(new ItemStack(Material.IRON_BOOTS));
                } else {
                    eq.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                    eq.setBoots(new ItemStack(Material.LEATHER_BOOTS));
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

        long time = world.getTime();
        // Якщо в цьому світі все ще триває ніч
        if (time >= 13000 && time < 23000) {
            int tierLvl = yaml.getInt("tier_level", 2);
            BloodmoonTier tier = tiers.getOrDefault(tierLvl, tiers.get(2));
            this.hordesSpawned = yaml.getInt("hordes_spawned", 0);
            this.mobsKilled = yaml.getInt("mobs_killed", 0);

            startBloodmoon(world, tier, false);
            plugin.getLogger().info("[BloodmoonMode] Стан Кривавого Місяця відновлено після рестарту! Світ: " + world.getName());
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
                eb.setTitle("☠️ [АРМАГЕДДОН] КРИВАВИЙ МІСЯЦЬ РІВНЯ IV! ☠️");
            } else {
                eb.setTitle("🩸 КРИВАВИЙ МІСЯЦЬ ЗІЙШОВ НАД СВІТОМ! 🩸");
            }
            eb.setColor(tier.getDiscordColorHex());
            String desc;
            if (tier.getLevel() >= 4) {
                desc = "**Рівень загрози:** `☠ " + tier.getName() + " ☠` (МАКСИМАЛЬНИЙ РІВЕНЬ 4)\n\n" +
                        "🔥 **БЕЗОДНЯ ПОГЛИНУЛА СВІТ — НАЙКОШМАРНІША НІЧ:**\n" +
                        "• Сон у ліжках повністю унеможливлено!\n" +
                        "• Монстри посилені у **" + tier.getHealthMultiplier() + " рази**, екіпіровані незеритом та смертельними чарами!\n" +
                        "• Заряджені кріпери та невпинні гігантські орди монстрів.\n" +
                        "• З високим шансом повстане **Володар Безодні**!\n" +
                        "• За перемогу над босом: Зірка Незеру, незерит, зачаровані золоті яблука та **x" + tier.getExpMultiplier() + "** досвіду!\n\n" +
                        "⚰️ *Шанси пережити цю ніч вкрай малі. Хай допоможуть вам небеса!*";
            } else {
                desc = "**Рівень загрози:** `" + tier.getName() + "` (Рівень " + tier.getLevel() + ")\n\n" +
                        "⚠️ **Увага всім гравцям на сервері:**\n" +
                        "• Сон у ліжках заблоковано до настання світанку!\n" +
                        "• Монстри отримали додаткове здоров'я (+" + (int)((tier.getHealthMultiplier() - 1.0) * 100) + "%) та бафи швидкості/сили.\n" +
                        "• Очікуються хвилі орд монстрів та можлива поява босів.\n" +
                        "• За знищення мобів видається **x" + tier.getExpMultiplier() + "** досвіду та рідкісний лут!\n\n" +
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

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        if (event.getEntity() instanceof Monster monster) {
            buffMonster(monster, currentTier);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!active || currentTier == null) return;

        LivingEntity entity = event.getEntity();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        if (pdc.has(mobKey, PersistentDataType.BYTE)) {
            mobsKilled++;

            // Збільшення досвіду
            event.setDroppedExp((int) (event.getDroppedExp() * currentTier.getExpMultiplier()));

            boolean isBoss = pdc.has(bossKey, PersistentDataType.BYTE);
            Location loc = entity.getLocation();

            if (isBoss) {
                // Бонусні нагороди за боса
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(3, 7)));
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(1, 3)));
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.IRON_INGOT, ThreadLocalRandom.current().nextInt(16, 33)));

                if (currentTier.getLevel() >= 4) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, ThreadLocalRandom.current().nextInt(1, 3)));
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, ThreadLocalRandom.current().nextInt(1, 3)));
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING, ThreadLocalRandom.current().nextInt(1, 3)));
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND_BLOCK, 1));
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHER_STAR, 1));
                } else if (currentTier.getLevel() >= 3) {
                    if (ThreadLocalRandom.current().nextInt(100) < 50) {
                        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 40) {
                        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, 1));
                    }
                    if (ThreadLocalRandom.current().nextInt(100) < 25) {
                        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                    }
                }

                Player killer = entity.getKiller();
                String killerName = (killer != null) ? killer.getName() : "Герої";
                if (currentTier.getLevel() >= 4) {
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§4§l☠ [ЛЕГЕНДА] Гравець §e" + killerName + " §4§lздолав ВОЛОДАРЯ БЕЗОДНІ §r" + entity.getCustomName() + "§4§l! Зірка Незеру та незерит випали на землю! ☠"
                    ));
                } else {
                    Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(
                            "§6§l[!] Гравець §e" + killerName + " §6переміг боса §r" + entity.getCustomName() + "§6! Трофеї випали на землю!"
                    ));
                }
            } else {
                // Шанс додаткового дропу зі звичайних кривавих мобів
                int roll = ThreadLocalRandom.current().nextInt(100);
                if (currentTier.getLevel() >= 4 && roll < 6) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, 1));
                }
                if (roll < 12) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.IRON_INGOT, 1));
                } else if (roll < 20) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, 1));
                } else if (roll < 24) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.EMERALD, 1));
                } else if (roll < 26) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 1));
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
