package com.gozarproductions.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gozarproductions.DiscordNickSync;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final DiscordNickSync plugin;
    private final String repoUrl; // GitHub API URL
    private final String repoName;
    private final String updatePermission;
    private boolean isLatest;
    private String latestVersion = null;
    private String currentVersion = null;
    private String downloadUrl = null;

    public UpdateChecker(DiscordNickSync plugin, String repoOwner, String repoName, String updatePermission) {
        this.plugin = plugin;
        this.repoUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
        this.repoName = repoName;
        this.updatePermission = updatePermission;
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
                String tagName = json.get("tag_name").getAsString();
                latestVersion = tagName.replace("v", ""); // Remove 'v' prefix if present
                String modrinthUrl = "https://modrinth.com/plugin/" + repoName + "/version/" + tagName;
                try {
                    HttpURLConnection modrinthCheck = (HttpURLConnection) new URL(modrinthUrl).openConnection();
                    modrinthCheck.setRequestMethod("HEAD");
                    modrinthCheck.setInstanceFollowRedirects(false);
                    int responseCode = modrinthCheck.getResponseCode();
                    if (responseCode == 200) {
                        downloadUrl = modrinthUrl;
                    } else {
                        downloadUrl = json.get("html_url").getAsString();
                    }
                } catch (Exception e) {
                    downloadUrl = json.get("html_url").getAsString();
                }

                // Get current plugin version
                currentVersion = plugin.getDescription().getVersion();
                isLatest = isLatestVersion();

                // Compare versions
                if (recallAndNotify(null)) {
                    plugin.getLogger().info("Plugin is up to date.");
                } else {
                    plugin.getLogger().warning(
                        "A new version is available: " + latestVersion +
                        "\n(Current version: " + currentVersion +
                        ")\n Download: " + downloadUrl);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getLocalizedMessage());
            }
        });
    }

    public boolean recallAndNotify(Player toNotify) {
        if (latestVersion == null) {
            //!TODO: notify admins if you can't find the latest version.
            return false;
        }
        if (isLatest) {
            return true;
        } else if (toNotify != null) {
            notify(toNotify);
            return false;
        } else {
            notifyAdmins();
            return false;
        }
    }

    private boolean isLatestVersion() {
        String[] latestSplit = latestVersion.split("-");
        // return true if is a dev release and they don't want to be notified.
        if (latestSplit.length >= 2 && plugin.getConfig().getBoolean("updater.notify-on-dev-release") == false) {
            return true;
        }
        String[] latestParts = latestSplit[0].split("\\.");
        String[] currentParts = currentVersion.split("-")[0].split("\\.");
        int latestLength = latestParts.length;
        int currentLength = currentParts.length;

        int length = Math.max(currentLength, latestLength);
        for (int i = 0; i < length; i++) {
            int lat = i < latestLength ? Integer.parseInt(latestParts[i]) : 0;
            int curr = i < currentLength ? Integer.parseInt(currentParts[i]) : 0;
            if (curr < lat) return false;
            if (curr > lat) return true;
        }
        return true; // Versions are equal
    }

    /**
     * Notifies all admins about the new update.
     */
    private void notifyAdmins() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(updatePermission)) {
                notify(player);
            }
        }
    }

    private void notify(Player player) {
        LanguageManager languageManager = plugin.getLanguageManager();
        String defaultColor = languageManager.getCachedColor("d");
        String highlight = languageManager.getCachedColor("h");
        
        player.sendMessage(
            highlight + "[" + ChatColor.BOLD + "DiscordNickSync" + highlight + "] " + defaultColor + "A new update is available: " + highlight + latestVersion +
            "\n" + defaultColor + "(Current version: " + highlight + currentVersion + defaultColor + ")" + 
            "\n" + defaultColor + "Download: " + highlight + downloadUrl);
    }
}
