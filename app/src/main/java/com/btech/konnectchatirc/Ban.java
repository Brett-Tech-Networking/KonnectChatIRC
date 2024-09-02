package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.List;

public class Ban {

    private Context context;
    private PircBotX bot;
    private Activity activity;

    public Ban(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startBanProcess() {
        // Fetch the list of users from the active channel
        List<String> userList = getUserListFromActiveChannel();

        // Check if the user list is empty
        if (userList == null || userList.isEmpty()) {
            Toast.makeText(context, "No users available to ban.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder banDialog = new AlertDialog.Builder(context);
        banDialog.setTitle("Select User to Ban");

        // Create a ListView for user selection
        ListView userListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, userList);
        userListView.setAdapter(adapter);
        banDialog.setView(userListView);

        AlertDialog dialog = banDialog.create();
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = userList.get(position);
            dialog.dismiss();

            // Execute the ban command
            executeBanCommand(selectedUser);
        });

        banDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());
        dialog.show();
    }

    private void executeBanCommand(String user) {
        String activeChannel = ((ChatActivity) activity).getActiveChannel(); // Get the active channel from ChatActivity

        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    // Send the ban command
                    bot.sendRaw().rawLine("MODE " + activeChannel + " +b " + user);
                    activity.runOnUiThread(() -> Toast.makeText(context, "Banned " + user + " from " + activeChannel, Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to execute ban command.", Toast.LENGTH_SHORT).show());
                    Log.e("Ban", "Error executing ban command", e);
                }
            }).start();
        } else {
            Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getUserListFromActiveChannel() {
        List<String> userList = new ArrayList<>();
        String activeChannel = ((ChatActivity) activity).getActiveChannel(); // Get the active channel from ChatActivity

        // Fetch the active channel object from the bot
        Channel channel = bot.getUserChannelDao().getChannel(activeChannel);

        if (channel != null) {
            // Add all users from the channel to the list
            for (User user : channel.getUsers()) {
                userList.add(user.getNick());
            }
        } else {
            Toast.makeText(context, "Active channel not found or bot not connected.", Toast.LENGTH_SHORT).show();
        }

        return userList;
    }
}
