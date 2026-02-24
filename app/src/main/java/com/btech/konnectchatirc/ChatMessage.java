package com.btech.konnectchatirc;

public class ChatMessage {
    private final Object content;
    private final long timestamp;
    private String translatedContent;
    private boolean isTranslationVisible = false;

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

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public boolean isTranslationVisible() {
        return isTranslationVisible;
    }

    public void setTranslationVisible(boolean visible) {
        this.isTranslationVisible = visible;
    }
}
