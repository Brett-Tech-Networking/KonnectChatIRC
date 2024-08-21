package com.btech.konnectchatirc;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import android.widget.TextView;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private PircBotX bot;
    private EditText chatEditText;
    private ChatAdapter chatAdapter;
    private List<String> chatMessages;
    private RecyclerView chatRecyclerView;
    private String userNick;
    private TextView channelNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatEditText = findViewById(R.id.chatEditText);
        Button sendButton = findViewById(R.id.sendButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);
        channelNameTextView = findViewById(R.id.ChannelName);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Generate a random nickname
        userNick = "Guest" + (1000 + (int) (Math.random() * 9000));

        sendButton.setOnClickListener(v -> {
            String message = chatEditText.getText().toString();
            if (!message.isEmpty()) {
                if (message.startsWith("/")) {
                    // Handle command
                    handleCommand(message);
                } else {
                    // Handle regular message
                    addChatMessage(userNick + ": " + message);
                    chatEditText.setText("");

                    // Send message to IRC server
                    new Thread(() -> {
                        try {
                            if (isNetworkAvailable()) {
                                bot.sendIRC().message("#ThePlaceToChat", message);
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

        // Connect to IRC server
        new Thread(this::connectToIrcServer).start();
    }
    public String getRequestedNick() {
        return requestedNick;
    }
    public void setNickInputToRetry() {
        chatEditText.setText("/nick "); // Set the text to "/nick "
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void connectToIrcServer() {
        if (!isNetworkAvailable()) {
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No internet connection. Please check your settings.", Toast.LENGTH_LONG).show());
            return;
        }

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
        new Thread(() -> {
            try {
                bot.startBot();
            } catch (IOException | IrcException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error connecting to IRC server. Please try again later.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void addChatMessage(String message) {
        chatMessages.add(message);
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public String getUserNick() {
        return userNick;
    }
    private void handleCommand(String command) {
        // Remove leading slash
        String commandText = command.substring(1);

        // Split command and arguments
        String[] parts = commandText.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (commandName) {
            case "nick":
                // Change nickname command
                changeNick(args);
                break;
            case "join":
                // Join channel command
                joinChannel(args);
                break;
            // Add more commands as needed
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

        // Change nickname
        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        // Send the nickname change request
                        bot.sendRaw().rawLine("NICK " + newNick);
                        chatEditText.setText(""); // Clear input
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
        runOnUiThread(() -> addChatMessage("Nickname changed to: " + newNick));
    }


    private void joinChannel(String channel) {
        if (channel.isEmpty()) {
            addChatMessage("Usage: /join <channel>");
            chatEditText.setText("");
            return;
        }
        // Join channel
        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    bot.sendIRC().joinChannel(channel);
                    runOnUiThread(() -> addChatMessage("Joined channel: " + channel));
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public void updateChannelName(String channelName) {
        runOnUiThread(() -> {
            channelNameTextView.setText(channelName);
        });
    }
}
