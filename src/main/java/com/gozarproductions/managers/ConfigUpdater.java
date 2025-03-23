package com.gozarproductions.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gozarproductions.DiscordNickSync;

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
        if (!file.exists()) return;

        try {
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

            InputStream defaultStream = plugin.getResource(fileName);
            if (defaultStream == null) return;

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource(fileName)));

            BufferedReader reader = new BufferedReader(new InputStreamReader(defaultStream));
            StringBuilder updatedYaml = new StringBuilder();
            StringBuilder commentBuffer = new StringBuilder();

            boolean updated = false;

            while (true) {
                String line = reader.readLine();
                if (line == null) break;

                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    commentBuffer.append(line).append("\n");
                    continue;
                }

                int colonIndex = line.indexOf(":");
                if (colonIndex == -1) {
                    updatedYaml.append(commentBuffer).append(line).append("\n");
                    commentBuffer.setLength(0);
                    continue;
                }

                // Determine indentation and key
                String indentation = line.substring(0, colonIndex).replaceAll("[^\\s]", ""); // preserve leading whitespace
                String rawKey = line.substring(0, colonIndex).trim();
                String fullKey = getFullYamlKey(line, indentation);

                Object value = userConfig.contains(fullKey)
                    ? userConfig.get(fullKey)
                    : defaultConfig.get(fullKey);

                if (!userConfig.contains(fullKey)) {
                    userConfig.set(fullKey, value);
                    updated = true;
                }

                // Write comment, then key: value
                updatedYaml.append(commentBuffer);
                updatedYaml.append(indentation)
                        .append(rawKey)
                        .append(": ")
                        .append(formatYamlValue(value, indentation.length()))
                        .append("\n");

                commentBuffer.setLength(0);
            }

            if (updated) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(updatedYaml.toString());
                }
                plugin.getLogger().info("Updated " + fileName + " with new settings and comments.");
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getLocalizedMessage());
        }
    }

    private String getFullYamlKey(String line, String indentation) {
        int indentSize = indentation.length();
        String rawKey = line.substring(0, line.indexOf(":")).trim();

        // Track parent keys by indentation level
        if (keyStack == null) keyStack = new java.util.TreeMap<>();
        keyStack.put(indentSize, rawKey);

        // Build full path
        StringBuilder fullKey = new StringBuilder();
        for (int i = 0; i <= indentSize; i++) {
            if (keyStack.containsKey(i)) {
                if (fullKey.length() > 0) fullKey.append(".");
                fullKey.append(keyStack.get(i));
            }
        }
        return fullKey.toString();
    }

    private String formatYamlValue(Object value, int indentLevel) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof java.util.List) {
            StringBuilder listYaml = new StringBuilder("\n");
            for (Object item : (java.util.List<?>) value) {
                for (int i = 0; i < indentLevel + 2; i++) listYaml.append(" ");
                listYaml.append("- ").append(item).append("\n");
            }
            return listYaml.toString().trim();
        }
        return "\"" + value.toString() + "\"";
    }

    private java.util.TreeMap<Integer, String> keyStack;



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
                        plugin.getLogger().info("Updated data.json with new settings.");
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update data.json: " + e.getLocalizedMessage());
        }
    }
}
