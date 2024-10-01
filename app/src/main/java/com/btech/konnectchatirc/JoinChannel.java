package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ServerResponseEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JoinChannel extends ListenerAdapter  implements OnServerResponse {

    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;
    private final View hoverPanel;
    private List<String> channelList = new ArrayList<>();
    private Set<String> channelSet = new HashSet<>();  // To prevent duplicate channels
    private AlertDialog dialog;
    private ArrayAdapter<String> adapter; // Adapter for the ListView

    public JoinChannel(Context context, PircBotX bot, ChatActivity chatActivity, View hoverPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.hoverPanel = hoverPanel;
        bot.getConfiguration().getListenerManager().addListener(this);
    }

    public void startJoinChannelProcess() {
        if (bot.isConnected()) {
            channelList.clear(); // Clear the previous channel list
            channelSet.clear();  // Clear the set of channels to avoid duplicates

            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("LIST");  // Send the LIST command to get channels

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
        if (response.startsWith(":") && response.contains(" 322 ")) {  // 322 is the RPL_LIST response
            // Parse the channel name from the response
            String[] parts = response.split(" ");
            if (parts.length > 3) {
                String channelName = parts[3];
                if (!channelSet.contains(channelName)) {  // Check for duplicates
                    channelList.add(channelName);
                    channelSet.add(channelName);  // Add to the set to avoid future duplicates
                }
            }
        } else if (response.contains(" 323 ")) {  // 323 is the end of the list
            chatActivity.runOnUiThread(this::showChannelListDialog);
        }
    }

    private void showChannelListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        hoverPanel.setVisibility(View.GONE);  // Hide hoverPanel when showing channel list

        builder.setTitle("Select a Channel to Join");

        // Create a ListView and EditText for searching
        ListView channelListView = new ListView(context);
        EditText searchBox = new EditText(context);
        searchBox.setHint("Search Channels...");

        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, channelList);
        channelListView.setAdapter(adapter);

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().toLowerCase();
                List<String> filteredList = new ArrayList<>();
                for (String channel : channelList) {
                    String formattedChannel = channel.toLowerCase().replace("#", "");
                    if (formattedChannel.contains(searchText)) {
                        filteredList.add(channel);
                    }
                }
                adapter.clear();
                adapter.addAll(filteredList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        channelListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedChannel = adapter.getItem(position);
            if (dialog != null) {
                dialog.dismiss();  // Dismiss the dialog when a channel is selected
            }
            joinSelectedChannel(selectedChannel);
        });

        // Combine the search box and ListView into one layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(searchBox);
        layout.addView(channelListView);

        builder.setView(layout);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        dialog = builder.create();  // Create the dialog and assign it to the class-level variable
        dialog.show();  // Show the dialog
    }

    private void joinSelectedChannel(String channel) {
        chatActivity.joinChannel(channel);  // Use the existing joinChannel method in ChatActivity
    }
}
