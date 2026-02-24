package com.btech.konnectchatirc;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages local storage of channel unread counts using SharedPreferences
 */
public class ChannelStorage {
    private final SharedPreferences prefs;
    private static final String PREFS_NAME = "channel_counts";
    private static final String KEY_PREFIX = "chan_unread_";

    public ChannelStorage(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Get unread count for a channel
     */
    public int getUnreadCount(String channelName) {
        String key = getChannelKey(channelName);
        return prefs.getInt(key, 0);
    }

    /**
     * Increment unread count for a channel
     */
    public void incrementUnreadCount(String channelName) {
        String key = getChannelKey(channelName);
        int currentCount = prefs.getInt(key, 0);
        prefs.edit().putInt(key, currentCount + 1).apply();
    }

    /**
     * Reset unread count for a channel
     */
    public void resetUnreadCount(String channelName) {
        String key = getChannelKey(channelName);
        prefs.edit().remove(key).apply();
    }

    /**
     * Clear all channel unread counts (used on session start)
     */
    public void clearAllUnreadCounts() {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allPrefs = prefs.getAll();
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    /**
     * Get unread counts for all channels
     */
    public Map<String, Integer> getAllUnreadCounts() {
        Map<String, ?> allPrefs = prefs.getAll();
        Map<String, Integer> counts = new HashMap<>();

        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                String channelName = key.substring(KEY_PREFIX.length());
                Object value = allPrefs.get(key);
                if (value instanceof Integer) {
                    counts.put(channelName, (Integer) value);
                }
            }
        }
        return counts;
    }

    private String getChannelKey(String channelName) {
        // Normalize channel name (lowercase)
        return KEY_PREFIX + channelName.toLowerCase();
    }
}
