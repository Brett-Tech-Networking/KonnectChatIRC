package com.btech.konnectchatirc;

import org.pircbotx.PircBotX;

public interface ChatActivityInterface {
    PircBotX getBot();
    String getActiveChannel();
}
