package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

public class Kick {

    private static final String TAG = "Kick";
    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;

    public Kick(Context context, PircBotX bot, ChatActivity chatActivity) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
    }

    public void startKickProcess() {
        AlertDialog.Builder nickDialog = new AlertDialog.Builder(context);
        nickDialog.setTitle("Enter Nickname");

        final EditText inputNick = new EditText(context);
        nickDialog.setView(inputNick);

        nickDialog.setPositiveButton("OK", (dialog, which) -> {
            String enteredNick = inputNick.getText().toString().trim();
            if (!enteredNick.isEmpty()) {
                promptForReason(enteredNick);
            } else {
                Toast.makeText(context, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        nickDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        nickDialog.show();
    }

    private void promptForReason(String nick) {
        AlertDialog.Builder reasonDialog = new AlertDialog.Builder(context);
        reasonDialog.setTitle("Enter Reason");

        final EditText inputReason = new EditText(context);
        reasonDialog.setView(inputReason);

        reasonDialog.setPositiveButton("OK", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (!reason.isEmpty()) {
                executeKick(nick, reason);
            } else {
                Toast.makeText(context, "Reason cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        reasonDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        reasonDialog.show();
    }

    public void executeKick(String nick, String reason) {
        String activeChannel = chatActivity.getActiveChannel();
        Log.d(TAG, "Active channel: " + activeChannel);
        if (activeChannel == null || activeChannel.isEmpty()) {
            Log.e(TAG, "No active channel set.");
            chatActivity.runOnUiThread(() ->
                    Toast.makeText(context, "Failed to execute kick command: No active channel.", Toast.LENGTH_SHORT).show());
            return;
        }

        if (chatActivity.isNetworkAvailable()) {
            new Thread(() -> {
                try {
                    if (bot.isConnected()) {
                        Channel channel = bot.getUserChannelDao().getChannel(activeChannel);
                        User user = bot.getUserChannelDao().getUser(nick);
                        if (channel != null && user != null) {
                            Log.d(TAG, "Executing kick command on channel " + channel.getName() + " for user " + user.getNick() + " with reason: " + reason);
                            bot.sendRaw().rawLine("KICK " + channel.getName() + " " + user.getNick() + " :" + reason);
                            chatActivity.runOnUiThread(() ->
                                    Toast.makeText(context, "Kicked " + nick + " from " + activeChannel, Toast.LENGTH_SHORT).show());
                        } else {
                            Log.e(TAG, "Channel or User not found.");
                            chatActivity.runOnUiThread(() ->
                                    Toast.makeText(context, "Failed to execute kick command: Channel or User not found.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Log.e(TAG, "Bot is not connected to the server.");
                        chatActivity.runOnUiThread(() ->
                                Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute kick command.", e);
                    chatActivity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to execute kick command.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Log.e(TAG, "No network connection.");
            chatActivity.runOnUiThread(() ->
                    Toast.makeText(context, "No network connection.", Toast.LENGTH_SHORT).show());
        }
    }
}
