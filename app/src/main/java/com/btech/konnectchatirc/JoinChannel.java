package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ServerResponseEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JoinChannel extends ListenerAdapter implements OnServerResponse {

    private final Context context;
    private final PircBotX bot;
    private final ChatActivity chatActivity;
    private final View hoverPanel;
    private List<ChannelInfo> channelList = new ArrayList<>();
    private Set<String> channelSet = new HashSet<>();  // To prevent duplicate channels
    private AlertDialog dialog;
    private boolean isDialogShowing = false;  // Prevent multiple dialogs
    private ChannelListAdapter adapter;
    private SharedPreferences favoritePrefs;

    public JoinChannel(Context context, PircBotX bot, ChatActivity chatActivity, View hoverPanel) {
        this.context = context;
        this.bot = bot;
        this.chatActivity = chatActivity;
        this.hoverPanel = hoverPanel;
        bot.getConfiguration().getListenerManager().addListener(this);
        favoritePrefs = context.getSharedPreferences("channel_favorites", Context.MODE_PRIVATE);
    }

    public void startJoinChannelProcess() {
        if (bot.isConnected()) {
            channelList.clear();
            channelSet.clear();
            isDialogShowing = false;  // Reset flag

            new Thread(() -> {
                try {
                    bot.sendRaw().rawLine("LIST");
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
        
        // RPL_LIST (322): :<server> 322 <nick> <channel> <user_count> :<topic>
        if (response.startsWith(":") && response.contains(" 322 ")) {
            String[] parts = response.split(" ", 6);
            
            if (parts.length >= 5) {
                String channelName = parts[3];
                int userCount = 0;
                String description = "";
                
                try {
                    userCount = Integer.parseInt(parts[4]);
                } catch (NumberFormatException e) {
                    userCount = 0;
                }
                
                // Extract description/topic (after the colon)
                if (parts.length >= 6 && parts[5].startsWith(":")) {
                    description = parts[5].substring(1);
                }
                
                if (!channelSet.contains(channelName)) {
                    channelList.add(new ChannelInfo(channelName, userCount, description));
                    channelSet.add(channelName);
                }
            }
        } else if (response.contains(" 323 ")) {  // RPL_LISTEND
            // Load favorites and mark channels
            loadFavorites();
            // Sort: favorites first, then by user count
            channelList.sort((c1, c2) -> {
                if (c1.isFavorite() != c2.isFavorite()) {
                    return c1.isFavorite() ? -1 : 1;  // Favorites first
                }
                return Integer.compare(c2.getUserCount(), c1.getUserCount());  // Then by user count
            });
            // Only show dialog if not already showing
            if (!isDialogShowing) {
                isDialogShowing = true;
                chatActivity.runOnUiThread(this::showChannelListDialog);
            }
        }
    }

    private void loadFavorites() {
        Set<String> favorites = favoritePrefs.getStringSet("favorites", new HashSet<>());
        for (ChannelInfo channel : channelList) {
            channel.setFavorite(favorites.contains(channel.getChannelName()));
        }
    }

    private void saveFavorites() {
        Set<String> favorites = new HashSet<>();
        for (ChannelInfo channel : channelList) {
            if (channel.isFavorite()) {
                favorites.add(channel.getChannelName());
            }
        }
        favoritePrefs.edit().putStringSet("favorites", favorites).apply();
    }

    private void showChannelListDialog() {
        // Dismiss any existing dialog first
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        hoverPanel.setVisibility(View.GONE);

        // Inflate custom layout with RecyclerView
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_channel_list, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.channel_recycler_view);
        EditText searchBox = dialogView.findViewById(R.id.channel_search_box);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        
        // Add divider between items
        androidx.recyclerview.widget.DividerItemDecoration divider = 
            new androidx.recyclerview.widget.DividerItemDecoration(context, LinearLayoutManager.VERTICAL);
        divider.setDrawable(new android.graphics.drawable.ColorDrawable(0x20FFFFFF)); // Subtle white divider
        recyclerView.addItemDecoration(divider);
        
        adapter = new ChannelListAdapter(channelList, channelInfo -> {
            if (dialog != null) {
                dialog.dismiss();
            }
            joinSelectedChannel(channelInfo.getChannelName());
        }, channelInfo -> {
            // Long-click handler: show pin/unpin menu
            showFavoriteMenu(channelInfo);
        });
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        builder.setView(dialogView);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        dialog = builder.create();
        
        // Reset flag when dialog is dismissed
        dialog.setOnDismissListener(d -> isDialogShowing = false);
        
        // Remove default background to only show our custom rounded design
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        
        dialog.show();
    }

    private void showFavoriteMenu(ChannelInfo channel) {
        String action = channel.isFavorite() ? "Unpin from Favorites" : "Pin to Favorites";
        
        new AlertDialog.Builder(context)
            .setTitle(channel.getChannelName())
            .setMessage(action + "?")
            .setPositiveButton(action, (d, which) -> {
                channel.setFavorite(!channel.isFavorite());
                saveFavorites();
                // Re-sort and refresh adapter
                channelList.sort((c1, c2) -> {
                    if (c1.isFavorite() != c2.isFavorite()) {
                        return c1.isFavorite() ? -1 : 1;
                    }
                    return Integer.compare(c2.getUserCount(), c1.getUserCount());
                });
                adapter.refreshList();
                Toast.makeText(context, action + " complete", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void joinSelectedChannel(String channel) {
        chatActivity.joinChannel(channel);
    }
}
