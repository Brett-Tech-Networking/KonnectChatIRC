package com.btech.konnectchatirc;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
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
    private ImageButton adminButton;
    private Button btnKick;
    private String activeChannel;
    private final Set<String> processedMessages = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Generate a random nickname before initializing the bot
        userNick = "Guest" + (1000 + (int) (Math.random() * 9000));
        initializeBot();

        // Initialize UI components
        adminButton = findViewById(R.id.adminButton);
        channelNameTextView = findViewById(R.id.ChannelName);
        chatEditText = findViewById(R.id.chatEditText);
        Button sendButton = findViewById(R.id.sendButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);

        // Inflate and set up hover panel
        LayoutInflater inflater = LayoutInflater.from(this);
        hoverPanel = inflater.inflate(R.layout.hover_panel, null);

        // Get the layout parameters for hoverPanel
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                (int) (300 * getResources().getDisplayMetrics().density), // Width in pixels
                (int) (450 * getResources().getDisplayMetrics().density)  // Height in pixels
        );

        // Set rules to position hoverPanel
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);

        // Add top margin to move hoverPanel slightly down from the top
        int topMargin = (int) (60 * getResources().getDisplayMetrics().density); // Adjust the value as needed
        params.setMargins(0, topMargin, 0, 0);

        // Add hoverPanel to the root of activity_chat.xml
        RelativeLayout rootLayout = findViewById(R.id.rootLayout); // Ensure your activity_chat.xml has an ID for the root layout
        rootLayout.addView(hoverPanel, params);

        // Initialize Kick class and set up button click listener
        Kick kickHandler = new Kick(this, bot, this);

        // Initialize Kill class and set up button click listener
        Kill killHandler = new Kill(this, bot, this);

        // Initialize Identify class and set up button click listener
        Identify identifyHandler = new Identify(this, bot, this);

        // Find buttons inside hoverPanel
        Button btnNick = hoverPanel.findViewById(R.id.btnNick);
        Button btnJoin = hoverPanel.findViewById(R.id.btnJoin);
        Button btnKill = hoverPanel.findViewById(R.id.btnKill);
        Button btnBan = hoverPanel.findViewById(R.id.btnBan);
        btnKick = hoverPanel.findViewById(R.id.btnKick); // Initialize btnKick from hoverPanel
        Button btnIdent = hoverPanel.findViewById(R.id.btnIdent);

        btnNick.setOnClickListener(v -> showNickChangeDialog());
        btnKick.setOnClickListener(v -> kickHandler.startKickProcess());
        btnKill.setOnClickListener(v -> killHandler.startKillProcess());
        btnIdent.setOnClickListener(v -> identifyHandler.startIdentifyProcess());

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
                        bot.sendIRC().quitServer("Brett Tech Client");
                        bot.close();
                        runOnUiThread(this::finish);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });

        // Start connection to IRC server
        connectToIrcServer();
    }

    private void initializeBot() {
        if (bot == null) {
            Configuration configuration = new Configuration.Builder()
                    .setName(userNick) // Set the bot's name
                    .addServer("irc.theplacetochat.net") // Set the server
                    .addAutoJoinChannel("#ThePlaceToChat") // Set the channel
                    .setServerPort(6667) // IRC port, adjust as needed
                    .setRealName("TPTC IRC Client")
                    .addListener(new Listeners(this)) // Pass this ChatActivity instance to Listeners
                    .addListener(new NickChangeListener(this)) // Add the custom listener
                    .buildConfiguration();

            bot = new PircBotX(configuration);
        }
    }

    private void connectToIrcServer() {
        new Thread(() -> {
            try {
                bot.startBot();
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

    private void joinChannel(String channel) {
        if (channel.isEmpty()) {
            addChatMessage("Usage: /join <channel>");
            chatEditText.setText("");
            return;
        }
        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    bot.sendIRC().joinChannel(channel);
                    runOnUiThread(() -> addChatMessage("Joined channel: " + channel));
                    activeChannel = channel;
                    updateChannelName(channel);
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
        if (hoverPanel.getVisibility() == View.GONE) {
            hoverPanel.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            hoverPanel.startAnimation(fadeIn);
        } else {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    hoverPanel.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            hoverPanel.startAnimation(fadeOut);
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


}
