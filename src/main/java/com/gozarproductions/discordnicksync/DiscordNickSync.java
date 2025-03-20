package com.gozarproductions.discordnicksync;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.HierarchyException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.earth2me.essentials.Essentials;

import com.gozarproductions.discordnicksync.commands.DiscordNickCommand;
import com.gozarproductions.discordnicksync.commands.DiscordNickTabCompleter;
import com.gozarproductions.discordnicksync.listeners.SyncListener;
import com.gozarproductions.discordnicksync.managers.ConfigUpdater;
import com.gozarproductions.discordnicksync.managers.DataManager;
import com.gozarproductions.discordnicksync.managers.LanguageManager;
import com.gozarproductions.discordnicksync.managers.UpdateChecker;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Retrieves the prefix and suffix from the config.
     */
    private Map<String, String> getPrefixAndSuffix(Player player) {
        Map<String, String> result = new HashMap<>();

        if (essentials != null && essentials.getUser(player).getNickname() != null &&
                !essentials.getUser(player).getNickname().isEmpty()) {
            result.put("prefix", getConfig().getString("essentials-nick-prefix", ""));
            result.put("suffix", getConfig().getString("essentials-nick-suffix", ""));
        } else {
            result.put("prefix", "");
            result.put("suffix", "");
            getLogger().info(player.getDisplayName() + " has no essentials nickname.");
        }

        return result;
    }

    /**
     * Syncs the player's Minecraft nickname to Discord.
     */
    public void syncMinecraftToDiscord(Player player, String discordId) {
        if (discordId == null) {
            getLogger().warning("Could not sync Minecraft nickname to Discord for " + player.getDisplayName() + ": discordId is null");
            return;
        }

        User discordUser = DiscordSRV.getPlugin().getJda().getUserById(discordId);
        if (discordUser == null) {
            getLogger().warning("Could not sync Minecraft nickname to Discord for " + player.getDisplayName() + ": discordUser is null");
            return;
        }

        Member discordMember = DiscordSRV.getPlugin().getJda().getGuilds().get(0).getMember(discordUser);
        if (discordMember == null) {
            getLogger().warning("Could not sync Minecraft nickname to Discord for " + player.getDisplayName() + ": discordMember is null");
            return;
        }

        String minecraftNickname = player.getDisplayName();
        String currentDiscordNick = discordMember.getEffectiveName();

        Map<String, String> prefixAndSuffixData = getPrefixAndSuffix(player);
        String prefix = prefixAndSuffixData.get("prefix");
        String suffix = prefixAndSuffixData.get("suffix");

        String formattedNick = prefix + currentDiscordNick + suffix;

        // Only update if the nickname is different
        if (!formattedNick.equals(minecraftNickname)) {
            String newName = minecraftNickname.substring(prefix.length(), minecraftNickname.length() - suffix.length());
            DiscordSRV.getPlugin().getJda().getGuilds().get(0)
                .modifyNickname(discordMember, newName)
                .queue(
                    success -> {
                        getLogger().info("Updated Discord nickname for " + player.getName() + " to " + newName);
                        player.sendMessage(
                            languageManager.getMessage(
                                "messages.nickname_updated",
                                "to", "Discord",
                                "from", "Minecraft",
                                "nickname", newName
                            ) + "\n \n" +
                            languageManager.getMessage("messages.sync_notif")
                        );
                    },
                    failure -> {
                        if (failure instanceof HierarchyException) {
                            getLogger().warning("Cannot modify nickname for " + player.getName() + 
                                " (Discord role hierarchy issue). Ensure the bot has permission and is above the user in the role list.");
                        } else {
                            getLogger().warning("Failed to update Discord nickname for " + player.getName() +
                                ": " + failure.getMessage());
                        }
                    }
                );
        } else {
            getLogger().info("Did not sync Minecraft nickname to Discord for " + player.getDisplayName() + " because they already match.");
        }
    }

    /**
     * Syncs the player's Discord nickname to Minecraft.
     */
    public void syncDiscordToMinecraft(Player player, String discordId) {
        if (discordId == null) {
            getLogger().warning("Could not sync Discord nickname to Minecraft for " + player.getDisplayName() + ": discordId is null");
            return;
        }

        User discordUser = DiscordSRV.getPlugin().getJda().getUserById(discordId);
        if (discordUser == null) {
            getLogger().warning("Could not sync Discord nickname to Minecraft for " + player.getDisplayName() + ": discordUser is null");
            return;
        }

        Member discordMember = DiscordSRV.getPlugin().getJda().getGuilds().get(0).getMember(discordUser);
        if (discordMember == null) {
            getLogger().warning("Could not sync Discord nickname to Minecraft for " + player.getDisplayName() + ": discordMember is null");
            return;
        }

        String discordNickname = discordMember.getEffectiveName();
        Map<String, String> prefixAndSuffixData = getPrefixAndSuffix(player);
        String prefix = prefixAndSuffixData.get("prefix");
        String suffix = prefixAndSuffixData.get("suffix");

        String formattedNick = prefix + discordNickname + suffix;
        String currentMcNick = player.getDisplayName();

        if (essentials != null) {
            if (!currentMcNick.equals(formattedNick)) {
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        essentials.getUser(player).setNickname(discordNickname);
                        player.sendMessage(
                            languageManager.getMessage(
                                "messages.nickname_updated",
                                "to", "Minecraft",
                                "from", "Discord",
                                "nickname", formattedNick
                            ) + "\n \n" +
                            languageManager.getMessage("messages.sync_notif")
                        );
                    } catch (Exception e) {
                        getLogger().warning("Failed to update nickname for " + player.getName());
                    }
                });
            } else {
                getLogger().info("Did not sync Discord nickname to Minecraft for " + player.getDisplayName() + " because they already match.");
            }
        } else {
            getLogger().warning("Could not sync Discord nickname to Minecraft for " + player.getDisplayName() + ": essentials could not be found");
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
