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
        processServerMessage(event.getUser().getNick(), event.getNotice());
    }

    @Override
    public void onServerResponse(ServerResponseEvent event) {
        String rawMessage = event.getRawLine().trim();

        // Exclude specific CAP ACK messages with exact matches
        if (rawMessage.matches(".*CAP.*ACK :Multi-prefix.*") || rawMessage.matches(".*CAP.*ACK :away-notify.*")) {
            return; // Skip processing these CAP ACK messages
        }

        // Additional generic check for any CAP ACK messages
        if (rawMessage.contains("CAP") && rawMessage.contains("ACK")) {
            return; // Skip processing any CAP ACK message
        }

        // Existing checks and processing logic
        int code = event.getCode();
        if (code == 001 || code == 002 || code == 003 || code == 004 || code == 005 ||
                code == 253 || code == 252 || code == 255 || code == 265 ||
                code == 422 || code == 266 || code == 353 || code == 366 ||
                code == 352 || code == 315 || code == 329 || code == 251 || code == 254) {
            return; // Skip processing these messages
        }

        // Process other server responses
        processServerMessage("SERVER", code + ": " + rawMessage);
    }

    @Override
    public void onConnect(ConnectEvent event) {
        String serverAddress = event.getBot().getServerHostname(); // Get the server address dynamically
        runOnUiThread(() -> {
            chatActivity.addChatMessage("Connected to: " + serverAddress + " a TPTC Client");
        });
    }

    @Override
    public void onMessage(MessageEvent event) {
        processServerMessage(event.getUser().getNick(), event.getMessage());
        runOnUiThread(() -> {
            String message = event.getMessage();
            String senderNick = event.getUser().getNick();

            // Log every message received
            Log.d("IRCMessage", "Message from " + senderNick + ": " + message);

            chatActivity.addChatMessage(senderNick + ": " + message);

            // Check if the message is from NickServ
            if (senderNick.equalsIgnoreCase("NickServ")) {
                Log.d("NickServMessage", "NickServ message: " + message);
                handleNickServResponse(message);
            }
        });
    }


    private void handleNickServResponse(String message) {
        // Log the message being processed
        Log.d("NickServResponse", "Processing NickServ message: " + message);

        // Display the NickServ message directly in the chat
        chatActivity.addChatMessage("NickServ: " + message);

        // Further handling specific cases if needed
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
        }else {
            chatActivity.addChatMessage("NickServ: " + message);
        }
    }

    @Override
    public void onJoin(JoinEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();
            chatActivity.setActiveChannel(channel); // Update active channel
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
        String rawLine = event.getLine().trim();

        // Apply the same filtering logic
        if (rawLine.matches(".*CAP.*ACK :Multi-prefix.*") || rawLine.matches(".*CAP.*ACK :away-notify.*")) {
            return; // Skip processing these CAP ACK messages
        }

        if (rawLine.contains("CAP") && rawLine.contains("ACK")) {
            return; // Skip processing any CAP ACK message
        }

       // processServerMessage("SERVER", event.getLine()); //shows huge block of connection info
        runOnUiThread(() -> {
        });
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage("Disconnected from IRC server.");
        });
    }

    @Override
    public void onNickChange(NickChangeEvent event) {
        runOnUiThread(() -> {
            chatActivity.addChatMessage(event.getOldNick() + " is now known as " + event.getNewNick());
        });
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
    private void processServerMessage(String sender, String message) {
        String cleanedMessage = message.replaceFirst("^(\\d{3} )?", "");
        runOnUiThread(() -> {
            // Log every message received for debugging purposes
            Log.d("IRCMessage", "Message from " + sender + ": " + message);

            // Add the message to the chat UI
            chatActivity.addChatMessage(sender + ": " + message);

            // Specific handling for NickServ messages
            if (sender.equalsIgnoreCase("NickServ")) {
                Log.d("NickServMessage", "NickServ message: " + message);
                handleNickServResponse(message);
            }
        });
    }
}
