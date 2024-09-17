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

import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.List;

public class Zline {

    private Context context;
    private PircBotX bot;
    private Activity activity;
    private List<String> userList = new ArrayList<>();
    private List<String> filteredUserList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    public Zline(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startZlineProcess() {
        // Clear previous user list
        userList.clear();

        // Fetch the active channel
        String activeChannel = ((ChatActivity) activity).getActiveChannel();
        org.pircbotx.Channel channel = bot.getUserChannelDao().getChannel(activeChannel);

        if (channel != null) {
            // Populate the user list
            for (User user : channel.getUsers()) {
                userList.add(user.getNick());
            }
        }

        if (userList.isEmpty()) {
            ((ChatActivity) activity).addChatMessage("No users found in the channel.");
            return;
        }

        // Copy all users to filteredUserList initially
        filteredUserList.clear();
        filteredUserList.addAll(userList);

        // Inflate the custom layout for the user list dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        View userListViewDialog = inflater.inflate(R.layout.dialog_user_list_kill, null);

        // Find the ListView and set the adapter
        ListView userListView = userListViewDialog.findViewById(R.id.userListView);
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, filteredUserList);
        userListView.setAdapter(adapter);

        // Handle the search EditText
        EditText searchUserEditText = userListViewDialog.findViewById(R.id.searchUserEditText);
        searchUserEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
            String selectedUser = filteredUserList.get(position); // Use filtered list
            dialog.dismiss();

            // Execute the Zline command
            executeZline(selectedUser);
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
            for (String user : userList) {
                if (user.toLowerCase().contains(lowerCaseQuery)) {
                    filteredUserList.add(user);
                }
            }
        }
        // Notify the adapter of the changes
        activity.runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    public void executeZline(String nick) {
        if (((ChatActivity) activity).isNetworkAvailable()) {
            new Thread(() -> {
                try {
                    if (bot.isConnected()) {
                        User user = bot.getUserChannelDao().getUser(nick);
                        if (user != null) {
                            String hostmask = user.getHostmask();  // Get the user's hostmask
                            bot.sendRaw().rawLine("ZLINE " + hostmask + " 1d");
                            ((ChatActivity) activity).processServerMessage("Server", nick + " Zlined for 1 day", ((ChatActivity) activity).getActiveChannel());

                            // Set operatorPanel visibility to GONE after the Zline command
                            activity.runOnUiThread(() -> {
                                View operatorPanel = activity.findViewById(R.id.operatorPanel);
                                if (operatorPanel != null) {
                                    operatorPanel.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            ((ChatActivity) activity).processServerMessage("Server", "Failed to execute Zline command: User not found.", ((ChatActivity) activity).getActiveChannel());
                        }
                    } else {
                        ((ChatActivity) activity).processServerMessage("Server", "Bot is not connected to the server.", ((ChatActivity) activity).getActiveChannel());
                    }
                } catch (Exception e) {
                    ((ChatActivity) activity).processServerMessage("Server", "Failed to execute Zline command.", ((ChatActivity) activity).getActiveChannel());
                }
            }).start();
        } else {
            ((ChatActivity) activity).processServerMessage("Server", "No network connection.", ((ChatActivity) activity).getActiveChannel());
        }
    }
}
