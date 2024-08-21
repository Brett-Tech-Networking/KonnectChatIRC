package com.btech.konnectchatirc;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatEditText = findViewById(R.id.chatEditText);
        Button sendButton = findViewById(R.id.sendButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Generate a random nickname
        userNick = "Guest" + (1000 + (int) (Math.random() * 9000));

        sendButton.setOnClickListener(v -> {
            String message = chatEditText.getText().toString();
            if (!message.isEmpty()) {
                addChatMessage(userNick + ": " + message); // Display user's own message
                chatEditText.setText("");

                // Send message to IRC server
                new Thread(() -> {
                    try {
                        if (isNetworkAvailable()) {
                            bot.sendIRC().message("#ThePlaceToChat", message); // Send message without nickname
                        } else {
                            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
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
                .setRealName("BrettTechClient")
                .addListener(new Listeners(this)) // Pass this ChatActivity instance to Listeners
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
}
