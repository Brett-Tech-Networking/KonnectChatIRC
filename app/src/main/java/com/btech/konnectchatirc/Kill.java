package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.pircbotx.PircBotX;
import org.pircbotx.User;

public class Kill {

    private static final String TAG = "Kill";
    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;

    public Kill(Context context, PircBotX bot, ChatActivity chatActivity) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
    }

    public void startKillProcess() {
        // Create the first dialog for entering the nickname
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
        // Create the second dialog for entering the reason
        AlertDialog.Builder reasonDialog = new AlertDialog.Builder(context);
        reasonDialog.setTitle("Enter Reason");

        final EditText inputReason = new EditText(context);
        reasonDialog.setView(inputReason);

        reasonDialog.setPositiveButton("OK", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (!reason.isEmpty()) {
                executeKill(nick, reason);
            } else {
                Toast.makeText(context, "Reason cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        reasonDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        reasonDialog.show();
    }

    public void executeKill(String nick, String reason) {
        Log.d(TAG, "Preparing to kill " + nick);

        if (chatActivity.isNetworkAvailable()) {
            new Thread(() -> {
                try {
                    if (bot.isConnected()) {
                        User user = bot.getUserChannelDao().getUser(nick);
                        if (user != null) {
                            Log.d(TAG, "Executing kill command for user " + user.getNick() + " with reason: " + reason);
                            bot.sendRaw().rawLine("KILL " + user.getNick() + " :" + reason);
                            chatActivity.runOnUiThread(() ->
                                    Toast.makeText(context, "Killed " + nick, Toast.LENGTH_SHORT).show());
                        } else {
                            Log.e(TAG, "User not found.");
                            chatActivity.runOnUiThread(() ->
                                    Toast.makeText(context, "Failed to execute kill command: User not found.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Log.e(TAG, "Bot is not connected to the server.");
                        chatActivity.runOnUiThread(() ->
                                Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute kill command.", e);
                    chatActivity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to execute kill command.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Log.e(TAG, "No network connection.");
            chatActivity.runOnUiThread(() ->
                    Toast.makeText(context, "No network connection.", Toast.LENGTH_SHORT).show());
        }
    }
}
