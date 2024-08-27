package com.btech.konnectchatirc;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private PircBotX bot;
    private EditText chatEditText;
    private ChatAdapter chatAdapter;
    private List<String> chatMessages;
    private RecyclerView chatRecyclerView;
    private String userNick;
    private TextView channelNameTextView;
    private View hoverPanel;
    private View operatorPanel;
    private Button operatorButton;
    private ImageButton adminButton;
    private Button btnKill;
    private Button btnOperLogin;
    private Button btnSajoin;
    private Button btnJoin;
    private String activeChannel;
    private final Set<String> processedMessages = new HashSet<>();
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        acquireWakeLock();
        acquireWifiLock();

        Intent serviceIntent = new Intent(this, IrcForegroundService.class);
        startForegroundService(serviceIntent);

        // Retrieve the selected channel from the intent
        String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");

        // Initialize UI components
        channelNameTextView = findViewById(R.id.ChannelName);
        chatEditText = findViewById(R.id.chatEditText);
        adminButton = findViewById(R.id.adminButton);
        ImageButton sendButton = findViewById(R.id.sendButton);
        ImageButton disconnectButton = findViewById(R.id.disconnectButton);

        // Generate a random nickname before initializing the bot
        userNick = "Guest" + (1000 + (int) (Math.random() * 9000));
        initializeBot();

        if (selectedChannel != null) {
            setActiveChannel(selectedChannel);
        } else {
            setActiveChannel("#ThePlaceToChat");
        }


        // Inflate hover panel and operator panel
        LayoutInflater inflater = LayoutInflater.from(this);
        hoverPanel = inflater.inflate(R.layout.hover_panel, null);
        operatorPanel = inflater.inflate(R.layout.operator_panel, null);

        // Get the layout parameters for hoverPanel and operatorPanel
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                (int) (320 * getResources().getDisplayMetrics().density), // Width in pixels
                (int) (550 * getResources().getDisplayMetrics().density)  // Height in pixels
        );


        // Set rules to position hoverPanel and operatorPanel
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);

        // Add top margin to move panels slightly down from the top
        int topMargin = (int) (60 * getResources().getDisplayMetrics().density); // Adjust the value as needed
        params.setMargins(0, topMargin, 0, 0);

        // Add panels to the root of activity_chat.xml
        RelativeLayout rootLayout = findViewById(R.id.rootLayout); // Ensure your activity_chat.xml has an ID for the root layout
        rootLayout.addView(hoverPanel, params);
        rootLayout.addView(operatorPanel, params);

        operatorPanel.setVisibility(View.GONE); // Initially hide operatorPanel

        // Initialize buttons from hover panel
        Button btnNick = hoverPanel.findViewById(R.id.btnNick);
        Button btnJoin = hoverPanel.findViewById(R.id.btnJoin);
        Button btnKick = hoverPanel.findViewById(R.id.btnKick);
        Button btnIdent = hoverPanel.findViewById(R.id.btnIdent);
        operatorButton = hoverPanel.findViewById(R.id.btnOperator);

        // Initialize buttons from operator panel
        btnKill = operatorPanel.findViewById(R.id.btnKill);
        btnOperLogin = operatorPanel.findViewById(R.id.btnOperLogin);
        btnSajoin = operatorPanel.findViewById(R.id.btnSajoin);
        Button btnSapart = operatorPanel.findViewById(R.id.btnSapart);

        // Initialize buttons from hover panel
        Button btnOP = hoverPanel.findViewById(R.id.btnOP);
        Button btnDEOP = hoverPanel.findViewById(R.id.btnDEOP);

        btnOP.setOnClickListener(v -> new UserOP(this, bot, this, hoverPanel).startOPProcess(true));
        btnDEOP.setOnClickListener(v -> new UserOP(this, bot, this, hoverPanel).startOPProcess(false));


        // Initialize buttons from hover panel
        btnJoin = hoverPanel.findViewById(R.id.btnJoin);
        btnJoin.setOnClickListener(v -> new JoinChannel(this, bot, this, hoverPanel).startJoinChannelProcess());

        // Set click listeners for hover panel buttons
        btnNick.setOnClickListener(v -> showNickChangeDialog());
        btnKick.setOnClickListener(v -> new Kick(this, bot, this).startKickProcess());
        btnIdent.setOnClickListener(v -> new Identify(this, bot, this, v).startIdentifyProcess());

        // Set click listener for kill button in operator panel
        btnKill.setOnClickListener(v -> new Kill(this, bot, this).startKillProcess());

        // Set click listener for oper login button in operator panel
        btnOperLogin.setOnClickListener(v -> new OperLogin(this, bot, this).startOperLoginProcess());

        // Set click listener for sajoin button in operator panel
        btnSajoin.setOnClickListener(v -> new Sajoin(this, bot, this).startSajoinProcess());
        // set sapart button
        btnSapart.setOnClickListener(v -> new Sajoin(this, bot, this).startSapartProcess());


        // Operator button functionality KEEP FADEOUT/ FADE IN OR THIS WILL BREAK *****************************************
        operatorButton.setOnClickListener(v -> {
            fadeOutPanel(hoverPanel, () -> fadeInPanel(operatorPanel));
        });

        // Set up hover panel visibility toggle
        adminButton.setOnClickListener(v -> toggleHoverPanel());

        // Set up chat RecyclerView
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Send button functionality
        sendButton.setOnClickListener(v -> {
            String message = chatEditText.getText().toString();
            if (!message.isEmpty()) {
                if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    addChatMessage(userNick + ": " + message);
                    chatEditText.setText("");

                    new Thread(() -> {
                        try {
                            if (isNetworkAvailable()) {
                                bot.sendIRC().message(activeChannel, message);
                                // Comment out or remove this line to keep the active channel as the selected one
                                // resetActiveChannelToDefault();
                            } else {
                                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        });


        // Disconnect button functionality
        disconnectButton.setOnClickListener(v -> {
            if (bot != null) {
                new Thread(() -> {
                    try {
                        bot.sendIRC().quitServer("good bye");
                        bot.close();
                        runOnUiThread(this::finish);
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(this::finish); //goes back to home page even if already disconnected
                    }
                }).start();
            }
        });

        // Start connection to IRC server
        connectToIrcServer();
    }

    private void initializeBot() {
        if (bot == null) {
            String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");
            // If no channel was selected, fallback to the default channel
            if (selectedChannel == null || selectedChannel.isEmpty()) {
                selectedChannel = "#ThePlaceToChat";
            }

            Configuration configuration = new Configuration.Builder()
                    .setName(userNick) // Set the bot's name
                    .setAutoNickChange(true) //Automatically change nick when the current one is in use
                    .setRealName("TPTC IRC Client")
                    .addServer("irc.theplacetochat.net", 6667) // Set the server and port
                    .addAutoJoinChannel(selectedChannel)
                    .addListener(new Listeners(this)) // Pass this ChatActivity instance to Listeners
                    .addListener(new NickChangeListener(this)) // Add the custom listener
                    .buildConfiguration();

            bot = new PircBotX(configuration);
            setActiveChannel(selectedChannel); // Ensure the active channel is set properly
        }
    }

    private void connectToIrcServer() {
        new Thread(() -> {
            try {
                // Start the bot and connect to the server
                bot.startBot();

                // Retrieve the selected channel from the intent
                String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");

                if (selectedChannel != null && !selectedChannel.isEmpty()) {
                    try {
                        // Attempt to join the selected channel
                        bot.sendIRC().joinChannel(selectedChannel);
                        runOnUiThread(() -> {
                            addChatMessage("Joining channel: " + selectedChannel);
                            setActiveChannel(selectedChannel);
                        });
                    } catch (IllegalArgumentException e) {
                        // Handle the exception when not connected to the server
                        runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "You have been disconnected from the server, Please try again", Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "No channel selected.", Toast.LENGTH_LONG).show();
                    });
                }

            } catch (IOException | IrcException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error connecting to IRC server. Please try again later.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    public String getRequestedNick() {
        return requestedNick;
    }

    public void setNickInputToRetry() {
        chatEditText.setText("/nick "); // Set the text to "/nick "
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void addChatMessage(String message) {
        chatMessages.add(message);
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public String getUserNick() {
        return userNick;
    }

    public String getActiveChannel() {
        return activeChannel;
    }

    public void setActiveChannel(String channel) {
        this.activeChannel = channel;
        updateChannelName(channel);
    }

    public void resetActiveChannelToDefault() {
      //  setActiveChannel("#ThePlaceToChat");
    }

    private void handleCommand(String command) {
        String commandText = command.substring(1);
        String[] parts = commandText.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (commandName) {
            case "nick":
                changeNick(args);
                break;
            case "join":
                joinChannel(args);
                break;
            default:
                addChatMessage("Unknown command: " + commandName);
                break;
        }
    }

    private String requestedNick; // Variable to keep track of the requested nickname

    private void changeNick(String newNick) {
        if (newNick.isEmpty()) {
            addChatMessage("Usage: /nick <new_nick>");
            chatEditText.setText(""); // Clear input
            return;
        }

        requestedNick = newNick; // Set the requested nickname

        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        // Send the nickname change command to the IRC server
                        bot.sendRaw().rawLine("NICK " + newNick);
                        runOnUiThread(() -> {
                            // Update local nickname and UI
                            updateLocalNick(newNick);
                            chatEditText.setText(""); // Clear input
                        });
                    } else {
                        runOnUiThread(() -> addChatMessage("Bot is not connected to the server."));
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to change nickname.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void updateLocalNick(String newNick) {
        userNick = newNick; // Update local nickname
    }

    public void joinChannel(String channel) {
        if (channel.isEmpty()) {
            addChatMessage("Usage: /join <channel>");
            chatEditText.setText("");
            return;
        }
        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        bot.sendIRC().joinChannel(channel);
                        runOnUiThread(() -> {
                            addChatMessage("Joining channel: " + channel);
                            setActiveChannel(channel);
                            chatEditText.setText("");

                        });
                    } else {
                        runOnUiThread(() -> addChatMessage("Bot is not connected to the server."));
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }





    public void updateChannelName(String channelName) {
        runOnUiThread(() -> channelNameTextView.setText(channelName));
    }

    private void toggleHoverPanel() {
        if (operatorPanel.getVisibility() == View.VISIBLE) {
            fadeOutPanel(operatorPanel, null);
        } else if (hoverPanel.getVisibility() == View.GONE) {
            fadeInPanel(hoverPanel);
        } else {
            fadeOutPanel(hoverPanel, null);
        }
    }

    private void fadeInPanel(View panel) {
        if (panel.getVisibility() == View.GONE) {
            panel.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            panel.startAnimation(fadeIn);
        }
    }

    private void fadeOutPanel(View panel, Runnable onAnimationEnd) {
        if (panel.getVisibility() == View.VISIBLE) {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // Optional: any action before animation starts
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    panel.setVisibility(View.GONE);
                    if (onAnimationEnd != null) {
                        onAnimationEnd.run();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // Not used
                }
            });
            panel.startAnimation(fadeOut);
        } else {
            // If the panel is already gone, just run the end action
            if (onAnimationEnd != null) {
                onAnimationEnd.run();
            }
        }
    }

    private void showNickChangeDialog() {
        // Create an AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Nickname");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newNick = input.getText().toString().trim();
            if (!newNick.isEmpty()) {
                changeNick(newNick);
                hoverPanel.setVisibility(View.GONE);
            } else {
                Toast.makeText(ChatActivity.this, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
    }

    public boolean hasMessageBeenProcessed(String message) {
        return processedMessages.contains(message);
    }

    public void markMessageAsProcessed(String message) {
        processedMessages.add(message);
    }

    public void processServerMessage(String sender, String message) {
        if (message.startsWith("005")) {
            return; // Do nothing if it's a 005 message
        }
        runOnUiThread(() -> addChatMessage(sender + ": " + message));
    }
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChatActivity::WakeLock");
        wakeLock.acquire();
        Log.d("WakeLock", "WakeLock acquired");
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("WakeLock", "WakeLock released");
        }
    }

    private void acquireWifiLock() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ChatActivity::WifiLock");
        wifiLock.acquire();
        Log.d("WifiLock", "WifiLock acquired");
    }

    private void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            Log.d("WifiLock", "WifiLock released");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the foreground service to ensure bot disconnection
        Intent stopServiceIntent = new Intent(this, IrcForegroundService.class);
        stopService(stopServiceIntent);

        // Ensure bot disconnects properly
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendIRC().quitServer("App closed");
                    bot.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

}
