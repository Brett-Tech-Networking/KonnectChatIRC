package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class SvsNick {

    private Context context;
    private PircBotX bot;
    private Activity activity;

    public SvsNick(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startSvsNickProcess() {
        // Step 1: Ask for the current nickname
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter Current Nickname");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Next", (dialog, which) -> {
            String currentNick = input.getText().toString().trim();
            if (!currentNick.isEmpty()) {
                askForNewNick(currentNick);
            } else {
                Toast.makeText(context, "Current nickname cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void askForNewNick(String currentNick) {
        // Step 2: Ask for the new nickname
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter New Nickname");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Change Nick", (dialog, which) -> {
            String newNick = input.getText().toString().trim();
            if (!newNick.isEmpty()) {
                executeSvsNickCommand(currentNick, newNick);
            } else {
                Toast.makeText(context, "New nickname cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void executeSvsNickCommand(String currentNick, String newNick) {
        // Step 3: Execute the /os svsnick command
        new Thread(() -> {
            try {
                if (bot != null && bot.isConnected()) {
                    bot.sendRaw().rawLine("PRIVMSG OperServ :SVSNICK " + currentNick + " " + newNick);
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "SVSNICK command executed: " , Toast.LENGTH_SHORT).show());
                } else {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Failed to execute SVSNICK command.", Toast.LENGTH_SHORT).show());
                Log.e("SvsNick", "Error executing SVSNICK command", e);
            }
        }).start();
    }
}
