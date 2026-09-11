package com.example.minecord.listeners;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DiscordRoleListener extends ListenerAdapter {

    private final MineCord plugin;

    public DiscordRoleListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        handleRoleChange(event.getMember().getId());
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        handleRoleChange(event.getMember().getId());
    }

    private void handleRoleChange(String discordId) {
        if (discordId == null || plugin.getRoleSyncManager() == null) return;
        if (!plugin.getConfig().getBoolean("role-sync.enabled", true)) return;

        List<UUID> uuids = plugin.getLinkManager().getAllUUIDsFromDiscordId(discordId);
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getRoleSyncManager().syncPlayer(player);
            }
        }
    }
}
