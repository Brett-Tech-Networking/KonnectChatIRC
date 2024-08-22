package com.btech.konnectchatirc;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import org.pircbotx.PircBotX;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

public class OperatorPanelActivity extends Activity {

    private PircBotX bot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.operator_panel);

        // Assuming the bot instance is passed through an Intent
        bot = (PircBotX) getIntent().getSerializableExtra("bot_instance");

        // Directly find the btnKill within the operator_panel layout
        Button btnKill = findViewById(R.id.btnKill);

        // Set the click listener for btnKill
        btnKill.setOnClickListener(v -> startKillProcess());
    }

    private void startKillProcess() {
        AlertDialog.Builder nickDialog = new AlertDialog.Builder(this);
        nickDialog.setTitle("Enter Nickname");

        final EditText inputNick = new EditText(this);
        nickDialog.setView(inputNick);

        nickDialog.setPositiveButton("OK", (dialog, which) -> {
            String enteredNick = inputNick.getText().toString().trim();
            if (!enteredNick.isEmpty()) {
                promptForReason(enteredNick);
            } else {
                Toast.makeText(this, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        nickDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        nickDialog.show();
    }

    private void promptForReason(String nick) {
        AlertDialog.Builder reasonDialog = new AlertDialog.Builder(this);
        reasonDialog.setTitle("Enter Reason");

        final EditText inputReason = new EditText(this);
        reasonDialog.setView(inputReason);

        reasonDialog.setPositiveButton("OK", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (!reason.isEmpty()) {
                executeKill(nick, reason);
            } else {
                Toast.makeText(this, "Reason cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        reasonDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        reasonDialog.show();
    }

    private void executeKill(String nick, String reason) {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("KILL " + nick + " :" + reason);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Killed " + nick, Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to execute kill command.", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }).start();
        } else {
            Toast.makeText(this, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show();
        }
    }
}
