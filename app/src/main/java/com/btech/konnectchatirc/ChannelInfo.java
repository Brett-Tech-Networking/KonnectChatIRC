package com.btech.konnectchatirc;

public class ChannelInfo {
    private final String channelName;
    private final int userCount;
    private final String description;
    private boolean isFavorite;

    public ChannelInfo(String channelName, int userCount, String description) {
        this.channelName = channelName;
        this.userCount = userCount;
        this.description = description;
        this.isFavorite = false;
    }

    public String getChannelName() {
        return channelName;
    }

    public int getUserCount() {
        return userCount;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
