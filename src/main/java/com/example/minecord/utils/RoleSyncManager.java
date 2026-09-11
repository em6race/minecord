package com.example.minecord.utils;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoleSyncManager {

    public static class RoleDefinition {
        public final String key;
        public final String roleId;
        public final String roleName;
        public final String prefix;
        public final ChatColor color;
        public final int priority;

        public RoleDefinition(String key, String roleId, String roleName, String prefix, ChatColor color, int priority) {
            this.key = key;
            this.roleId = roleId != null ? roleId.trim() : "";
            this.roleName = roleName != null ? roleName.trim().toLowerCase() : "";
            this.prefix = prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "";
            this.color = color;
            this.priority = priority;
        }

        public boolean matches(Role discordRole) {
            if (discordRole == null) return false;
            if (!roleId.isEmpty() && roleId.equals(discordRole.getId())) {
                return true;
            }
            if (!roleName.isEmpty() && discordRole.getName() != null) {
                return roleName.equalsIgnoreCase(discordRole.getName().trim());
            }
            return false;
        }
    }

    private final MineCord plugin;
    private final List<RoleDefinition> configuredRoles = new CopyOnWriteArrayList<>();
    private RoleDefinition unlinkedRole;
    private RoleDefinition defaultLinkedRole;
    private final Map<UUID, RoleDefinition> playerRoleCache = new ConcurrentHashMap<>();
    private int syncTaskId = -1;

    public RoleSyncManager(MineCord plugin) {
        this.plugin = plugin;
    }

    public void start() {
        reloadConfig();

        if (!plugin.getConfig().getBoolean("role-sync.enabled", true)) {
            return;
        }

        int intervalSeconds = Math.max(15, plugin.getConfig().getInt("role-sync.sync-interval-seconds", 60));
        syncTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::syncAllOnlinePlayers);
        }, 100L, intervalSeconds * 20L);
    }

    public void stop() {
        if (syncTaskId != -1) {
            Bukkit.getScheduler().cancelTask(syncTaskId);
            syncTaskId = -1;
        }
        cleanupScoreboardTeams();
        playerRoleCache.clear();
    }

    public synchronized void reloadConfig() {
        configuredRoles.clear();

        ConfigurationSection rolesSection = plugin.getConfig().getConfigurationSection("role-sync.roles");
        if (rolesSection != null) {
            for (String key : rolesSection.getKeys(false)) {
                String roleId = rolesSection.getString(key + ".role-id", "");
                String roleName = rolesSection.getString(key + ".role-name", "");
                String prefix = rolesSection.getString(key + ".prefix", "");
                String colorStr = rolesSection.getString(key + ".color", "WHITE");
                int priority = rolesSection.getInt(key + ".priority", 50);

                ChatColor color = parseChatColor(colorStr, ChatColor.WHITE);
                configuredRoles.add(new RoleDefinition(key, roleId, roleName, prefix, color, priority));
            }
        }

        // Sort by priority ascending (1 = highest priority)
        configuredRoles.sort(Comparator.comparingInt(r -> r.priority));

        // Unlinked role settings
        String unlinkedPrefix = plugin.getConfig().getString("role-sync.unlinked.prefix", "&7");
        String unlinkedColor = plugin.getConfig().getString("role-sync.unlinked.color", "GRAY");
        int unlinkedPriority = plugin.getConfig().getInt("role-sync.unlinked.priority", 99);
        unlinkedRole = new RoleDefinition("unlinked", "", "", unlinkedPrefix, parseChatColor(unlinkedColor, ChatColor.GRAY), unlinkedPriority);

        // Find fallback default linked role if configured
        defaultLinkedRole = configuredRoles.stream()
                .filter(r -> r.key.equalsIgnoreCase("player") || r.roleName.equalsIgnoreCase("гравець"))
                .findFirst()
                .orElse(null);
    }

    private ChatColor parseChatColor(String name, ChatColor fallback) {
        if (name == null || name.trim().isEmpty()) return fallback;
        try {
            return ChatColor.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public void syncAllOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            syncPlayerAsync(p);
        }
    }

    /**
     * Triggers asynchronous Discord role fetch and synchronizes player's TAB and Scoreboard team.
     */
    public void syncPlayer(Player player) {
        if (player == null) return;
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> syncPlayerAsync(player));
        } else {
            syncPlayerAsync(player);
        }
    }

    private void syncPlayerAsync(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!plugin.getConfig().getBoolean("role-sync.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        String discordId = plugin.getLinkManager().getDiscordId(uuid);

        if (discordId == null || discordId.isEmpty()) {
            applyRole(player, unlinkedRole);
            return;
        }

        if (plugin.getBotManager() == null || plugin.getBotManager().getJda() == null) {
            applyRole(player, defaultLinkedRole != null ? defaultLinkedRole : unlinkedRole);
            return;
        }

        JDA jda = plugin.getBotManager().getJda();
        String guildId = plugin.getConfig().getString("discord.guild-id");
        Guild guild = (guildId != null && !guildId.isEmpty() && !guildId.equals("000000000000000000")) 
                ? jda.getGuildById(guildId) 
                : null;

        if (guild == null) {
            List<Guild> guilds = jda.getGuilds();
            if (!guilds.isEmpty()) {
                guild = guilds.get(0);
            }
        }

        if (guild == null) {
            applyRole(player, defaultLinkedRole != null ? defaultLinkedRole : unlinkedRole);
            return;
        }

        try {
            guild.retrieveMemberById(discordId).queue(
                    member -> {
                        RoleDefinition best = findHighestRole(member);
                        if (best == null) {
                            best = (defaultLinkedRole != null) ? defaultLinkedRole : unlinkedRole;
                        }
                        applyRole(player, best);
                    },
                    error -> {
                        // User not in guild or error
                        applyRole(player, unlinkedRole);
                    }
            );
        } catch (Throwable t) {
            applyRole(player, unlinkedRole);
        }
    }

    private RoleDefinition findHighestRole(Member member) {
        if (member == null) return null;
        List<Role> memberRoles = member.getRoles();
        if (memberRoles.isEmpty()) return null;

        for (RoleDefinition def : configuredRoles) {
            for (Role role : memberRoles) {
                if (def.matches(role)) {
                    return def;
                }
            }
        }

        return null;
    }

    private void applyRole(Player player, RoleDefinition role) {
        if (player == null || role == null) return;
        playerRoleCache.put(player.getUniqueId(), role);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            // 1. Update Scoreboard Team (for sorting in TAB and nametag color)
            boolean tabSorting = plugin.getConfig().getBoolean("role-sync.tab-sorting", true);
            boolean nametagColor = plugin.getConfig().getBoolean("role-sync.nametag-color", true);

            if (tabSorting || nametagColor) {
                applyScoreboardTeam(player, role);
            }

            // 2. Update player list name in TAB
            boolean tabPrefix = plugin.getConfig().getBoolean("role-sync.tab-prefix", true);
            if (tabPrefix) {
                boolean isAfk = plugin.getAfkManager() != null && plugin.getAfkManager().isAfk(player);
                player.setPlayerListName(formatPlayerListName(player, role, isAfk));
            }
        });
    }

    private void applyScoreboardTeam(Player player, RoleDefinition role) {
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = String.format("mc_%02d_%s", Math.min(99, role.priority), role.key);
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            Team team = sb.getTeam(teamName);
            if (team == null) {
                team = sb.registerNewTeam(teamName);
            }

            if (role.color != null) {
                try {
                    team.setColor(role.color);
                } catch (Throwable ignored) {}
            }

            team.setPrefix("");
            team.setSuffix("");

            // Remove from any other mc_* teams
            for (Team t : sb.getTeams()) {
                if (t.getName().startsWith("mc_") && !t.getName().equals(teamName)) {
                    if (t.hasEntry(player.getName())) {
                        t.removeEntry(player.getName());
                    }
                }
            }

            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[MineCord] Не вдалося оновити команду Scoreboard для " + player.getName() + ": " + t.getMessage());
        }
    }

    public String formatPlayerListName(Player player, boolean isAfk) {
        if (player == null) return "";
        RoleDefinition role = playerRoleCache.get(player.getUniqueId());
        if (role == null) {
            role = unlinkedRole;
        }
        return formatPlayerListName(player, role, isAfk);
    }

    public String formatPlayerListName(Player player, RoleDefinition role, boolean isAfk) {
        String pName = player.getName();
        String pfx = (role != null && role.prefix != null) ? role.prefix : "";

        if (isAfk) {
            return ChatColor.GRAY + "[АФК] " + ChatColor.RESET + pfx + pName;
        } else {
            return pfx + pName;
        }
    }

    public String getRolePrefix(Player player) {
        if (player == null) return "";
        RoleDefinition role = playerRoleCache.get(player.getUniqueId());
        if (role == null) {
            role = unlinkedRole;
        }
        return (role != null && role.prefix != null) ? role.prefix : "";
    }

    public RoleDefinition getPlayerRole(UUID uuid) {
        if (uuid == null) return unlinkedRole;
        return playerRoleCache.getOrDefault(uuid, unlinkedRole);
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        playerRoleCache.remove(player.getUniqueId());
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team t : sb.getTeams()) {
                if (t.getName().startsWith("mc_") && t.hasEntry(player.getName())) {
                    t.removeEntry(player.getName());
                }
            }
        } catch (Throwable ignored) {}
    }

    private void cleanupScoreboardTeams() {
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Team t : new ArrayList<>(sb.getTeams())) {
                if (t.getName().startsWith("mc_")) {
                    t.unregister();
                }
            }
        } catch (Throwable ignored) {}
    }
}
