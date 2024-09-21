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
    private List<ServerItem> servers;

    public ServerSpinnerAdapter(@NonNull Context context, @NonNull List<ServerItem> servers) {
        super(context, 0, servers);
        this.context = context;
        this.servers = servers;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);
        }

        ImageView icon = view.findViewById(R.id.serverIcon);
        TextView name = view.findViewById(R.id.serverName);

        ServerItem server = servers.get(position);

        icon.setImageResource(server.getIconResId());
        name.setText(server.getName());

        return view;
    }
}
