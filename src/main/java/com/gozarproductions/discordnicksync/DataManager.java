package com.gozarproductions.discordnicksync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final File dataFile;
    private final Gson gson;
    private Map<UUID, String> syncPreferences;

    public DataManager(File pluginFolder) {
        this.dataFile = new File(pluginFolder, "data.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.syncPreferences = new HashMap<>();
        loadData();
    }

    /**
     * Loads the sync preferences from `data.json` into memory.
     */
    private void loadData() {
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
                    syncPreferences.put(UUID.fromString(entry.getKey()), entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the sync preferences to `data.json`.
     */
    public void saveData() {
        try (Writer writer = new FileWriter(dataFile)) {
            // Convert UUID keys to Strings before saving
            Map<String, String> convertedData = new HashMap<>();
            for (Map.Entry<UUID, String> entry : syncPreferences.entrySet()) {
                convertedData.put(entry.getKey().toString(), entry.getValue());
            }

            gson.toJson(convertedData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the sync mode for a player in memory (but does NOT save immediately).
     */
    public void setSyncMode(UUID uuid, String mode) {
        syncPreferences.put(uuid, mode.toLowerCase());
    }

    /**
     * Gets the sync mode for a player. Defaults to "discord" if not set.
     */
    public String getSyncMode(UUID uuid) {
        return SyncMode.fromString(syncPreferences.getOrDefault(uuid, "discord")).name();
    }

}
