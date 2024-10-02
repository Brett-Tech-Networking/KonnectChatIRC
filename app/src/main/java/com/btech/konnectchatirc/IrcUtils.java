package com.btech.konnectchatirc;

import org.pircbotx.User;
import org.pircbotx.UserLevel;
import org.pircbotx.Channel;

import java.util.Set;

public class IrcUtils {
    public static UserPrefix getUserPrefix(User user, Channel channel) {
        if (user == null || channel == null) return new UserPrefix("", 0);
        Set<UserLevel> levels = user.getUserLevels(channel);

        if (levels.contains(UserLevel.OWNER)) return new UserPrefix("~", R.drawable.shield);
        if (levels.contains(UserLevel.SUPEROP)) return new UserPrefix("&", R.drawable.mod);
        if (levels.contains(UserLevel.OP)) return new UserPrefix("@", R.drawable.mod);
        if (levels.contains(UserLevel.HALFOP)) return new UserPrefix("%", R.drawable.mod);
        if (levels.contains(UserLevel.VOICE)) return new UserPrefix("+", R.drawable.voice);
        return new UserPrefix("", 0); // No prefix
    }

    // Custom class to hold prefix symbol and drawable resource ID
    public static class UserPrefix {
        private String symbol;
        private int drawableResId;

        public UserPrefix(String symbol, int drawableResId) {
            this.symbol = symbol;
            this.drawableResId = drawableResId;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getDrawableResId() {
            return drawableResId;
        }
    }
}
