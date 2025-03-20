package com.gozarproductions.discordnicksync.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gozarproductions.discordnicksync.DiscordNickSync;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final DiscordNickSync plugin;
    private final String repoUrl; // GitHub API URL
    public String latestVersion = null;
    public String downloadUrl = null;

    public UpdateChecker(DiscordNickSync plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.repoUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Open connection to GitHub API
                HttpURLConnection connection = (HttpURLConnection) new URL(repoUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0"); // Required for GitHub API

                // Parse JSON response
                JsonObject json = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
                latestVersion = json.get("tag_name").getAsString().replace("v", ""); // Remove 'v' prefix if present
                downloadUrl = json.get("html_url").getAsString();

                // Get current plugin version
                String currentVersion = plugin.getDescription().getVersion();

                // Compare versions
                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    plugin.getLogger().warning("A new version is available: " + latestVersion);
                    notifyAdmins(latestVersion, downloadUrl);
                } else {
                    plugin.getLogger().info("Plugin is up to date.");
                }
            } catch (Exception e) {
                plugin.getLogger().warning(" Could not check for updates: " + e.getMessage());
            }
        });
    }

    /**
     * Notifies all admins about the new update.
     */
    public void notifyAdmins(String latestVersion, String downloadUrl) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("discordsync.admin")) {
                player.sendMessage(plugin.getLanguageManager().getColor("highlight") + "[DiscordNickSync] " + plugin.getLanguageManager().getColor("default") + "A new update is available: " + plugin.getLanguageManager().getColor("highlight") + latestVersion);
                player.sendMessage(plugin.getLanguageManager().getColor("default") + "Download: " + plugin.getLanguageManager().getColor("highlight") + downloadUrl);
            }
        }
    }
}
