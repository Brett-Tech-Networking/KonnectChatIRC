package com.btech.konnectchatirc;

public class ServerItem {
    private String name;
    private int iconResId;

    public ServerItem(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
}
