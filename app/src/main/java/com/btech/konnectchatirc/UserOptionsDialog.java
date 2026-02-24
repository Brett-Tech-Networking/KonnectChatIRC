package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.pircbotx.User;

import java.util.List;

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

        // Extract and format host using ChatActivity's reliable extractor
        String rawHost = selectedUser.getHostname();
        if (rawHost == null || rawHost.isEmpty()) {
            rawHost = selectedUser.getHostmask();
        }
        String cleanHost = chatActivity.extractHost(rawHost);

        // If we couldn't extract a clean host, fall back to whatever is available
        if (cleanHost == null || cleanHost.isEmpty()) {
            cleanHost = rawHost != null ? rawHost : "N/A";
        }
        optionsHost.setText("Host: " + cleanHost);

        // Set Ident
        TextView optionsIdent = dialogView.findViewById(R.id.options_ident);
        String ident = selectedUser.getLogin();
        optionsIdent.setText("Ident: " + (ident != null ? ident : "N/A"));

        // Set Real Name
        TextView optionsRealName = dialogView.findViewById(R.id.options_realname);
        String realname = selectedUser.getRealName();
        optionsRealName.setText("Real Name: " + (realname != null ? realname : "N/A"));

        // Set Account Status & Append Duplicate Users natively to the Status string
        TextView optionsStatus = dialogView.findViewById(R.id.options_account);
        String statusText = "Status: Not Away";
        int statusColor = Color.parseColor("#AAAAAA"); // Default gray

        if (selectedUser.isAway()) {
            statusText = "Status: Away (" + selectedUser.getAwayMessage() + ")";
            statusColor = Color.parseColor("#FF9800"); // Orange
        } else if (selectedUser.isIrcop()) {
            statusText = "Status: IRC Operator";
            statusColor = Color.parseColor("#F44336"); // Red
        }

        // Use SpannableStringBuilder to format the main status string
        SpannableStringBuilder ssb = new SpannableStringBuilder(statusText);
        ssb.setSpan(new ForegroundColorSpan(statusColor), 0, statusText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Get Duplicate Users and append directly under Status
        List<String> dupNicks = chatActivity.findDuplicateUsers(selectedUser);
        if (!dupNicks.isEmpty()) {
            ssb.append("\nDuplicates: ");
            int start = ssb.length();
            String dupesStr = android.text.TextUtils.join(", ", dupNicks);
            ssb.append(dupesStr);
            // Make the duplicate nicks bright red so they stand out
            ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#FF5252")), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        optionsStatus.setText(ssb);

        // Hide the old legacy duplicate layout if it exists in XML to prevent double text
        LinearLayout layoutDupUser = dialogView.findViewById(R.id.layout_dup_user);
        if (layoutDupUser != null) {
            layoutDupUser.setVisibility(View.GONE);
        }

        // Set up button listeners
        Button btnKick = dialogView.findViewById(R.id.btnKick);
        if (btnKick != null) {
            btnKick.setOnClickListener(v -> {
                Kick kick = new Kick(context, chatActivity.getBot(), chatActivity);
                kick.executeKick(selectedUser.getNick(), "Default Reason");
                dialog.dismiss();
            });
        }

        Button btnBan = dialogView.findViewById(R.id.btnBan);
        if (btnBan != null) {
            btnBan.setOnClickListener(v -> {
                Ban ban = new Ban(context, chatActivity.getBot(), chatActivity);
                ban.executeBanCommand(selectedUser.getHostmask());
                dialog.dismiss();
            });
        }

        Button btnSlap = dialogView.findViewById(R.id.btnSlap);
        if (btnSlap != null) {
            btnSlap.setOnClickListener(v -> {
                ListUsers listUsers = new ListUsers(context, chatActivity.getBot(), chatActivity);
                listUsers.executeSlapCommand(selectedUser.getNick(), chatActivity.getActiveChannel());
                dialog.dismiss();
            });
        }

        Button btnPrivateMessage = dialogView.findViewById(R.id.btnPrivateMessage);
        if (btnPrivateMessage != null) {
            btnPrivateMessage.setOnClickListener(v -> {
                SharedPreferences prefs = context.getSharedPreferences("konnect_chat", Context.MODE_PRIVATE);
                PrivateMessageStorage messageStorage = new PrivateMessageStorage(prefs);

                String userNick = chatActivity.getUserNick();
                String recipientNick = selectedUser.getNick();

                // Create an empty conversation
                messageStorage.createConversation(userNick, recipientNick);

                Intent intent = new Intent(context, PrivateChatActivity.class);
                intent.putExtra("RECIPIENT_NICK", recipientNick);
                intent.putExtra("USER_NICK", userNick);
                context.startActivity(intent);
                dialog.dismiss();
            });
        }

        // Show the dialog
        dialog.show();

        // Make the dialog wider to use more screen space
        int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(width, -2); // -2 is WRAP_CONTENT
        }
    }
}