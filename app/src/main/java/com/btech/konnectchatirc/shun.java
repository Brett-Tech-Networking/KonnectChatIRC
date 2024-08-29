package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.PircBotX;

import java.util.List;

public class shun {

    private Context context;
    private PircBotX bot;
    private Activity activity;
    private List<String> userList;

    public shun(Context context, PircBotX bot, Activity activity, List<String> userList) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
        this.userList = userList;
    }

    public void startShunProcess() {
        if (userList == null || userList.isEmpty()) {
            Toast.makeText(context, "No users available to shun.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder userDialog = new AlertDialog.Builder(context);
        userDialog.setTitle("Select User to Shun");

        // Create a ListView for selecting the user
        ListView userListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, userList);
        userListView.setAdapter(adapter);
        userDialog.setView(userListView);

        AlertDialog dialog = userDialog.create();
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = userList.get(position);
            dialog.dismiss();
            promptForTime(selectedUser);
        });

        userDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());

        dialog.show();
    }

    private void promptForTime(String user) {
        AlertDialog.Builder timeDialog = new AlertDialog.Builder(context);
        timeDialog.setTitle("Select Shun Duration");

        // Create a ListView for selecting time duration
        ListView timeListView = new ListView(context);
        String[] times = {"5m", "15m", "30m", "1h", "1d", "5d", "10d"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, times);
        timeListView.setAdapter(adapter);
        timeDialog.setView(timeListView);

        AlertDialog dialog = timeDialog.create();
        timeListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTime = times[position];
            dialog.dismiss();
            promptForReason(user, selectedTime);
        });

        timeDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());

        dialog.show();
    }

    private void promptForReason(String user, String time) {
        AlertDialog.Builder reasonDialog = new AlertDialog.Builder(context);
        reasonDialog.setTitle("Enter Reason");

        final EditText inputReason = new EditText(context);
        reasonDialog.setView(inputReason);

        reasonDialog.setPositiveButton("Shun", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            if (!reason.isEmpty()) {
                executeShun(user, time, reason);
            } else {
                Toast.makeText(context, "Reason cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        reasonDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        reasonDialog.show();
    }

    private void executeShun(String user, String time, String reason) {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("SHUN " + user + " " + time + " :" + reason);
                    activity.runOnUiThread(() -> Toast.makeText(context, "Shunned " + user, Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to execute shun command.", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }).start();
        } else {
            Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show();
        }
    }
}
