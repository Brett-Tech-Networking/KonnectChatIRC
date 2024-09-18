package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import org.pircbotx.PircBotX;

public class Identify {

    private static final String TAG = "Identify";
    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;
    private final View operatorPanel;


    public Identify(Context context, PircBotX bot, ChatActivity chatActivity, View operatorPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.operatorPanel = operatorPanel;

    }

    public void startIdentifyProcess() {
        String currentNick = chatActivity.getUserNick(); // Get the current nickname
        promptForPassword(currentNick);
    }

    private void promptForPassword(String nick) {
        // Create the dialog for entering the password
        AlertDialog.Builder passwordDialog = new AlertDialog.Builder(context);
        passwordDialog.setTitle("Enter Password");

        final EditText inputPassword = new EditText(context);
        passwordDialog.setView(inputPassword);

        passwordDialog.setPositiveButton("OK", (dialog, which) -> {
            String password = inputPassword.getText().toString().trim();
            if (!password.isEmpty()) {
                executeIdentify(nick, password);
            } else {
                chatActivity.addChatMessage("Password cannot be empty.");
            }
        });

        passwordDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        passwordDialog.show();
    }

    public void executeIdentify(String nick, String password) {
        Log.d(TAG, "Preparing to identify for nick " + nick);

        if (chatActivity.isNetworkAvailable()) {
            new Thread(() -> {
                try {
                    if (bot.isConnected()) {
                        Log.d(TAG, "Executing identify command for nick " + nick);
                        bot.sendRaw().rawLine("PRIVMSG NickServ :IDENTIFY " + nick + " " + password);
                        refreshChat();
                        // Don't display any messages here, let NickServ's response be handled by the Listeners class
                    } else {
                        chatActivity.runOnUiThread(() -> {
                            chatActivity.addChatMessage("Bot is not connected to the server.");
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to execute identify command.", e);
                    chatActivity.runOnUiThread(() -> {
                        chatActivity.addChatMessage("Failed to execute identify command.");
                    });
                }
            }).start();
        } else {
            chatActivity.addChatMessage("No network connection.");
        }
    }
    private void fadeOutOperatorPanel() {
        Animation fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        operatorPanel.startAnimation(fadeOut);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Optional: any action before animation starts
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                operatorPanel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
    private void refreshChat() {
        new Handler(Looper.getMainLooper()).post(() -> chatActivity.getChatAdapter().notifyDataSetChanged());
    }
}
