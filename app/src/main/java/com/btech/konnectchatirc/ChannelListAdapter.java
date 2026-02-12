package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChannelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHANNEL = 1;

    private List<ChannelInfo> channelList;
    private List<Object> displayList;  // Mix of headers (String) and channels (ChannelInfo)
    private List<ChannelInfo> filteredChannelList;
    private final OnChannelClickListener listener;
    private final OnChannelLongClickListener longClickListener;

    public interface OnChannelClickListener {
        void onChannelClick(ChannelInfo channelInfo);
    }

    public interface OnChannelLongClickListener {
        void onChannelLongClick(ChannelInfo channelInfo);
    }

    public ChannelListAdapter(List<ChannelInfo> channelList, OnChannelClickListener listener, OnChannelLongClickListener longClickListener) {
        this.channelList = channelList;
        this.filteredChannelList = new ArrayList<>(channelList);
        this.listener = listener;
        this.longClickListener = longClickListener;
        buildDisplayList();
    }

    private void buildDisplayList() {
        displayList = new ArrayList<>();
        
        // Separate favorites and non-favorites
        List<ChannelInfo> favorites = new ArrayList<>();
        List<ChannelInfo> nonFavorites = new ArrayList<>();
        
        for (ChannelInfo channel : filteredChannelList) {
            if (channel.isFavorite()) {
                favorites.add(channel);
            } else {
                nonFavorites.add(channel);
            }
        }
        
        // Add favorites section if there are any
        if (!favorites.isEmpty()) {
            displayList.add("FAVORITES");  // Header
            displayList.addAll(favorites);
        }
        
        // Add non-favorites with header if there are any
        if (!nonFavorites.isEmpty()) {
            displayList.add("CHANNELS");  // Header
            displayList.addAll(nonFavorites);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayList.get(position);
        return (item instanceof String) ? VIEW_TYPE_HEADER : VIEW_TYPE_CHANNEL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_channel_joinable, parent, false);
            return new ChannelViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ChannelViewHolder) {
            ChannelInfo channel = (ChannelInfo) displayList.get(position);
            ((ChannelViewHolder) holder).bind(channel, listener, longClickListener);
        } else if (holder instanceof HeaderViewHolder) {
            String headerText = (String) displayList.get(position);
            ((HeaderViewHolder) holder).bind(headerText);
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public void filter(String query) {
        filteredChannelList.clear();
        if (query.isEmpty()) {
            filteredChannelList.addAll(channelList);
        } else {
            String lowerCaseQuery = query.toLowerCase().replace("#", "");
            for (ChannelInfo channel : channelList) {
                String channelName = channel.getChannelName().toLowerCase().replace("#", "");
                if (channelName.contains(lowerCaseQuery) || 
                    channel.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    filteredChannelList.add(channel);
                }
            }
        }
        buildDisplayList();
        notifyDataSetChanged();
    }

    public void refreshList() {
        filteredChannelList.clear();
        filteredChannelList.addAll(channelList);
        buildDisplayList();
        notifyDataSetChanged();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.section_header_text);
        }

        public void bind(String text) {
            headerText.setText(text);
        }
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final TextView channelName;
        private final TextView userCount;
        private final TextView description;
        private final TextView star;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channel_name);
            userCount = itemView.findViewById(R.id.channel_user_count);
            description = itemView.findViewById(R.id.channel_description);
            star = itemView.findViewById(R.id.channel_star);
        }

        public void bind(ChannelInfo channel, OnChannelClickListener listener, OnChannelLongClickListener longClickListener) {
            // Show/hide star in top right
            star.setVisibility(channel.isFavorite() ? View.VISIBLE : View.GONE);
            
            channelName.setText(channel.getChannelName());
            userCount.setText(channel.getUserCount() + " users");
            
            String desc = channel.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                description.setText("No description available");
                description.setTextColor(0xFF666666);
            } else {
                description.setText(desc);
                description.setTextColor(0xFF888888);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChannelClick(channel);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onChannelLongClick(channel);
                }
                return true;
            });
        }
    }
}
