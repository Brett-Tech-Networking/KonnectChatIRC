package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class OperLogin {

    private static final String TAG = "OperLogin";
    private final Context context;
    private final Activity activity;  // Use a more generic Activity type
    private final PircBotX bot;

    public OperLogin(Context context, Activity activity, PircBotX bot) {
        this.context = context;
        this.activity = activity;
        this.bot = bot;
    }

    public void startOperLoginProcess() {
        Log.d(TAG, "startOperLoginProcess triggered!");

        // Create a dialog with two input fields: Nick and Password
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Enter OPER Credentials");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputNick = new EditText(context);
        inputNick.setHint("Nick");
        layout.addView(inputNick);

        final EditText inputPassword = new EditText(context);
        inputPassword.setHint("Password");
        inputPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputPassword);

        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton("OK", (dialog, which) -> {
            String nick = inputNick.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            Log.d(TAG, "Entered Nick: " + nick);
            // Log.d(TAG, "Entered Password: " + password); // Avoid logging passwords in production

            if (!nick.isEmpty() && !password.isEmpty()) {
                executeOperCommand(nick, password);
            } else {
                showMessage("Nick and Password cannot be empty");
            }
        });

        dialogBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
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
                        refreshChat();
                        // Additional actions if needed
                        // For example, switch to a specific channel or update UI

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
        // Implement actual network check logic here
        return true;
    }

    private void refreshChat() {
        new Handler(Looper.getMainLooper()).post(() -> ((ChatActivity) activity).getChatAdapter().notifyDataSetChanged());
    }
}
