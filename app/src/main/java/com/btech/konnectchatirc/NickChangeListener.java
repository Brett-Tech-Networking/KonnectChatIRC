package com.btech.konnectchatirc;

import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.Listener;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.NickChangeEvent;

public class NickChangeListener extends ListenerAdapter implements NickChangeListenerr {
    private final ChatActivity chatActivity;

    public NickChangeListener(ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
    }

    @Override
    public void onServerResponse(ServerResponseEvent event) {
        int replyCode = event.getCode();
        String replyMessage = event.getRawLine();

        if (replyCode == 433) { // Error 433: Nickname is already in use
            chatActivity.runOnUiThread(() -> {
                chatActivity.addChatMessage("Nickname is already in use. Please choose a different one.");
                chatActivity.setNickInputToRetry(); // Call method to set input text
            });
        } else if (replyCode == 001) { // Reply code 001: Welcome message (indicates successful connection)
            // Update the local nickname if the server has confirmed the change
            String newNick = event.getBot().getNick(); // Get the current nickname from the bot
            if (newNick != null && !newNick.equals(chatActivity.getUserNick())) {
                chatActivity.updateLocalNick(newNick); // Update the local nickname
            }
        }
    }
}
