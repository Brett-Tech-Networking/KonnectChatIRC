package com.btech.konnectchatirc;

import org.pircbotx.PircBotX;

public interface BotProvider {
    PircBotX getBot();
    String getActiveChannel();
}
