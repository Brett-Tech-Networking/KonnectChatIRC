package com.btech.konnectchatirc;

import org.pircbotx.hooks.events.ServerResponseEvent;

public interface OnServerResponse {
    void onServerResponse(ServerResponseEvent event);
}
