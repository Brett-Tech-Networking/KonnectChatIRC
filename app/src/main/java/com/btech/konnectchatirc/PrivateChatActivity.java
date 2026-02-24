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
    private BroadcastReceiver nickChangeReceiver;

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

        // Get user info from intent
        userNick = getIntent().getStringExtra("USER_NICK");
        if (userNick == null || userNick.isEmpty()) {
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
                // Check both "nick" and "sender" keys based on logs
                String sender = intent.getStringExtra("nick");
                if (sender == null) sender = intent.getStringExtra("sender");

                String message = intent.getStringExtra("message");

                if (sender != null && message != null && !message.isEmpty()) {
                    Log.d("PrivateChat", "Received message from: " + sender);
                    messageStorage.saveMessage(userNick, sender, message, false);

                    if (selectedRecipient == null || !selectedRecipient.equalsIgnoreCase(sender)) {
                        messageStorage.incrementUnreadCount(userNick, sender);
                    }

                    final String finalSender = sender;
                    runOnUiThread(() -> {
                        privateConversations.remove(finalSender);
                        privateConversations.add(0, finalSender);
                        privateConversationAdapter.notifyDataSetChanged();

                        if (selectedRecipient != null && finalSender.equalsIgnoreCase(selectedRecipient)) {
                            displayConversation(finalSender, false);
                        }
                        updateGlobalUnreadCount();
                    });
                }
            }
        };

        channelMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.btech.konnectchatirc.CHANNEL_MESSAGE".equals(intent.getAction())) {
                    String channelName = intent.getStringExtra("channel");
                    if (channelName != null) {
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

        nickChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.btech.konnectchatirc.NICK_CHANGED".equals(intent.getAction())) {
                    String newNick = intent.getStringExtra("new_nick");
                    if (newNick != null) {
                        userNick = newNick;
                        runOnUiThread(() -> {
                            currentNickTextView.setText("You: " + userNick);
                            loadPrivateConversationList();
                        });
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

        currentNickTextView.setText("You: " + userNick);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter((BotProvider) this, chatMessages);

        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        chatAdapter.setShowTimestamps(prefs.getBoolean("show_timestamps", false));
        chatAdapter.setTimestampFormat(prefs.getString("timestamp_format", "HH:mm"));

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatRecyclerView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob) chatRecyclerView.post(() -> scrollToBottom(true));
        });

        sendButton.setOnClickListener(v -> sendMessage());
        btnHamburgerMenu.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });

        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Required by other classes (Kill, ListUsers, Zline) to display status/system messages
     */
    public void addChatMessage(String message) {
        runOnUiThread(() -> {
            chatMessages.add(new ChatMessage(message, System.currentTimeMillis()));
            chatAdapter.notifyDataSetChanged();
            scrollToBottom(false);
        });
    }

    private void initializeSidebar() {
        privateConversationListView = findViewById(R.id.privateConversationListView);
        Button btnChannelsTab = findViewById(R.id.btnChannelsTab);
        Button btnPrivateMessagesTab = findViewById(R.id.btnPrivateMessagesTab);
        FrameLayout channelsSection = findViewById(R.id.channelsSection);
        FrameLayout privateMessagesSection = findViewById(R.id.privateMessagesSection);
        RecyclerView sidebarChannelRecyclerView = findViewById(R.id.channelRecyclerView);

        privateConversationAdapter = new PrivateConversationAdapter(this, privateConversations, messageStorage, userNick);
        privateConversationListView.setAdapter(privateConversationAdapter);

        privateConversationAdapter.setOnConversationSelectListener((nick, position) -> {
            selectedRecipient = nick;
            privateConversationAdapter.setSelectedPosition(position);
            displayConversation(selectedRecipient, true);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        privateConversationAdapter.setOnConversationDeleteListener(nick -> {
            messageStorage.clearConversation(userNick, nick);
            updateGlobalUnreadCount();
        });

        channelAdapter = new ChannelAdapter(channelList, this);
        sidebarChannelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sidebarChannelRecyclerView.setAdapter(channelAdapter);

        btnChannelsTab.setOnClickListener(v -> switchToChannelsTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));
        btnPrivateMessagesTab.setOnClickListener(v -> switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));

        switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection);
        updateSidebarChannels();
    }

    private void updateSidebarChannels() {
        BotProvider bp = BotManager.getActiveBotProvider();
        if (bp != null && bp.getBot() != null && bp.getBot().isConnected()) {
            channelList.clear();
            for (Channel channel : bp.getBot().getUserChannelDao().getAllChannels()) {
                ChannelItem newItem = new ChannelItem(channel.getName());
                if (channelStorage != null) newItem.setUnreadCount(channelStorage.getUnreadCount(channel.getName()));
                channelList.add(newItem);
            }
            channelAdapter.notifyDataSetChanged();
        }
    }

    private void loadPrivateConversationList() {
        privateConversations.clear();
        List<String> convs = messageStorage.getAllConversations(userNick);
        Collections.sort(convs, (c1, c2) -> {
            List<PrivateMessageStorage.PrivateMessage> m1 = messageStorage.getMessages(userNick, c1);
            List<PrivateMessageStorage.PrivateMessage> m2 = messageStorage.getMessages(userNick, c2);
            long t1 = m1.isEmpty() ? 0 : m1.get(m1.size() - 1).timestamp;
            long t2 = m2.isEmpty() ? 0 : m2.get(m2.size() - 1).timestamp;
            return Long.compare(t2, t1);
        });
        privateConversations.addAll(convs);
        privateConversationAdapter.notifyDataSetChanged();
    }

    private void switchToChannelsTab(Button ch, Button pm, FrameLayout chS, FrameLayout pmS) {
        animateSectionTransition(chS, pmS);
        ch.setBackgroundResource(R.drawable.tab_indicator_active);
        ch.setTextColor(Color.WHITE);
        pm.setBackgroundResource(R.drawable.tab_indicator);
        pm.setTextColor(getResources().getColor(R.color.tab_text_inactive));
    }

    private void switchToPrivateMessagesTab(Button ch, Button pm, FrameLayout chS, FrameLayout pmS) {
        animateSectionTransition(pmS, chS);
        pm.setBackgroundResource(R.drawable.tab_indicator_active);
        pm.setTextColor(Color.WHITE);
        ch.setBackgroundResource(R.drawable.tab_indicator);
        ch.setTextColor(getResources().getColor(R.color.tab_text_inactive));
        loadPrivateConversationList();
    }

    private void animateSectionTransition(FrameLayout show, FrameLayout hide) {
        hide.setVisibility(View.GONE);
        show.setVisibility(View.VISIBLE);
        show.setAlpha(0.0f);
        show.animate().alpha(1.0f).setDuration(200).start();
    }

    private void loadConversations() {
        String intentRecipient = getIntent().getStringExtra("RECIPIENT_NICK");
        if (intentRecipient != null && !intentRecipient.isEmpty()) {
            messageStorage.createConversation(userNick, intentRecipient);
            selectedRecipient = intentRecipient;
            displayConversation(selectedRecipient, true);
        }
    }

    private void displayConversation(String recipient, boolean force) {
        selectedRecipient = recipient;
        recipientNameTextView.setText("Chat with " + recipient);
        messageStorage.resetUnreadCount(userNick, recipient);
        updateGlobalUnreadCount();

        List<PrivateMessageStorage.PrivateMessage> messages = messageStorage.getMessages(userNick, recipient);
        chatMessages.clear();
        for (PrivateMessageStorage.PrivateMessage msg : messages) {
            chatMessages.add(new ChatMessage(msg.sender + ": " + msg.message, msg.timestamp));
        }
        chatAdapter.notifyDataSetChanged();
        scrollToBottom(force);
    }

    private void scrollToBottom(boolean force) {
        if (chatMessages.isEmpty()) return;
        LinearLayoutManager lm = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (lm != null) {
            int last = lm.findLastVisibleItemPosition();
            if (force || last >= lm.getItemCount() - 3 || last == -1) {
                chatRecyclerView.post(() -> chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1));
            }
        }
    }

    private void sendMessage() {
        if (selectedRecipient == null || selectedRecipient.isEmpty()) return;
        String message = chatEditText.getText().toString().trim();
        if (message.isEmpty()) return;

        BotProvider bp = BotManager.getActiveBotProvider();
        if (bp == null || bp.getBot() == null || !bp.getBot().isConnected()) return;

        new Thread(() -> {
            try {
                bp.getBot().sendIRC().message(selectedRecipient, message);
                messageStorage.saveMessage(userNick, selectedRecipient, message, true);
                runOnUiThread(() -> {
                    privateConversations.remove(selectedRecipient);
                    privateConversations.add(0, selectedRecipient);
                    privateConversationAdapter.notifyDataSetChanged();
                    displayConversation(selectedRecipient, true);
                    chatEditText.setText("");
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            ContextCompat.registerReceiver(this, privateMessageReceiver, new IntentFilter("private_message"), ContextCompat.RECEIVER_NOT_EXPORTED);
            ContextCompat.registerReceiver(this, channelMessageReceiver, new IntentFilter("com.btech.konnectchatirc.CHANNEL_MESSAGE"), ContextCompat.RECEIVER_NOT_EXPORTED);
            ContextCompat.registerReceiver(this, nickChangeReceiver, new IntentFilter("com.btech.konnectchatirc.NICK_CHANGED"), ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(privateMessageReceiver);
            unregisterReceiver(channelMessageReceiver);
            if (nickChangeReceiver != null) unregisterReceiver(nickChangeReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrivateConversationList();
        updateGlobalUnreadCount();
    }

    @Override public PircBotX getBot() { return bot; }

    private void updateGlobalUnreadCount() {
        int total = 0;
        for (String r : privateConversations) total += messageStorage.getUnreadCount(userNick, r);
        if (channelStorage != null) {
            for (ChannelItem i : channelList) total += channelStorage.getUnreadCount(i.getChannelName());
        }
        unreadBadge.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        if (total > 0) {
            unreadBadge.setText(String.valueOf(total));
            unreadBadge.bringToFront();
        }
    }

    @Override public String getActiveChannel() { return null; }
    @Override public void onChannelClick(ChannelItem c) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("SELECTED_CHANNEL", c.getChannelName());
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }
    @Override public void onLeaveChannelClick(ChannelItem c) {}
}