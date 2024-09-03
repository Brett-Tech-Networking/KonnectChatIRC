package com.btech.konnectchatirc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private List<ChannelItem> channelList;
    private OnChannelClickListener listener;

    public ChannelAdapter(List<ChannelItem> channelList, OnChannelClickListener listener) {
        this.channelList = channelList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_item, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        ChannelItem channel = channelList.get(position);
        holder.channelName.setText(channel.getChannelName());

        if (channel.getUnreadCount() > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(String.valueOf(channel.getUnreadCount()));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onChannelClick(channel));
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public interface OnChannelClickListener {
        void onChannelClick(ChannelItem channel);
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {

        TextView channelName;
        TextView unreadBadge;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channelName);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
        }
    }
}
