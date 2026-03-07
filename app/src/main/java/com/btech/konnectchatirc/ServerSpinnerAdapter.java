package com.btech.konnectchatirc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ServerSpinnerAdapter extends ArrayAdapter<ServerItem> {

    public interface OnServerLongClickListener {
        void onServerLongClick(int position, ServerItem item);
    }

    private Context context;
    private List<ServerItem> serverList;
    private OnServerLongClickListener longClickListener;

    public ServerSpinnerAdapter(@NonNull Context context, @NonNull List<ServerItem> objects) {
        super(context, 0, objects);
        this.context = context;
        this.serverList = objects;
    }

    public void setOnServerLongClickListener(OnServerLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);
        }
        bindView(convertView, position);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Always inflate fresh for dropdown to avoid recycled listener issues
        convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);
        bindView(convertView, position);

        // Add long-click listener to dropdown items
        if (longClickListener != null && position > 0 && position < serverList.size() - 1) {
            final ServerItem item = getItem(position);
            convertView.setOnLongClickListener(v -> {
                longClickListener.onServerLongClick(position, item);
                return false;
            });
        }

        return convertView;
    }

    private void bindView(View view, int position) {
        ServerItem serverItem = getItem(position);
        ImageView serverIcon = view.findViewById(R.id.serverIcon);
        TextView serverName = view.findViewById(R.id.serverName);

        if (serverItem != null) {
            if (serverItem.getIconResId() != 0) {
                serverIcon.setImageResource(serverItem.getIconResId());
                serverIcon.setVisibility(View.VISIBLE);
            } else {
                serverIcon.setVisibility(View.GONE);
            }
            serverName.setText(serverItem.getServerName());
        }
    }
}
