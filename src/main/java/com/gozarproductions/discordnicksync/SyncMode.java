package com.gozarproductions.discordnicksync;

/**
 * Enum representing the available sync modes for nickname synchronization.
 */
public enum SyncMode {
    MINECRAFT, DISCORD, OFF;

    /**
     * Converts a string to a SyncMode, defaulting to DISCORD if invalid.
     */
    public static SyncMode fromString(String mode) {
        try {
            return SyncMode.valueOf(mode.toUpperCase()); // Convert input to uppercase before matching
        } catch (IllegalArgumentException e) {
            return DISCORD; // Default to discord if an invalid value is provided
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
