package com.btech.konnectchatirc;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages local storage of private messages using SharedPreferences
 */
public class PrivateMessageStorage {
    private final SharedPreferences prefs;
    private static final String PREFS_NAME = "private_messages";
    private static final String KEY_PREFIX = "pm_";

    public PrivateMessageStorage(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Save a message to local storage
     */
    public void saveMessage(String userNick, String recipientNick, String message, boolean isSent) {
        String conversationKey = getConversationKey(userNick, recipientNick);
        List<PrivateMessage> messages = getMessages(userNick, recipientNick);
        
        messages.add(new PrivateMessage(
                isSent ? userNick : recipientNick,
                message,
                System.currentTimeMillis(),
                isSent
        ));
        
        String json = messagesToJson(messages);
        prefs.edit().putString(conversationKey, json).apply();
    }

    /**
     * Get all messages for a conversation
     */
    public List<PrivateMessage> getMessages(String userNick, String recipientNick) {
        String conversationKey = getConversationKey(userNick, recipientNick);
        String json = prefs.getString(conversationKey, "[]");
        
        return messagesFromJson(json);
    }

    /**
     * Get all conversations (unique list of users)
     */
    public List<String> getAllConversations(String userNick) {
        Map<String, ?> allPrefs = prefs.getAll();
        List<String> conversations = new ArrayList<>();
        String userNickLower = userNick.toLowerCase();

        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                if (key.endsWith("_unread")) {
                    continue;
                }
                
                String remaining = key.substring(KEY_PREFIX.length());
                String otherNick = null;

                // Check if the key starts with the userNick followed by a separator
                if (remaining.startsWith(userNickLower + "_")) {
                    otherNick = remaining.substring(userNickLower.length() + 1);
                } 
                // Check if the key ends with the userNick preceded by a separator
                else if (remaining.endsWith("_" + userNickLower)) {
                    otherNick = remaining.substring(0, remaining.length() - userNickLower.length() - 1);
                }

                if (otherNick != null && !conversations.contains(otherNick)) {
                    conversations.add(otherNick);
                }
            }
        }
        return conversations;
    }

    /**
     * Clear all messages for a conversation
     */
    public void clearConversation(String userNick, String recipientNick) {
        String conversationKey = getConversationKey(userNick, recipientNick);
        prefs.edit().remove(conversationKey).apply();
    }

    /**
     * Create an empty conversation if it doesn't exist
     */
    public void createConversation(String userNick, String recipientNick) {
        String conversationKey = getConversationKey(userNick, recipientNick);
        if (!prefs.contains(conversationKey)) {
            prefs.edit().putString(conversationKey, "[]").apply();
        }
    }

    /**
     * Get or create a conversation key
     */
    private String getConversationKey(String userNick, String recipientNick) {
        // Always use alphabetical order to ensure same key regardless of direction
        String[] nicks = {userNick.toLowerCase(), recipientNick.toLowerCase()};
        java.util.Arrays.sort(nicks);
        return KEY_PREFIX + nicks[0] + "_" + nicks[1];
    }

    /**
     * Convert messages to JSON string
     */
    private String messagesToJson(List<PrivateMessage> messages) {
        JSONArray array = new JSONArray();
        for (PrivateMessage msg : messages) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("sender", msg.sender);
                obj.put("message", msg.message);
                obj.put("timestamp", msg.timestamp);
                obj.put("isSent", msg.isSent);
                array.put(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return array.toString();
    }

    /**
     * Convert JSON string to messages
     */
    private List<PrivateMessage> messagesFromJson(String json) {
        List<PrivateMessage> messages = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                messages.add(new PrivateMessage(
                        obj.getString("sender"),
                        obj.getString("message"),
                        obj.getLong("timestamp"),
                        obj.getBoolean("isSent")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Model class for private messages
     */
    public static class PrivateMessage {
        public String sender;
        public String message;
        public long timestamp;
        public boolean isSent;

        public PrivateMessage(String sender, String message, long timestamp, boolean isSent) {
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
            this.isSent = isSent;
        }
    }

    /**
     * Get unread count for a conversation
     */
    public int getUnreadCount(String userNick, String recipientNick) {
        String conversationKey = getConversationKey(userNick, recipientNick) + "_unread";
        return prefs.getInt(conversationKey, 0);
    }

    /**
     * Increment unread count for a conversation
     */
    public void incrementUnreadCount(String userNick, String recipientNick) {
        String conversationKey = getConversationKey(userNick, recipientNick) + "_unread";
        int currentCount = prefs.getInt(conversationKey, 0);
        prefs.edit().putInt(conversationKey, currentCount + 1).apply();
    }

    /**
     * Reset unread count for a conversation
     */
    public void resetUnreadCount(String userNick, String recipientNick) {
        String conversationKey = getConversationKey(userNick, recipientNick) + "_unread";
        prefs.edit().remove(conversationKey).apply();
    }

    /**
     * Get total unread count for all private messages
     */
    public int getTotalUnreadCount(String userNick) {
        int total = 0;
        List<String> conversations = getAllConversations(userNick);
        for (String recipient : conversations) {
            total += getUnreadCount(userNick, recipient);
        }
        return total;
    }


}
