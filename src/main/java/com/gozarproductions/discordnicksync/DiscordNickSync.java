package com.gozarproductions.discordnicksync;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.HierarchyException;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.earth2me.essentials.Essentials;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DiscordNickSync extends JavaPlugin {

    private Essentials essentials;
    private DataManager dataManager;
    private File languageFile;
    private FileConfiguration languageConfig;

    @Override
    public void onEnable() {
        getLogger().info("DiscordNickSync enabled!");

        // Save default config
        saveDefaultConfig();
        reloadConfig();
        
        // Load language.yml
        loadLanguageFile();

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


    private void loadLanguageFile() {
        languageFile = new File(getDataFolder(), "language.yml");

        if (!languageFile.exists()) {
            saveResource("language.yml", false); // Copy default file from JAR
        }

        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }


    public String getColor(String key) {
        return languageConfig.getString("colors." + key, "");
    }

    public String getMessage(String key, String... replacements) {
        //String message = languageConfig.getString("messages." + key, key);
        String message = languageConfig.getString(key, key);

        // Apply placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            message = message
                .replace("{d}", getColor("default"))
                .replace("{h}", getColor("highlight"))
                .replace("{e}", getColor("error"))
                .replace("{eh}", getColor("error_highlight"))
                .replace("{" + replacements[i] + "}", replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
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
                            getMessage(
                                "messages.nickname_updated",
                                "{to}", "Discord",
                                "{from}", "Minecraft",
                                "{nickname}", newName
                            ) + "\n \n" +
                            getMessage("message.sync_notif")
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
                            getMessage(
                                "messages.nickname_updated",
                                "{to}", "Minecraft",
                                "{from}", "Discord",
                                "{nickname}", formattedNick
                            ) + "\n \n" +
                            getMessage("message.sync_notif")
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

    public DataManager getDataManager() {
        return dataManager;
    }

    public void reloadPluginConfig() {
        saveDefaultConfig();
        reloadConfig(); // Reload config.yml from disk
        loadLanguageFile();
        dataManager.loadData(); // Reload data.json if needed
        getLogger().info("Configuration reloaded.");
    }
}
