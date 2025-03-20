package com.gozarproductions.discordnicksync;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LanguageManager {
    private final DiscordNickSync plugin;
    private File languageFile;
    private FileConfiguration languageConfig;

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
        String color = languageConfig.getString("colors." + key, "");
        return ChatColor.translateAlternateColorCodes('&', color);
    }

    /**
     * Retrieves a message from the language config and applies placeholders.
     */
    public String getMessage(String key, String... replacements) {
        String message = languageConfig.getString(key, key);

        // Apply color codes
        message = message
            .replace("{d}", getColor("default"))
            .replace("{h}", getColor("highlight"))
            .replace("{e}", getColor("error"))
            .replace("{eh}", getColor("error_highlight"));

        // Apply placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
