package com.btech.konnectchatirc;

public class UserItem {
    private String prefix;
    private String nick;
    private int drawableResId;

    public UserItem(String prefix, String nick, int drawableResId) {
        this.prefix = prefix;
        this.nick = nick;
        this.drawableResId = drawableResId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNick() {
        return nick;
    }

    public int getDrawableResId() {
        return drawableResId;
    }

    public int getSortOrder() {
        // Define the sort order based on the prefix
        if ("~".equals(prefix)) return 1;
        if ("&".equals(prefix)) return 2;
        if ("@".equals(prefix)) return 3;
        if ("%".equals(prefix)) return 4;
        if ("+".equals(prefix)) return 5;
        return 6; // No prefix
    }

    public String getDisplayName() {
        return prefix + nick;
    }
}
