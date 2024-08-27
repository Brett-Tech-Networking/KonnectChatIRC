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
// Process the raw server response
        String response = event.getRawLine().trim();
        if (response.startsWith(":") && response.contains(" 005 ")) {
            return;
        }
        if (response.contains("MODE")) {
            chatActivity.processServerMessage("SERVER", response);
        }
        if (event.getCode() == 381) {
            // Successful OPER login
            chatActivity.processServerMessage("Server", "You are now an IRC Operator");
        } else if (event.getCode() == 491) {
            // Failed OPER login
            chatActivity.processServerMessage("Server", "Failed to become an IRC Operator: " + event.getRawLine());
        }

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
                code == 352 || code == 315 || code == 329 || code == 251 ||
                code == 254 || code == 324 || code == 333 || code == 332 ||
                code == 322) {
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
        String activeChannel = chatActivity.getActiveChannel();
        String messageChannel = event.getChannel().getName();

        if (messageChannel.equalsIgnoreCase(activeChannel)) {
            processServerMessage(event.getUser().getNick(), event.getMessage());
        }
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
        } else {
            chatActivity.addChatMessage("NickServ: " + message);
        }
    }
    @Override
    public void onMode(ModeEvent event) {
        // Get the mode string and split it into individual mode changes
        String mode = event.getMode();
        String user = event.getUser().getNick();
        String channel = event.getChannel().getName();

        // Initialize a variable to hold the action description
        String action = "";

        // Check each mode and assign the appropriate action description
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

        String targetUser = event.getUser().getNick(); // The user who was affected by the mode change
        String performingUser = event.getUserHostmask().getNick(); // The user who performed the action
        String message = targetUser + " was " + action + " in " + channel + " by " + performingUser;
        chatActivity.processServerMessage("SERVER", message);
    }


    @Override
    public void onJoin(JoinEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();

            if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
                chatActivity.setActiveChannel(channel);
                chatActivity.addChatMessage(userNick + " has joined the channel: " + channel);
            } else {
                chatActivity.addChatMessage(userNick + " has joined the channel " + channel + ".");
            }
        });
    }



    @Override
    public void onPart(PartEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();
            if (channel.equalsIgnoreCase(chatActivity.getActiveChannel())) {
                chatActivity.addChatMessage(userNick + " has left the channel " + channel + ".");
            }
        });
    }

    @Override
    public void onKick(KickEvent event) {
        // Handle kick events
        String kicker = event.getUser().getNick();
        String kickedUser = event.getRecipient().getNick();
        String channel = event.getChannel().getName();
        String reason = event.getReason();
        String message = kickedUser + " was kicked from " + channel + " by " + kicker + " (" + reason + ")";
        chatActivity.processServerMessage("SERVER", message);
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
