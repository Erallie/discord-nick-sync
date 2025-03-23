package com.gozarproductions.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gozarproductions.DiscordNickSync;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Map;

public class ConfigUpdater {
    private final DiscordNickSync plugin;

    public ConfigUpdater(DiscordNickSync plugin) {
        this.plugin = plugin;
    }

    public void checkAndUpdateConfigs() {
        String currentVersion = plugin.getDescription().getVersion();
        String storedVersion = plugin.getConfig().getString("internal.plugin-version", "0.0");

        if (!currentVersion.equals(storedVersion)) {
            plugin.getLogger().info("Detected new version (" + currentVersion + "). Updating configuration files...");

            updateYamlFile("config.yml");
            updateYamlFile("language.yml");
            updateJsonFile();

            plugin.reloadConfig();
            plugin.getConfig().set("internal.plugin-version", currentVersion);
            plugin.saveConfig();
        }
    }

    private void updateYamlFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) return;

        try {
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
            InputStream defaultStream = plugin.getResource(fileName);
            if (defaultStream == null) return;

            // Read the entire default config as text
            StringBuilder defaultText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(defaultStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    defaultText.append(line).append("\n");
                }
            }

            // Replace any values in the default text with the user's values
            String mergedText = defaultText.toString();
            for (String key : userConfig.getKeys(true)) {
                Object userValue = userConfig.get(key);
                if (userValue == null) continue;
                String userFormatted = formatYamlValue(userValue);

                // Escape regex characters in the key pattern
                String regexKeyPattern = "(?m)^([ \t]*)" + key.replace(".", "[ \\t]*\\n?\\1") + ": .*?$";
                String replacement = "$1" + key.substring(key.lastIndexOf('.') + 1) + ": " + userFormatted;

                // Try replacing the value in-place
                mergedText = mergedText.replaceAll(regexKeyPattern, replacement);
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(mergedText);
            }

            plugin.getLogger().info("Updated " + fileName + " with user values merged into the latest default.");

        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getLocalizedMessage());
        }
    }

    private String formatYamlValue(Object value) {
        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof java.util.List) {
            StringBuilder sb = new StringBuilder("\n");
            for (Object item : (java.util.List<?>) value) {
                sb.append("  - ").append(item).append("\n");
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private void updateJsonFile() {
        File dataFile = new File(plugin.getDataFolder(), "data.json");

        if (!dataFile.exists()) {
            plugin.saveResource("data.json", false);
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Gson gson = new Gson();
            JsonObject currentData = JsonParser.parseReader(reader).getAsJsonObject();

            InputStream defaultStream = plugin.getResource("data.json");
            if (defaultStream != null) {
                JsonObject defaultData = JsonParser.parseReader(new InputStreamReader(defaultStream)).getAsJsonObject();

                boolean updated = false;
                for (Map.Entry<String, JsonElement> entry : defaultData.entrySet()) {
                    if (!currentData.has(entry.getKey())) {
                        currentData.add(entry.getKey(), entry.getValue());
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
