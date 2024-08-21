package com.btech.konnectchatirc;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import android.os.Handler;
import android.os.Looper;

public class Listeners extends ListenerAdapter {

    private final ChatActivity chatActivity;

    public Listeners(ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
    }

    @Override
    public void onConnect(ConnectEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage("Connected to IRC server.");
            chatActivity.addChatMessage("A TPTC Client");
        });
    }

    @Override
    public void onMessage(MessageEvent event) {
        runOnUiThread(() -> {
            String message = event.getMessage();
            String senderNick = event.getUser().getNick();
            chatActivity.addChatMessage(senderNick + ": " + message);
        });
    }

    @Override
    public void onJoin(JoinEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();
            chatActivity.updateChannelName(channel);
            chatActivity.addChatMessage(userNick + " has joined the channel " + channel + ".");
        });
    }

    @Override
    public void onPart(PartEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();
            chatActivity.addChatMessage(userNick + " has left the channel " + channel + ".");
        });
    }

    @Override
    public void onKick(KickEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage(event.getRecipient().getNick() + " was kicked by " + event.getUser().getNick() + " for " + event.getReason());
        });
    }

    @Override
    public void onUnknown(UnknownEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage("SERVER: " + event.getLine());
        });
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage("Disconnected from IRC server.");
        });
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    @Override
    public void onNickChange(NickChangeEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage(event.getOldNick() + " is now known as " + event.getNewNick());
        });
    }

}
