package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

        // Reference TextView elements
        TextView optionsNick = dialogView.findViewById(R.id.options_nick);
        TextView optionsHostIP = dialogView.findViewById(R.id.options_host_ip);

        // Set nickname in TextView
        optionsNick.setText(selectedUser.getNick());

        // Extract hostmask and IP address
        String hostmask = selectedUser.getHostmask();
        String hostname = null;
        String ipAddress = "N/A";

        if (hostmask != null && hostmask.contains("@")) {
            hostname = hostmask.split("@")[1];
            ipAddress = extractIPAddress(hostname);
        }

        // Update the optionsHostIP TextView with the extracted information
        optionsHostIP.setText("Host: " + (hostname != null ? hostname : "N/A") + " | IP: " + ipAddress);

        // Set up button listeners
        Button btnKick = dialogView.findViewById(R.id.btnKick);
        btnKick.setOnClickListener(v -> {
            Kick kick = new Kick(context, chatActivity.getBot(), chatActivity);
            kick.executeKick(selectedUser.getNick(), "Default Reason");
        });

        Button btnBan = dialogView.findViewById(R.id.btnBan);
        btnBan.setOnClickListener(v -> {
            Ban ban = new Ban(context, chatActivity.getBot(), chatActivity);
            ban.executeBanCommand(selectedUser.getHostmask());
        });

        Button btnSlap = dialogView.findViewById(R.id.btnSlap);
        btnSlap.setOnClickListener(v -> {
            ListUsers listUsers = new ListUsers(context, chatActivity.getBot(), chatActivity);
            listUsers.executeSlapCommand(selectedUser.getNick(), chatActivity.getActiveChannel());
        });

        // Show the dialog
        builder.create().show();
    }

    private String extractIPAddress(String hostname) {
        try {
            InetAddress inetAddress = InetAddress.getByName(hostname);
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "N/A";
        }
    }
}
