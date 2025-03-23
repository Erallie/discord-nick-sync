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
            // Load user's current values
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);

            // Load default file as plain text
            InputStream defaultStream = plugin.getResource(fileName);
            if (defaultStream == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(defaultStream));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("#") && line.contains(":")) {
                    // Extract key path (e.g., a.b.c)
                    String indent = line.substring(0, line.indexOf(line.trim()));
                    String key = line.trim().split(":")[0];
                    String path = buildFullPath(line, indent);

                    Object userValue = userConfig.get(path);
                    if (userValue != null) {
                        line = indent + key + ": " + formatYamlValue(userValue);
                    }
                }
                result.append(line).append("\n");
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(result.toString());
            }

            plugin.getLogger().info("Updated " + fileName + " with user values merged into the latest default.");

        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getLocalizedMessage());
        }
    }

    private String buildFullPath(String line, String indent) {
        String[] parts = line.trim().split(":")[0].split("\\.");
        int indentLevel = indent.length() / 2; // assuming 2 spaces per indent
        StringBuilder path = new StringBuilder();
        for (int i = 0; i <= indentLevel && i < parts.length; i++) {
            if (i > 0) path.append(".");
            path.append(parts[i]);
        }
        return path.toString();
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
