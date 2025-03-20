package com.gozarproductions.discordnicksync.commands;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gozarproductions.discordnicksync.DiscordNickSync;
import com.gozarproductions.discordnicksync.utils.SyncMode;

import java.util.UUID;

public class DiscordNickCommand implements CommandExecutor {

    private final DiscordNickSync plugin;

    public DiscordNickCommand(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("discordsync.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("errors.no_permission"));
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.reload_success"));
            return true;
        }

        // Handle "/discordnick sync"
        if (subCommand.equals("sync")) {
            if (args.length == 1) {
                // Regular players: sync their own nickname
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("errors.only_players"));
                    return true;
                }
                syncPlayer((Player) sender, sender);
                return true;
            }

            // Handle "/discordnick sync all" or "/discordnick sync <player>"
            if (!sender.hasPermission("discordsync.admin")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("errors.no_permission"));
                return true;
            }

            if (args[1].equalsIgnoreCase("all")) {
                syncAllPlayers(sender);
                return true;
            }

            // Try to find the specific player
            Player targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("errors.player_not_found", "player", args[1]));
                return true;
            }

            syncPlayer(targetPlayer, sender);
            return true;
        }

        // Handle "/discordnick <discord|minecraft|off>"
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("errors.only_players"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUUID);

        SyncMode mode = SyncMode.fromString(subCommand.toUpperCase());
        plugin.getDataManager().setSyncMode(playerUUID, mode.name());
        plugin.getDataManager().saveData();


        switch (mode) {
            case MINECRAFT:
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.mode_set", "to", "Discord", "from", "Minecraft"));
                plugin.syncMinecraftToDiscord(player, discordId);
                break;
            case DISCORD:
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.mode_set", "to", "Minecraft", "from", "Discord"));
                plugin.syncMinecraftToDiscord(player, discordId);
                break;
            case OFF:
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.mode_off"));
                break;
            default:
                player.sendMessage(plugin.getLanguageManager().getMessage("errors.invalid_command", "usage", plugin.getLanguageManager().getMessage("messages.usage")));
                break;
        }

        return true;
    }

    /**
     * Syncs a specific player's nickname.
     */
    private void syncPlayer(Player player, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = player.getUniqueId();
            String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

            if (discordId == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("errors.sync_not_linked", "player", player.getName()));
                return;
            }

            SyncMode syncMode = SyncMode.fromString(plugin.getDataManager().getSyncMode(uuid));

            switch (syncMode) {
                case MINECRAFT:
                    plugin.syncMinecraftToDiscord(player, discordId);
                    sender.sendMessage(
                        plugin.getLanguageManager().getMessage(
                                "messages.sync_success", 
                                "player", player.getName(), 
                                "from", "Minecraft", 
                                "to", "Discord"
                            )
                        );
                    break;
                case DISCORD:
                    plugin.syncDiscordToMinecraft(player, discordId);
                    sender.sendMessage(
                        plugin.getLanguageManager().getMessage(
                                "messages.sync_success", 
                                "player", player.getName(), 
                                "from", "Discord", 
                                "to", "Minecraft"
                            )
                        );
                    break;
                case OFF:
                    sender.sendMessage(
                        plugin.getLanguageManager().getMessage(
                            "messages.sync_disabled", 
                            "player", player.getName()
                            )
                        );
                    break;
            }
        });
    }

    /**
     * Syncs all online players' nicknames.
     */
    private void syncAllPlayers(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int syncedCount = 0;

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

                if (discordId == null) continue; // Skip if not linked

                SyncMode syncMode = SyncMode.fromString(plugin.getDataManager().getSyncMode(uuid));

                switch (syncMode) {
                    case MINECRAFT:
                        plugin.syncMinecraftToDiscord(player, discordId);
                        syncedCount++;
                        break;
                    case DISCORD:
                        plugin.syncDiscordToMinecraft(player, discordId);
                        syncedCount++;
                        break;
                    case OFF:
                        break;
                }
            }

            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.sync_all_success", "count", String.valueOf(syncedCount)));
        });
    }
}
