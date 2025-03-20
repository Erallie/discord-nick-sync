package com.gozarproductions.discordnicksync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public class ConfigUpdater {
    private final DiscordNickSync plugin;

    public ConfigUpdater(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the plugin version has changed and updates config files if necessary.
     */
    public void checkAndUpdateConfigs() {
        String currentVersion = plugin.getDescription().getVersion();
        String storedVersion = plugin.getConfig().getString("internal.plugin-version", "0.0");

        if (!currentVersion.equals(storedVersion)) {
            plugin.getLogger().info("Detected new version (" + currentVersion + "). Updating configuration files...");

            updateYamlFile("config.yml");
            updateYamlFile("language.yml");
            updateJsonFile();

            // Save the new version to config.yml
            plugin.getConfig().set("internal.plugin-version", currentVersion);
            plugin.saveConfig();
        }
    }

    /**
     * Updates a YAML file by adding missing keys without overwriting existing values.
     */
    private void updateYamlFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        // Load current file (user's existing version)
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(file);

        // Load default file from JAR
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream == null) return; // No default file found in JAR

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));

        boolean updated = false;

        // Check for missing keys and add them
        for (String key : defaultConfig.getKeys(true)) {
            if (!currentConfig.contains(key)) {
                currentConfig.set(key, defaultConfig.get(key));
                updated = true;
            }
        }

        // Save if changes were made
        if (updated) {
            try {
                currentConfig.save(file);
                plugin.getLogger().info("Updated " + fileName + " with new settings.");
            } catch (IOException e) {
                plugin.getLogger().warning("Could not update " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Updates data.json by adding missing keys without overwriting existing data.
     */
    private void updateJsonFile() {
        File dataFile = new File(plugin.getDataFolder(), "data.json");

        if (!dataFile.exists()) {
            plugin.saveResource("data.json", false);
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Gson gson = new Gson();
            JsonObject currentData = JsonParser.parseReader(reader).getAsJsonObject();

            // Load default file from JAR (if available)
            InputStream defaultStream = plugin.getResource("data.json");
            if (defaultStream != null) {
                JsonObject defaultData = JsonParser.parseReader(new InputStreamReader(defaultStream)).getAsJsonObject();

                boolean updated = false;

                // Add missing keys from the default file
                for (String key : defaultData.keySet()) {
                    if (!currentData.has(key)) {
                        currentData.add(key, defaultData.get(key));
                        updated = true;
                    }
                }

                if (updated) {
                    try (Writer writer = new FileWriter(dataFile)) {
                        gson.toJson(currentData, writer);
                        plugin.getLogger().info("[DiscordNickSync] Updated data.json with new settings.");
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[DiscordNickSync] Could not update data.json: " + e.getMessage());
        }
    }
}
