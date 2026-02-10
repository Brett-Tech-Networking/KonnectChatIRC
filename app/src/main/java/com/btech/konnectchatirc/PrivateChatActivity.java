package com.btech.konnectchatirc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;

import java.util.ArrayList;
import java.util.List;

public class PrivateChatActivity extends AppCompatActivity implements BotProvider, ChannelAdapter.OnChannelClickListener {

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

    // Sidebar fields
    private DrawerLayout drawerLayout;
    private ChannelAdapter channelAdapter;
    private List<ChannelItem> channelList = new ArrayList<>();
    private List<String> privateConversations = new ArrayList<>();
    private PrivateConversationAdapter privateConversationAdapter;
    private ListView privateConversationListView;
    private boolean isViewingPrivateMessages = true;

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

        // Initialize Sidebar
        initializeSidebar();

        // Load conversations
        loadConversations();
        loadPrivateConversationList();

        // Set up broadcast receiver for incoming private messages
        privateMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");
                
                if (sender != null && !message.isEmpty()) {
                    // Save message to storage
                    messageStorage.saveMessage(userNick, sender, message, false);
                    
                    // Add to conversation list if not already there
                    if (!privateConversations.contains(sender)) {
                        privateConversations.add(0, sender);
                        privateConversationAdapter.notifyDataSetChanged();
                    }

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
        ImageButton btnHamburgerMenu = findViewById(R.id.btnHamburgerMenu);
        drawerLayout = findViewById(R.id.drawerLayout);

        // Set user nick in header
        currentNickTextView.setText("You: " + userNick);

        // Setup chat display
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter((BotProvider) this, chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Setup hamburger menu
        btnHamburgerMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void initializeSidebar() {
        // Initialize sidebar components
        privateConversationListView = findViewById(R.id.privateConversationListView);
        Button btnChannelsTab = findViewById(R.id.btnChannelsTab);
        Button btnPrivateMessagesTab = findViewById(R.id.btnPrivateMessagesTab);
        FrameLayout channelsSection = findViewById(R.id.channelsSection);
        FrameLayout privateMessagesSection = findViewById(R.id.privateMessagesSection);
        RecyclerView sidebarChannelRecyclerView = findViewById(R.id.channelRecyclerView);

        // Set up adapter for conversations
        privateConversationAdapter = new PrivateConversationAdapter(this, privateConversations, messageStorage, userNick);
        privateConversationListView.setAdapter(privateConversationAdapter);

        // Set selection listener for conversations
        privateConversationAdapter.setOnConversationSelectListener((nick, position) -> {
            selectedRecipient = nick;
            privateConversationAdapter.setSelectedPosition(position);
            displayConversation(selectedRecipient);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Set delete listener for conversations
        privateConversationAdapter.setOnConversationDeleteListener(nick -> {
            messageStorage.clearConversation(userNick, nick);
        });

        // Set up channel recycler view in sidebar
        channelAdapter = new ChannelAdapter(channelList, this);
        sidebarChannelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sidebarChannelRecyclerView.setAdapter(channelAdapter);

        // Tab switching
        btnChannelsTab.setOnClickListener(v -> switchToChannelsTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));
        btnPrivateMessagesTab.setOnClickListener(v -> switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));

        // Initial tab state (Private Messages tab active since we are in PrivateChatActivity)
        switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection);

        // Load active channels into sidebar if available
        updateSidebarChannels();
    }

    private void updateSidebarChannels() {
        BotProvider botProvider = BotManager.getActiveBotProvider();
        if (botProvider != null) {
            PircBotX activeBot = botProvider.getBot();
            if (activeBot != null && activeBot.isConnected()) {
                channelList.clear();
                for (Channel channel : activeBot.getUserChannelDao().getAllChannels()) {
                    channelList.add(new ChannelItem(channel.getName()));
                }
                channelAdapter.notifyDataSetChanged();
            }
        }
    }

    private void loadPrivateConversationList() {
        privateConversations.clear();
        privateConversations.addAll(messageStorage.getAllConversations(userNick));
        privateConversationAdapter.notifyDataSetChanged();
    }

    private void switchToChannelsTab(Button btnChannelsTab, Button btnPrivateMessagesTab, 
                                     FrameLayout channelsSection, FrameLayout privateMessagesSection) {
        animateSectionTransition(channelsSection, privateMessagesSection);
        
        btnChannelsTab.setBackgroundResource(R.drawable.tab_indicator_active);
        btnChannelsTab.setTextColor(Color.WHITE);
        
        btnPrivateMessagesTab.setBackgroundResource(R.drawable.tab_indicator);
        btnPrivateMessagesTab.setTextColor(getResources().getColor(R.color.tab_text_inactive));
    }

    private void switchToPrivateMessagesTab(Button btnChannelsTab, Button btnPrivateMessagesTab,
                                            FrameLayout channelsSection, FrameLayout privateMessagesSection) {
        animateSectionTransition(privateMessagesSection, channelsSection);
        
        btnPrivateMessagesTab.setBackgroundResource(R.drawable.tab_indicator_active);
        btnPrivateMessagesTab.setTextColor(Color.WHITE);
        
        btnChannelsTab.setBackgroundResource(R.drawable.tab_indicator);
        btnChannelsTab.setTextColor(getResources().getColor(R.color.tab_text_inactive));
        
        loadPrivateConversationList();
    }

    private void animateSectionTransition(FrameLayout showSection, FrameLayout hideSection) {
        hideSection.setVisibility(View.GONE);
        showSection.setVisibility(View.VISIBLE);
        showSection.setAlpha(0.0f);
        showSection.animate().alpha(1.0f).setDuration(200).start();
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
            String content = msg.sender + ": " + msg.message;
            chatMessages.add(new ChatMessage(content, msg.timestamp));
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(privateMessageReceiver, new IntentFilter("private_message"), 
                        Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(privateMessageReceiver, new IntentFilter("private_message"));
            }
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

    @Override
    protected void onResume() {
        super.onResume();
        loadPrivateConversationList();
        updateSidebarChannels();
    }

    public void addChatMessage(String message) {
        runOnUiThread(() -> {
            chatMessages.add(new ChatMessage(message, System.currentTimeMillis()));
            chatAdapter.notifyDataSetChanged();
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        });
    }

    @Override
    public PircBotX getBot() {
        return bot;
    }

    @Override
    public String getActiveChannel() {
        return null;
    }

    @Override
    public void onChannelClick(ChannelItem channel) {
        // Switch to ChatActivity for this channel
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("SELECTED_CHANNEL", channel.getChannelName());
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onLeaveChannelClick(ChannelItem channel) {
        // Handle leaving channel if needed (can broadcast or use BotManager)
        Toast.makeText(this, "Leaving " + channel.getChannelName(), Toast.LENGTH_SHORT).show();
    }
}
