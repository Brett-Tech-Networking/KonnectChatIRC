package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.DaoException;
import org.pircbotx.exception.IrcException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private PircBotX bot;
    private EditText chatEditText;
    private ChatAdapter chatAdapter;
    private List<Object> chatMessages = new ArrayList<>();
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
    private static final int PICK_IMAGE_REQUEST = 1;
    private OkHttpClient client;
    private static final String IMGUR_CLIENT_ID = "4968ca92805f1b2"; // Replace with your Imgur client ID
    private ConnectivityManager connectivityManager;
    private DrawerLayout drawerLayout;
    private ChannelAdapter channelAdapter;
    private List<ChannelItem> channelList = new ArrayList<>(); // List to hold channel items
    private Map<String, List<String>> channelMessagesMap = new HashMap<>(); // Stores messages for each channel
    private TextView unreadBadge;
    private int totalUnreadMessages = 0;

    // Add bannedUsers list
    private List<String> bannedUsers = new ArrayList<>();

    // Define networkCallback once at the class level
    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d("NetworkCallback", "Network available, attempting to connect.");
            if (bot == null || !bot.isConnected()) {
                connectToIrcServer();
            } else {
                // Bind bot to the current active network
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.bindProcessToNetwork(network);
                } else {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }
                Log.d("NetworkCallback", "Bound to network: " + network.toString());
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d("NetworkCallback", "Network lost.");
            // Handle lost network if necessary, retry connection, etc.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_chat);
        acquireWakeLock();
        acquireWifiLock();

        unreadBadge = findViewById(R.id.unreadBadge);
        drawerLayout = findViewById(R.id.drawerLayout);
        RelativeLayout rootLayout = findViewById(R.id.rootLayout);

        // Initialize ConnectivityManager here
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        client = new OkHttpClient(); // Initialize OkHttpClient

        // Initialize the RecyclerView for chat messages
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);  // Ensure the adapter is set here

        Intent serviceIntent = new Intent(this, IrcForegroundService.class);
        serviceIntent.putExtra("#ThePlaceToChat", activeChannel);
        startForegroundService(serviceIntent);

        ImageButton uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> {
            if (bot != null && bot.isConnected()) {
                if (activeChannel != null && bot.getUserChannelDao().containsChannel(activeChannel)) {
                    // Proceed with image selection
                    openImageSelector();
                } else {
                    // Not connected to the active channel
                    Toast.makeText(ChatActivity.this, "Cannot upload images without being connected to a channel.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Not connected to the server
                Toast.makeText(ChatActivity.this, "Cannot upload images without being connected to the server.", Toast.LENGTH_SHORT).show();
            }
        });


        String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");
        String desiredNick = getIntent().getStringExtra("DESIRED_NICK");

        channelNameTextView = findViewById(R.id.ChannelName);
        chatEditText = findViewById(R.id.chatEditText);
        adminButton = findViewById(R.id.adminButton);
        ImageButton sendButton = findViewById(R.id.sendButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);

        if (desiredNick != null && !desiredNick.isEmpty()) {
            userNick = desiredNick;
        } else {
            userNick = "Guest" + (1000 + (int) (Math.random() * 9000));
        }

        initializeBot();

        if (selectedChannel != null) {
            setActiveChannel(selectedChannel);
        } else {
            setActiveChannel("#ThePlaceToChat");
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        hoverPanel = inflater.inflate(R.layout.hover_panel, rootLayout, false);
        operatorPanel = inflater.inflate(R.layout.operator_panel, rootLayout, false);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                (int) (320 * getResources().getDisplayMetrics().density), // Width in pixels
                (int) (600 * getResources().getDisplayMetrics().density)  // Height in pixels
        );

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);

        int topMargin = (int) (60 * getResources().getDisplayMetrics().density); // Adjust the value as needed
        params.setMargins(0, topMargin, 0, 0);

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

        // Ensure the btnShun initialization after inflating operatorPanel
        Button btnShun = operatorPanel.findViewById(R.id.btnShun);
        if (btnShun != null) {
            btnShun.setOnClickListener(v -> {
                List<String> userList = getUserListFromActiveChannel();
                new shun(this, bot, this, userList).startShunProcess();
            });
        } else {
            Log.e("ChatActivity", "btnShun is null, check operatorPanel inflation.");
        }

        Button btnOP = hoverPanel.findViewById(R.id.btnOP);
        Button btnDEOP = hoverPanel.findViewById(R.id.btnDEOP);

        btnOP.setOnClickListener(v -> new UserOP(this, bot, this, hoverPanel).startOPProcess(true));
        btnDEOP.setOnClickListener(v -> new UserOP(this, bot, this, hoverPanel).startOPProcess(false));

        btnJoin = hoverPanel.findViewById(R.id.btnJoin);
        btnJoin.setOnClickListener(v -> new JoinChannel(this, bot, this, hoverPanel).startJoinChannelProcess());

        btnNick.setOnClickListener(v -> showNickChangeDialog());
        btnKick.setOnClickListener(v -> new Kick(this, bot, this).startKickProcess());
        btnIdent.setOnClickListener(v -> new Identify(this, bot, this, v).startIdentifyProcess());

        btnKill.setOnClickListener(v -> new Kill(this, bot, this).startKillProcess());

        btnOperLogin.setOnClickListener(v -> new OperLogin(this, bot, this).startOperLoginProcess());

        btnSajoin.setOnClickListener(v -> new Sajoin(this, bot, this).startSajoinProcess());
        btnSapart.setOnClickListener(v -> new Sajoin(this, bot, this).startSapartProcess());

        operatorButton.setOnClickListener(v -> {
            fadeOutPanel(hoverPanel, () -> fadeInPanel(operatorPanel));
        });

        adminButton.setOnClickListener(v -> toggleHoverPanel());

        ImageButton btnChannelSwitcher = findViewById(R.id.btnChannelSwitcher);
        btnChannelSwitcher.setOnClickListener(v -> {
            checkAndAddActiveChannel();
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        RecyclerView channelRecyclerView = findViewById(R.id.channelRecyclerView);
        channelAdapter = new ChannelAdapter(channelList, this::switchChannel);
        channelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        channelRecyclerView.setAdapter(channelAdapter);

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
                                storeMessageForChannel(activeChannel, userNick + ": " + message);
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

        Button btnDefcon = operatorPanel.findViewById(R.id.btnDefcon);
        if (btnDefcon != null) {
            btnDefcon.setOnClickListener(v -> new Defcon(this, bot, this).startDefconProcess());
        } else {
            Log.e("ChatActivity", "btnDefcon is null, check operatorPanel inflation.");
        }

        Button btnSvsnick = operatorPanel.findViewById(R.id.btnSvsnick);
        if (btnSvsnick != null) {
            btnSvsnick.setOnClickListener(v -> new SvsNick(this, bot, this).startSvsNickProcess());
        } else {
            Log.e("ChatActivity", "btnSvsnick is null, check operatorPanel inflation.");
        }

        Button btnBan = hoverPanel.findViewById(R.id.btnBan);
        if (btnBan != null) {
            btnBan.setOnClickListener(v -> new Ban(this, bot, this).startBanProcess());
        } else {
            Log.e("ChatActivity", "btnBan is null, check hoverPanel inflation.");
        }

        Button btnUnban = hoverPanel.findViewById(R.id.btnUnban);
        if (btnUnban != null) {
            btnUnban.setOnClickListener(v -> new Unban(this, bot, this).startUnbanProcess());
        } else {
            Log.e("ChatActivity", "btnUnban is null, check hoverPanel inflation.");
        }

        disconnectButton.setOnClickListener(v -> {
            if (bot != null) {
                new Thread(() -> {
                    try {
                        bot.sendIRC().quitServer("good bye");
                        bot.close();
                        runOnUiThread(this::finish);
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(this::finish);
                    }
                }).start();
            }
        });

        ImageButton btnUsers = findViewById(R.id.btnUsers);
        btnUsers.setOnClickListener(v -> {
            if (bot != null && bot.isConnected()) {
                List<String> userList = getUserListFromActiveChannel();
                if (!userList.isEmpty()) {
                    ListUsers listUsers = new ListUsers(this, bot, this);
                    listUsers.showUserList();
                } else {
                    Toast.makeText(ChatActivity.this, "No users available in the current channel or channel not joined.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ChatActivity.this, "Not connected to a channel yet.", Toast.LENGTH_SHORT).show();
            }
        });

        connectToIrcServer();
    }

    private void initializeBot() {
        if (bot == null) {
            String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");
            String selectedServer = getIntent().getStringExtra("SELECTED_SERVER");

            if (selectedChannel == null || selectedChannel.isEmpty()) {
                selectedChannel = "#ThePlaceToChat";
            }

            Configuration.Builder configurationBuilder = new Configuration.Builder()
                    .setName(userNick)
                    .setAutoNickChange(true)
                    .setRealName("TPTC IRC Client")
                    .addAutoJoinChannel(selectedChannel)
                    .addListener(new Listeners(this))
                    .addListener(new NickChangeListener(this));

            if ("KonnectChat".equals(selectedServer)) {
                configurationBuilder.addServer("irc.konnectchatirc.net", 6667);
            } else if ("KonnectChat NSFW".equals(selectedServer)) {
                configurationBuilder.addServer("Aaronz.konnectchatirc.net", 7100);
            }

            Configuration configuration = configurationBuilder.buildConfiguration();
            bot = new PircBotX(configuration);
            setActiveChannel(selectedChannel);
            updateCurrentNick(userNick);
        }
    }

    public PircBotX getBot() {
        return bot;
    }

    private void connectToIrcServer() {
        new Thread(() -> {
            int retries = 0;
            while (retries < 5 && (bot == null || !bot.isConnected())) {
                try {
                    bot.startBot();
                    if (bot.isConnected()) {
                        String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");

                        if (selectedChannel != null && !selectedChannel.isEmpty()) {
                            bot.sendIRC().joinChannel(selectedChannel);
                            runOnUiThread(() -> {
                                addChatMessage("Joining channel: " + selectedChannel);
                                setActiveChannel(selectedChannel);
                                updateCurrentNick(userNick);
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No channel selected.", Toast.LENGTH_LONG).show());
                        }

                        updateChannelListAfterDelay();
                        break;
                    } else {
                        Log.e("IRC Connection", "Bot not connected, retrying...");
                        retries++;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            Log.e("IRC Connection", "Thread interrupted during sleep", e);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (IOException | IrcException e) {
                    e.printStackTrace();
                    retries++;
                    Log.e("IRC Connection", "Connection attempt " + retries + " failed, retrying...");
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error connecting to IRC server. Retrying...", Toast.LENGTH_LONG).show());

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Log.e("IRC Connection", "Thread interrupted during sleep", ex);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (retries >= 5) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Unable to connect to IRC server after multiple attempts.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void updateChannelListAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (bot != null && bot.isConnected()) {
                for (Channel channel : bot.getUserChannelDao().getAllChannels()) {
                    if (!isChannelInList(channel.getName())) {
                        channelList.add(new ChannelItem(channel.getName()));
                        channelMessagesMap.put(channel.getName(), new ArrayList<>());
                    }
                }
                channelAdapter.notifyDataSetChanged();
            }
        }, 5000);
    }

    public String getRequestedNick() {
        return requestedNick;
    }

    public void setNickInputToRetry() {
        chatEditText.setText("/nick ");
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

    public void addChatMessage(String message, String channel) {
        if (!channelMessagesMap.containsKey(channel)) {
            channelMessagesMap.put(channel, new ArrayList<>());
        }
        channelMessagesMap.get(channel).add(message);
        if (channel.equals(activeChannel)) {
            runOnUiThread(() -> {
                chatMessages.add(message);
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            });
        }
    }

    private void storeMessageForChannel(String channel, String message) {
        if (!channelMessagesMap.containsKey(channel)) {
            channelMessagesMap.put(channel, new ArrayList<>());
        }
        channelMessagesMap.get(channel).add(message);
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
        chatMessages.clear();
        if (channelMessagesMap.containsKey(channel)) {
            chatMessages.addAll(channelMessagesMap.get(channel));
        } else {
            channelMessagesMap.put(channel, new ArrayList<>());
        }
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void handleCommand(String command) {
        String commandText = command.substring(1);
        String[] parts = commandText.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (commandName) {
            case "me":
                if (!args.isEmpty()) {
                    sendAction(args);
                } else {
                    addChatMessage("Usage: /me <action>");
                }
                break;
            case "nick":
                changeNick(args);
                break;
            case "join":
                joinChannel(args);
                break;
            case "clear":
                clearChat();
                break;
            default:
                addChatMessage("Unknown command: " + commandName);
                break;
        }
    }

    public void clearChat() {
        chatMessages.clear();
        chatEditText.setText("");
        chatAdapter.notifyDataSetChanged();
        addChatMessage("Chat cleared.");
    }

    private void sendAction(String action) {
        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        bot.sendIRC().action(activeChannel, action);
                        runOnUiThread(() -> addChatMessage("* " + userNick + " " + action));
                        chatEditText.setText("");
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

    private String requestedNick; // Variable to keep track of the requested nickname

    private void changeNick(String newNick) {
        if (newNick.isEmpty()) {
            addChatMessage("Usage: /nick <new_nick>");
            chatEditText.setText("");
            return;
        }

        requestedNick = newNick;

        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        bot.sendRaw().rawLine("NICK " + newNick);
                        runOnUiThread(() -> {
                            updateLocalNick(newNick);
                            updateCurrentNick(newNick);
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
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to change nickname.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void updateLocalNick(String newNick) {
        userNick = newNick;
    }

    public void joinChannel(String channelName) {
        if (channelName.isEmpty()) {
            addChatMessage("Usage: /join <channel>");
            chatEditText.setText("");
            return;
        }
        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        bot.sendIRC().joinChannel(channelName);
                        runOnUiThread(() -> {
                            if (!isChannelInList(channelName)) {
                                channelList.add(new ChannelItem(channelName));
                                channelMessagesMap.put(channelName, new ArrayList<>());
                                channelAdapter.notifyDataSetChanged();
                            }
                            setActiveChannel(channelName);
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

    private boolean isChannelInList(String channelName) {
        for (ChannelItem channel : channelList) {
            if (channel.getChannelName().equals(channelName)) {
                return true;
            }
        }
        return false;
    }

    private void switchChannel(ChannelItem channel) {
        setActiveChannel(channel.getChannelName());
        channel.resetUnreadCount();
        resetUnreadCount();
        channelAdapter.notifyDataSetChanged();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void sendMessageToChannel(String channel, String message) {
        new Thread(() -> {
            try {
                if (bot.isConnected()) {
                    bot.sendIRC().message(channel, message);
                    storeMessageForChannel(channel, message);
                    runOnUiThread(() -> addChatMessage(message));
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Not connected to IRC server.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void removeChannel(String channelName) {
        for (int i = 0; i < channelList.size(); i++) {
            if (channelList.get(i).getChannelName().equals(channelName)) {
                channelList.remove(i);
                channelAdapter.notifyItemRemoved(i);
                channelMessagesMap.remove(channelName);
                break;
            }
        }
    }

    public void partChannel(String channelName) {
        new Thread(() -> {
            try {
                if (bot.isConnected()) {
                    bot.sendRaw().rawLine("PART " + channelName);
                    runOnUiThread(() -> removeChannel(channelName));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updateChannelName(String channelName) {
        runOnUiThread(() -> channelNameTextView.setText(channelName));
    }

    public void updateCurrentNick(String newNick) {
        TextView currentNickTextView = findViewById(R.id.CurrentNick);
        currentNickTextView.setText("Nick: " + newNick);
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
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    panel.setVisibility(View.GONE);
                    if (onAnimationEnd != null) {
                        onAnimationEnd.run();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            panel.startAnimation(fadeOut);
        } else {
            if (onAnimationEnd != null) {
                onAnimationEnd.run();
            }
        }
    }

    private void showNickChangeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Nickname");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

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

    private void incrementUnreadCount() {
        totalUnreadMessages++;
        runOnUiThread(() -> {
            unreadBadge.setText(String.valueOf(totalUnreadMessages));
            unreadBadge.setVisibility(View.VISIBLE);
        });
    }

    private void resetUnreadCount() {
        totalUnreadMessages = 0;
        runOnUiThread(() -> unreadBadge.setVisibility(View.GONE));
    }

    public void processServerMessage(String sender, String message, String channel) {
        String formattedMessage = sender + ": " + message;
        storeMessageForChannel(channel, formattedMessage);

        if (message.startsWith("005")) {
            return;
        }

        boolean isActiveChannel = channel.equalsIgnoreCase(getActiveChannel());

        if (isActiveChannel) {
            if (message.contains("DEFCON")) {
                runOnUiThread(() -> addChatMessage(formattedMessage));
                return;
            }

            if ("ACTION".equals(sender)) {
                runOnUiThread(() -> addChatMessage("* " + sender + " " + message));
            } else {
                runOnUiThread(() -> addChatMessage(formattedMessage));
            }
        } else {
            for (ChannelItem channelItem : channelList) {
                if (channelItem.getChannelName().equals(channel)) {
                    channelItem.incrementUnreadCount();
                    runOnUiThread(() -> channelAdapter.notifyDataSetChanged());
                    incrementUnreadCount();
                    break;
                }
            }
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChatActivity::WakeLock");
            wakeLock.setReferenceCounted(false);
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d("WakeLock", "WakeLock acquired");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("WakeLock", "WakeLock released");
        }
    }

    private void acquireWifiLock() {
        if (wifiLock == null) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ChatActivity::WifiLock");
            wifiLock.setReferenceCounted(false);
        }
        if (!wifiLock.isHeld()) {
            wifiLock.acquire();
            Log.d("WifiLock", "WifiLock acquired");
        }
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
        Intent stopServiceIntent = new Intent(this, IrcForegroundService.class);
        stopService(stopServiceIntent);

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

    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // Close the image selector immediately by resetting the data
            data.setData(null);

            // Process the image in a separate thread to avoid blocking the UI thread
            new Thread(() -> {
                Bitmap bitmap = null;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), imageUri));
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (bitmap != null) {
                    String encodedImage = encodeImageToBase64(bitmap);
                    uploadImageToImgur(encodedImage);
                }
            }).start();
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void uploadImageToImgur(String encodedImage) {
        // Show a toast to inform the user that the image is being processed
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Processing image...", Toast.LENGTH_SHORT).show());

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", encodedImage)
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgur.com/3/image")
                .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        String imageUrl = json.getJSONObject("data").getString("link");

                        // Send the link to the server immediately
                        new Thread(() -> {
                            try {
                                if (bot != null && bot.isConnected()) {
                                    bot.sendIRC().message(activeChannel, imageUrl);
                                    runOnUiThread(() -> {
                                        chatEditText.setText("");  // Clear the text box
                                        addChatMessage(userNick + ": " + imageUrl);  // Display the sent message in the chat
                                    });
                                } else {
                                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Not connected to IRC server.", Toast.LENGTH_SHORT).show());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Image upload failed.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    public List<String> getUserListFromActiveChannel() {
        List<String> userList = new ArrayList<>();

        if (bot == null || !bot.isConnected()) {
            return userList; // Return an empty list if the bot is not connected
        }

        try {
            Channel activeChannelObj = bot.getUserChannelDao().getChannel(activeChannel);
            if (activeChannelObj != null) {
                for (User user : activeChannelObj.getUsers()) {
                    userList.add(user.getNick());
                }
            }
        } catch (DaoException e) {
            // Handle the case where the channel is not found
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Channel not found or not yet joined.", Toast.LENGTH_SHORT).show());
        }

        return userList;
    }

    public void addBannedUser(String banEntry) {
        if (!bannedUsers.contains(banEntry)) {
            bannedUsers.add(banEntry);
            Log.d("ChatActivity", "Banned user added: " + banEntry);
            addChatMessage("Banned user added: " + banEntry);
        } else {
            Log.d("ChatActivity", "Banned user already in list: " + banEntry);
        }
    }

    public void checkAndAddActiveChannel() {
        if (activeChannel != null && !isChannelInList(activeChannel)) {
            channelList.add(new ChannelItem(activeChannel));
            channelMessagesMap.put(activeChannel, new ArrayList<>());
            channelAdapter.notifyDataSetChanged();
        }
    }
}
