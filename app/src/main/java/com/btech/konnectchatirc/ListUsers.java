package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ListUsers {

    private Context context;
    private PircBotX bot;
    private Activity activity;
    private List<UserItem> userList = new ArrayList<>();
    private List<UserItem> filteredUserList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // List of slap messages
    private List<String> slapMessages = Arrays.asList(
            "slaps %s viciously with a sharp rock",
            "delivers a powerful backhand to %s",
            "smacks %s across the face with force",
            "gives %s a brutal slap upside the head",
            "whips %s with a leather belt",
            "throws a punch at %s's jaw",
            "kicks %s hard in the shin",
            "shoves %s aggressively",
            "lands a fierce slap on %s's cheek",
            "tosses a bucket of ice water on %s",
            "grabs %s and shakes them vigorously",
            "hurls a chair in %s's direction",
            "slams a door behind %s abruptly",
            "sprays %s with a cold water hose",
            "drops a heavy book near %s's feet"
    );

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
            // Populate the user list with prefixes
            for (User user : channel.getUsers()) {
                String prefix = IrcUtils.getUserPrefix(user, channel);
                userList.add(new UserItem(prefix, user.getNick()));
            }
        } else {
            Toast.makeText(context, "Active channel not found or bot not connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userList.isEmpty()) {
            Toast.makeText(context, "No users found in the channel.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sort the user list based on prefix and nickname
        Collections.sort(userList, Comparator.comparingInt(UserItem::getSortOrder)
                .thenComparing(UserItem::getNick, String.CASE_INSENSITIVE_ORDER));

        // Copy all users to filteredUserList initially
        filteredUserList.clear();
        filteredUserList.addAll(userList);

        // Prepare the list of display names for the adapter
        List<String> displayNames = new ArrayList<>();
        for (UserItem userItem : filteredUserList) {
            displayNames.add(userItem.getDisplayName());
        }

        // Inflate the custom layout for the user list dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        View userListViewDialog = inflater.inflate(R.layout.dialog_user_list, null);

        // Find the ListView and set the adapter
        ListView userListView = userListViewDialog.findViewById(R.id.userListView);
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, displayNames);
        userListView.setAdapter(adapter);

        // Handle the search EditText
        EditText searchUserEditText = userListViewDialog.findViewById(R.id.searchUserEditText);
        searchUserEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                // Filter the user list as the user types
                filterUserList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });

        // Create and show the user list dialog with a proper background
        AlertDialog.Builder userDialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        userDialog.setView(userListViewDialog);

        AlertDialog dialog = userDialog.create();

        // Set custom size and style for the dialog
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setLayout(
                    (int) (300 * context.getResources().getDisplayMetrics().density), // Custom width
                    (int) (600 * context.getResources().getDisplayMetrics().density)  // Custom height
            );
        });

        // Set up the ListView item click listener
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDisplayName = adapter.getItem(position); // Use adapter's data
            dialog.dismiss();

            // Show options for the selected user
            showUserOptions(selectedDisplayName);
        });

        // Set the dialog to dismiss when clicking outside
        dialog.setCanceledOnTouchOutside(true);

        dialog.show();
    }

    private void filterUserList(String query) {
        filteredUserList.clear();
        if (query.isEmpty()) {
            filteredUserList.addAll(userList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (UserItem userItem : userList) {
                String nick = userItem.getNick();
                if (nick.toLowerCase().contains(lowerCaseQuery)) {
                    filteredUserList.add(userItem);
                }
            }
        }

        // Update the adapter data
        List<String> displayNames = new ArrayList<>();
        for (UserItem userItem : filteredUserList) {
            displayNames.add(userItem.getDisplayName());
        }

        activity.runOnUiThread(() -> {
            adapter.clear();
            adapter.addAll(displayNames);
            adapter.notifyDataSetChanged();
        });
    }

    private void showUserOptions(String selectedDisplayName) {
        final String selectedUserWithPrefix = selectedDisplayName;
        final String selectedUser;
        if (selectedUserWithPrefix.length() > 0 && "~&@%+".indexOf(selectedUserWithPrefix.charAt(0)) != -1) {
            selectedUser = selectedUserWithPrefix.substring(1);
        } else {
            selectedUser = selectedUserWithPrefix;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View optionsView = inflater.inflate(R.layout.dialog_user_options, null);

        // Fetch the user object from the bot to get detailed information
        User user = bot.getUserChannelDao().getUser(selectedUser);

        // Set the nickname in the dialog
        TextView nickTextView = optionsView.findViewById(R.id.options_nick);
        nickTextView.setText(selectedUserWithPrefix);

        // Set the host and IP information
        TextView hostIpTextView = optionsView.findViewById(R.id.options_host_ip);
        String host = (user != null && user.getHostmask() != null) ? user.getHostmask() : "N/A";
        hostIpTextView.setText("Host: " + host);

        AlertDialog.Builder optionsDialog = new AlertDialog.Builder(context, R.style.CustomDialogTheme_NoAnimation);
        optionsDialog.setView(optionsView);

        AlertDialog dialog = optionsDialog.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setLayout(
                    (int) (240 * context.getResources().getDisplayMetrics().density), // Custom width
                    (int) (400 * context.getResources().getDisplayMetrics().density)  // Custom height
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

    public void executeSlapCommand(String selectedUser, String activeChannel) {
        new Thread(() -> {
            if (bot != null && bot.isConnected()) {
                try {
                    // Choose a random slap message
                    Random random = new Random();
                    String slapMessageTemplate = slapMessages.get(random.nextInt(slapMessages.size()));
                    String slapMessage = String.format(slapMessageTemplate, selectedUser);

                    // Correctly sending a /me action
                    bot.sendIRC().action(activeChannel, slapMessage);

                    // Display the slap action immediately in the sender's chat
                    activity.runOnUiThread(() -> {
                        String message = "* " + bot.getNick() + " " + slapMessage;
                        ((ChatActivity) activity).addChatMessage(message);
                    });

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
