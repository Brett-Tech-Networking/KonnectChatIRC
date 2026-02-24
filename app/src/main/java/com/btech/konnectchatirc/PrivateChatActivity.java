package com.btech.konnectchatirc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
    private ChannelStorage channelStorage;
    private BroadcastReceiver privateMessageReceiver;
    private BroadcastReceiver channelMessageReceiver;

    // Sidebar fields
    private DrawerLayout drawerLayout;
    private ChannelAdapter channelAdapter;
    private List<ChannelItem> channelList = new ArrayList<>();
    private List<String> privateConversations = new ArrayList<>();
    private PrivateConversationAdapter privateConversationAdapter;
    private ListView privateConversationListView;
    private boolean isViewingPrivateMessages = true;
    private TextView unreadBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);

        // Initialize message storage
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        messageStorage = new PrivateMessageStorage(prefs);
        channelStorage = new ChannelStorage(prefs);

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
                    
                    // Move/Add to conversation list (always move to top)
                    runOnUiThread(() -> {
                        privateConversations.remove(sender);
                        privateConversations.add(0, sender);
                        privateConversationAdapter.notifyDataSetChanged();

                        // If this conversation is selected, update display
                        if (sender.equalsIgnoreCase(selectedRecipient)) {
                            displayConversation(sender, false);
                        }
                        
                        updateGlobalUnreadCount();
                    });
                }
            }
        };
        
        // Set up broadcast receiver for channel messages
        channelMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("NotificationDebug", "PrivateChatActivity: Broadcast received: " + intent.getAction());
                if ("com.btech.konnectchatirc.CHANNEL_MESSAGE".equals(intent.getAction())) {
                    String channelName = intent.getStringExtra("channel");
                    Log.d("NotificationDebug", "PrivateChatActivity: Channel message for: " + channelName);
                    if (channelName != null) {
                        // Update channel list item
                        for (ChannelItem item : channelList) {
                            if (item.getChannelName().equalsIgnoreCase(channelName)) {
                                if (channelStorage != null) {
                                     item.setUnreadCount(channelStorage.getUnreadCount(channelName));
                                } else {
                                     item.incrementUnreadCount(); 
                                }
                                break;
                            }
                        }
                        channelAdapter.notifyDataSetChanged();
                        
                        updateGlobalUnreadCount();
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
        unreadBadge = findViewById(R.id.unreadBadge);
        drawerLayout = findViewById(R.id.drawerLayout);

        // Set user nick in header
        currentNickTextView.setText("You: " + userNick);

        // Setup chat display
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter((BotProvider) this, chatMessages);
        
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        boolean showTimestamps = prefs.getBoolean("show_timestamps", false);
        String timestampFormat = prefs.getString("timestamp_format", "HH:mm");
        chatAdapter.setShowTimestamps(showTimestamps);
        chatAdapter.setTimestampFormat(timestampFormat);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Add layout change listener for keyboard handling
        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                // Keyboard likely opened (height decreased)
                chatRecyclerView.post(() -> scrollToBottom(true));
            }
        });

        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Setup upload button (placeholder for future implementation)
        ImageButton uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> {
            // TODO: Implement file upload for private messages
        });

        btnHamburgerMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Apply WindowInsets for Edge-to-Edge
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
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
            displayConversation(selectedRecipient, true);
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
                    ChannelItem newItem = new ChannelItem(channel.getName());
                    // Sync with storage if available
                    if (channelStorage != null) {
                        newItem.setUnreadCount(channelStorage.getUnreadCount(channel.getName()));
                    }
                    channelList.add(newItem);
                }
                channelAdapter.notifyDataSetChanged();
            }
        }
    }

    private void loadPrivateConversationList() {
        privateConversations.clear();
        List<String> convs = messageStorage.getAllConversations(userNick);
        
        // Sort conversations by last message timestamp (newest first)
        Collections.sort(convs, (c1, c2) -> {
            List<PrivateMessageStorage.PrivateMessage> m1 = messageStorage.getMessages(userNick, c1);
            List<PrivateMessageStorage.PrivateMessage> m2 = messageStorage.getMessages(userNick, c2);
            long t1 = m1.isEmpty() ? 0 : m1.get(m1.size() - 1).timestamp;
            long t2 = m2.isEmpty() ? 0 : m2.get(m2.size() - 1).timestamp;
            return Long.compare(t2, t1); // Newest first
        });
        
        privateConversations.addAll(convs);
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
            displayConversation(selectedRecipient, true);
        }
    }

    private void displayConversation(String recipient, boolean forceScroll) {
        selectedRecipient = recipient;
        recipientNameTextView.setText("Chat with " + recipient);

        // Reset unread count for this conversation
        messageStorage.resetUnreadCount(userNick, recipient);
        updateGlobalUnreadCount();

        // Load messages for this conversation
        List<PrivateMessageStorage.PrivateMessage> messages = 
                messageStorage.getMessages(userNick, recipient);
        
        chatMessages.clear();
        for (PrivateMessageStorage.PrivateMessage msg : messages) {
            String content = msg.sender + ": " + msg.message;
            chatMessages.add(new ChatMessage(content, msg.timestamp));
        }
        
        chatAdapter.notifyDataSetChanged();
        scrollToBottom(forceScroll);
    }

    private void scrollToBottom(boolean force) {
        if (chatMessages.isEmpty()) return;
        if (force) {
             chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
             return;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            int itemCount = layoutManager.getItemCount();
            
            // Allow scrolling if we are near the bottom (within last 3 items) or if it's the very first load
            boolean isAtBottom = (lastVisibleItemPosition >= itemCount - 3) || lastVisibleItemPosition == -1;

            if (isAtBottom) {
                chatRecyclerView.post(() -> chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1));
            }
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
            Log.e("PrivateChat", "Bot is null or not connected! Bot: " + bot);
            Toast.makeText(this, "Not connected to IRC server", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send message through IRC
        new Thread(() -> {
            try {
                Log.d("PrivateChat", "Sending message to " + selectedRecipient + ": " + message);
                bot.sendIRC().message(selectedRecipient, message);
                
                // Save to local storage
                messageStorage.saveMessage(userNick, selectedRecipient, message, true);
                
                // Update UI
                runOnUiThread(() -> {
                    // Move conversation to top
                    privateConversations.remove(selectedRecipient);
                    privateConversations.add(0, selectedRecipient);
                    privateConversationAdapter.notifyDataSetChanged();
                    privateConversationAdapter.setSelectedPosition(0);
                    
                    displayConversation(selectedRecipient, true);
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
            ContextCompat.registerReceiver(this, privateMessageReceiver, new IntentFilter("private_message"), ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception e) {
            // Already registered
        }
        
        // Register channel message receiver
        try {
            ContextCompat.registerReceiver(this, channelMessageReceiver, new IntentFilter("com.btech.konnectchatirc.CHANNEL_MESSAGE"), ContextCompat.RECEIVER_NOT_EXPORTED);
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
        
        try {
            unregisterReceiver(channelMessageReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrivateConversationList();
        updateSidebarChannels();
        updateGlobalUnreadCount();
    }

    public void addChatMessage(String message) {
        runOnUiThread(() -> {
            chatMessages.add(new ChatMessage(message, System.currentTimeMillis()));
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(false);
        });
    }

    @Override
    public PircBotX getBot() {
        return bot;
    }

    private void updateGlobalUnreadCount() {
        int totalUnread = 0;
        
        // Count unread private messages
        for (String recipient : privateConversations) {
            totalUnread += messageStorage.getUnreadCount(userNick, recipient);
        }
        
        // Count unread channel messages
        if (channelStorage != null) {
            for (ChannelItem item : channelList) {
                totalUnread += channelStorage.getUnreadCount(item.getChannelName());
            }
        }
        
        if (totalUnread > 0) {
            Log.d("NotificationDebug", "PrivateChatActivity: Updating badge to " + totalUnread);
            unreadBadge.setText(String.valueOf(totalUnread));
            unreadBadge.setVisibility(View.VISIBLE);
            unreadBadge.bringToFront(); // Ensure it's on top
        } else {
            Log.d("NotificationDebug", "PrivateChatActivity: Hiding badge");
            unreadBadge.setVisibility(View.GONE);
        }
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
