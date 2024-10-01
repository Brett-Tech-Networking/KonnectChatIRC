package com.btech.konnectchatirc;

import org.pircbotx.User;
import org.pircbotx.UserLevel;
import org.pircbotx.Channel;

import java.util.Set;

public class IrcUtils {
    public static String getUserPrefix(User user, Channel channel) {
        if (user == null || channel == null) return "";
        Set<UserLevel> levels = user.getUserLevels(channel);

        if (levels.contains(UserLevel.OWNER)) return "~";
        if (levels.contains(UserLevel.SUPEROP)) return "&";
        if (levels.contains(UserLevel.OP)) return "@";
        if (levels.contains(UserLevel.HALFOP)) return "%";
        if (levels.contains(UserLevel.VOICE)) return "+";
        return "";
    }
}
