package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class NickRegister {
    private static final String TAG = "NickRegister";
    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;
    private final View hoverPanel;

    public NickRegister(Context context, PircBotX bot, ChatActivity chatActivity, View hoverPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.hoverPanel = hoverPanel;
    }

    public void startRegistrationProcess() {
        promptForPassword();
    }

    private void promptForPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Step 1: Choose Password");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Next", (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (!password.isEmpty()) {
                promptForEmail(password);
            } else {
                Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void promptForEmail(String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Step 2: Enter Email (Optional)");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("email@example.com");
        builder.setView(input);

        builder.setPositiveButton("Register", (dialog, which) -> {
            String email = input.getText().toString().trim();
            executeRegister(password, email);
            if (hoverPanel != null) {
                hoverPanel.setVisibility(View.GONE);
            }
        });
        builder.setNegativeButton("Back", (dialog, which) -> promptForPassword());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void executeRegister(String password, String email) {
        Log.d(TAG, "Preparing to register nickname");

        if (chatActivity.isNetworkAvailable()) {
            new Thread(() -> {
                try {
                    if (bot != null && bot.isConnected()) {
                        String command = "PRIVMSG NickServ :REGISTER " + password + (email.isEmpty() ? "" : " " + email);
                        bot.sendRaw().rawLine(command);
                        chatActivity.runOnUiThread(() ->
                            chatActivity.addChatMessage("SYSTEM: Registration command sent to NickServ.")
                        );
                    } else {
                        chatActivity.runOnUiThread(() ->
                            chatActivity.addChatMessage("SYSTEM: Cannot register, bot is disconnected.")
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute register command.", e);
                }
            }).start();
        } else {
            chatActivity.addChatMessage("SYSTEM: No network connection.");
        }
    }
}
