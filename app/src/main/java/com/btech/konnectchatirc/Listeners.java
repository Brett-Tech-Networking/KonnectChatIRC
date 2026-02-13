package com.btech.konnectchatirc;

import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserLevel;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.events.UnknownEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.WhoisEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Listeners extends ListenerAdapter {

    private final ChatActivity chatActivity;

    public Listeners(ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
    }

    @Override
    public void onNotice(NoticeEvent event) {
        String channel = event.getChannel() != null ? event.getChannel().getName() : "Server Notices";
        String notice = event.getNotice();

        // Redirect general notices to Server Notices channel
        if (channel.equals("Server Notices") || event.getChannel() == null) {
             chatActivity.processServerMessage("SERVER", notice, "Server Notices");
        } else {
             chatActivity.processServerMessage(event.getUser() != null ? event.getUser().getNick() : "SERVER", notice, channel);
        }
        
        // Handle Discord Relay Errors
        if (notice.toLowerCase().contains("privacy settings") || 
            notice.toLowerCase().contains("permit private messaging") ||
            (notice.toLowerCase().contains("cannot send") && notice.toLowerCase().contains("discord"))) {
            chatActivity.showDiscordPrivacyError();
        }
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
        
        // Route Mode changes to the relevant channel if possible, or Server Notices
        if (rawMessage.contains("MODE")) {
             String[] parts = rawMessage.split(" ");
             if (parts.length > 2 && parts[2].startsWith("#")) {
                 chatActivity.processServerMessage("SERVER", rawMessage, parts[2]);
             } else {
                 chatActivity.processServerMessage("SERVER", rawMessage, "Server Notices");
             }
             return; // Stop further processing for MODE
        }
        
        // Handle Operator status messages
        if (event.getCode() == 381) {
            chatActivity.processServerMessage("Server", "You are now an IRC Operator", "Server Notices");
            return;
        } else if (event.getCode() == 491) {
            chatActivity.processServerMessage("Server", "Failed to become an IRC Operator: " + rawMessage, "Server Notices");
            return;
        }

        try {
            if (rawMessage.matches(".*CAP.*ACK :Multi-prefix.*") || rawMessage.matches(".*CAP.*ACK :away-notify.*")) {
                return;
            }
            if (rawMessage.contains("CAP") && rawMessage.contains("ACK")) {
                return;
            }
        } catch (Exception e) {
             Log.e("Listeners", "Error handler CAP message in onServerResponse: " + e.getMessage());
        }
        if (event.getCode() == 353) { // RPL_NAMREPLY
            String rawLine = event.getRawLine();
            // Example rawLine: ":server 353 yournick = #channel :@nick1 +nick2 nick3"
            String[] parts = rawLine.split(" :");
            if (parts.length >= 2) {
                String[] users = parts[1].split(" ");
                for (String userEntry : users) {
                    char prefix = userEntry.charAt(0);
                    String nick = userEntry;
                    if ("~&@%+".indexOf(prefix) != -1) {
                        nick = userEntry.substring(1);
                    } else {
                        prefix = ' '; // No prefix
                    }
                    System.out.println("User: " + nick + ", Prefix: " + prefix);
                    // Store this information in a map for later use
                }
            }
            System.out.println("Server Response: " + rawLine);

        }
        int code = event.getCode();
        if (code == 001 || code == 002 || code == 003 || code == 004 || code == 005 ||
                code == 253 || code == 252 || code == 255 || code == 265 ||
                code == 422 || code == 266 || code == 353 || code == 366 ||
                code == 352 || code == 315 || code == 329 || code == 251 ||
                code == 254 || code == 324 || code == 333 || code == 332 ||
                code == 322 || code == 372 || code == 375 || code == 376 ||
                code == 311 || code == 312 || code == 313 || code == 317 ||
                code == 318 || code == 319 || code == 301 || code == 330 ||
                code == 307 || code == 320 ||
                code == 378 || code == 379 || code == 671) {
            return;
        }

        chatActivity.processServerMessage("SERVER", code + ": " + rawMessage, "Server Notices");
    }

    @Override
    public void onConnect(ConnectEvent event) {
        String serverAddress = event.getBot().getServerHostname();
        runOnUiThread(() -> chatActivity.processServerMessage("SERVER", "Connected to: " + serverAddress + " a TPTC Client", "Server Notices"));
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
        if (message.contains("sets mode +v")) {
            String[] parts = message.split(" ");
            // Extracting the target nick who received the mode
            if (parts.length >= 5) { // Ensure we have enough parts
                String targetNick = parts[4]; // The user being voiced
                String modeChangeMessage = "[" + sender + "] - " + targetNick + " (Nick: " + event.getUser().getNick() + ") has been voiced.";
                chatActivity.processServerMessage("SERVER", modeChangeMessage, channel);
                return; // Skip processing as it's already handled
            }
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
        String channel = event.getChannel().getName();
        String performingUser = event.getUser().getNick(); // User who set the mode
        String modeLine = event.getMode(); // This might include the mode and parameters

        if (modeLine == null || modeLine.isEmpty()) {
            return;
        }

        // Split the mode line into mode changes and parameters
        String[] modeParts = modeLine.split(" ");
        if (modeParts.length < 2) {
            // If there are no parameters, we can't proceed
            return;
        }

        String modeString = modeParts[0]; // "+v", "-o", etc.
        List<String> params = new ArrayList<>();
        for (int i = 1; i < modeParts.length; i++) {
            params.add(modeParts[i]); // Collect target nicknames
        }

        char modeSign = modeString.charAt(0); // '+' or '-'
        String modeFlags = modeString.substring(1); // "v", "o", etc.

        int targetIndex = 0;
        for (char modeChar : modeFlags.toCharArray()) {
            if (targetIndex < params.size()) {
                String targetNick = params.get(targetIndex);
                String modeAction = "";

                switch (modeChar) {
                    case 'v':
                        modeAction = "voiced";
                        break;
                    case 'o':
                        modeAction = "opped";
                        break;
                    case 'h':
                        modeAction = "half-opped";
                        break;
                    // Add other modes if needed
                    default:
                        modeAction = "had mode " + modeChar + " set";
                        break;
                }

                if (!modeAction.isEmpty()) {
                    String action = (modeSign == '+') ? "has been " + modeAction + " by " + performingUser
                            : "has been de-" + modeAction + " by " + performingUser;
                    String message = targetNick + " " + action + " in " + channel;
                    runOnUiThread(() -> chatActivity.processServerMessage("SERVER", message, channel));
                }

                targetIndex++;
            } else {
                // No more targets, break out of the loop
                break;
            }
        }

        // Refresh the user list to reflect the status change
        refreshChat();
    }



    private List<String> appChannelsList = new ArrayList<>();

    @Override
    public void onJoin(JoinEvent event) {
        String userNick = event.getUser().getNick();
        String channel = event.getChannel().getName();

        // Check if the bot itself joined a channel
        if (userNick.equalsIgnoreCase(event.getBot().getNick())) {
            // Ensure each joined channel is processed and updated in the app's channel list
            if (!appChannelsList.contains(channel)) {
                appChannelsList.add(channel);  // Add to the list of joined channels
            }

            // Log the joined channel
            System.out.println("Bot joined channel: " + channel);

            runOnUiThread(() -> {
                chatActivity.setActiveChannel(channel);
                chatActivity.addChatMessage("You have joined the channel: " + channel);
                chatActivity.checkAndAddActiveChannel();
            });

            // Send identification command to NickServ if needed
            new Thread(() -> {
                try {
                     // Using sendIRC().message() or sendRaw().rawLine() are both valid, using rawLine for consistency with existing code
                     String identifyCommand = "PRIVMSG NickServ :IDENTIFY " + chatActivity.getUserNick() + " " + chatActivity.getDesiredPassword();
                     event.getBot().sendRaw().rawLine(identifyCommand);
                } catch (Exception e) {
                    Log.e("Listeners", "Error sending IDENTIFY command: " + e.getMessage());
                }
            }).start();

            // Send WHO command for the channel after joining
            new Thread(() -> {
                try {
                    event.getBot().sendRaw().rawLine("WHO " + channel);
                } catch (Exception e) {
                    Log.e("Listeners", "Error sending WHO command: " + e.getMessage());
                }
            }).start();
            
            // Send MODE command for own nick to get user modes
            new Thread(() -> {
                 try {
                    event.getBot().sendRaw().rawLine("MODE " + event.getBot().getNick());
                } catch (Exception e) {
                    Log.e("Listeners", "Error sending MODE command: " + e.getMessage());
                }
            }).start();

        } else {
            // Handle when another user joins the channel
            chatActivity.runOnUiThread(() -> {
                String joinMessage = userNick + " has joined the channel.";
                chatActivity.processServerMessage("SERVER", joinMessage, channel);
                chatActivity.markMessageAsProcessed(joinMessage);  // Mark as processed to prevent duplicates
            });
        }
        
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

        try {
            // Debug CAP Negotiation
            if (rawLine.contains("CAP") && rawLine.contains("ACK")) {
                 Log.d("IRC_CAP", "CAP negotiation: " + rawLine);
                 return; // Use return here to skip further processing for CAP ACK
            }

            if (rawLine.matches(".*CAP.*ACK :Multi-prefix.*") || rawLine.matches(".*CAP.*ACK :away-notify.*")) {
                return;
            }
        } catch (UnsupportedOperationException e) {
            Log.e("Listeners", "UnsupportedOperationException during CAP handling in onUnknown: " + e.getMessage());
        } catch (Exception e) {
            Log.e("Listeners", "Error during CAP handling in onUnknown: " + e.getMessage());
        }

        // Debug TAGMSG
        if (rawLine.contains("TAGMSG")) {
             Log.d("IRC_TAGMSG", "Received TAGMSG: " + rawLine);
        }

        // Parse for TAGMSG with +typing tag
        // Format: @+typing=active :nick!user@host TAGMSG #channel
        if (rawLine.contains("TAGMSG") && rawLine.contains("+typing=")) {
            try {
                String[] parts = rawLine.split(" ");
                // Find index of TAGMSG command to properly locate other parts
                int tagMsgIndex = -1;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("TAGMSG")) {
                        tagMsgIndex = i;
                        break;
                    }
                }

                if (tagMsgIndex > 0) {
                    String tags = parts[0];
                    String source = "";
                    
                    // Adjust source if tags are present (which they should be)
                    // If tags present, source is at index 1 usually: @tags :source TAGMSG
                    if (tags.startsWith("@")) {
                       // Find the source part which starts with :
                       for(int i=1; i < tagMsgIndex; i++) {
                           if(parts[i].startsWith(":")) {
                               source = parts[i];
                               break;
                           }
                       }
                    } else {
                        source = parts[tagMsgIndex - 1];
                    }
                    

                    boolean isTyping = tags.contains("+typing=active");
                    
                    // Extract Nick from source (:nick!user@host)
                    String nick = "";
                    if (source.startsWith(":")) {
                        int exclamationIndex = source.indexOf("!");
                        if (exclamationIndex != -1) {
                            nick = source.substring(1, exclamationIndex);
                        } else {
                             nick = source.substring(1); // Just :nick
                        }
                    }

                    if (!nick.isEmpty() && !nick.equals(chatActivity.getBot().getNick())) {
                       chatActivity.onUserTyping(nick, isTyping);
                    }
                }
            } catch (Exception e) {
                Log.e("Listeners", "Error parsing TAGMSG: " + rawLine, e);
            }
        }
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        runOnUiThread(() -> chatActivity.processServerMessage("SERVER", "Disconnected from IRC server.", "Server Notices"));
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
            });
        } else {
            // Message is from an inactive channel
            runOnUiThread(() -> {
                chatActivity.storeMessageForChannel(channel, sender + ": " + message);
                chatActivity
                        .updateGlobalUnreadCount(); // Increment unread count and show badge
            });
                if (sender.equalsIgnoreCase("NickServ")) {
                    Log.d("NickServMessage", "NickServ message: " + message);
                    handleNickServResponse(message);
                }
            }
    }

    private void handleNickServResponse(String message) {
        Log.d("NickServResponse", "Processing NickServ message: " + message);

        String targetChannel = "Server Notices";

        chatActivity.processServerMessage("NickServ", message, targetChannel);

        if (message.contains("Password accepted")) {
            chatActivity.processServerMessage("NickServ", "Identification successful.", targetChannel);
        } else if (message.contains("Password incorrect")) {
            chatActivity.processServerMessage("NickServ", "Identification failed: Incorrect password.", targetChannel);
        } else if (message.contains("isn't registered")) {
            chatActivity.processServerMessage("NickServ", "Identification failed: Nickname isn't registered.", targetChannel);
        } else if (message.contains("You are now logged in as")) {
             chatActivity.processServerMessage("NickServ", message, targetChannel);
        } else if (message.contains("sets mode: +r")) {
            chatActivity.processServerMessage("NickServ", "Mode +r set, you are now recognized.", targetChannel);
        }
    }


    @Override
    public void onWhois(WhoisEvent event) {
        String nick = event.getNick();
        String realName = event.getRealname();
        String ident = event.getLogin();
        String host = event.getHostname();
        String server = event.getServer();
        String channels = event.getChannels().toString();
        String idleTime = String.valueOf(event.getIdleSeconds());
        String signonTime = String.valueOf(event.getSignOnTime());
        String awayMessage = event.getAwayMessage();

        // Flag if it's a Discord user
        if (chatActivity.isDiscordUser(realName)) {
            realName = "🛡️ Discord User | " + realName;
        }

        chatActivity.showWhoisDialog(nick, realName, ident, host, server, channels, idleTime, signonTime, awayMessage);
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) {
        String sender = event.getUser().getNick();
        String message = event.getMessage();
        
        // Don't broadcast messages from ourselves (server echo)
        if (sender.equalsIgnoreCase(chatActivity.getUserNick())) {
            return;
        }
        
        // Broadcast the private message to ChatActivity
        Intent intent = new Intent("private_message");
        intent.putExtra("sender", sender);
        intent.putExtra("message", message);
        Log.d("Listeners", "Sending private_message broadcast: " + sender + ": " + message);
        chatActivity.sendBroadcast(intent);
        
        Log.d("Listeners", "Private message from " + sender + ": " + message);
    }

    private void refreshChat() {
        new Handler(Looper.getMainLooper()).post(() -> chatActivity.getChatAdapter().notifyDataSetChanged());
    }

}
