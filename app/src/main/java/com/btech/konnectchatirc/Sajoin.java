package com.btech.konnectchatirc;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.app.AlertDialog;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class Sajoin {

    private static final String TAG = "Sajoin";
    private final Context context;
    private final PircBotX bot;
    private final Activity activity;

    public Sajoin(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startSajoinProcess() {
        promptForNick(false); // false indicates this is for SAJOIN
    }

    public void startSapartProcess() {
        promptForNick(true); // true indicates this is for SAPART
    }

    private void promptForNick(boolean isSapart) {
        AlertDialog.Builder nickDialog = new AlertDialog.Builder(context);
        nickDialog.setTitle("Enter Nickname");

        final EditText inputNick = new EditText(context);
        nickDialog.setView(inputNick);

        nickDialog.setPositiveButton("OK", (dialog, which) -> {
            String enteredNick = inputNick.getText().toString().trim();
            if (!enteredNick.isEmpty()) {
                promptForChannel(enteredNick, isSapart);
            } else {
                showMessage("Nickname cannot be empty");
            }
        });

        nickDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        nickDialog.show();
    }

    private void promptForChannel(String nick, boolean isSapart) {
        AlertDialog.Builder channelDialog = new AlertDialog.Builder(context);
        channelDialog.setTitle("Enter Channel");

        final EditText inputChannel = new EditText(context);
        channelDialog.setView(inputChannel);

        channelDialog.setPositiveButton("OK", (dialog, which) -> {
            String enteredChannel = inputChannel.getText().toString().trim();
            if (!enteredChannel.isEmpty()) {
                if (isSapart) {
                    executeSapartCommand(nick, enteredChannel);
                } else {
                    executeSajoinCommand(nick, enteredChannel);
                }
            } else {
                showMessage("Channel cannot be empty");
            }
        });

        channelDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        channelDialog.show();
    }

    private void executeSajoinCommand(String nick, String channel) {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("SAJOIN " + nick + " " + channel);
                    activity.runOnUiThread(() -> {
                        Toast.makeText(context, "Sent SAJOIN command for " + nick + " to " + channel, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send SAJOIN command.", e);
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to send SAJOIN command.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            showMessage("Bot is not connected to the server.");
        }
    }

    private void executeSapartCommand(String nick, String channel) {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("SAPART " + nick + " " + channel);
                    activity.runOnUiThread(() -> {
                        Toast.makeText(context, "Sent SAPART command for " + nick + " from " + channel, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send SAPART command.", e);
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to send SAPART command.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            showMessage("Bot is not connected to the server.");
        }
    }

    private void showMessage(String message) {
        activity.runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}
