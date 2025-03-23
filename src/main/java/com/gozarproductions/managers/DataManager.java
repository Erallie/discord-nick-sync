package com.gozarproductions.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.gozarproductions.DiscordNickSync;
import com.gozarproductions.utils.SyncMode;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class DataManager {
    private final DiscordNickSync plugin;
    private final File dataFile;
    private final Gson gson;
    private Map<UUID, SyncMode> syncPreferences;

    public DataManager(File pluginFolder, DiscordNickSync plugin) {
        this.plugin = plugin;
        this.dataFile = new File(pluginFolder, "data.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.syncPreferences = new HashMap<>();
        loadData();
    }

    /**
     * Loads the sync preferences from `data.json` into memory.
     */
    public void loadData() {
        if (!dataFile.exists()) {
            saveData(); // Create the file if it doesn't exist
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loadedData = gson.fromJson(reader, type);

            // Convert string keys back to UUID
            if (loadedData != null) {
                syncPreferences.clear();
                for (Map.Entry<String, String> entry : loadedData.entrySet()) {
                    SyncMode mode = SyncMode.fromString(entry.getValue());
                    syncPreferences.put(UUID.fromString(entry.getKey()), mode);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe(e.getLocalizedMessage());
        }
    }

    /**
     * Saves the sync preferences to `data.json`.
     */
    public void saveData() {
        try (Writer writer = new FileWriter(dataFile)) {
            // Convert UUID keys to Strings before saving
            Map<String, String> convertedData = new HashMap<>();
            for (Map.Entry<UUID, SyncMode> entry : syncPreferences.entrySet()) {
                convertedData.put(entry.getKey().toString(), entry.getValue().toString());
            }
            gson.toJson(convertedData, writer);
        } catch (IOException e) {
            plugin.getLogger().severe(e.getLocalizedMessage());
        }
    }

    /**
     * Sets the sync mode for a player in memory (but does NOT save immediately).
     */
    public void setSyncMode(UUID uuid, SyncMode mode) {
        syncPreferences.put(uuid, mode);
    }

    /**
     * Gets the sync mode for a player. Defaults to "discord" if not set.
     */
    public SyncMode getSyncMode(UUID uuid) {
        SyncMode storedMode = syncPreferences.get(uuid);

        if (storedMode == null) {
            // If no specific setting exists, get the default sync mode from config.yml
            String defaultMode = plugin.getConfig().getString("default-sync", "discord");
            return SyncMode.fromString(defaultMode);
        }
        return storedMode;
    }

}
