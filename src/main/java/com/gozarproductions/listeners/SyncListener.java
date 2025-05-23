package com.gozarproductions.listeners;

import com.earth2me.essentials.User;
import com.gozarproductions.DiscordNickSync;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class SyncListener extends ListenerAdapter implements Listener {

    private final DiscordNickSync plugin;

    public SyncListener(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check if the player is an admin
        if (player.hasPermission("discordnick.admin")) {
            // Check if an update is available
            plugin.getUpdateChecker().recallAndNotify(player);
        }
        plugin.syncPlayerWithMode(player, null);
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayer().getUniqueId());
        if (player != null) {
            plugin.syncPlayerWithMode(player, null);
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

    private void syncIfLinked(Player player) {
        plugin.syncPlayerWithMode(player, null);
    }
}
