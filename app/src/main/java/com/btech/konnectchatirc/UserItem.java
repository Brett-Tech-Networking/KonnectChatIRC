package com.btech.konnectchatirc;

public class UserItem {
    private String prefix;
    private String nick;

    public UserItem(String prefix, String nick) {
        this.prefix = prefix;
        this.nick = nick;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNick() {
        return nick;
    }

    public String getDisplayName() {
        return prefix + nick;
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
                return 6; // Normal users
        }
    }
}
