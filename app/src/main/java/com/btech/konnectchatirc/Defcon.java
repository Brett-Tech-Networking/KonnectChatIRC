package com.btech.konnectchatirc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.pircbotx.PircBotX;

public class Defcon {

    private Context context;
    private PircBotX bot;
    private Activity activity;

    // Descriptions of DEFCON levels
    private final String[] defconLevels = {
            "DEFCON 0: Normal operation, no restrictions.",
            "DEFCON 1: Low-level restrictions, often just a warning.",
            "DEFCON 2: Moderate restrictions, such as preventing new users from connecting.",
            "DEFCON 3: Increased restrictions, prevent specific commands.",
            "DEFCON 4: High-level restrictions, block more commands.",
            "DEFCON 5: Maximum restrictions, severe threats like widespread attacks."
    };

    public Defcon(Context context, PircBotX bot, Activity activity) {
        this.context = context;
        this.bot = bot;
        this.activity = activity;
    }

    public void startDefconProcess() {
        AlertDialog.Builder defconDialog = new AlertDialog.Builder(context);
        defconDialog.setTitle("Select DEFCON Level");

        // Create a list view for DEFCON levels
        ListView defconListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, defconLevels);
        defconListView.setAdapter(adapter);
        defconDialog.setView(defconListView);

        AlertDialog dialog = defconDialog.create();
        defconListView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedLevel = position;
            dialog.dismiss();

            // Show warning for levels 2 and above
            if (selectedLevel >= 2) {
                showWarningDialog(selectedLevel + 1);
            } else {
                executeDefconCommand(selectedLevel + 1);
            }
        });

        defconDialog.setNegativeButton("Cancel", (d, which) -> d.cancel());

        dialog.show();
    }

    private void showWarningDialog(int level) {
        AlertDialog.Builder warningDialog = new AlertDialog.Builder(context);
        warningDialog.setTitle("Warning")
                .setMessage("This will prevent server connections and affect other server abilities. Continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", (dialog, which) -> executeDefconCommand(level))
                .setNegativeButton("No", (dialog, which) -> dialog.cancel());

        warningDialog.show();
    }

    private void executeDefconCommand(int level) {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    // Send the /os defcon level command
                    bot.sendIRC().message("ChanServ", "/os defcon " + level);

                    // Display feedback to the user in chat that the command was sent
                    activity.runOnUiThread(() -> {
                        Toast.makeText(context, "DEFCON " + level + " command sent", Toast.LENGTH_SHORT).show();
                        // Optionally log or display that the command was sent
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(context, "Failed to execute DEFCON command.", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }).start();
        } else {
            Toast.makeText(context, "Bot is not connected to the server.", Toast.LENGTH_SHORT).show();
        }
    }
}
