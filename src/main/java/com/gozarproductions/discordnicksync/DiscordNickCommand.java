package com.gozarproductions.discordnicksync;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordNickCommand implements CommandExecutor {

    private final DiscordNickSync plugin;

    public DiscordNickCommand(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6Usage: §e/discordnick <discord|minecraft|off> OR /discordnick sync [all|<username>]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("discordsync.admin")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage("§eDiscordNickSync configuration reloaded.");
            return true;
        }

        // Handle "/discordnick sync"
        if (subCommand.equals("sync")) {
            if (args.length == 1) {
                // Regular players: sync their own nickname
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can sync their own nickname.");
                    return true;
                }
                syncPlayer((Player) sender, sender);
                return true;
            }

            // Handle "/discordnick sync all" or "/discordnick sync <player>"
            if (!sender.hasPermission("discordsync.admin")) {
                sender.sendMessage("§cYou do not have permission to use this command");
                return true;
            }

            if (args[1].equalsIgnoreCase("all")) {
                syncAllPlayers(sender);
                return true;
            }

            // Try to find the specific player
            Player targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }

            syncPlayer(targetPlayer, sender);
            return true;
        }

        // Handle "/discordnick <discord|minecraft|off>"
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can set their own sync mode.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        SyncMode mode = SyncMode.fromString(subCommand.toUpperCase());
        plugin.getDataManager().setSyncMode(playerUUID, mode.name());
        plugin.getDataManager().saveData();

        switch (mode) {
            case MINECRAFT:
                player.sendMessage("§eYour Discord nickname will now use your §6Minecraft §enickname.");
                break;
            case DISCORD:
                player.sendMessage("§eYour Minecraft nickname will now use your §6Discord §enickname.");
                break;
            case OFF:
                player.sendMessage("§eYour nickname sync has been disabled.");
                break;
            default:
                player.sendMessage("§cInvalid option! Use `/discordnick <discord|minecraft|off>`.");
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
                sender.sendMessage("§c" + player.getName() + " has not linked their Discord account.");
                return;
            }

            SyncMode syncMode = SyncMode.fromString(plugin.getDataManager().getSyncMode(uuid));

            switch (syncMode) {
                case MINECRAFT:
                    plugin.syncMinecraftToDiscord(player, discordId);
                    sender.sendMessage("§eSynced §6" + player.getName() + "§e (Minecraft → Discord).");
                    break;
                case DISCORD:
                    plugin.syncDiscordToMinecraft(player, discordId);
                    sender.sendMessage("§eSynced §6" + player.getName() + "§e (Discord → Minecraft).");
                    break;
                case OFF:
                    sender.sendMessage("§6" + player.getName() + "§e has syncing disabled.");
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

            sender.sendMessage("§aSynchronized " + syncedCount + " players.");
        });
    }
}
