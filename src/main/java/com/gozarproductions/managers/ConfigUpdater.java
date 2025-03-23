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

            Map<String, String> commentsBefore = new LinkedHashMap<>();
            Map<String, String> commentsAfter = new LinkedHashMap<>();
            Map<Integer, String> indentPath = new HashMap<>();
            StringBuilder commentBuffer = new StringBuilder();

            String lastKey = null;
            boolean inCommentBlock = false;
            boolean blankBeforeComment = true;

            BufferedReader reader = new BufferedReader(new InputStreamReader(defaultStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    if (inCommentBlock) blankBeforeComment = true;
                    continue;
                }

                if (trimmed.startsWith("#")) {
                    if (!inCommentBlock) {
                        inCommentBlock = true;
                        blankBeforeComment = false;
                        commentBuffer.setLength(0);
                    }
                    commentBuffer.append(line).append("\n");
                    continue;
                }

                int indent = line.indexOf(trimmed);
                String key = trimmed.split(":")[0].trim();
                indentPath.put(indent, key);

                StringBuilder fullPathBuilder = new StringBuilder();
                for (int i = 0; i <= indent; i++) {
                    if (indentPath.containsKey(i)) {
                        if (fullPathBuilder.length() > 0) fullPathBuilder.append(".");
                        fullPathBuilder.append(indentPath.get(i));
                    }
                }
                String fullPath = fullPathBuilder.toString();

                if (commentBuffer.length() > 0) {
                    boolean blankAfter = false;
                    reader.mark(1000);
                    String nextLine = reader.readLine();
                    if (nextLine == null || nextLine.trim().isEmpty()) {
                        blankAfter = true;
                    }
                    reader.reset();

                    if (!blankBeforeComment && !blankAfter) {
                        commentsBefore.put(fullPath, commentBuffer.toString());
                    } else if (!blankBeforeComment) {
                        commentsBefore.put(fullPath, commentBuffer.toString());
                    } else if (!blankAfter && lastKey != null) {
                        commentsAfter.put(lastKey, commentBuffer.toString());
                    }

                    commentBuffer.setLength(0);
                    inCommentBlock = false;
                }

                lastKey = fullPath;
            }

            // Rebuild YAML
            StringBuilder output = new StringBuilder();
            writeSection(output, defaultConfig, 0, "", userConfig, commentsBefore, commentsAfter);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(output.toString());
            }

            plugin.getLogger().info("Updated " + fileName + ".");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getLocalizedMessage());
        }
    }

    private void writeSection(StringBuilder yaml, ConfigurationSection section, int indent, String pathPrefix, FileConfiguration userConfig, Map<String, String> commentsBefore, Map<String, String> commentsAfter) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            String fullPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;

            // Build indent
            StringBuilder indentBuilder = new StringBuilder();
            for (int i = 0; i < indent; i++) indentBuilder.append(" ");
            String indentStr = indentBuilder.toString();

            if (commentsBefore.containsKey(fullPath)) {
                yaml.append(commentsBefore.get(fullPath));
            }

            yaml.append(indentStr).append(key).append(": ");

            if (value instanceof ConfigurationSection) {
                yaml.append("\n");
                writeSection(yaml, (ConfigurationSection) value, indent + 2, fullPath, userConfig, commentsBefore, commentsAfter);
            } else {
                Object val = userConfig.contains(fullPath) ? userConfig.get(fullPath) : value;
                yaml.append(formatYamlValue(val, indent)).append("\n");
            }

            if (commentsAfter.containsKey(fullPath)) {
                yaml.append(commentsAfter.get(fullPath));
            }
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

            InputStream defaultStream = plugin.getResource("data.json");
            if (defaultStream != null) {
                JsonObject defaultData = JsonParser.parseReader(new InputStreamReader(defaultStream)).getAsJsonObject();

                boolean updated = false;
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
