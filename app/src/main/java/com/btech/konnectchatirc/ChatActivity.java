package com.btech.konnectchatirc;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import android.content.Context;
import android.graphics.Color;
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

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = chatEditText.getText().toString();
                if (!message.isEmpty()) {
                    chatMessages.add("Me: " + message);
                    chatAdapter.notifyDataSetChanged();
                    chatEditText.setText("");

                    // Send message to IRC server
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (isNetworkAvailable()) {
                                    bot.sendIRC().message("#ThePlaceToChat", message);
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ChatActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Handle message sending failure
                            }
                        }
                    }).start();
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bot != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                bot.sendIRC().quitServer("Brett Tech Client");
                                bot.close(); // Closes the bot connection
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish(); // Ensure this is called on the main thread
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Handle disconnection failure
                            }
                        }
                    }).start();
                }
            }
        });

        // Connect to IRC server
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectToIrcServer();
            }
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void connectToIrcServer() {
        if (!isNetworkAvailable()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ChatActivity.this, "No internet connection. Please check your settings.", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        Configuration configuration = new Configuration.Builder()
                .setName("CoolDudeBot") // Set the bot's name
                .addServer("irc.theplacetochat.net") // Set the server
                .addAutoJoinChannel("#ThePlaceToChat") // Set the channel
                .setServerPort(6667) // IRC port, adjust as needed
                .addListener(new ListenerAdapter() {
                    @Override
                    public void onMessage(MessageEvent event) {
                        // Handle incoming messages
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chatMessages.add(event.getUser().getNick() + ": " + event.getMessage());
                                chatAdapter.notifyDataSetChanged();
                                chatRecyclerView.scrollToPosition(chatMessages.size() - 1); // Scroll to the latest message
                            }
                        });
                    }
                })
                .buildConfiguration();

        bot = new PircBotX(configuration);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bot.startBot();
                } catch (IOException | IrcException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChatActivity.this, "Error connecting to IRC server. Please try again later.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}
