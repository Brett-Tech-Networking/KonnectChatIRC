package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ServerResponseEvent;

import java.util.ArrayList;
import java.util.List;

public class JoinChannel extends ListenerAdapter {

    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;
    private final View hoverPanel;
    private List<String> channelList = new ArrayList<>();
    private AlertDialog dialog; // Declare the dialog at the class level

    public JoinChannel(Context context, PircBotX bot, ChatActivity chatActivity, View hoverPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.hoverPanel = hoverPanel; // Initialize hoverPanel
        bot.getConfiguration().getListenerManager().addListener(this); // Add listener for server responses
    }

    public void startJoinChannelProcess() {
        if (bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("LIST"); // Send the LIST command to get channels

                } catch (Exception e) {
                    chatActivity.runOnUiThread(() -> Toast.makeText(context, "Failed to request channel list.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Toast.makeText(context, "Not connected to server.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onServerResponse(ServerResponseEvent event) {
        String response = event.getRawLine();
        if (response.startsWith(":") && response.contains(" 322 ")) { // 322 is the RPL_LIST response
            // Parse the channel name from the response
            String[] parts = response.split(" ");
            if (parts.length > 3) {
                String channelName = parts[3];
                channelList.add(channelName);
            }
        } else if (response.contains(" 323 ")) { // 323 is the end of the list
            chatActivity.runOnUiThread(this::showChannelListDialog);
        }
    }

    private void showChannelListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        hoverPanel.setVisibility(View.GONE); // Hide hoverPanel when showing channel list

        builder.setTitle("Select a Channel to Join");

        ListView channelListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, channelList);
        channelListView.setAdapter(adapter);

        channelListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedChannel = channelList.get(position);
            if (dialog != null) {
                dialog.dismiss(); // Dismiss the dialog when a channel is selected
            }
            joinSelectedChannel(selectedChannel);
        });

        builder.setView(channelListView);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        dialog = builder.create(); // Create the dialog and assign it to the class-level variable
        dialog.show(); // Show the dialog
    }

    private void joinSelectedChannel(String channel) {
        chatActivity.joinChannel(channel);
    }
}
