package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;  // Import EditText
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.List;

public class ListUsers {

    private Context context;
    private PircBotX bot;
    private Activity activity;
    private List<String> userList = new ArrayList<>();

    public ListUsers(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void showUserList() {
        // Clear previous user list
        userList.clear();

        // Fetch the active channel
        String activeChannel = ((ChatActivity) activity).getActiveChannel();
        Channel channel = bot.getUserChannelDao().getChannel(activeChannel);

        if (channel != null) {
            // Populate the user list
            for (User user : channel.getUsers()) {
                userList.add(user.getNick());
            }
        } else {
            Toast.makeText(context, "Active channel not found or bot not connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userList.isEmpty()) {
            Toast.makeText(context, "No users found in the channel.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show the user list in a dialog
        AlertDialog.Builder userDialog = new AlertDialog.Builder(context);
        userDialog.setTitle("Select a User");

        ListView userListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, userList);
        userListView.setAdapter(adapter);
        userDialog.setView(userListView);

        AlertDialog dialog = userDialog.create();
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = userList.get(position);
            dialog.dismiss();

            // Show options for the selected user
            showUserOptions(selectedUser);
        });

        userDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());
        dialog.show();
    }

    private void showUserOptions(String selectedUser) {
        String activeChannel = ((ChatActivity) activity).getActiveChannel();

        // Options: Kick, Ban, Slap
        AlertDialog.Builder optionsDialog = new AlertDialog.Builder(context);
        optionsDialog.setTitle("Options for " + selectedUser);

        String[] options = {"Kick", "Ban", "Slap"};
        optionsDialog.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Kick
                    showKickDialog(selectedUser, activeChannel);
                    break;
                case 1: // Ban
                    executeBanCommand(selectedUser, activeChannel);
                    break;
                case 2: // Slap
                    executeSlapCommand(selectedUser, activeChannel);
                    break;
            }
        });

        optionsDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());
        optionsDialog.show();
    }

    private void showKickDialog(String selectedUser, String activeChannel) {
        AlertDialog.Builder kickDialog = new AlertDialog.Builder(context);
        kickDialog.setTitle("Kick " + selectedUser);

        // Input for kick reason
        final EditText input = new EditText(context);  // Ensure EditText is imported
        input.setHint("Enter reason (optional)");
        kickDialog.setView(input);

        kickDialog.setPositiveButton("Kick", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            executeKickCommand(selectedUser, activeChannel, reason);
        });

        kickDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        kickDialog.show();
    }

    private void executeKickCommand(String selectedUser, String activeChannel, String reason) {
        new Thread(() -> {
            if (bot != null && bot.isConnected()) {
                try {
                    // Use rawLine to send the kick command manually
                    if (!reason.isEmpty()) {
                        bot.sendRaw().rawLine("KICK " + activeChannel + " " + selectedUser + " :" + reason);
                    } else {
                        bot.sendRaw().rawLine("KICK " + activeChannel + " " + selectedUser);
                    }
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, selectedUser + " has been kicked.", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to kick user.", Toast.LENGTH_SHORT).show());
                }
            } else {
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void executeBanCommand(String selectedUser, String activeChannel) {
        new Thread(() -> {
            if (bot != null && bot.isConnected()) {
                try {
                    bot.sendRaw().rawLine("MODE " + activeChannel + " +b " + selectedUser);
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, selectedUser + " has been banned.", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to ban user.", Toast.LENGTH_SHORT).show());
                }
            } else {
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void executeSlapCommand(String selectedUser, String activeChannel) {
        new Thread(() -> {
            if (bot != null && bot.isConnected()) {
                try {
                    bot.sendIRC().action(activeChannel, "slapped " + selectedUser + " with a sharp rock");
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Slapped " + selectedUser, Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to slap user.", Toast.LENGTH_SHORT).show());
                }
            } else {
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
