package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class OperLogin {

    private static final String TAG = "OperLogin";
    private final Context context;
    private final PircBotX bot;
    private final Activity activity;

    public OperLogin(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startOperLoginProcess() {
        Log.d(TAG, "startOperLoginProcess triggered!");

        String currentNick = bot.getNick();
        if (currentNick == null || currentNick.isEmpty()) {
            showMessage("Unable to retrieve current nick.");
            return;
        }

        AlertDialog.Builder passwordDialog = new AlertDialog.Builder(context);
        passwordDialog.setTitle("Enter OPER Password");

        final EditText inputPassword = new EditText(context);
        passwordDialog.setView(inputPassword);

        passwordDialog.setPositiveButton("OK", (dialog, which) -> {
            String password = inputPassword.getText().toString().trim();
            Log.d(TAG, "Entered Password: " + password);

            if (!password.isEmpty()) {
                executeOperCommand(currentNick, password);
            } else {
                showMessage("Password cannot be empty");
            }
        });

        passwordDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        passwordDialog.show();
    }

    private void executeOperCommand(String nick, String password) {
        Log.d(TAG, "Preparing to send OPER command for nick: " + nick);

        if (isNetworkAvailable()) {
            new Thread(() -> {
                try {
                    if (bot.isConnected()) {
                        String command = "OPER " + nick + " " + password;
                        bot.sendRaw().rawLine(command);
                        showMessage("Sent OPER command for " + nick);

                        // Add a short delay before switching the active channel
                        Thread.sleep(500);

                        // Check if connected to a channel and set the active channel accordingly
                        ((ChatActivity) context).runOnUiThread(() -> {
                            String activeChannel = ((ChatActivity) context).getActiveChannel();
                            if (activeChannel == null || activeChannel.isEmpty()) {
                                ((ChatActivity) context).setActiveChannel("#ThePlaceToChat");
                            } else {
                                showMessage("Active channel is: " + activeChannel);
                            }
                        });

                    } else {
                        showMessage("Bot is not connected to the server.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send OPER command.", e);
                    showMessage("Failed to send OPER command.");
                }
            }).start();
        } else {
            showMessage("No network connection.");
        }
    }

    private void showMessage(String message) {
        activity.runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private boolean isNetworkAvailable() {
        // Replace this with the actual method to check network availability
        return true;
    }
}
