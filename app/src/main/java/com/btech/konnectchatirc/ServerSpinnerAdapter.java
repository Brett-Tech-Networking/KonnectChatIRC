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

    private Context context;
    private List<ServerItem> serverList;

    public ServerSpinnerAdapter(@NonNull Context context, @NonNull List<ServerItem> objects) {
        super(context, 0, objects);
        this.context = context;
        this.serverList = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createServerView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createServerView(position, convertView, parent);
    }

    private View createServerView(int position, View convertView, ViewGroup parent) {
        ServerItem serverItem = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);
        }

        ImageView serverIcon = convertView.findViewById(R.id.serverIcon);
        TextView serverName = convertView.findViewById(R.id.serverName);

        if (serverItem != null) {
            serverIcon.setImageResource(serverItem.getIconResId());
            serverName.setText(serverItem.getServerName());
        }

        return convertView;
    }
}
