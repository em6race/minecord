package com.example.minecord.utils;

import com.example.minecord.MineCord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LeaderboardManager {

    public static class TopEntry {
        private final String name;
        private final UUID uuid;
        private final long value;
        private final String formattedValue;

        public TopEntry(String name, UUID uuid, long value, String formattedValue) {
            this.name = name != null ? name : "Гравець";
            this.uuid = uuid;
            this.value = value;
            this.formattedValue = formattedValue;
        }

        public String getName() { return name; }
        public UUID getUuid() { return uuid; }
        public long getValue() { return value; }
        public String getFormattedValue() { return formattedValue; }
    }

    private final MineCord plugin;
    private final Map<String, List<TopEntry>> cachedTops = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCacheTime = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 180_000; // 3 minutes

    public LeaderboardManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void getTopAsync(String category, int limit, Consumer<List<TopEntry>> callback) {
        String cat = normalizeCategory(category);
        long now = System.currentTimeMillis();
        Long cachedTime = lastCacheTime.get(cat);
        List<TopEntry> cached = cachedTops.get(cat);

        if (cached != null && cachedTime != null && (now - cachedTime < CACHE_DURATION_MS)) {
            int subLimit = Math.min(limit, cached.size());
            callback.accept(new ArrayList<>(cached.subList(0, subLimit)));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<TopEntry> fresh = computeTop(cat);
            cachedTops.put(cat, fresh);
            lastCacheTime.put(cat, System.currentTimeMillis());

            int subLimit = Math.min(limit, fresh.size());
            List<TopEntry> result = new ArrayList<>(fresh.subList(0, subLimit));
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public String normalizeCategory(String category) {
        if (category == null) return "overall";
        String lower = category.toLowerCase().trim();
        return switch (lower) {
            case "overall", "absolute", "абсолютний", "абсолют", "топ", "all" -> "overall";
            case "distance", "dist", "відстань", "дистанція" -> "distance";
            case "time", "playtime", "час", "плейтайм" -> "time";
            case "kills", "kill", "моби", "вбивства", "mobkills" -> "kills";
            case "deaths", "death", "смерті", "смертей" -> "deaths";
            case "diamonds", "diamond", "алмази", "алмаз" -> "diamonds";
            case "blocks", "block", "блоки", "блоків" -> "blocks";
            default -> "overall";
        };
    }

    public String getCategoryTitle(String category) {
        return switch (normalizeCategory(category)) {
            case "overall" -> "👑 Абсолютний топ";
            case "distance" -> "🏃 Подолана відстань";
            case "time" -> "⏱️ Награний час";
            case "kills" -> "⚔️ Вбито мобів";
            case "deaths" -> "💀 Смертей";
            case "diamonds" -> "💎 Добуто алмазів";
            case "blocks" -> "⛏️ Зламано блоків";
            default -> "🏆 Рейтинг";
        };
    }

    private List<TopEntry> computeTop(String category) {
        if ("overall".equals(category)) {
            return computeOverallTop();
        }

        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        List<TopEntry> list = new ArrayList<>();

        for (OfflinePlayer p : players) {
            boolean hasPlayed = p.hasPlayedBefore() || p.isOnline() || p.getLastPlayed() > 0;
            if (!hasPlayed && plugin.getPlayerCacheManager() != null) {
                hasPlayed = plugin.getPlayerCacheManager().hasPlayerData(p);
            }
            if (!hasPlayed) continue;

            String name = p.getName();
            if ((name == null || name.isEmpty()) && plugin.getPlayerCacheManager() != null) {
                name = plugin.getPlayerCacheManager().resolvePlayerName(p.getUniqueId());
            }
            if (name == null || name.isEmpty()) continue;

            long val = 0;
            String formatted = "";

            try {
                switch (category) {
                    case "distance" -> {
                        long cm = 0;
                        Statistic[] distStats = {
                            Statistic.WALK_ONE_CM,
                            Statistic.SPRINT_ONE_CM,
                            Statistic.SWIM_ONE_CM,
                            Statistic.FALL_ONE_CM,
                            Statistic.FLY_ONE_CM,
                            Statistic.AVIATE_ONE_CM,
                            Statistic.BOAT_ONE_CM,
                            Statistic.HORSE_ONE_CM,
                            Statistic.MINECART_ONE_CM,
                            Statistic.PIG_ONE_CM,
                            Statistic.STRIDER_ONE_CM,
                            Statistic.CROUCH_ONE_CM,
                            Statistic.CLIMB_ONE_CM
                        };
                        for (Statistic s : distStats) {
                            try {
                                cm += p.getStatistic(s);
                            } catch (Throwable ignored) {}
                        }
                        val = cm;
                        long meters = cm / 100;
                        if (meters >= 1000) {
                            double km = meters / 1000.0;
                            formatted = String.format(java.util.Locale.US, "%.1f км", km);
                        } else {
                            formatted = meters + " м";
                        }
                    }
                    case "time" -> {
                        long ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
                        val = ticks;
                        long totalMinutes = ticks / (20 * 60);
                        long hours = totalMinutes / 60;
                        long mins = totalMinutes % 60;
                        if (hours >= 24) {
                            long days = hours / 24;
                            long remHours = hours % 24;
                            formatted = days + " дн. " + remHours + " год.";
                        } else {
                            formatted = hours + " год. " + mins + " хв.";
                        }
                    }
                    case "kills" -> {
                        int mobKills = p.getStatistic(Statistic.MOB_KILLS);
                        int playerKills = p.getStatistic(Statistic.PLAYER_KILLS);
                        val = mobKills + playerKills;
                        formatted = val + " мобів";
                    }
                    case "deaths" -> {
                        val = p.getStatistic(Statistic.DEATHS);
                        formatted = val + " смертей";
                    }
                    case "diamonds" -> {
                        int normal = 0;
                        int deepslate = 0;
                        try { normal = p.getStatistic(Statistic.MINE_BLOCK, Material.DIAMOND_ORE); } catch (Throwable ignored) {}
                        try { deepslate = p.getStatistic(Statistic.MINE_BLOCK, Material.DEEPSLATE_DIAMOND_ORE); } catch (Throwable ignored) {}
                        val = normal + deepslate;
                        formatted = val + " шт.";
                    }
                    case "blocks" -> {
                        int broken = 0;
                        for (Material mat : Material.values()) {
                            if (mat.isBlock()) {
                                try { broken += p.getStatistic(Statistic.MINE_BLOCK, mat); } catch (Throwable ignored) {}
                            }
                        }
                        val = broken;
                        formatted = String.format("%,d блоків", val);
                    }
                }
            } catch (Throwable ignored) {}

            if (val > 0) {
                list.add(new TopEntry(name, p.getUniqueId(), val, formatted));
            }
        }

        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list;
    }

    private List<TopEntry> computeOverallTop() {
        String[] categories = {"time", "distance", "kills", "diamonds", "blocks"};
        Map<UUID, String> playerNames = new HashMap<>();
        Map<UUID, Integer> totalScores = new HashMap<>();
        Map<UUID, int[]> podiumCounts = new HashMap<>();

        for (String cat : categories) {
            List<TopEntry> catTop = computeTop(cat);
            int topCount = Math.min(10, catTop.size());
            for (int i = 0; i < topCount; i++) {
                TopEntry entry = catTop.get(i);
                UUID uuid = entry.getUuid();
                playerNames.put(uuid, entry.getName());

                int points = 10 - i;
                totalScores.put(uuid, totalScores.getOrDefault(uuid, 0) + points);

                int[] podium = podiumCounts.computeIfAbsent(uuid, k -> new int[3]);
                if (i == 0) podium[0]++;
                else if (i == 1) podium[1]++;
                else if (i == 2) podium[2]++;
            }
        }

        List<TopEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : totalScores.entrySet()) {
            UUID uuid = entry.getKey();
            int score = entry.getValue();
            String name = playerNames.getOrDefault(uuid, "Гравець");
            int[] podium = podiumCounts.getOrDefault(uuid, new int[3]);

            StringBuilder details = new StringBuilder();
            if (podium[0] > 0) details.append("🥇x").append(podium[0]).append(" ");
            if (podium[1] > 0) details.append("🥈x").append(podium[1]).append(" ");
            if (podium[2] > 0) details.append("🥉x").append(podium[2]).append(" ");

            String detailStr = details.toString().trim();
            String formatted = score + " балів" + (detailStr.isEmpty() ? "" : " (" + detailStr + ")");

            list.add(new TopEntry(name, uuid, score, formatted));
        }

        list.sort((a, b) -> {
            int cmp = Long.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            int[] podA = podiumCounts.getOrDefault(a.getUuid(), new int[3]);
            int[] podB = podiumCounts.getOrDefault(b.getUuid(), new int[3]);
            if (podB[0] != podA[0]) return Integer.compare(podB[0], podA[0]);
            if (podB[1] != podA[1]) return Integer.compare(podB[1], podA[1]);
            return Integer.compare(podB[2], podA[2]);
        });

        return list;
    }
}
