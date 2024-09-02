package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.PircBotX;

import java.util.ArrayList;
import java.util.List;

public class Unban {

    private final Context context;
    private final PircBotX bot;
    private final Activity activity;
    private final List<String> bannedUsers = new ArrayList<>();

    public Unban(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startUnbanProcess() {
        // Clear previous banned users list
        bannedUsers.clear();

        // Run the ban list request on a background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            String activeChannel = ((ChatActivity) activity).getActiveChannel();
            if (bot != null && bot.isConnected()) {
                try {
                    // Send the request to get the ban list
                    bot.sendRaw().rawLine("MODE " + activeChannel + " +b");

                    // Give some time for the server to respond
                    Thread.sleep(2000); // 2 seconds delay

                    // After the delay, update the UI with the ban list
                    new Handler(Looper.getMainLooper()).post(this::showBannedUsersDialog);

                } catch (Exception e) {
                    Log.e("Unban", "Error during ban list retrieval", e);
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Failed to retrieve ban list.", Toast.LENGTH_SHORT).show());
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showBannedUsersDialog() {
        if (bannedUsers.isEmpty()) {
            Log.d("Unban", "No banned users found.");
            Toast.makeText(context, "No banned users available to unban.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder unbanDialog = new AlertDialog.Builder(context);
        unbanDialog.setTitle("Select User to Unban");

        // Create a ListView for banned user selection
        ListView bannedUserListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, bannedUsers);
        bannedUserListView.setAdapter(adapter);
        unbanDialog.setView(bannedUserListView);

        AlertDialog dialog = unbanDialog.create();
        bannedUserListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = bannedUsers.get(position);
            dialog.dismiss();

            // Execute the unban command
            executeUnbanCommand(selectedUser);
        });

        unbanDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());
        dialog.show();
    }

    private void executeUnbanCommand(String user) {
        String activeChannel = ((ChatActivity) activity).getActiveChannel(); // Get the active channel from ChatActivity

        new Thread(() -> {
            if (bot != null && bot.isConnected()) {
                try {
                    // Send the unban command
                    bot.sendRaw().rawLine("MODE " + activeChannel + " -b " + user);
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Unbanned " + user + " from " + activeChannel, Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Failed to execute unban command.", Toast.LENGTH_SHORT).show());
                    Log.e("Unban", "Error executing unban command", e);
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Method to add a banned user to the list
    public void addBannedUser(String banEntry) {
        Log.d("Unban", "Adding banned user: " + banEntry);
        if (!bannedUsers.contains(banEntry)) {
            bannedUsers.add(banEntry);
        }
    }
}
