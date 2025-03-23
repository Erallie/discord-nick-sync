package com.gozarproductions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.HierarchyException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.gozarproductions.commands.DiscordNickCommand;
import com.gozarproductions.commands.DiscordNickTabCompleter;
import com.gozarproductions.listeners.SyncListener;
import com.gozarproductions.managers.ConfigUpdater;
import com.gozarproductions.managers.DataManager;
import com.gozarproductions.managers.LanguageManager;
import com.gozarproductions.managers.UpdateChecker;
import com.gozarproductions.utils.SyncMode;

import java.util.UUID;

public class DiscordNickSync extends JavaPlugin {

    private Essentials essentials;
    private DataManager dataManager;
    private UpdateChecker updateChecker;
    private LanguageManager languageManager;

    public DataManager getDataManager() {
        return dataManager;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    @Override
    public void onEnable() {
        getLogger().info("DiscordNickSync enabled!");

        // Save default config
        saveDefaultConfig();

        // Create ConfigUpdater and check for updates
        ConfigUpdater configUpdater = new ConfigUpdater(this);
        configUpdater.checkAndUpdateConfigs();
        
        // Run the Update Checker using GitHub API
        updateChecker = new UpdateChecker(this, "Erallie", "discord-nick-sync");
        updateChecker.checkForUpdates();

        reloadConfig();
        
        languageManager = new LanguageManager(this);
        languageManager.reloadLanguageFile();

        // Initialize DataManager to handle `data.json`
        dataManager = new DataManager(getDataFolder(), this);

        // Hook into Essentials
        if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
            essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
            getLogger().info("Hooked into EssentialsX!");
        } else {
            getLogger().warning("EssentialsX not found! Nickname syncing will not work.");
        }

        // Register commands
        getCommand("discordnick").setExecutor(new DiscordNickCommand(this));
        getCommand("discordnick").setTabCompleter(new DiscordNickTabCompleter());

        // Register event listeners
        SyncListener syncListener = new SyncListener(this);
        getServer().getPluginManager().registerEvents(syncListener, this);
        DiscordSRV.api.subscribe(syncListener);
    }

    @Override
    public void onDisable() {
        getLogger().info("DiscordNickSync disabled!");
    }

    public void syncAllOnlinePlayers(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            int syncedCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
                if (discordId == null) continue;

                String mode = dataManager.getSyncMode(uuid);
                syncPlayerWithMode(player, null);
                
                if (SyncMode.fromString(mode) != SyncMode.OFF){
                    syncedCount++;
                }
            }
            sender.sendMessage(languageManager.getMessage("messages.sync_all_success", "count", String.valueOf(syncedCount)));
        });
    }
        
    public void nullWarning(String variable, String playerName, CommandSender notifySender) {
        String part1 = "Could not sync nicknames for ";
        String part2 = ": ";
        String part3 = " is null";
        LanguageManager languageManager = getLanguageManager();
        String error = languageManager.getColor("error", true);
        String errorHighlight = languageManager.getColor("error_highlight", true);
        
        getLogger().warning(part1 + playerName + part2 + variable + part3);
        if (notifySender != null) {
            notifySender.sendMessage(
                error + part1 +
                errorHighlight + playerName +
                error + part2 +
                errorHighlight + variable +
                error + part3
            );
        }
    }

    public void syncPlayerWithMode(Player player, CommandSender notifySender) {
        SyncMode syncMode = SyncMode.fromString(dataManager.getSyncMode(player.getUniqueId()));
        
        String playerName = player.getName();

        if (syncMode == SyncMode.OFF) {    
            if (notifySender != null) {
                notifySender.sendMessage(languageManager.getMessage(
                    "messages.sync_disabled",
                    "player", playerName
                ));
            }
            return;
        }
        
        UUID uuid = player.getUniqueId();
        DiscordSRV discordSRV = DiscordSRV.getPlugin();
        JDA jda = discordSRV.getJda();
        String discordId = discordSRV.getAccountLinkManager().getDiscordId(uuid);
        if (discordId == null) {
            nullWarning("discordId", playerName, notifySender);
            return;
        }

        User discordUser = jda.getUserById(discordId);
        if (discordUser == null) {
            nullWarning("discordUser", playerName, notifySender);
            return;
        }

        Guild guild = discordSRV.getMainGuild();

        Member discordMember = guild.getMember(discordUser);
        if (discordMember == null) {
            nullWarning("discordMember", playerName, notifySender);
            return;
        }

        String discordNickOrig = discordMember.getEffectiveName();
        FileConfiguration config = getConfig();
        String replaceWith = config.getString("replace-whitespaces-with");
        boolean merge = config.getBoolean("merge-whitespaces-before-replacing");
        String discordNick;
        if (merge) {
            discordNick = discordNickOrig.replaceAll("\\s+", replaceWith);
        }
        else {
            discordNick = discordNickOrig.replaceAll("\\s", replaceWith);
        }

        String minecraftNick = ChatColor.stripColor(essentials.getUser(player).getNickname());
        if (minecraftNick == null || minecraftNick.isEmpty()) {
            minecraftNick = player.getDisplayName();
        }

        if (discordNick.equals(minecraftNick)) {
            String part1 = "Did not sync nicknames for ";
            String part2 = " because they already match.";
            LanguageManager languageManager = getLanguageManager();
            String defaultColor = languageManager.getColor("default", true);
            String highlight = languageManager.getColor("highlight", true);
            
            getLogger().warning(part1 + minecraftNick + part2);
            if (notifySender != null) {
                notifySender.sendMessage(
                    defaultColor + part1 +
                    highlight + minecraftNick +
                    defaultColor + part2
                );
            }
            return;
        }
        String from;
        String to;
        String newNick;
        
        String error = languageManager.getColor("error", true);
        String errorHighlight = languageManager.getColor("error_highlight", true);
        
        switch (syncMode) {
            case MINECRAFT:
                from = "Minecraft";
                to = "Discord";
                newNick = minecraftNick;
                try {
                    guild
                        .modifyNickname(discordMember, newNick)
                        .queue(
                            success -> {
                                getLogger().info("Updated Discord nickname for " + playerName + " to " + newNick);
                            }
                        );
                } catch (HierarchyException e) {
                    getLogger().warning("Cannot modify nickname for " + playerName + 
                        "  (Discord role hierarchy issue): ensure the bot has permission and is above the user in the role list.");
                    if (notifySender != null) {
                        if (notifySender != null); {
                            notifySender.sendMessage(error + "Cannot modify nickname for " + errorHighlight + playerName + error + "  (Discord role hierarchy issue): ensure the bot has permission and is above the user in the role list.");
                        }
                    }
                    return;
                } catch (Exception e) {
                    String localizedError = e.getLocalizedMessage();
                    getLogger().warning("Cannot modify nickname for " + playerName + 
                        ": " + localizedError);
                    if (notifySender != null) {
                        if (notifySender != null); {
                            notifySender.sendMessage(error + "Cannot modify nickname for " + errorHighlight + playerName + error + ": " + localizedError);
                        }
                    }
                    return;
                }
                break;
            case DISCORD:
                from = "Discord";
                to = "Minecraft";
                newNick = discordNick;
                if (essentials != null) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        try {
                            essentials.getUser(player).setNickname(newNick);
                        } catch (Exception e) {
                            String localizedError = e.getLocalizedMessage();
                            getLogger().warning("Failed to update nickname for " + playerName + localizedError);
                            if (notifySender != null) {
                                notifySender.sendMessage(error + "Failed to update nickname for " + errorHighlight + playerName + error + ": " + localizedError);
                            }
                            return;
                        }
                    });
                } else {
                    getLogger().warning("Could not sync Discord nickname to Minecraft for " + playerName + ": essentials could not be found");
                    if (notifySender != null) {
                        notifySender.sendMessage(error + "Could not sync Discord nickname to Minecraft for " + errorHighlight + playerName + error + ": essentials could not be found");
                    }
                    return;
                }
                break;
            default:
                return;
        }

        player.sendMessage(
            languageManager.getMessage(
                "messages.nickname_updated",
                "to", to,
                "from", from,
                "nickname", newNick
            ) + "\n" +
            languageManager.getMessage("messages.sync_notif")
        );
        
        if (player != notifySender && notifySender != null) {
            notifySender.sendMessage(languageManager.getMessage(
                "messages.sync_success",
                "player", newNick,
                "from", from,
                "to", to
            ));
        }
    }

    public void reloadPluginConfig() {
        saveDefaultConfig();
        reloadConfig(); // Reload config.yml from disk
        languageManager.loadLanguageFile();
        dataManager.loadData(); // Reload data.json if needed
        getLogger().info("Configuration reloaded.");
        // Run the Update Checker using GitHub API
        updateChecker.checkForUpdates();
    }
}
