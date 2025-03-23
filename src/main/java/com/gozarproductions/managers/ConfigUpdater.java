package com.gozarproductions.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gozarproductions.DiscordNickSync;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
            plugin.reloadConfig();
            plugin.getConfig().set("internal.plugin-version", currentVersion);
            plugin.saveConfig();
        }
    }

    private void writeSection(StringBuilder yaml, ConfigurationSection section, int indent, String pathPrefix, FileConfiguration userConfig, Map<String, String> commentMap) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            String fullPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;

            // Build indent
            StringBuilder indentBuilder = new StringBuilder();
            for (int i = 0; i < indent; i++) indentBuilder.append(" ");
            String indentStr = indentBuilder.toString();

            // Insert comment before key (if exists)
            if (commentMap.containsKey(fullPath)) {
                yaml.append(commentMap.get(fullPath));
            }

            if (value instanceof ConfigurationSection) {
                yaml.append(indentStr).append(key).append(":\n");
                writeSection(yaml, (ConfigurationSection) value, indent + 2, fullPath, userConfig, commentMap);
            } else {
                Object val = userConfig.contains(fullPath) ? userConfig.get(fullPath) : value;
                yaml.append(indentStr).append(key).append(": ").append(formatYamlValue(val, indent)).append("\n");
            }
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

            // Step 1: Parse default config file to collect comments per full key
            Map<String, String> commentMap = new LinkedHashMap<>();
            Map<Integer, String> indentPath = new HashMap<>();
            StringBuilder commentBuffer = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(defaultStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    commentBuffer.append(line).append("\n");
                    continue;
                }

                int indent = line.indexOf(trimmed); // number of leading spaces
                String key = trimmed.split(":")[0].trim();
                indentPath.put(indent, key);

                // Build full path from indentPath
                StringBuilder fullPath = new StringBuilder();
                for (int i = 0; i <= indent; i++) {
                    if (indentPath.containsKey(i)) {
                        if (fullPath.length() > 0) fullPath.append(".");
                        fullPath.append(indentPath.get(i));
                    }
                }

                if (commentBuffer.length() > 0) {
                    commentMap.put(fullPath.toString(), commentBuffer.toString());
                    commentBuffer.setLength(0);
                }
            }

            // Step 2: Rebuild YAML with preserved comments and values
            StringBuilder output = new StringBuilder();
            writeSection(output, defaultConfig, 0, "", userConfig, commentMap);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(output.toString());
            }

            plugin.getLogger().info("Updated " + fileName + ".");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getLocalizedMessage());
        }
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
