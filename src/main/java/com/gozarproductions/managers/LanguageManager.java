package com.gozarproductions.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.gozarproductions.DiscordNickSync;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final DiscordNickSync plugin;
    private File languageFile;
    private FileConfiguration languageConfig;
    private final Map<String, String> cachedColors = new HashMap<>();

    public String getCachedColor(String key) {
        String color = cachedColors.get(key);
        return ChatColor.translateAlternateColorCodes('&', color);
    }

    public LanguageManager(DiscordNickSync plugin) {
        this.plugin = plugin;
        loadLanguageFile();
    }

    /**
     * Loads or creates the language.yml file.
     */
    public void loadLanguageFile() {
        languageFile = new File(plugin.getDataFolder(), "language.yml");

        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false); // Copy default file from JAR
        }

        languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        cachedColors.clear();
        cachedColors.put("d", getColor("default"));
        cachedColors.put("h", getColor("highlight"));
        cachedColors.put("e", getColor("error"));
        cachedColors.put("eh", getColor("error_highlight"));
    }

    /**
     * Reloads the language file (when running /discordnick reload).
     */
    public void reloadLanguageFile() {
        loadLanguageFile();
    }

    /**
     * Retrieves a color from the language config.
     */
    public String getColor(String key) {
        return languageConfig.getString("colors." + key, "");
    }

    /**
     * Retrieves a message from the language config and applies placeholders.
     */
    public String getMessage(String key, String... replacements) {
        String message = languageConfig.getString(key, key);

        // Apply color codes
        for (Map.Entry<String, String> entry : cachedColors.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }


        // Apply placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
