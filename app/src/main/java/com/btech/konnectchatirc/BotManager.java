package com.btech.konnectchatirc;

import java.lang.ref.WeakReference;

/**
 * Singleton manager for accessing the active IRC bot across activities.
 * This allows activities like PrivateChatActivity to access the bot from ChatActivity.
 */
public class BotManager {
    private static WeakReference<BotProvider> activeBotProvider;

    /**
     * Set the active bot provider (typically ChatActivity)
     */
    public static void setActiveBotProvider(BotProvider provider) {
        activeBotProvider = new WeakReference<>(provider);
    }

    /**
     * Get the active bot provider
     */
    public static BotProvider getActiveBotProvider() {
        if (activeBotProvider != null) {
            return activeBotProvider.get();
        }
        return null;
    }

    /**
     * Check if a bot provider is currently active
     */
    public static boolean hasActiveBotProvider() {
        return activeBotProvider != null && activeBotProvider.get() != null;
    }

    /**
     * Clear the reference when done
     */
    public static void clearActiveBotProvider() {
        if (activeBotProvider != null) {
            activeBotProvider.clear();
        }
        activeBotProvider = null;
    }
}
