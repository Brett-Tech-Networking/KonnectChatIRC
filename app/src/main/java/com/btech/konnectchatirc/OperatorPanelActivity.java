package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class OperatorPanelActivity extends Activity {

    private PircBotX bot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.operator_panel);

        // Assuming the bot instance is passed through an Intent
        bot = (PircBotX) getIntent().getSerializableExtra("bot_instance");
        if (bot == null) {
            Toast.makeText(this, "Error: Bot instance not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize the OperLogin button listener
        Button btnOperLogin = findViewById(R.id.btnOperLogin);
        btnOperLogin.setOnClickListener(v -> startOperLoginProcess());

        // Initialize the Sajoin button listener
        Button btnSajoin = findViewById(R.id.btnSajoin);
        btnSajoin.setOnClickListener(v -> new Sajoin(OperatorPanelActivity.this, bot, OperatorPanelActivity.this).startSajoinProcess());

        // Initialize the Kill button listener
        Button btnKill = findViewById(R.id.btnKill);
        btnKill.setOnClickListener(v -> startKillProcess());
    }

    // Method to handle OperLogin process
    private void startOperLoginProcess() {
        OperLogin operLogin = new OperLogin(this, bot, this);  // Create an instance of OperLogin
        operLogin.startOperLoginProcess();  // Call the method to start the login process
    }

    // Kill functionality - Unchanged, just for reference
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
                    runOnUiThread(() -> {
                        Toast.makeText(OperatorPanelActivity.this, "Killed " + nick, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(OperatorPanelActivity.this, "Failed to execute kill command.", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }).start();
        } else {
            Toast.makeText(this, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show();
        }
    }
}
