package com.btech.konnectchatirc;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class Listeners extends ListenerAdapter {

    private final ChatActivity chatActivity;

    public Listeners(ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
    }

    @Override
    public void onNotice(NoticeEvent event) {
        String channel = event.getChannel() != null ? event.getChannel().getName() : "";
        processServerMessage(event.getUser().getNick(), event.getNotice(), channel);
    }

    @Override
    public void onServerResponse(ServerResponseEvent event) {
        String rawMessage = event.getRawLine().trim();

        // Capture ban list response
        if (rawMessage.contains(" +b ")) {
            String[] parts = rawMessage.split(" ");
            if (parts.length >= 4) {
                String banEntry = parts[3];
                chatActivity.addBannedUser(banEntry);  // Add the hostmask ban entry to the list
            }
        }

        if (rawMessage.startsWith(":") && rawMessage.contains(" 005 ")) {
            return;
        }
        if (rawMessage.contains("MODE")) {
            processServerMessage("SERVER", rawMessage, chatActivity.getActiveChannel());
        }

        if (event.getCode() == 381) {
            processServerMessage("Server", "You are now an IRC Operator", chatActivity.getActiveChannel());
        } else if (event.getCode() == 491) {
            processServerMessage("Server", "Failed to become an IRC Operator: " + rawMessage, chatActivity.getActiveChannel());
        }

        if (rawMessage.matches(".*CAP.*ACK :Multi-prefix.*") || rawMessage.matches(".*CAP.*ACK :away-notify.*")) {
            return;
        }
        if (rawMessage.contains("CAP") && rawMessage.contains("ACK")) {
            return;
        }

        int code = event.getCode();
        if (code == 001 || code == 002 || code == 003 || code == 004 || code == 005 ||
                code == 253 || code == 252 || code == 255 || code == 265 ||
                code == 422 || code == 266 || code == 353 || code == 366 ||
                code == 352 || code == 315 || code == 329 || code == 251 ||
                code == 254 || code == 324 || code == 333 || code == 332 ||
                code == 322) {
            return;
        }

        processServerMessage("SERVER", code + ": " + rawMessage, chatActivity.getActiveChannel());
    }

    @Override
    public void onConnect(ConnectEvent event) {
        String serverAddress = event.getBot().getServerHostname();
        runOnUiThread(() -> chatActivity.addChatMessage("Connected to: " + serverAddress + " a TPTC Client"));
    }

    @Override
    public void onMessage(MessageEvent event) {
        String channel = event.getChannel().getName();
        chatActivity.processServerMessage(event.getUser().getNick(), event.getMessage(), channel);
    }

    @Override
    public void onMode(ModeEvent event) {
        String mode = event.getMode();
        String user = event.getUser().getNick();
        String channel = event.getChannel().getName();

        String action = "";

        if (mode.contains("+o")) {
            action = "opped";
        } else if (mode.contains("-o")) {
            action = "deopped";
        } else if (mode.contains("+v")) {
            action = "voiced";
        } else if (mode.contains("-v")) {
            action = "devoiced";
        } else if (mode.contains("+q")) {
            action = "given owner status";
        } else if (mode.contains("-q")) {
            action = "removed from owner status";
        } else if (mode.contains("+h")) {
            action = "given half-operator status";
        } else if (mode.contains("-h")) {
            action = "removed from half-operator status";
        }

        String targetUser = event.getUser().getNick();
        String performingUser = event.getUserHostmask().getNick();
        String message = targetUser + " was " + action + " in " + channel + " by " + performingUser;

        processServerMessage("SERVER", message, channel);
    }

    @Override
    public void onJoin(JoinEvent event) {
        String userNick = event.getUser().getNick();
        String channel = event.getChannel().getName();
        String hostmask = event.getUserHostmask().getHostmask(); // Correct way to get hostmask

        runOnUiThread(() -> {
            if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
                chatActivity.setActiveChannel(channel);
                chatActivity.addChatMessage("You have joined the channel: " + channel);
                chatActivity.checkAndAddActiveChannel();
            } else if (channel.equalsIgnoreCase(chatActivity.getActiveChannel())) {
                chatActivity.addChatMessage(userNick + " has joined the channel.");
            }
        });
    }

    @Override
    public void onPart(PartEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();
            if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
                chatActivity.addChatMessage("You have left the channel: " + channel);
                chatActivity.partChannel(channel);
            } else if (channel.equalsIgnoreCase(chatActivity.getActiveChannel())) {
                chatActivity.addChatMessage(userNick + " has left the channel.");
            }
        });
    }

    @Override
    public void onKick(KickEvent event) {
        String kicker = event.getUser().getNick();
        String kickedUser = event.getRecipient().getNick();
        String channel = event.getChannel().getName();
        String reason = event.getReason();
        String message = kickedUser + " was kicked from " + channel + " by " + kicker + " (" + reason + ")";

        processServerMessage("SERVER", message, channel);
    }

    @Override
    public void onAction(ActionEvent event) {
        String userNick = event.getUser().getNick();
        String actionMessage = event.getAction();
        String channel = event.getChannel().getName();

        String formattedMessage = "* " + userNick + " " + actionMessage;
        chatActivity.processServerMessage(userNick, formattedMessage, channel);
    }

    @Override
    public void onUnknown(UnknownEvent event) {
        String rawLine = event.getLine().trim();

        if (rawLine.matches(".*CAP.*ACK :Multi-prefix.*") || rawLine.matches(".*CAP.*ACK :away-notify.*")) {
            return;
        }

        if (rawLine.contains("CAP") && rawLine.contains("ACK")) {
            return;
        }
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        runOnUiThread(() -> chatActivity.addChatMessage("Disconnected from IRC server."));
    }

    @Override
    public void onNickChange(NickChangeEvent event) {
        runOnUiThread(() -> chatActivity.addChatMessage(event.getOldNick() + " is now known as " + event.getNewNick()));
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    private void processServerMessage(String sender, String message, String channel) {
        String activeChannel = chatActivity.getActiveChannel();
        if (channel == null || channel.equalsIgnoreCase(activeChannel)) {
            runOnUiThread(() -> {
                Log.d("IRCMessage", "Message from " + sender + ": " + message);
                chatActivity.addChatMessage(sender + ": " + message);

                if (sender.equalsIgnoreCase("NickServ")) {
                    Log.d("NickServMessage", "NickServ message: " + message);
                    handleNickServResponse(message);
                }
            });
        }
    }

    private void handleNickServResponse(String message) {
        Log.d("NickServResponse", "Processing NickServ message: " + message);

        chatActivity.addChatMessage("NickServ: " + message);

        if (message.contains("Password accepted")) {
            chatActivity.addChatMessage("Identification successful.");
        } else if (message.contains("Password incorrect")) {
            chatActivity.addChatMessage("Identification failed: Incorrect password.");
        } else if (message.contains("isn't registered")) {
            chatActivity.addChatMessage("Identification failed: Nickname isn't registered.");
    } else if (message.contains("You are now logged in as")) {
            chatActivity.addChatMessage(message);
        } else if (message.contains("sets mode: +r")) {
            chatActivity.addChatMessage("NickServ: Mode +r set, you are now recognized.");
        } else {
            chatActivity.addChatMessage("NickServ: " + message);
        }
    }
}
