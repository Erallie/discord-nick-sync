package com.gozarproductions.discordnicksync.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordNickTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // First argument suggestions
        if (args.length == 1) {
            completions.add("discord");
            completions.add("minecraft");
            completions.add("off");
            completions.add("sync");
            if (sender.hasPermission("discordsync.admin")) {
                completions.add("reload");
            }
        }

        // Second argument suggestions for "/discordnick sync ..."
        if (args.length == 2 && args[0].equalsIgnoreCase("sync")) {
            if (sender.hasPermission("discordsync.admin")) {
                completions.add("all"); // Admins can sync all
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList())); // Add online player names
            }
        }

        return completions;
    }
}
