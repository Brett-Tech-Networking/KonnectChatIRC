package com.btech.konnectchatirc;

public class UserItem {
    private String prefix;
    private String nick;
    private int drawableResId;  // New field for drawable resource ID

    public UserItem(String prefix, String nick, int drawableResId) {
        this.prefix = prefix;
        this.nick = nick;
        this.drawableResId = drawableResId;  // Initialize the drawable resource ID
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
        switch (prefix) {
            case "~":
                return 1;
            case "&":
                return 2;
            case "@":
                return 3;
            case "%":
                return 4;
            case "+":
                return 5;
            default:
                return 6;
        }
    }

    public String getDisplayName() {
        return prefix + nick;
    }
}
