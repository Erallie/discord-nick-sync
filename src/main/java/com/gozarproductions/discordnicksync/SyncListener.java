package com.gozarproductions.discordnicksync;

import com.earth2me.essentials.User;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
// import github.scarsz.discordsrv.dependencies.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

// import javax.annotation.Nonnull;
import java.util.UUID;

public class SyncListener extends ListenerAdapter implements Listener {

    private final DiscordNickSync plugin;

    public SyncListener(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check if the player is an admin
        if (player.hasPermission("discordsync.admin")) {
            // Check if an update is available
            UpdateChecker updateChecker = plugin.updateChecker;
            String latestVersion = updateChecker.latestVersion;
            String downloadUrl = updateChecker.downloadUrl;

            if (latestVersion != null) {
                updateChecker.notifyAdmins(latestVersion, downloadUrl);
            }
        }

        UUID uuid = player.getUniqueId();
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

        if (discordId != null) {
            syncPlayer(player, discordId);
        }
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayer().getUniqueId());
        if (player != null) {
            syncPlayer(player, event.getUser().getId());
        } else {
            plugin.getLogger().warning("Cannot sync newly linked account: player is null.");
        }
    }

    @EventHandler
    public void onEssentialsNickChange(NickChangeEvent event) {
        if (!(event.getAffected() instanceof User)) {
            return;
        }

        User user = (User) event.getAffected();
        Player player = user.getBase();

        if (player != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> syncIfLinked(player), 5L); // Delay by 5 ticks
        }
    }

    /* @Override
    public void onGuildMemberUpdateNickname(@Nonnull GuildMemberUpdateNicknameEvent event) {
        String discordId = event.getUser().getId();
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordId);

        if (uuid != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                syncPlayer(player, discordId);
            }
        }
    } */

    private void syncIfLinked(Player player) {
        UUID uuid = player.getUniqueId();
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

        if (discordId != null) {
            syncPlayer(player, discordId);
        }
    }

    private void syncPlayer(Player player, String discordId) {
        String syncMode = plugin.getDataManager().getSyncMode(player.getUniqueId());

        switch (SyncMode.fromString(syncMode)) {
            case MINECRAFT:
                plugin.syncMinecraftToDiscord(player, discordId);
                break;
            case DISCORD:
                plugin.syncDiscordToMinecraft(player, discordId);
                break;
            case OFF:
                break;
        }
    }
}
