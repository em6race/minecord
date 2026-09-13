package com.example.minecord.fun;

import org.bukkit.boss.BarColor;

/**
 * Пресет складності Кривавого Місяця.
 */
public class BloodmoonTier {

    private final int level;
    private final String id;
    private final String name;
    private final int weight;
    private final double healthMultiplier;
    private final int speedLevel;
    private final int strengthLevel;
    private final int resistanceLevel;
    private final int armorChancePercent;
    private final double expMultiplier;
    private final double bossChancePercent;
    private final int hordeMin;
    private final int hordeMax;
    private final String bossName;
    private final BarColor barColor;
    private final int discordColorHex;

    public BloodmoonTier(
            int level,
            String id,
            String name,
            int weight,
            double healthMultiplier,
            int speedLevel,
            int strengthLevel,
            int resistanceLevel,
            int armorChancePercent,
            double expMultiplier,
            double bossChancePercent,
            int hordeMin,
            int hordeMax,
            String bossName,
            BarColor barColor,
            int discordColorHex
    ) {
        this.level = level;
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.healthMultiplier = healthMultiplier;
        this.speedLevel = speedLevel;
        this.strengthLevel = strengthLevel;
        this.resistanceLevel = resistanceLevel;
        this.armorChancePercent = armorChancePercent;
        this.expMultiplier = expMultiplier;
        this.bossChancePercent = bossChancePercent;
        this.hordeMin = hordeMin;
        this.hordeMax = hordeMax;
        this.bossName = bossName;
        this.barColor = barColor;
        this.discordColorHex = discordColorHex;
    }

    public int getLevel() {
        return level;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public int getStrengthLevel() {
        return strengthLevel;
    }

    public int getResistanceLevel() {
        return resistanceLevel;
    }

    public int getArmorChancePercent() {
        return armorChancePercent;
    }

    public double getExpMultiplier() {
        return expMultiplier;
    }

    public double getBossChancePercent() {
        return bossChancePercent;
    }

    public int getHordeMin() {
        return hordeMin;
    }

    public int getHordeMax() {
        return hordeMax;
    }

    public String getBossName() {
        return bossName;
    }

    public BarColor getBarColor() {
        return barColor;
    }

    public int getDiscordColorHex() {
        return discordColorHex;
    }
}
