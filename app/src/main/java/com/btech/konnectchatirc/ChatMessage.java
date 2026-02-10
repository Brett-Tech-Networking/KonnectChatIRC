package com.btech.konnectchatirc;

public class ChatMessage {
    private final Object content;
    private final long timestamp;

    public ChatMessage(Object content, long timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }

    public Object getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
