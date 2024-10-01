package com.btech.konnectchatirc;

import org.pircbotx.hooks.events.ServerResponseEvent;

public interface NickChangeListenerr {
    void onServerResponse(ServerResponseEvent event);
}
