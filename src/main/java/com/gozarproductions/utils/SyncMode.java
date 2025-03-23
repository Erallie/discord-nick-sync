package com.gozarproductions.utils;

import org.bukkit.Bukkit;

/**
 * Enum representing the available sync modes for nickname synchronization.
 */
public enum SyncMode {
    MINECRAFT, DISCORD, OFF;

    /**
     * Converts a string to a SyncMode, defaulting to DISCORD if invalid.
     */
    public static SyncMode fromString(String mode) {
        if (mode == null) return null;
        try {
            return SyncMode.valueOf(mode.toUpperCase()); // Convert input to uppercase before matching
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Invalid sync mode: '" + mode + "'.");
            return null;
        }
    }

    /**
     * Ensures sync mode is always stored as lowercase.
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
