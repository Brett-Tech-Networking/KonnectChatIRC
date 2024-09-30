package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.pircbotx.User;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class UserOptionsDialog {

    private Context context;
    private User selectedUser;
    private ChatActivity chatActivity;

    public UserOptionsDialog(Context context, User selectedUser, ChatActivity chatActivity) {
        this.context = context;
        this.selectedUser = selectedUser;
        this.chatActivity = chatActivity;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_user_options, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Reference TextView elements
        TextView optionsNick = dialogView.findViewById(R.id.options_nick);
        TextView optionsHost = dialogView.findViewById(R.id.options_host_ip);

        // Set nickname in TextView
        optionsNick.setText(selectedUser.getNick());

        // Extract hostmask
        String hostmask = selectedUser.getHostmask();
        String hostname = null;

        if (hostmask != null && hostmask.contains("@")) {
            hostname = hostmask.split("@")[1];
            optionsHost.setText("Host: " + hostname); // Display the hostname directly
        } else {
            optionsHost.setText("Host: N/A");
        }

        // Set up button listeners
        Button btnKick = dialogView.findViewById(R.id.btnKick);
        btnKick.setOnClickListener(v -> {
            Kick kick = new Kick(context, chatActivity.getBot(), chatActivity);
            kick.executeKick(selectedUser.getNick(), "Default Reason");
            dialog.dismiss();
        });

        Button btnBan = dialogView.findViewById(R.id.btnBan);
        btnBan.setOnClickListener(v -> {
            Ban ban = new Ban(context, chatActivity.getBot(), chatActivity);
            ban.executeBanCommand(selectedUser.getHostmask());
            dialog.dismiss();
        });

        Button btnSlap = dialogView.findViewById(R.id.btnSlap);
        btnSlap.setOnClickListener(v -> {
            ListUsers listUsers = new ListUsers(context, chatActivity.getBot(), chatActivity);
            listUsers.executeSlapCommand(selectedUser.getNick(), chatActivity.getActiveChannel());
            dialog.dismiss();
        });

        // Show the dialog
        dialog.show();
    }
}
