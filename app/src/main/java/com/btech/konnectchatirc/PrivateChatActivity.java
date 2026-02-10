package com.btech.konnectchatirc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.PircBotX;

import java.util.ArrayList;
import java.util.List;

public class PrivateChatActivity extends AppCompatActivity implements BotProvider {

    private PircBotX bot;
    private EditText chatEditText;
    private ChatAdapter chatAdapter;
    private List<Object> chatMessages = new ArrayList<>();
    private RecyclerView chatRecyclerView;
    private String userNick;
    private String selectedRecipient;
    private TextView recipientNameTextView;
    private TextView currentNickTextView;
    private PrivateMessageStorage messageStorage;
    private BroadcastReceiver privateMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);

        // Initialize message storage
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        messageStorage = new PrivateMessageStorage(prefs);

        // Get user info from intent (optional, can be updated when selecting conversation)
        userNick = getIntent().getStringExtra("USER_NICK");
        if (userNick == null || userNick.isEmpty()) {
            // Try to get from ChatActivity
            BotProvider botProvider = BotManager.getActiveBotProvider();
            if (botProvider instanceof ChatActivity) {
                userNick = ((ChatActivity) botProvider).getUserNick();
            }
            if (userNick == null) {
                userNick = "Guest";
            }
        }

        // Initialize UI
        initializationViews();

        // Load conversations
        loadConversations();

        // Set up broadcast receiver for incoming private messages
        privateMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");
                
                if (sender != null && !message.isEmpty()) {
                    // Save message to storage
                    messageStorage.saveMessage(userNick, sender, message, false);
                    
                    // If this conversation is selected, update display
                    if (sender.equalsIgnoreCase(selectedRecipient)) {
                        displayConversation(sender);
                    }
                }
            }
        };
    }

    private void initializationViews() {
        recipientNameTextView = findViewById(R.id.RecipientName);
        currentNickTextView = findViewById(R.id.CurrentNick);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatEditText = findViewById(R.id.chatEditText);
        ImageButton sendButton = findViewById(R.id.sendButton);

        // Set user nick in header
        currentNickTextView.setText("You: " + userNick);

        // Setup chat display
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter((BotProvider) this, chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadConversations() {
        // Also check for recipient from intent (when coming from ListUsers)
        String intentRecipient = getIntent().getStringExtra("RECIPIENT_NICK");
        if (intentRecipient != null && !intentRecipient.isEmpty()) {
            // Create an empty conversation if it doesn't exist
            messageStorage.createConversation(userNick, intentRecipient);
            selectedRecipient = intentRecipient;
            displayConversation(selectedRecipient);
        }
    }

    private void displayConversation(String recipient) {
        selectedRecipient = recipient;
        recipientNameTextView.setText("Chat with " + recipient);

        // Load messages for this conversation
        List<PrivateMessageStorage.PrivateMessage> messages = 
                messageStorage.getMessages(userNick, recipient);
        
        chatMessages.clear();
        for (PrivateMessageStorage.PrivateMessage msg : messages) {
            chatMessages.add(msg.sender + ": " + msg.message);
        }
        
        chatAdapter.notifyDataSetChanged();
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    private void sendMessage() {
        if (selectedRecipient == null || selectedRecipient.isEmpty()) {
            Toast.makeText(this, "No conversation selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = chatEditText.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }

        // Get bot from BotManager
        BotProvider botProvider = BotManager.getActiveBotProvider();
        if (botProvider == null) {
            Toast.makeText(this, "Bot not available. Ensure ChatActivity is running.", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        bot = botProvider.getBot();
        if (bot == null || !bot.isConnected()) {
            Toast.makeText(this, "Not connected to IRC server", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send message through IRC
        new Thread(() -> {
            try {
                bot.sendIRC().message(selectedRecipient, message);
                
                // Save to local storage
                messageStorage.saveMessage(userNick, selectedRecipient, message, true);
                
                // Update UI
                runOnUiThread(() -> {
                    displayConversation(selectedRecipient);
                    chatEditText.setText("");
                });
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(PrivateChatActivity.this, "Error sending message: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register to receive private message broadcasts
        try {
            registerReceiver(privateMessageReceiver, new IntentFilter("private_message"), 
                    Context.RECEIVER_EXPORTED);
        } catch (Exception e) {
            // Already registered
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(privateMessageReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }

    public void addChatMessage(String message) {
        runOnUiThread(() -> {
            chatMessages.add(message);
            chatAdapter.notifyDataSetChanged();
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        });
    }

    @Override
    public org.pircbotx.PircBotX getBot() {
        return bot;
    }

    @Override
    public String getActiveChannel() {
        return null;
    }
}
