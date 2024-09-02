package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
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

        // Inflate the custom layout for the user list dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        View userListViewDialog = inflater.inflate(R.layout.dialog_user_list, null);

        // Find the ListView and set the adapter
        ListView userListView = userListViewDialog.findViewById(R.id.userListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, userList);
        userListView.setAdapter(adapter);

        // Create and show the user list dialog with a proper background
        AlertDialog.Builder userDialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        userDialog.setView(userListViewDialog);

        AlertDialog dialog = userDialog.create();

        // Set custom size and style for the dialog
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setLayout(
                    (int) (300 * context.getResources().getDisplayMetrics().density), // Custom width
                    (int) (400 * context.getResources().getDisplayMetrics().density)  // Custom height
            );
        });

        // Set up the ListView item click listener
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = userList.get(position);
            dialog.dismiss();

            // Show options for the selected user
            showUserOptions(selectedUser);
        });

        // Set the dialog to dismiss when clicking outside
        dialog.setCanceledOnTouchOutside(true);

        dialog.show();
    }

    private void showUserOptions(String selectedUser) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View optionsView = inflater.inflate(R.layout.dialog_user_options, null);

        // Set the nickname in the dialog
        TextView nickTextView = optionsView.findViewById(R.id.options_nick);
        nickTextView.setText(selectedUser);

        AlertDialog.Builder optionsDialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme_NoAnimation);
        optionsDialog.setView(optionsView);

        AlertDialog dialog = optionsDialog.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setLayout(
                    (int) (240 * context.getResources().getDisplayMetrics().density),
                    (int) (280 * context.getResources().getDisplayMetrics().density)
            );
        });

        dialog.show();

        // Set up button click listeners
        optionsView.findViewById(R.id.btnKick).setOnClickListener(v -> {
            showKickDialog(selectedUser, ((ChatActivity) activity).getActiveChannel());
            dialog.dismiss();
        });

        optionsView.findViewById(R.id.btnBan).setOnClickListener(v -> {
            executeBanCommand(selectedUser, ((ChatActivity) activity).getActiveChannel());
            dialog.dismiss();
        });

        optionsView.findViewById(R.id.btnSlap).setOnClickListener(v -> {
            executeSlapCommand(selectedUser, ((ChatActivity) activity).getActiveChannel());
            dialog.dismiss();
        });

        // Set the dialog to dismiss when clicking outside
        dialog.setCanceledOnTouchOutside(true);
    }

    private void showKickDialog(String selectedUser, String activeChannel) {
        AlertDialog.Builder kickDialog = new AlertDialog.Builder(context);
        kickDialog.setTitle("Kick " + selectedUser);

        // Input for kick reason
        final EditText input = new EditText(context);
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
                    // Correctly sending a /me action
                    bot.sendIRC().action(activeChannel, "slaps " + selectedUser + " with a sharp rock");

                    // Display the slap action immediately in the sender's chat
                    activity.runOnUiThread(() -> ((ChatActivity) activity).addChatMessage("* " + bot.getNick() + " slaps " + selectedUser + " with a sharp rock", true));

                } catch (Exception e) {
                    activity.runOnUiThread(() -> ((ChatActivity) activity).addChatMessage("Failed to slap user."));
                }
            } else {
                activity.runOnUiThread(() -> ((ChatActivity) activity).addChatMessage("Bot is not connected to the server."));
            }
        }).start();
    }

    private void showToast(String message) {
        activity.runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}