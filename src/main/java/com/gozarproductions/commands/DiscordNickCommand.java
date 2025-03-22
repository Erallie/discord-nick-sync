package com.gozarproductions.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gozarproductions.DiscordNickSync;
import com.gozarproductions.managers.DataManager;
import com.gozarproductions.managers.LanguageManager;
import com.gozarproductions.utils.SyncMode;

import java.util.UUID;

public class DiscordNickCommand implements CommandExecutor {

    private final DiscordNickSync plugin;

    public DiscordNickCommand(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String subCommand = args[0].toLowerCase();
        LanguageManager languageManager = plugin.getLanguageManager();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("discordnick.admin")) {
                sender.sendMessage(languageManager.getMessage("errors.no_permission"));
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(languageManager.getMessage("messages.reload_success"));
            return true;
        }

        // Handle "/discordnick sync"
        if (subCommand.equals("sync")) {
            if (args.length == 1) {
                // Regular players: sync their own nickname
                if (!(sender instanceof Player)) {
                    sender.sendMessage(languageManager.getMessage("errors.only_players"));
                    return true;
                }
                plugin.syncPlayerWithMode((Player) sender, sender, false);
                return true;
            }

            // Handle "/discordnick sync all" or "/discordnick sync <player>"
            if (!sender.hasPermission("discordnick.admin")) {
                sender.sendMessage(languageManager.getMessage("errors.no_permission"));
                return true;
            }

            if (args[1].equalsIgnoreCase("all")) {
                plugin.syncAllOnlinePlayers(sender);
                return true;
            }

            // Try to find the specific player
            Player targetPlayer = Bukkit.getPlayerExact(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(languageManager.getMessage("errors.player_not_found", "player", args[1]));
                return true;
            }

            plugin.syncPlayerWithMode(targetPlayer, sender, true);
            return true;
        }

        // Handle "/discordnick <discord|minecraft|off>"
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("errors.only_players"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        String usage = command.getUsage()
            .replace("§e", languageManager.getColor("default", true))
            .replace("§6", languageManager.getColor("highlight", true));

        SyncMode mode = SyncMode.fromString(subCommand.toUpperCase());

        if (mode == null) {
            player.sendMessage(languageManager.getMessage("errors.invalid_command") + "\n" + usage);
            return true;
        }
        DataManager dataManager = plugin.getDataManager();
        dataManager.setSyncMode(playerUUID, mode.name());
        dataManager.saveData();

        String to;
        String from;

        switch (mode) {
            case MINECRAFT:
                to = "Discord";
                from = "Minecraft";
                break;
            case DISCORD:
                to = "Minecraft";
                from = "Discord";
                break;
            case OFF:
                player.sendMessage(languageManager.getMessage("messages.mode_off"));
                return true;
            default:
                player.sendMessage(languageManager.getMessage("errors.invalid_command") + "\n" + usage);
                return true;
        }
        
        player.sendMessage(languageManager.getMessage("messages.mode_set", "to", to, "from", from));
        plugin.syncPlayerWithMode(player, null, true);

        return true;
    }
}
