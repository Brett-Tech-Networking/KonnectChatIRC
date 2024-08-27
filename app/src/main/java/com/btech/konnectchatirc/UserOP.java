package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UserOP {
    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;
    private final View hoverPanel;
    private AlertDialog dialog; // This is the class-level dialog

    public UserOP(Context context, PircBotX bot, ChatActivity chatActivity, View hoverPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.hoverPanel = hoverPanel; // Initialize hoverPanel if needed
    }

    public void startOPProcess() {
        if (bot.isConnected()) {
            Channel activeChannel = bot.getUserChannelDao().getChannel(chatActivity.getActiveChannel());

            if (activeChannel != null) {
                Set<User> users = activeChannel.getUsers();
                showUserListDialog(new ArrayList<>(users));
            }
        }
    }

    private void showUserListDialog(List<User> userList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select a User to OP");

        ListView userListView = new ListView(context);
        List<String> userNicks = new ArrayList<>();

        for (User user : userList) {
            userNicks.add(user.getNick());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, userNicks);
        userListView.setAdapter(adapter);

        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = userNicks.get(position);
            if (selectedUser != null) {
                opUser(selectedUser);
                if (dialog != null) {
                    dialog.dismiss(); // Close the dialog when a user is selected
                }
                hoverPanel.setVisibility(View.GONE); // Hide the hover panel after selection
            }
        });

        builder.setView(userListView);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        dialog = builder.create(); // Assign the dialog to the class-level variable
        dialog.show(); // Show the dialog
    }

    private void opUser(String nick) {
        String activeChannel = chatActivity.getActiveChannel();
        if (activeChannel != null && !activeChannel.isEmpty()) {
            new Thread(() -> {
                try {
                    // Replace /op with the correct mode command to op a user
                    String command = "MODE " + activeChannel + " +o " + nick;
                    bot.sendRaw().rawLineNow(command);  // Send the command directly to the server

                    // Ensure hoverPanel is hidden after the command is executed
                    chatActivity.runOnUiThread(() -> hoverPanel.setVisibility(View.GONE));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
