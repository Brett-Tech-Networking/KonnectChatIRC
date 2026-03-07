package com.btech.konnectchatirc;

public class ServerItem {
    private String serverName;
    private int iconResId;
    private String serverAddress;
    private int serverPort;

    public ServerItem(String serverName, int iconResId) {
        this.serverName = serverName;
        this.iconResId = iconResId;
        this.serverAddress = null;
        this.serverPort = 0;
    }

    public ServerItem(String serverName, int iconResId, String serverAddress, int serverPort) {
        this.serverName = serverName;
        this.iconResId = iconResId;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public String getServerName() {
        return serverName;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isCustomServer() {
        return serverAddress != null;
    }

    @Override
    public String toString() {
        return serverName;
    }
}
