package com.btech.konnectchatirc;

import java.util.ArrayList;
import java.util.List;

public class ChannelItem {
    private String channelName;
    private List<String> messages;
    private int unreadCount;

    public ChannelItem(String channelName) {
        this.channelName = channelName;
        this.messages = new ArrayList<>();
        this.unreadCount = 0;
    }

    public String getChannelName() {
        return channelName;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String message) {
        messages.add(message);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void incrementUnreadCount() {
        unreadCount++;
    }

    public void resetUnreadCount() {
        unreadCount = 0;
    }
}
