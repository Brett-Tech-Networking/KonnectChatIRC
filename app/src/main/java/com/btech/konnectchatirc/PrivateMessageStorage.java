package com.btech.konnectchatirc;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrivateMessageStorage {
    private final SharedPreferences prefs;
    private static final String KEY_PREFIX = "pm_";

    public PrivateMessageStorage(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void saveMessage(String userNick, String recipientNick, String message, boolean isSent) {
        createConversation(userNick, recipientNick);
        String key = getConversationKey(userNick, recipientNick);
        List<PrivateMessage> messages = getMessages(userNick, recipientNick);
        messages.add(new PrivateMessage(isSent ? userNick : recipientNick, message, System.currentTimeMillis(), isSent));
        prefs.edit().putString(key, messagesToJson(messages)).apply();
    }

    public List<PrivateMessage> getMessages(String userNick, String recipientNick) {
        return messagesFromJson(prefs.getString(getConversationKey(userNick, recipientNick), "[]"));
    }

    public List<String> getAllConversations(String userNick) {
        Map<String, ?> all = prefs.getAll();
        List<String> conversations = new ArrayList<>();
        String u = userNick.toLowerCase();
        for (String key : all.keySet()) {
            if (key.startsWith(KEY_PREFIX) && !key.endsWith("_unread")) {
                String rem = key.substring(KEY_PREFIX.length());
                String other = null;
                if (rem.startsWith(u + "_")) other = rem.substring(u.length() + 1);
                else if (rem.endsWith("_" + u)) other = rem.substring(0, rem.length() - u.length() - 1);
                if (other != null && !conversations.contains(other)) conversations.add(other);
            }
        }
        return conversations;
    }

    public void clearConversation(String userNick, String recipientNick) {
        String key = getConversationKey(userNick, recipientNick);
        prefs.edit().remove(key).remove(key + "_unread").apply();
    }

    public void createConversation(String userNick, String recipientNick) {
        String key = getConversationKey(userNick, recipientNick);
        if (!prefs.contains(key)) prefs.edit().putString(key, "[]").apply();
    }

    private String getConversationKey(String u1, String u2) {
        String[] n = {u1.toLowerCase(), u2.toLowerCase()};
        java.util.Arrays.sort(n);
        return KEY_PREFIX + n[0] + "_" + n[1];
    }

    private String messagesToJson(List<PrivateMessage> msgs) {
        JSONArray arr = new JSONArray();
        for (PrivateMessage m : msgs) {
            try {
                JSONObject o = new JSONObject();
                o.put("sender", m.sender); o.put("message", m.message);
                o.put("timestamp", m.timestamp); o.put("isSent", m.isSent);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    private List<PrivateMessage> messagesFromJson(String json) {
        List<PrivateMessage> msgs = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                msgs.add(new PrivateMessage(o.getString("sender"), o.getString("message"), o.getLong("timestamp"), o.getBoolean("isSent")));
            }
        } catch (Exception ignored) {}
        return msgs;
    }

    public static class PrivateMessage {
        public String sender, message;
        public long timestamp;
        public boolean isSent;
        public PrivateMessage(String s, String m, long t, boolean i) { sender=s; message=m; timestamp=t; isSent=i; }
    }

    public int getUnreadCount(String u1, String u2) {
        return prefs.getInt(getConversationKey(u1, u2) + "_unread", 0);
    }

    public void incrementUnreadCount(String u1, String u2) {
        String k = getConversationKey(u1, u2) + "_unread";
        prefs.edit().putInt(k, prefs.getInt(k, 0) + 1).apply();
    }

    public void resetUnreadCount(String u1, String u2) {
        prefs.edit().remove(getConversationKey(u1, u2) + "_unread").apply();
    }

    public int getTotalUnreadCount(String userNick) {
        int total = 0;
        List<String> convs = getAllConversations(userNick);
        for (String recipient : convs) {
            total += getUnreadCount(userNick, recipient);
        }
        return total;
    }
}