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
        String notice = event.getNotice();

        chatActivity.processServerMessage(event.getUser().getNick(), notice, channel);
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
            chatActivity.processServerMessage("SERVER", rawMessage, chatActivity.getActiveChannel());
        }

        if (event.getCode() == 381) {
            chatActivity.processServerMessage("Server", "You are now an IRC Operator", chatActivity.getActiveChannel());
        } else if (event.getCode() == 491) {
            chatActivity.processServerMessage("Server", "Failed to become an IRC Operator: " + rawMessage, chatActivity.getActiveChannel());
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

        chatActivity.processServerMessage("SERVER", code + ": " + rawMessage, chatActivity.getActiveChannel());
    }

    @Override
    public void onConnect(ConnectEvent event) {
        String serverAddress = event.getBot().getServerHostname();
        runOnUiThread(() -> chatActivity.addChatMessage("Connected to: " + serverAddress + " a TPTC Client"));
    }

    @Override
    public void onMessage(MessageEvent event) {
        String channel = event.getChannel().getName();
        String message = event.getMessage();
        String sender = event.getUser().getNick();

        // Check if the message is a join message and skip processing to prevent duplication
        if (message.contains("has joined the channel")) {
            return; // Skip server-sent join messages
        }

        // Process other messages normally
        chatActivity.processServerMessage(sender, message, channel);

        // Process server messages related to identification
        if (sender.equalsIgnoreCase("NickServ")) {
            if (message.contains("Password accepted")) {
                // Identification successful
                chatActivity.addChatMessage("Successfully identified with NickServ.");
                Log.d("IRC", "Identification successful.");
            } else if (message.contains("Incorrect password")) {
                // Identification failed
                chatActivity.addChatMessage("Failed to identify with NickServ. Incorrect password.");
                Log.e("IRC", "Identification failed: Incorrect password.");
            } else {
                // Other NickServ messages
                chatActivity.addChatMessage("NickServ: " + message);
                Log.d("IRC", "NickServ message: " + message);
            }
        }
    }
    @Override
    public void onMode(ModeEvent event) {
        String mode = event.getMode();
        String channel = event.getChannel().getName();
        String action = "";
        String targetNick = null;

        // Modes that affect users typically have a parameter (e.g., +o nick, -v nick)
        if (event.getUser() != null) {
            targetNick = event.getUser().getNick();  // Get the target user's nickname
        }

        // Determine the type of mode change
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

        String performingUser = event.getUserHostmask().getNick();
        String message = (targetNick != null ? targetNick : "Someone") + " was " + action + " in " + channel + " by " + performingUser;

        // Display the server message
        runOnUiThread(() -> chatActivity.processServerMessage("SERVER", message, channel));

        // Refresh the user list to reflect the status change
        refreshChat();
    }


    @Override
    public void onJoin(JoinEvent event) {
        String userNick = event.getUser().getNick();
        String channel = event.getChannel().getName();
        if (!userNick.equalsIgnoreCase(event.getBot().getNick())) {
            chatActivity.runOnUiThread(() -> {
                String joinMessage = userNick + " has joined the channel.";
                chatActivity.processServerMessage("SERVER", joinMessage, channel);
                chatActivity.markMessageAsProcessed(joinMessage); // Mark as processed to prevent duplicates

            });
        }
        // Only handle join messages for the bot itself
        if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
            runOnUiThread(() -> {
                chatActivity.setActiveChannel(channel);
                chatActivity.addChatMessage("You have joined the channel: " + channel);
                chatActivity.checkAndAddActiveChannel();
            });

            // Send identification command to NickServ
            new Thread(() -> {
                try {
                    String identifyCommand = "PRIVMSG NickServ :IDENTIFY " + chatActivity.getUserNick() + " " + chatActivity.getDesiredPassword();
                    event.getBot().sendRaw().rawLine(identifyCommand);
                } catch (Exception e) {
                    Log.e("Listeners", "Error sending IDENTIFY command: " + e.getMessage());
                }
            }).start();
        }

        // Do not handle join messages for other users to prevent duplication

    // Handle bot join
        if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
            runOnUiThread(() -> {
                chatActivity.setActiveChannel(channel);
                chatActivity.addChatMessage("You have joined the channel: " + channel);
                chatActivity.checkAndAddActiveChannel();
            });
        }

        // Send IDENTIFY command to NickServ if the bot joins
        new Thread(() -> {
            try {
                if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
                    String identifyCommand = "PRIVMSG NickServ :IDENTIFY " + chatActivity.getUserNick() + " " + chatActivity.getDesiredPassword();
                    event.getBot().sendRaw().rawLine(identifyCommand);
                }
            } catch (Exception e) {
                Log.e("Listeners", "Error sending IDENTIFY command: " + e.getMessage());
            }
        }).start();

        refreshChat();
    }

    @Override
    public void onPart(PartEvent event) {
        runOnUiThread(() -> {
            String userNick = event.getUser().getNick();
            String channel = event.getChannel().getName();

            if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
                // Show the message when the bot itself leaves the channel
                chatActivity.addChatMessage("You have left the channel: " + channel);
                chatActivity.partChannel(channel);
            } else if (channel.equalsIgnoreCase(chatActivity.getActiveChannel())) {
                // Process server message only for other users
                String partMessage = userNick + " has left the channel.";
                chatActivity.processServerMessage("SERVER", partMessage, channel);
            }

            refreshChat();
        });
    }


    @Override
    public void onKick(KickEvent event) {
        String kicker = event.getUser().getNick();
        String kickedUser = event.getRecipient().getNick();
        String channel = event.getChannel().getName();
        String reason = event.getReason();
        String message = kickedUser + " was kicked from " + channel + " by " + kicker + " (" + reason + ")";

        // Refresh the user list if the kicked user was the active user
        chatActivity.runOnUiThread(() -> chatActivity.getChatAdapter().notifyDataSetChanged());

        chatActivity.processServerMessage("SERVER", message, channel);
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
        runOnUiThread(() -> {
            String newNick = event.getNewNick();
            String oldNick = event.getOldNick();
            String message = oldNick + " is now known as " + newNick;

            // Only process server messages for nick changes, avoiding the extra display
            chatActivity.processServerMessage("SERVER", message, null);
            chatActivity.getChatAdapter().notifyDataSetChanged();  // Update the UI to reflect the nick change
            refreshChat();
        });
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

    private void refreshChat() {
        new Handler(Looper.getMainLooper()).post(() -> chatActivity.getChatAdapter().notifyDataSetChanged());
    }
}
