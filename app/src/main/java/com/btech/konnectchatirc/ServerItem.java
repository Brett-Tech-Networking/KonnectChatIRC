package com.btech.konnectchatirc;

public class ServerItem {
    private String serverName;
    private int iconResId;

    public ServerItem(String serverName, int iconResId) {
        this.serverName = serverName;
        this.iconResId = iconResId;
    }

    public String getServerName() {
        return serverName;
    }

    public int getIconResId() {
        return iconResId;
    }

    @Override
    public String toString() {
        return serverName;
    }
}
