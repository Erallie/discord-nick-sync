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
            sender.sendMessage("§6Usage: §e/discordnick <discord|minecraft|off> OR /discordnick sync");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("sync")) {
            if (!sender.hasPermission("discordsync.admin")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            handleSyncCommand(sender);
            return true;
        }

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
     * Handles `/discordnick sync` for all players (Admin Only).
     */
    private void handleSyncCommand(CommandSender sender) {
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
