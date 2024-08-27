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
    private boolean isOpAction; // Flag to determine if it's an OP or DEOP action

    public UserOP(Context context, PircBotX bot, ChatActivity chatActivity, View hoverPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.hoverPanel = hoverPanel;
    }

    public void startOPProcess(boolean isOpAction) {
        this.isOpAction = isOpAction;
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
        builder.setTitle(isOpAction ? "Select a User to OP" : "Select a User to DEOP");

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
                hoverPanel.setVisibility(View.GONE);
            }
        });

        builder.setView(userListView);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        dialog = builder.create();
        dialog.show();
    }

    private void opUser(String nick) {
        String activeChannel = chatActivity.getActiveChannel();
        if (activeChannel != null && !activeChannel.isEmpty()) {
            new Thread(() -> {
                try {
                    String command = "MODE " + activeChannel + (isOpAction ? " +o " : " -o ") + nick;
                    bot.sendRaw().rawLine(command);  // Send the command directly to the server
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
