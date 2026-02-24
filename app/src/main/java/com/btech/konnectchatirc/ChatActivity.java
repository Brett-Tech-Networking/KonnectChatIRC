package com.btech.konnectchatirc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Network;
import android.media.AudioManager;
import android.media.ToneGenerator;
import org.pircbotx.UserLevel;
import java.util.Set;
import org.pircbotx.cap.EnableCapHandler;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.pircbotx.hooks.Listener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;

public class ChatActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener, BotProvider {

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
    private static final String IMGUR_CLIENT_ID = "4968ca92805f1b2";
    private ConnectivityManager connectivityManager;
    private DrawerLayout drawerLayout;
    private ChannelAdapter channelAdapter;
    private FallingItemsView fallingItemsView;
    private boolean fallingItemsEnabled = false;
    private List<ChannelItem> channelList = new ArrayList<>();
    private Map<String, List<ChatMessage>> channelMessagesMap = new HashMap<>();
    private TextView unreadBadge;
    private int totalUnreadMessages = 0;
    private String desiredPassword;
    private ProgressDialog progressDialog;
    private String desiredNick;
    private PopupWindow mentionPopupWindow;
    private MentionSuggestionAdapter suggestionAdapter;
    private RecyclerView suggestionRecyclerView;
    private boolean isMentionActive = false;
    private int mentionStartIndex = -1;
    private MentionSuggestionAdapter commandSuggestionAdapter;
    private boolean isCommandActive = false;
    private int commandStartIndex = -1;
    private PopupWindow commandPopupWindow;
    private RecyclerView commandSuggestionRecyclerView;
    private List<String> commandList = Arrays.asList("/nick", "/id", "/join", "/part", "/list", "/clear");
    private String currentQuery;
    private ArrayAdapter<String> userListAdapter;
    private List<String> userList = new ArrayList<>();

    // Private messaging fields
    private boolean showTimestamps;
    private ListView privateConversationListView;
    private PrivateConversationAdapter privateConversationAdapter;
    private List<String> privateConversations = new ArrayList<>();
    private String selectedPrivateConversation = null;
    private boolean isViewingPrivateMessages = false;
    private boolean isActivityResumed = false;
    private PrivateMessageStorage messageStorage;
    private BroadcastReceiver privateMessageReceiver;

    public SpannableString createMentionSpannable(String messageContent) {
        SpannableString spannableString = new SpannableString(messageContent);

        // Apply @mentions ClickableSpans
        Pattern mentionPattern = Pattern.compile("@\\w+");
        Matcher mentionMatcher = mentionPattern.matcher(messageContent);

        while (mentionMatcher.find()) {
            final String mention = mentionMatcher.group();
            ClickableSpan mentionClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Toast.makeText(ChatActivity.this, "Clicked on mention: " + mention, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                }
            };
            spannableString.setSpan(
                    mentionClickableSpan,
                    mentionMatcher.start(),
                    mentionMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // Apply URL ClickableSpans
        Pattern urlPattern = Pattern.compile("(http://|https://|www\\.)[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(/[a-zA-Z0-9\\-\\._~:/?#\\[\\]@!$&'()*+,;=]*)?");
        Matcher urlMatcher = urlPattern.matcher(messageContent);

        while (urlMatcher.find()) {
            final String url = urlMatcher.group();
            final String normalizedUrl = url.startsWith("www.") ? "http://" + url : url;

            ClickableSpan urlClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl));
                    widget.getContext().startActivity(browserIntent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.CYAN);
                    ds.setUnderlineText(true);
                }
            };
            spannableString.setSpan(
                    urlClickableSpan,
                    urlMatcher.start(),
                    urlMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return spannableString;
    }

    public String getDesiredPassword() {
        return desiredPassword;
    }

    // Add bannedUsers list
    private List<String> bannedUsers = new ArrayList<>();

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d("NetworkCallback", "Network available, reconnecting if needed.");
            if (bot != null && !bot.isConnected()) {
                connectToIrcServer();
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d("NetworkCallback", "Network lost.");
        }
    };

    public boolean isViewingPrivateMessages() {
        return isViewingPrivateMessages;
    }

    // Typing Indicator Fields
    private TextView typingIndicator;
    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable stopTypingRunnable;
    private long lastTypingSent = 0;
    private static final long TYPING_SEND_INTERVAL = 3000;
    private Set<String> typingUsers = new HashSet<>();
    private ChannelStorage channelStorage;
    private boolean rainbowNicks = false;
    private boolean pmSoundEnabled = true;
    private boolean pmBarNotificationEnabled = true;
    private boolean detailedJPQEnabled = true;
    private static final String PM_CHANNEL_ID = "PM_Notifications";

    private final Handler rainbowHandler = new Handler(Looper.getMainLooper());
    private final Runnable rainbowRunnable = new Runnable() {
        @Override
        public void run() {
            if (rainbowNicks && chatAdapter != null) {
                chatAdapter.notifyDataSetChanged();
                rainbowHandler.postDelayed(this, 20);
            }
        }
    };

    public void onUserTyping(String nick, boolean isTyping) {
        runOnUiThread(() -> {
            if (isTyping) {
                typingUsers.add(nick);
            } else {
                typingUsers.remove(nick);
            }
            updateTypingIndicator();
        });
    }

    private void updateTypingIndicator() {
        if (typingUsers.isEmpty()) {
            typingIndicator.setVisibility(View.GONE);
            typingIndicator.setText("");
        } else {
            typingIndicator.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String nick : typingUsers) {
                if (count > 0) sb.append(", ");
                sb.append(nick);
                count++;
                if (count >= 3) break;
            }
            if (typingUsers.size() > 3) {
                sb.append(" and ").append(typingUsers.size() - 3).append(" others");
            }
            sb.append(typingUsers.size() == 1 ? " is typing..." : " are typing...");
            typingIndicator.setText(sb.toString());

            typingHandler.removeCallbacksAndMessages(null);
            typingHandler.postDelayed(() -> {
                typingUsers.clear();
                updateTypingIndicator();
            }, 6000);
        }
    }

    private void sendTyping(boolean active) {
        if (bot != null && bot.isConnected() && activeChannel != null) {
            if (activeChannel.equalsIgnoreCase("Server Notices")) {
                return;
            }

            long now = System.currentTimeMillis();
            if (active) {
                if (now - lastTypingSent > TYPING_SEND_INTERVAL) {
                    new Thread(() -> bot.sendRaw().rawLine("@+typing=active TAGMSG " + activeChannel)).start();
                    lastTypingSent = now;
                }
            } else {
                new Thread(() -> bot.sendRaw().rawLine("@+typing=done TAGMSG " + activeChannel)).start();
                lastTypingSent = 0;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BotManager.setActiveBotProvider(this);

        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_chat);
        acquireWakeLock();
        acquireWifiLock();

        if (rainbowNicks) {
            rainbowHandler.post(rainbowRunnable);
        }

        unreadBadge = findViewById(R.id.unreadBadge);
        drawerLayout = findViewById(R.id.drawerLayout);
        RelativeLayout rootLayout = findViewById(R.id.rootLayout);

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        createNotificationChannel();

        fallingItemsView = findViewById(R.id.fallingItemsView);
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        fallingItemsEnabled = prefs.getBoolean("falling_items_enabled", false);
        if (fallingItemsEnabled && fallingItemsView != null) {
            fallingItemsView.setVisibility(View.VISIBLE);
            fallingItemsView.startAnimation();
        }

        desiredNick = getIntent().getStringExtra("DESIRED_NICK");
        desiredPassword = getIntent().getStringExtra("DESIRED_PASSWORD");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        client = new OkHttpClient();

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        ChannelItem serverNotices = new ChannelItem("Server Notices");
        channelList.add(serverNotices);
        channelMessagesMap.put("server notices", new ArrayList<>());

        chatAdapter = new ChatAdapter((BotProvider) this, chatMessages);

        showTimestamps = prefs.getBoolean("show_timestamps", false);
        String timestampFormat = prefs.getString("timestamp_format", "HH:mm");
        rainbowNicks = prefs.getBoolean("rainbow_nicks", false);
        chatAdapter.setShowTimestamps(showTimestamps);
        chatAdapter.setTimestampFormat(timestampFormat);
        chatAdapter.setRainbowEnabled(rainbowNicks);
        pmSoundEnabled = prefs.getBoolean("pm_sound_enabled", true);
        pmBarNotificationEnabled = prefs.getBoolean("pm_bar_notification_enabled", true);
        detailedJPQEnabled = prefs.getBoolean("detailed_jpq_enabled", true);

        messageStorage = new PrivateMessageStorage(prefs);
        channelStorage = new ChannelStorage(prefs);

        if (savedInstanceState == null && channelStorage != null) {
            channelStorage.clearAllUnreadCounts();
        }

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                chatRecyclerView.post(() -> {
                    if (chatMessages.size() > 0) {
                        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                    }
                });
            }
        });

        Intent serviceIntent = new Intent(this, IrcForegroundService.class);
        serviceIntent.putExtra("#ThePlaceToChat", activeChannel);
        startForegroundService(serviceIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        if (getIntent().hasExtra("OPEN_PM_NICK")) {
            String targetNick = getIntent().getStringExtra("OPEN_PM_NICK");
            if (targetNick != null && !targetNick.isEmpty()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Button btnChannelsTab = findViewById(R.id.btnChannelsTab);
                    Button btnPrivateMessagesTab = findViewById(R.id.btnPrivateMessagesTab);
                    FrameLayout channelsSection = findViewById(R.id.channelsSection);
                    FrameLayout privateMessagesSection = findViewById(R.id.privateMessagesSection);

                    switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection);

                    if (!privateConversations.contains(targetNick)) {
                        privateConversations.add(0, targetNick);
                        privateConversationAdapter.notifyDataSetChanged();
                    }

                    loadPrivateConversation(targetNick);
                }, 500);
            }
        }

        ImageButton uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> {
            if (bot != null && bot.isConnected()) {
                if (activeChannel != null && bot.getUserChannelDao().containsChannel(activeChannel)) {
                    openImageSelector();
                } else {
                    Toast.makeText(ChatActivity.this, "Cannot upload images without being connected to a channel.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ChatActivity.this, "Cannot upload images without being connected to the server.", Toast.LENGTH_SHORT).show();
            }
        });

        userListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        ListView userListView = findViewById(R.id.userListView);
        userListView.setAdapter(userListAdapter);

        String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");

        channelNameTextView = findViewById(R.id.ChannelName);
        chatEditText = findViewById(R.id.chatEditText);

        initializeCommandPopup();
        initializeMentionPopup();

        typingIndicator = findViewById(R.id.typingIndicator);

        chatEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int cursorPosition = chatEditText.getSelectionStart();
                if (cursorPosition < 0) return;

                String text = s.toString();

                if (!text.startsWith("/")) {
                    if (text.length() > 0) {
                        sendTyping(true);
                        if (stopTypingRunnable != null) typingHandler.removeCallbacks(stopTypingRunnable);
                        stopTypingRunnable = () -> sendTyping(false);
                        typingHandler.postDelayed(stopTypingRunnable, 3000);
                    } else {
                        sendTyping(false);
                    }
                }

                if (cursorPosition > text.length()) return;

                int slashIndex = text.lastIndexOf('/', cursorPosition - 1);
                if (slashIndex != -1 && (slashIndex == 0 || Character.isWhitespace(text.charAt(slashIndex - 1)))) {
                    String commandText = text.substring(slashIndex + 1, cursorPosition);
                    if (!commandText.contains(" ")) {
                        isCommandActive = true;
                        commandStartIndex = slashIndex;
                        currentQuery = text.substring(commandStartIndex + 1, cursorPosition);
                        showCommandPopup(currentQuery);
                    }
                } else {
                    if (isCommandActive) {
                        isCommandActive = false;
                        commandStartIndex = -1;
                        dismissCommandPopup();
                    }
                }

                int atIndex = text.lastIndexOf('@', cursorPosition - 1);
                if (atIndex != -1 && (atIndex == 0 || Character.isWhitespace(text.charAt(atIndex - 1)))) {
                    String mentionText = text.substring(atIndex + 1, cursorPosition);
                    if (!mentionText.contains(" ")) {
                        isMentionActive = true;
                        mentionStartIndex = atIndex;
                        currentQuery = text.substring(mentionStartIndex + 1, cursorPosition);
                        showMentionPopup(currentQuery);
                    }
                } else {
                    if (isMentionActive) {
                        isMentionActive = false;
                        mentionStartIndex = -1;
                        dismissMentionPopup();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        adminButton = findViewById(R.id.adminButton);
        ImageButton sendButton = findViewById(R.id.sendButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);

        if (desiredNick != null && !desiredNick.isEmpty()) {
            userNick = desiredNick.trim();
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
                (int) (320 * getResources().getDisplayMetrics().density),
                (int) (600 * getResources().getDisplayMetrics().density)
        );

        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);

        int topMargin = (int) (60 * getResources().getDisplayMetrics().density);
        params.setMargins(0, topMargin, 0, 0);

        rootLayout.addView(hoverPanel, params);
        rootLayout.addView(operatorPanel, params);

        operatorPanel.setVisibility(View.GONE);

        Button btnNick = hoverPanel.findViewById(R.id.btnNick);
        Button btnJoin = hoverPanel.findViewById(R.id.btnJoin);
        Button btnKick = hoverPanel.findViewById(R.id.btnKick);
        Button btnIdent = hoverPanel.findViewById(R.id.btnIdent);
        Button btnNickRegister = hoverPanel.findViewById(R.id.btnNickRegister);
        Button btnChanRegister = hoverPanel.findViewById(R.id.btnChanRegister);
        Button btnSOP = hoverPanel.findViewById(R.id.btnSOP);
        Button btnAOP = hoverPanel.findViewById(R.id.btnAOP);
        Button btnBan = hoverPanel.findViewById(R.id.btnBan);
        Button btnUnban = hoverPanel.findViewById(R.id.btnUnban);
        operatorButton = hoverPanel.findViewById(R.id.btnOperator);

        btnKill = operatorPanel.findViewById(R.id.btnKill);
        btnOperLogin = operatorPanel.findViewById(R.id.btnOperLogin);
        btnSajoin = operatorPanel.findViewById(R.id.btnSajoin);
        Button btnSapart = operatorPanel.findViewById(R.id.btnSapart);
        Button btnOSLogin = operatorPanel.findViewById(R.id.btnOSLogin);
        Button btnZline = operatorPanel.findViewById(R.id.btnZline);
        ImageButton btnOperatorBack = operatorPanel.findViewById(R.id.btnOperatorBack);

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

        btnNickRegister.setOnClickListener(v -> new NickRegister(this, bot, this, hoverPanel).startRegistrationProcess());
        btnChanRegister.setOnClickListener(v -> Toast.makeText(this, "Chan Registration: Use /msg ChanServ REGISTER #chan <pass> <desc>", Toast.LENGTH_LONG).show());
        btnSOP.setOnClickListener(v -> Toast.makeText(this, "SOP: Use /msg ChanServ SOP #chan ADD <nick>", Toast.LENGTH_SHORT).show());
        btnAOP.setOnClickListener(v -> Toast.makeText(this, "AOP: Use /msg ChanServ AOP #chan ADD <nick>", Toast.LENGTH_SHORT).show());

        btnKill.setOnClickListener(v -> new Kill(this, bot, this).startKillProcess());

        btnOperLogin.setOnClickListener(v -> new OperLogin(this, this, bot).startOperLoginProcess());

        btnSajoin.setOnClickListener(v -> new Sajoin(this, bot, this).startSajoinProcess());
        btnSapart.setOnClickListener(v -> new Sajoin(this, bot, this).startSapartProcess());

        View.OnClickListener backToMain = v -> {
            fadeOutPanel(operatorPanel, () -> fadeInPanel(hoverPanel));
        };
        btnOperatorBack.setOnClickListener(backToMain);

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

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        RecyclerView channelRecyclerView = findViewById(R.id.channelRecyclerView);
        channelAdapter = new ChannelAdapter(channelList, this);
        channelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        channelRecyclerView.setAdapter(channelAdapter);

        if (activeChannel != null) {
            channelAdapter.setSelectedChannelName(activeChannel);
        }

        initializePrivateMessaging();

        btnOSLogin.setOnClickListener(v -> Toast.makeText(this, "OS Login not yet implemented", Toast.LENGTH_SHORT).show());
        btnZline.setOnClickListener(v -> new Kill(this, bot, this).startKillProcess());

        sendButton.setOnClickListener(v -> {
            String message = chatEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                if (message.startsWith("/")) {
                    handleCommand(message);
                } else if (isViewingPrivateMessages && selectedPrivateConversation != null) {
                    sendPrivateMessage(message);
                } else {
                    addChatMessage(userNick + ": " + message);
                    chatEditText.setText("");
                    sendTyping(false);

                    dismissMentionPopup();

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

        if (btnBan != null) {
            btnBan.setOnClickListener(v -> new Ban(this, bot, this).startBanProcess());
        }

        if (btnUnban != null) {
            btnUnban.setOnClickListener(v -> new Unban(this, bot, this).startUnbanProcess());
        }

        disconnectButton.setOnClickListener(v -> {
            if (bot != null) {
                new Thread(() -> {
                    try {
                        bot.sendIRC().quitServer("https://play.google.com/store/apps/details?id=com.btech.konnectchatirc, Download KonnectChatIRC app today!");
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

            Log.d("ChatActivity", "Selected Server: " + selectedServer);

            if (selectedServer == null || selectedServer.isEmpty()) {
                selectedServer = "KonnectChat IRC";
            }


            Configuration.Builder configurationBuilder = new Configuration.Builder()
                    .setName(userNick)
                    .setLogin("KCIRC")
                    .setAutoNickChange(true)
                    .setRealName("KonnectChatIRC Client")
                    .addAutoJoinChannel(selectedChannel)
                    .addListener(new Listeners(this))
                    .addCapHandler(new EnableCapHandler("extended-join"))
                    .addCapHandler(new EnableCapHandler("account-notify"))
                    .addCapHandler(new EnableCapHandler("message-tags"))
                    .setAutoSplitMessage(true)
                    .setAutoReconnect(true)

                    .addListener((Listener) new NickChangeListener(this));


            if ("KonnectChat IRC".equals(selectedServer)) {
                configurationBuilder.addServer("irc.konnectchatirc.net", 6667);
            } else if ("KonnectChat IRC NSFW".equals(selectedServer)) {
                configurationBuilder.addServer("Aaronz.konnectchatirc.net", 7100);
            } else if ("ThePlaceToChat IRC".equals(selectedServer)) {
                configurationBuilder.addServer("irc.theplacetochat.net", 6667);
            } else {
                throw new IllegalArgumentException("Unknown server: " + selectedServer);
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
            boolean connected = false;

            while (retries < 5 && !connected && !isFinishing()) {
                try {
                    bot.startBot();
                    if (bot.isConnected()) {
                        connected = true;
                        String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");

                        if (selectedChannel != null && !selectedChannel.isEmpty()) {
                            bot.sendIRC().joinChannel(selectedChannel);
                            runOnUiThread(() -> {
                                addChatMessage("Joining channel: " + selectedChannel);
                                setActiveChannel(selectedChannel);
                                updateCurrentNick(userNick);
                            });

                            bot.sendRaw().rawLine("NICK " + userNick);

                            if (desiredNick != null && !desiredNick.isEmpty() && (desiredPassword == null || desiredPassword.isEmpty())) {
                                runOnUiThread(() -> addChatMessage("Nick changed to: " + userNick + ". No password provided, skipping identification."));
                            }

                            if (desiredNick != null && !desiredNick.isEmpty() && desiredPassword != null && !desiredPassword.isEmpty()) {
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    String identifyCommand = "PRIVMSG NickServ :IDENTIFY " + userNick + " " + desiredPassword;
                                    bot.sendRaw().rawLine(identifyCommand);
                                    runOnUiThread(() -> addChatMessage("Identifying user: " + userNick));
                                }, 500);
                            }

                            updateChannelListAfterDelay();

                            new Thread(() -> {
                                while (true) {
                                    try {
                                        bot.sendRaw().rawLine("PING " + bot.getServerInfo().getServerName());
                                        Thread.sleep(60 * 1000);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                            }).start();

                            break;
                        } else {
                            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No channel selected.", Toast.LENGTH_LONG).show());
                        }
                    } else {
                        Log.e("IRC Connection", "Bot not connected, retrying...");
                        retries++;
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (IOException | IrcException e) {
                    retries++;
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error connecting to IRC server. Retrying...", Toast.LENGTH_LONG).show());

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (!connected) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Unable to connect to IRC server after multiple attempts.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private void updateChannelListAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (bot != null && bot.isConnected()) {
                for (Channel channel : bot.getUserChannelDao().getAllChannels()) {
                    if (!isChannelInList(channel.getName())) {
                        ChannelItem newItem = new ChannelItem(channel.getName());
                        if (channelStorage != null) {
                            newItem.setUnreadCount(channelStorage.getUnreadCount(channel.getName()));
                        }
                        channelList.add(newItem);
                        if (!channelMessagesMap.containsKey(channel.getName().toLowerCase())) {
                            channelMessagesMap.put(channel.getName().toLowerCase(), new ArrayList<>());
                        }
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
        ChatMessage chatMsg = new ChatMessage(message, System.currentTimeMillis());
        chatMessages.add(chatMsg);

        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatMessages.isEmpty()) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            int itemCount = layoutManager.getItemCount();

            boolean isAtBottom = (lastVisibleItemPosition >= itemCount - 3) || lastVisibleItemPosition == -1;

            if (isAtBottom) {
                chatRecyclerView.post(() -> chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1));
            }
        }
    }

    void storeMessageForChannel(String channel, String message) {
        String lowerCaseChannel = channel.toLowerCase();
        if (channelMessagesMap.containsKey(lowerCaseChannel)) {
            ChatMessage chatMsg = new ChatMessage(message, System.currentTimeMillis());
            channelMessagesMap.get(lowerCaseChannel).add(chatMsg);
        }
    }

    void storeMessageForChannel(String channel, ChatMessage message) {
        String lowerCaseChannel = channel.toLowerCase();
        if (!channelMessagesMap.containsKey(lowerCaseChannel)) {
            channelMessagesMap.put(lowerCaseChannel, new ArrayList<>());
        }
        channelMessagesMap.get(lowerCaseChannel).add(message);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final CheckBox timestampCheck = new CheckBox(this);
        timestampCheck.setText("Show Timestamps");
        timestampCheck.setChecked(showTimestamps);
        timestampCheck.setTextSize(16);

        TextView formatLabel = new TextView(this);
        formatLabel.setText("Timestamp Format");
        formatLabel.setTextSize(16);
        formatLabel.setPadding(0, 20, 0, 5);
        layout.addView(formatLabel);

        final android.widget.Spinner formatSpinner = new android.widget.Spinner(this);
        String[] formats = {"24-hour (HH:mm)", "24-hour w/ seconds (HH:mm:ss)", "12-hour (h:mm a)", "12-hour w/ seconds (h:mm:ss a)"};
        final String[] formatValues = {"HH:mm", "HH:mm:ss", "h:mm a", "h:mm:ss a"};

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, formats);
        formatSpinner.setAdapter(adapter);

        final CheckBox fallingItemsCheck = new CheckBox(this);
        fallingItemsCheck.setText("Fun Mode (Titties)");
        fallingItemsCheck.setChecked(fallingItemsEnabled);
        fallingItemsCheck.setTextSize(16);
        fallingItemsCheck.setTextColor(Color.parseColor("#FF69B4"));

        String currentFormat = chatAdapter.getTimestampFormat();
        int selectionIndex = 0;
        for (int i = 0; i < formatValues.length; i++) {
            if (formatValues[i].equals(currentFormat)) {
                selectionIndex = i;
                break;
            }
        }
        formatSpinner.setSelection(selectionIndex);
        layout.addView(formatSpinner);

        final CheckBox rainbowCheck = new CheckBox(this);
        rainbowCheck.setText("Rainbow Nicks");
        rainbowCheck.setChecked(rainbowNicks);
        rainbowCheck.setTextSize(16);
        rainbowCheck.setTextColor(Color.MAGENTA);

        TextView notificationHeader = new TextView(this);
        notificationHeader.setText("Notifications");
        notificationHeader.setTextSize(18);
        notificationHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        notificationHeader.setPadding(0, 30, 0, 10);

        final CheckBox pmSoundCheck = new CheckBox(this);
        pmSoundCheck.setText("Play Sound (Pop)");
        pmSoundCheck.setChecked(pmSoundEnabled);
        pmSoundCheck.setTextSize(16);

        final CheckBox pmBarCheck = new CheckBox(this);
        pmBarCheck.setText("Show in Status Bar");
        pmBarCheck.setChecked(pmBarNotificationEnabled);
        pmBarCheck.setTextSize(16);

        final CheckBox detailedJPQCheck = new CheckBox(this);
        detailedJPQCheck.setText("Detailed Join/Part/Quit");
        detailedJPQCheck.setChecked(detailedJPQEnabled);
        detailedJPQCheck.setTextSize(16);

        layout.addView(timestampCheck);
        layout.addView(rainbowCheck);
        layout.addView(notificationHeader);
        layout.addView(pmSoundCheck);
        layout.addView(pmBarCheck);
        layout.addView(detailedJPQCheck);
        layout.addView(fallingItemsCheck);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            boolean newShowTimestamps = timestampCheck.isChecked();
            boolean newRainbowNicks = rainbowCheck.isChecked();
            boolean newPmSoundEnabled = pmSoundCheck.isChecked();
            boolean newPmBarNotificationEnabled = pmBarCheck.isChecked();
            boolean newFallingItemsEnabled = fallingItemsCheck.isChecked();
            boolean newDetailedJPQEnabled = detailedJPQCheck.isChecked();

            SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            boolean changesMade = false;

            if (pmSoundEnabled != newPmSoundEnabled) {
                pmSoundEnabled = newPmSoundEnabled;
                editor.putBoolean("pm_sound_enabled", pmSoundEnabled);
                changesMade = true;
            }

            if (pmBarNotificationEnabled != newPmBarNotificationEnabled) {
                pmBarNotificationEnabled = newPmBarNotificationEnabled;
                editor.putBoolean("pm_bar_notification_enabled", pmBarNotificationEnabled);
                changesMade = true;
            }

            if (fallingItemsEnabled != newFallingItemsEnabled) {
                fallingItemsEnabled = newFallingItemsEnabled;
                editor.putBoolean("falling_items_enabled", fallingItemsEnabled);
                changesMade = true;

                if (fallingItemsView != null) {
                    if (fallingItemsEnabled) {
                        fallingItemsView.setVisibility(View.VISIBLE);
                        fallingItemsView.startAnimation();
                    } else {
                        fallingItemsView.stopAnimation();
                        fallingItemsView.setVisibility(View.GONE);
                    }
                }
            }

            if (detailedJPQEnabled != newDetailedJPQEnabled) {
                detailedJPQEnabled = newDetailedJPQEnabled;
                editor.putBoolean("detailed_jpq_enabled", detailedJPQEnabled);
                changesMade = true;
            }

            if (showTimestamps != newShowTimestamps) {
                showTimestamps = newShowTimestamps;
                editor.putBoolean("show_timestamps", showTimestamps);
                chatAdapter.setShowTimestamps(showTimestamps);
                changesMade = true;
            }

            int selectedFormatIndex = formatSpinner.getSelectedItemPosition();
            String newTimestampFormat = formatValues[selectedFormatIndex];

            if (!chatAdapter.getTimestampFormat().equals(newTimestampFormat)) {
                editor.putString("timestamp_format", newTimestampFormat);
                chatAdapter.setTimestampFormat(newTimestampFormat);
                changesMade = true;
            }

            if (rainbowNicks != newRainbowNicks) {
                rainbowNicks = newRainbowNicks;
                editor.putBoolean("rainbow_nicks", rainbowNicks);
                chatAdapter.setRainbowEnabled(rainbowNicks);
                if (rainbowNicks) {
                    rainbowHandler.post(rainbowRunnable);
                } else {
                    rainbowHandler.removeCallbacks(rainbowRunnable);
                }
                changesMade = true;
            }

            if (changesMade) {
                editor.apply();
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    public String getUserNick() {
        return userNick;
    }

    public boolean isDetailedJPQEnabled() {
        return detailedJPQEnabled;
    }

    public String getActiveChannel() {
        return activeChannel;
    }

    public void setActiveChannel(String channel) {
        this.activeChannel = channel;
        updateChannelName(channel);
        updateUserCount();

        if (channelStorage != null) {
            channelStorage.resetUnreadCount(channel);
        }
        for (ChannelItem item : channelList) {
            if (item.getChannelName().equalsIgnoreCase(channel)) {
                item.setUnreadCount(0);
                break;
            }
        }
        if (channelAdapter != null) {
            channelAdapter.notifyDataSetChanged();
        }
        updateGlobalUnreadCount();

        chatMessages.clear();
        String lowerCaseChannel = channel.toLowerCase();
        if (channelMessagesMap.containsKey(lowerCaseChannel)) {
            chatMessages.addAll(channelMessagesMap.get(lowerCaseChannel));
        } else {
            channelMessagesMap.put(lowerCaseChannel, new ArrayList<>());
        }
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        if (channelAdapter != null) {
            channelAdapter.setSelectedChannelName(channel);
        }

        updateInputVisibility(!channel.equalsIgnoreCase("Server Notices"), channel);
    }

    private void updateInputVisibility(boolean visible, String targetName) {
        View chatInputLayout = findViewById(R.id.chatInputLayout);
        if (chatInputLayout == null) return;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) chatRecyclerView.getLayoutParams();

        if (!visible) {
            chatInputLayout.setVisibility(View.GONE);
            params.removeRule(RelativeLayout.ABOVE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            chatEditText.setEnabled(false);
            chatEditText.setHint(targetName + " (Read-Only)");
        } else {
            chatInputLayout.setVisibility(View.VISIBLE);
            params.addRule(RelativeLayout.ABOVE, R.id.chatInputLayout);
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            chatEditText.setEnabled(true);
            chatEditText.setHint("Message " + targetName);
        }
        params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        chatRecyclerView.setLayoutParams(params);
        chatRecyclerView.requestLayout();
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
            case "msg":
                String[] msgParts = args.split(" ", 2);
                if (msgParts.length == 2) {
                    sendPrivateMessageTo(msgParts[0], msgParts[1], false);
                } else {
                    addChatMessage("Usage: /msg <target> <message>");
                }
                break;
            case "ns":
            case "nickserv":
                if (!args.isEmpty()) {
                    sendPrivateMessageTo("NickServ", args, false);
                } else {
                    addChatMessage("Usage: /ns <message>");
                }
                break;
            case "cs":
            case "chanserv":
                if (!args.isEmpty()) {
                    sendPrivateMessageTo("ChanServ", args, false);
                } else {
                    addChatMessage("Usage: /cs <message>");
                }
                break;
            case "os":
            case "operserv":
                if (!args.isEmpty()) {
                    sendPrivateMessageTo("OperServ", args, false);
                } else {
                    addChatMessage("Usage: /os <message>");
                }
                break;
            case "register":
                new NickRegister(this, bot, this, hoverPanel).startRegistrationProcess();
                break;
            case "clear":
                clearChat();
                break;
            default:
                addChatMessage("Unknown command: " + commandName);
                break;
        }
        chatEditText.setText("");
    }

    private void sendPrivateMessageTo(String target, String message, boolean switchToTarget) {
        if (target == null || target.isEmpty()) return;

        if (bot != null && bot.isConnected()) {
            runOnUiThread(() -> {
                if (!privateConversations.contains(target)) {
                    privateConversations.add(0, target);
                } else {
                    privateConversations.remove(target);
                    privateConversations.add(0, target);
                }
                privateConversationAdapter.notifyDataSetChanged();

                messageStorage.saveMessage(userNick, target, message, true);

                if (switchToTarget) {
                    selectedPrivateConversation = target;
                    loadPrivateConversation(target);
                    chatEditText.setText("");
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                } else if (isViewingPrivateMessages && target.equalsIgnoreCase(selectedPrivateConversation)) {
                    loadPrivateConversation(target);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
            });

            new Thread(() -> {
                try {
                    bot.sendIRC().message(target, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Not connected to IRC.", Toast.LENGTH_SHORT).show());
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
                        String target = isViewingPrivateMessages && selectedPrivateConversation != null ? selectedPrivateConversation : activeChannel;
                        if (target == null) {
                            runOnUiThread(() -> addChatMessage("No target for action."));
                            return;
                        }
                        bot.sendIRC().action(target, action);
                        String actionMsg = "* " + userNick + " " + action;
                        if (isViewingPrivateMessages && selectedPrivateConversation != null) {
                            messageStorage.saveMessage(userNick, selectedPrivateConversation, actionMsg, true);
                        } else {
                            storeMessageForChannel(activeChannel, actionMsg);
                        }
                        runOnUiThread(() -> addChatMessage(actionMsg));
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

    private String requestedNick;

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
                        bot.sendRaw().rawLine("NICK " + newNick);
                        runOnUiThread(() -> {
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

    public void updateNickUI(String newNick) {
        userNick = newNick;
        if (hoverPanel != null) {
            Button btnNick = hoverPanel.findViewById(R.id.btnNick);
            if (btnNick != null) {
                btnNick.setText(newNick);
            }
        }
        updateCurrentNick(newNick);
        
        // Broadcast nick change to other activities
        Intent intent = new Intent("com.btech.konnectchatirc.NICK_CHANGED");
        intent.putExtra("new_nick", newNick);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    public void joinChannel(String channelName) {
        if (channelName.isEmpty()) {
            addChatMessage("Usage: /join <channel>");
            chatEditText.setText("");
            return;
        }

        final String lowerCaseChannelName = channelName.toLowerCase();

        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        bot.sendIRC().joinChannel(lowerCaseChannelName);
                        runOnUiThread(() -> {
                            if (!isChannelInList(lowerCaseChannelName)) {
                                channelList.add(new ChannelItem(lowerCaseChannelName));
                                if (!channelMessagesMap.containsKey(lowerCaseChannelName)) {
                                    channelMessagesMap.put(lowerCaseChannelName, new ArrayList<>());
                                }
                                channelAdapter.notifyDataSetChanged();
                            }
                            setActiveChannel(lowerCaseChannelName);
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
            if (channel.getChannelName().equalsIgnoreCase(channelName)) {
                return true;
            }
        }
        return false;
    }


    private void switchChannel(ChannelItem channel) {
        isViewingPrivateMessages = false;
        selectedPrivateConversation = null;
        setActiveChannel(channel.getChannelName());
        selectedPrivateConversation = null;
        setActiveChannel(channel.getChannelName());
        channel.resetUnreadCount();
        if (channelStorage != null) {
            channelStorage.resetUnreadCount(channel.getChannelName());
        }
        resetUnreadCount();
        channelAdapter.setSelectedChannelName(channel.getChannelName());
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void sendMessageToChannel(String channel, String message) {
        new Thread(() -> {
            try {
                if (bot.isConnected()) {
                    bot.sendIRC().message(channel, message);
                    storeMessageForChannel(channel, message);
                    runOnUiThread(() -> {
                        addChatMessage(message);
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                    });
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
                channelMessagesMap.remove(channelName.toLowerCase());
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
        currentNickTextView.setText(newNick);
        updateUserCount();
    }

    public void updateUserCount() {
        runOnUiThread(() -> {
            TextView userCountBadge = findViewById(R.id.userCountBadge);
            if (bot != null && bot.isConnected() && activeChannel != null && !isViewingPrivateMessages) {
                try {
                    Channel activeChannelObj = bot.getUserChannelDao().getChannel(activeChannel);
                    if (activeChannelObj != null) {
                        int userCount = activeChannelObj.getUsers().size();
                        if (userCount > 0) {
                            userCountBadge.setText("👥 " + userCount);
                            userCountBadge.setVisibility(View.VISIBLE);
                        } else {
                            userCountBadge.setVisibility(View.GONE);
                        }
                    } else {
                        userCountBadge.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    userCountBadge.setVisibility(View.GONE);
                }
            } else {
                userCountBadge.setVisibility(View.GONE);
            }
        });
    }

    public boolean isDiscordUser(String realName) {
        if (realName == null || realName.isEmpty()) return false;
        return realName.matches(".*#\\d{4,}.*@ Discord/Konnect-Chat.*") || realName.contains("IRCRELAY");
    }

    public void showDiscordPrivacyError() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Discord Privacy Error: The user's settings do not permit private messaging.", Toast.LENGTH_LONG).show();
            addChatMessage("SYSTEM: Discord delivery failed. User's privacy settings block PMs.");
        });
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
                }
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

    public String extractHost(String hostmaskOrHost) {
        if (hostmaskOrHost == null) return "";
        if (hostmaskOrHost.contains("@")) {
            return hostmaskOrHost.substring(hostmaskOrHost.lastIndexOf("@") + 1).trim();
        }
        return hostmaskOrHost.trim();
    }

    public void showWhoisDialog(String nick, String realName, String ident, String host, String server, String channels, String idleTime, String signonTime, String awayMessage) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme_NoAnimation);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_whois, null);
            builder.setView(dialogView);

            TextView nickView = dialogView.findViewById(R.id.whois_nick);
            TextView realNameView = dialogView.findViewById(R.id.whois_realname);
            TextView identView = dialogView.findViewById(R.id.whois_ident);
            TextView hostView = dialogView.findViewById(R.id.whois_host);
            TextView serverView = dialogView.findViewById(R.id.whois_server);
            TextView channelsView = dialogView.findViewById(R.id.whois_channels);
            TextView idleView = dialogView.findViewById(R.id.whois_idle);
            TextView awayView = dialogView.findViewById(R.id.whois_away);
            ImageView avatarView = dialogView.findViewById(R.id.whois_avatar);

            nickView.setText(nick);

            if (isDiscordUser(realName)) {
                avatarView.setImageResource(android.R.drawable.ic_lock_idle_lock);
                avatarView.setBackgroundResource(R.drawable.circle_badge);
                avatarView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7289DA")));
            }

            realNameView.setText("Name: " + (realName != null ? realName : "N/A"));
            identView.setText("User: " + (ident != null ? ident : "N/A"));
            hostView.setText("Host: " + (host != null ? host : "N/A"));
            serverView.setText("Server: " + (server != null ? server : "N/A"));

            if (channels != null && !channels.isEmpty()) {
                SpannableStringBuilder builderChannels = new SpannableStringBuilder("Channels: ");
                String listContent = channels;
                if (listContent.startsWith("[") && listContent.endsWith("]")) {
                    listContent = listContent.substring(1, listContent.length() - 1);
                }

                String[] channelArray = listContent.split(",?\\s+");
                for (int i = 0; i < channelArray.length; i++) {
                    final String rawChan = channelArray[i].trim();
                    if (rawChan.isEmpty()) continue;

                    String cleanChan = rawChan;
                    while (!cleanChan.isEmpty() && !cleanChan.startsWith("#") && !cleanChan.startsWith("&")) {
                        cleanChan = cleanChan.substring(1);
                    }

                    if (cleanChan.isEmpty()) {
                        builderChannels.append(rawChan);
                    } else {
                        final String finalChan = cleanChan;
                        int start = builderChannels.length();
                        builderChannels.append(rawChan);
                        int end = builderChannels.length();

                        builderChannels.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                new Thread(() -> {
                                    if (bot != null && bot.isConnected()) {
                                        bot.sendIRC().joinChannel(finalChan);
                                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Joining " + finalChan, Toast.LENGTH_SHORT).show());
                                    }
                                }).start();
                            }

                            @Override
                            public void updateDrawState(@NonNull TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setColor(Color.parseColor("#4FC3F7"));
                                ds.setUnderlineText(true);
                            }
                        }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    if (i < channelArray.length - 1) {
                        builderChannels.append(", ");
                    }
                }
                channelsView.setText(builderChannels);
                channelsView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                channelsView.setText("Channels: N/A");
            }

            idleView.setText("Idle: " + (idleTime != null ? idleTime : "0") + "s (Signed on: " + (signonTime != null ? signonTime : "N/A") + ")");

            if (awayMessage != null && !awayMessage.isEmpty()) {
                awayView.setText("Away: " + awayMessage);
                awayView.setTextColor(Color.parseColor("#FF9800"));
            } else {
                awayView.setText("Status: Not Away");
            }

            // FIND DUPLICATES LOGIC
            List<String> dupes = new ArrayList<>();
            String extractedHost = extractHost(host);

            if (bot != null && bot.isConnected() && !extractedHost.isEmpty()) {
                try {
                    // Search globally across all users the bot knows about
                    for (User u : bot.getUserChannelDao().getAllUsers()) {

                        String uHost = extractHost(u.getHostname());
                        if (uHost.isEmpty()) uHost = extractHost(u.getHostmask());

                        if (!uHost.isEmpty() && uHost.equalsIgnoreCase(extractedHost)) {
                            if (!isExcludedHost(u) && !dupes.contains(u.getNick())) {
                                dupes.add(u.getNick());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // ONLY SHOW IF MORE THAN 1 RESULT
            if (dupes.size() > 1) {
                String dupesStr = android.text.TextUtils.join(", ", dupes);
                SpannableStringBuilder ssb = new SpannableStringBuilder(awayView.getText());
                ssb.append("\n\nDuplicate Hosts: ");
                int start = ssb.length();
                ssb.append(dupesStr);
                ssb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#FF5252")), start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                awayView.setText(ssb);
            }

            AlertDialog dialog = builder.create();
            dialogView.findViewById(R.id.btnWhoisClose).setOnClickListener(v -> dialog.dismiss());
            dialog.show();

            if (dialog.getWindow() != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(dialog.getWindow().getAttributes());
                layoutParams.width = (int) (350 * getResources().getDisplayMetrics().density);
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                layoutParams.gravity = Gravity.CENTER;
                dialog.getWindow().setAttributes(layoutParams);
            }
        });
    }

    public boolean hasMessageBeenProcessed(String message) {
        return processedMessages.contains(message);
    }

    public void markMessageAsProcessed(String message) {
        processedMessages.add(message);
    }

    void updateGlobalUnreadCount() {
        int pmUnreadCount = 0;
        if (messageStorage != null && userNick != null) {
            pmUnreadCount = messageStorage.getTotalUnreadCount(userNick);
        }

        totalUnreadMessages = 0;

        if (channelStorage != null) {
            for (ChannelItem item : channelList) {
                String key = item.getChannelName();

                int count = channelStorage.getUnreadCount(key);
                item.setUnreadCount(count);
                totalUnreadMessages += count;
            }
        } else {
            for (ChannelItem item : channelList) {
                totalUnreadMessages += item.getUnreadCount();
            }
        }

        int total = totalUnreadMessages + pmUnreadCount;

        runOnUiThread(() -> {
            if (total > 0) {
                unreadBadge.setVisibility(View.VISIBLE);
                unreadBadge.setText(String.valueOf(total));
            } else {
                unreadBadge.setVisibility(View.GONE);
            }
        });
    }


    private void resetUnreadCount() {
        updateGlobalUnreadCount();
    }

    public void processServerMessage(String sender, String message, String requestChannel) {
        runOnUiThread(() -> {
            if (isViewingPrivateMessages) {
                storeMessageForChannel(requestChannel, sender + ": " + message);
                return;
            }

            String channel = requestChannel;
            if (channel == null) {
                channel = getActiveChannel();
            }

            String prefix = "";
            User senderUser = null;

            if (bot != null && bot.isConnected()) {
                Channel channelObj = null;
                try {
                    channelObj = bot.getUserChannelDao().getChannel(channel);
                } catch (Exception e) {
                    Log.w("processServerMessage", "Channel not found in DAO: " + channel);
                }

                if (channelObj != null) {
                    senderUser = channelObj.getUsers().stream()
                            .filter(user -> user.getNick().equals(sender))
                            .findFirst()
                            .orElse(null);

                    if (senderUser != null) {
                        prefix = getUserPrefix(senderUser, channelObj);
                        System.out.println("User: " + senderUser.getNick() + ", Prefix: " + prefix);
                    } else {
                        System.out.println("Sender user not found in channel user list.");
                    }
                } else {
                    System.out.println("Channel object is null or not found for channel: " + channel);
                }
            } else {
                System.out.println("Bot is null or not connected.");
            }

            String formattedMessage = prefix + " " +  sender + ": " + message;
            storeMessageForChannel(channel, formattedMessage);

            boolean isActiveChannel = channel.equalsIgnoreCase(getActiveChannel());

            if (isActiveChannel && !isViewingPrivateMessages && isActivityResumed) {
                addChatMessage(formattedMessage);
            } else {
                updateUnreadCountForChannel(channel);
                updateGlobalUnreadCount();
            }
        });
    }

    private void updateUnreadCountForChannel(String channel) {
        Log.d("NotificationDebug", "ChatActivity: Updating unread count for channel: " + channel);

        if (channelStorage != null) {
            channelStorage.incrementUnreadCount(channel);
            Log.d("NotificationDebug", "ChatActivity: Storage updated. New count: " + channelStorage.getUnreadCount(channel));
        }

        for (ChannelItem channelItem : channelList) {
            if (channelItem.getChannelName().equalsIgnoreCase(channel)) {
                channelItem.incrementUnreadCount();
                break;
            }
        }
        channelAdapter.notifyDataSetChanged();

        Intent intent = new Intent("com.btech.konnectchatirc.CHANNEL_MESSAGE");
        intent.putExtra("channel", channel);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
        Log.d("NotificationDebug", "ChatActivity: Broadcast sent for channel: " + channel);
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
    public void refreshChat() {
        runOnUiThread(() -> {
            if (isViewingPrivateMessages) {
                if (selectedPrivateConversation != null) {
                    loadPrivateConversation(selectedPrivateConversation);
                }
            } else if (activeChannel != null) {
                setActiveChannel(activeChannel);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (privateMessageReceiver != null) {
            try {
                ContextCompat.registerReceiver(this, privateMessageReceiver, new IntentFilter("private_message"), ContextCompat.RECEIVER_NOT_EXPORTED);
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rainbowNicks) {
            rainbowHandler.post(rainbowRunnable);
        }
        isActivityResumed = true;
        refreshChat();
        loadPrivateConversationList();
        updateGlobalUnreadCount();
    }

    @Override
    protected void onPause() {
        super.onPause();
        rainbowHandler.removeCallbacks(rainbowRunnable);
        isActivityResumed = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (privateMessageReceiver != null) {
            try {
                unregisterReceiver(privateMessageReceiver);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BotManager.clearActiveBotProvider();

        dismissMentionPopup();
        disconnectFromServer();
        releaseWakeLock();
        releaseWifiLock();

        if (privateMessageReceiver != null) {
            try {
                unregisterReceiver(privateMessageReceiver);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void disconnectFromServer() {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendIRC().quitServer("https://play.google.com/store/apps/details?id=com.btech.konnectchatirc, Download KonnectChatIRC app today!");
                    bot.stopBotReconnect();
                    bot.close();
                    Log.d("ChatActivity", "IRC connection terminated.");
                } catch (Exception e) {
                    Log.e("ChatActivity", "Error while disconnecting from IRC", e);
                }
            }).start();
        }
    }

    private void initializePrivateMessaging() {
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        messageStorage = new PrivateMessageStorage(prefs);

        privateConversationListView = findViewById(R.id.privateConversationListView);
        Button btnChannelsTab = findViewById(R.id.btnChannelsTab);
        Button btnPrivateMessagesTab = findViewById(R.id.btnPrivateMessagesTab);
        FrameLayout channelsSection = findViewById(R.id.channelsSection);
        FrameLayout privateMessagesSection = findViewById(R.id.privateMessagesSection);

        privateConversationAdapter = new PrivateConversationAdapter(this, privateConversations, messageStorage, userNick);
        privateConversationListView.setAdapter(privateConversationAdapter);

        privateConversationAdapter.setOnConversationDeleteListener(nick -> {
            messageStorage.clearConversation(userNick, nick);
        });

        privateConversationAdapter.setOnConversationSelectListener((nick, position) -> {
            selectedPrivateConversation = nick;
            privateConversationAdapter.setSelectedPosition(position);
            loadPrivateConversation(selectedPrivateConversation);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        privateConversationListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedPrivateConversation = privateConversations.get(position);
            loadPrivateConversation(selectedPrivateConversation);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnChannelsTab.setOnClickListener(v -> switchToChannelsTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));

        btnPrivateMessagesTab.setOnClickListener(v -> switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));

        privateMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");

                if (sender != null && !sender.equalsIgnoreCase(userNick) && !message.isEmpty()) {
                    messageStorage.saveMessage(userNick, sender, message, false);

                    runOnUiThread(() -> {
                        if (privateConversations.contains(sender)) {
                            privateConversations.remove(sender);
                        }
                        privateConversations.add(0, sender);

                        if (sender.equalsIgnoreCase(selectedPrivateConversation) && isViewingPrivateMessages) {
                            loadPrivateConversation(sender);
                        } else {
                            messageStorage.incrementUnreadCount(userNick, sender);
                            updateGlobalUnreadCount();
                            if (pmSoundEnabled || pmBarNotificationEnabled) {
                                showPrivateMessageNotification(sender, message);
                            }
                        }
                        privateConversationAdapter.notifyDataSetChanged();
                    });
                }
            }
        };
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

    private void loadPrivateConversation(String nick) {
        selectedPrivateConversation = nick;
        isViewingPrivateMessages = true;
        updateInputVisibility(true, nick);
        channelNameTextView.setText("Private: " + nick);

        chatMessages.clear();

        List<PrivateMessageStorage.PrivateMessage> messages = messageStorage.getMessages(userNick, nick);
        for (PrivateMessageStorage.PrivateMessage msg : messages) {
            String content = msg.sender + ": " + msg.message;
            chatMessages.add(new ChatMessage(content, msg.timestamp));
        }

        messageStorage.resetUnreadCount(userNick, nick);
        updateGlobalUnreadCount();
        chatAdapter.notifyDataSetChanged();
        if (chatMessages.size() > 0) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    private void sendPrivateMessage(String message) {
        if (message.startsWith("/")) {
            handleCommand(message);
            return;
        }
        if (selectedPrivateConversation == null || selectedPrivateConversation.isEmpty()) {
            Toast.makeText(this, "No conversation selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bot != null && bot.isConnected()) {
            privateConversations.remove(selectedPrivateConversation);
            privateConversations.add(0, selectedPrivateConversation);
            privateConversationAdapter.notifyDataSetChanged();

            privateConversationAdapter.setSelectedPosition(0);

            messageStorage.saveMessage(userNick, selectedPrivateConversation, message, true);

            loadPrivateConversation(selectedPrivateConversation);
            chatEditText.setText("");

            new Thread(() -> {
                try {
                    bot.sendIRC().message(selectedPrivateConversation, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            Toast.makeText(this, "Not connected to IRC.", Toast.LENGTH_SHORT).show();
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

            data.setData(null);

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

                        new Thread(() -> {
                            try {
                                if (bot != null && bot.isConnected()) {
                                    bot.sendIRC().message(activeChannel, imageUrl);
                                    runOnUiThread(() -> {
                                        chatEditText.setText("");
                                        String msg = userNick + ": " + imageUrl;
                                        storeMessageForChannel(activeChannel, msg);
                                        addChatMessage(msg);
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
            return userList;
        }

        try {
            Channel activeChannelObj = bot.getUserChannelDao().getChannel(activeChannel);
            if (activeChannelObj != null) {
                for (User user : activeChannelObj.getUsers()) {
                    String prefix = getUserPrefix(user, activeChannelObj);
                    userList.add(prefix + user.getNick());
                }
            }
        } catch (DaoException e) {
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Channel not found or not yet joined.", Toast.LENGTH_SHORT).show());
        }

        return userList;
    }

    // Refactored to scan the entire server context globally
    public List<String> findDuplicateUsers(User selectedUser) {
        List<String> dupNicks = new ArrayList<>();
        if (selectedUser == null || bot == null || !bot.isConnected()) return dupNicks;

        String currentHost = extractHost(selectedUser.getHostname());
        if (currentHost.isEmpty()) {
            currentHost = extractHost(selectedUser.getHostmask());
        }

        if (currentHost.isEmpty() || isExcludedHost(selectedUser)) {
            return dupNicks;
        }

        try {
            // Search globally across all users the bot knows about
            for (User user : bot.getUserChannelDao().getAllUsers()) {

                String uHost = extractHost(user.getHostname());
                if (uHost.isEmpty()) {
                    uHost = extractHost(user.getHostmask());
                }

                if (!uHost.isEmpty() && currentHost.equalsIgnoreCase(uHost)) {
                    if (!isExcludedHost(user) && !dupNicks.contains(user.getNick())) {
                        dupNicks.add(user.getNick());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dupNicks;
    }

    public boolean isExcludedHost(User user) {
        if (user == null) return false;
        if (user.isIrcop()) return true;

        String hostmask = user.getHostmask();
        String hostname = user.getHostname();
        String lowerHost = "";

        if (hostmask != null) {
            lowerHost = hostmask.toLowerCase();
        } else if (hostname != null) {
            lowerHost = hostname.toLowerCase();
        }

        return lowerHost.contains("ircoperator") || lowerHost.contains("netadmin");
    }

    private String getUserPrefix(User user, Channel channel) {
        if (user == null || channel == null) return "";
        Set<UserLevel> levels = user.getUserLevels(channel);

        if (levels.contains(UserLevel.OWNER)) return "~";
        if (levels.contains(UserLevel.SUPEROP)) return "&";
        if (levels.contains(UserLevel.OP)) return "@";
        if (levels.contains(UserLevel.HALFOP)) return "%";
        if (levels.contains(UserLevel.VOICE)) return "+";
        return "";
    }

    public void addBannedUser(String banEntry) {
        if (!bannedUsers.contains(banEntry)) {
            bannedUsers.add(banEntry);
            Log.d("ChatActivity", "Banned user added: " + banEntry);
            String msg = "Banned user added: " + banEntry;
            storeMessageForChannel("Server Notices", msg);
            addChatMessage(msg);
        } else {
            Log.d("ChatActivity", "Banned user already in list: " + banEntry);
        }
    }

    public void checkAndAddActiveChannel() {
        if (activeChannel != null && !isChannelInList(activeChannel)) {
            channelList.add(new ChannelItem(activeChannel));
            if (!channelMessagesMap.containsKey(activeChannel.toLowerCase())) {
                channelMessagesMap.put(activeChannel.toLowerCase(), new ArrayList<>());
            }
            channelAdapter.notifyDataSetChanged();
        }
    }
    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }

    private void initializeMentionPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_mentions, null);
        suggestionRecyclerView = popupView.findViewById(R.id.suggestionRecyclerView);
        suggestionRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        suggestionAdapter = new MentionSuggestionAdapter(new ArrayList<>(), suggestion -> {
            insertMention(suggestion, mentionStartIndex);
            dismissMentionPopup();
        });

        suggestionRecyclerView.setAdapter(suggestionAdapter);

        suggestionRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int height = suggestionRecyclerView.getHeight();
            if (height > 600) {
                height = 600;
            }
            mentionPopupWindow.setHeight(height);
        });

        mentionPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(200),
                true
        );

        mentionPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mentionPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background));
        mentionPopupWindow.setOutsideTouchable(true);
        mentionPopupWindow.setFocusable(false);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void showMentionPopup(String currentQuery) {
        if (bot == null || !bot.isConnected()) {
            return;
        }

        List<String> userList = getUserListFromActiveChannel();
        if (userList.isEmpty()) {
            return;
        }

        List<String> filteredList;
        if (currentQuery.isEmpty()) {
            filteredList = new ArrayList<>(userList);
        } else {
            filteredList = new ArrayList<>();
            for (String user : userList) {
                if (user.toLowerCase().startsWith(currentQuery.toLowerCase())) {
                    filteredList.add(user);
                }
            }
        }

        if (filteredList.isEmpty()) {
            dismissMentionPopup();
            return;
        }

        suggestionAdapter.updateSuggestions(filteredList);

        int[] location = new int[2];
        chatEditText.getLocationOnScreen(location);
        int yOffset = location[1] - chatEditText.getHeight() - mentionPopupWindow.getHeight();

        if (yOffset < 0) {
            yOffset = 0;
        }

        mentionPopupWindow.showAtLocation(chatEditText, Gravity.NO_GRAVITY, 0, yOffset);
    }

    private void updateMentionSuggestions() {
        int cursorPosition = chatEditText.getSelectionStart();
        if (mentionStartIndex < 0 || cursorPosition < mentionStartIndex) {
            dismissMentionPopup();
            return;
        }

        if (mentionStartIndex + 1 > cursorPosition) {
            dismissMentionPopup();
            return;
        }

        String query = chatEditText.getText().toString().substring(mentionStartIndex + 1, cursorPosition).toLowerCase();

        if (query.isEmpty()) {
            List<String> userList = getUserListFromActiveChannel();
            suggestionAdapter.updateSuggestions(userList);
        } else {
            List<String> userList = getUserListFromActiveChannel();
            List<String> filteredList = new ArrayList<>();

            for (String user : userList) {
                if (user.toLowerCase().startsWith(query)) {
                    filteredList.add(user);
                }
            }

            if (filteredList.isEmpty()) {
                dismissMentionPopup();
            } else {
                suggestionAdapter.updateSuggestions(filteredList);
            }
        }
    }

    private void dismissMentionPopup() {
        if (mentionPopupWindow != null && mentionPopupWindow.isShowing()) {
            mentionPopupWindow.dismiss();
        }
        isMentionActive = false;
        mentionStartIndex = -1;
        Log.d("MentionFeature", "Mention popup dismissed and indices reset.");
    }

    private void insertMention(String nickname, int mentionStartIndex) {
        if (mentionStartIndex < 0) {
            Log.e("MentionFeature", "Invalid mentionStartIndex: " + mentionStartIndex);
            return;
        }

        Editable editable = chatEditText.getText();
        if (editable == null) {
            Log.e("MentionFeature", "Editable text is null.");
            return;
        }

        int cursorPosition = chatEditText.getSelectionStart();
        if (cursorPosition < mentionStartIndex) {
            Log.e("MentionFeature", "Cursor position (" + cursorPosition + ") is before mentionStartIndex (" + mentionStartIndex + ").");
            dismissMentionPopup();
            return;
        }

        if (mentionStartIndex + 1 > cursorPosition) {
            Log.e("MentionFeature", "mentionStartIndex + 1 (" + (mentionStartIndex + 1) + ") exceeds cursorPosition (" + cursorPosition + ").");
            dismissMentionPopup();
            return;
        }

        try {
            editable.delete(mentionStartIndex, cursorPosition);
        } catch (IndexOutOfBoundsException e) {
            Log.e("MentionFeature", "Error deleting text for mention insertion.", e);
            dismissMentionPopup();
            return;
        }

        editable.insert(mentionStartIndex, "@" + nickname + " ");

        isMentionActive = false;
        mentionStartIndex = -1;

        Log.d("MentionFeature", "Inserted mention: @" + nickname + " at index: " + mentionStartIndex);
    }

    public void onRemoveChannel(String channelName) {
        partChannel(channelName);
        removeChannel(channelName);

        if (!channelList.isEmpty()) {
            setActiveChannel(channelList.get(channelList.size() - 1).getChannelName());
        } else {
            activeChannel = null;
            updateChannelName("");
            chatMessages.clear();
            chatAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onChannelClick(ChannelItem channel) {
        switchChannel(channel);
    }

    @Override
    public void onLeaveChannelClick(ChannelItem channel) {
        onRemoveChannel(channel.getChannelName());
    }

    public interface OnChannelClickListener {
        void onChannelClick(ChannelItem channel);
        void onLeaveChannelClick(ChannelItem channel);
    }

    private void initializeCommandPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_commands, null);
        commandSuggestionRecyclerView = popupView.findViewById(R.id.suggestionRecyclerView);
        commandSuggestionRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        commandSuggestionAdapter = new MentionSuggestionAdapter(new ArrayList<>(), suggestion -> {
            insertCommand(suggestion, commandStartIndex);
            dismissCommandPopup();
        });

        commandSuggestionRecyclerView.setAdapter(commandSuggestionAdapter);

        commandPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        commandPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        commandPopupWindow.setOutsideTouchable(true);
        commandPopupWindow.setFocusable(false);
    }

    private void showCommandPopup(String currentQuery) {
        if (commandPopupWindow == null) return;

        List<String> filteredCommands;
        if (currentQuery.isEmpty()) {
            filteredCommands = new ArrayList<>(commandList);
        } else {
            filteredCommands = new ArrayList<>();
            for (String command : commandList) {
                if (command.toLowerCase().startsWith(currentQuery.toLowerCase())) {
                    filteredCommands.add(command);
                }
            }
        }

        if (filteredCommands.isEmpty()) {
            dismissCommandPopup();
            return;
        }

        commandSuggestionAdapter.updateSuggestions(filteredCommands);
        commandPopupWindow.showAsDropDown(chatEditText);
    }

    private void dismissCommandPopup() {
        if (commandPopupWindow != null && commandPopupWindow.isShowing()) {
            commandPopupWindow.dismiss();
        }
        isCommandActive = false;
        commandStartIndex = -1;
    }

    private void insertCommand(String command, int commandStartIndex) {
        if (commandStartIndex < 0) return;

        Editable editable = chatEditText.getText();
        if (editable == null) return;

        int cursorPosition = chatEditText.getSelectionStart();
        if (cursorPosition < commandStartIndex) return;

        try {
            editable.delete(commandStartIndex, cursorPosition);
        } catch (IndexOutOfBoundsException e) {
            dismissCommandPopup();
            return;
        }

        editable.insert(commandStartIndex, command + " ");
    }

    public void updateUserList() {
        String activeChannel = getActiveChannel();
        if (activeChannel != null && bot != null) {
            Channel channel = bot.getUserChannelDao().getChannel(activeChannel);
            if (channel != null) {
                userList.clear();
                for (User user : channel.getUsers()) {
                    String prefix = String.valueOf(IrcUtils.getUserPrefix(user, channel));
                    userList.add(prefix + user.getNick());
                }
                runOnUiThread(() -> userListAdapter.notifyDataSetChanged());
            }
        }
    }

    private void switchToChannelsTab(Button btnChannelsTab, Button btnPrivateMessagesTab,
                                     FrameLayout channelsSection, FrameLayout privateMessagesSection) {
        isViewingPrivateMessages = false;
        selectedPrivateConversation = null;
        chatMessages.clear();
        if (activeChannel != null && channelMessagesMap.containsKey(activeChannel.toLowerCase())) {
            chatMessages.addAll(channelMessagesMap.get(activeChannel.toLowerCase()));
        }
        chatAdapter.notifyDataSetChanged();
        animateSectionTransition(channelsSection, privateMessagesSection);

        btnChannelsTab.clearAnimation();
        btnChannelsTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_select));
        btnChannelsTab.setBackgroundResource(R.drawable.tab_indicator_active);
        btnChannelsTab.setTextColor(Color.WHITE);
        btnChannelsTab.setAlpha(1.0f);

        btnPrivateMessagesTab.clearAnimation();
        btnPrivateMessagesTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_deselect));
        btnPrivateMessagesTab.setBackgroundResource(R.drawable.tab_indicator);
        btnPrivateMessagesTab.setTextColor(getResources().getColor(R.color.tab_text_inactive));
        btnPrivateMessagesTab.setAlpha(0.8f);
    }

    private void switchToPrivateMessagesTab(Button btnChannelsTab, Button btnPrivateMessagesTab,
                                            FrameLayout channelsSection, FrameLayout privateMessagesSection) {
        isViewingPrivateMessages = true;
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        loadPrivateConversationList();
        updateInputVisibility(selectedPrivateConversation != null, selectedPrivateConversation != null ? selectedPrivateConversation : "Private Messages");
        animateSectionTransition(privateMessagesSection, channelsSection);

        btnPrivateMessagesTab.clearAnimation();
        btnPrivateMessagesTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_select));
        btnPrivateMessagesTab.setBackgroundResource(R.drawable.tab_indicator_active);
        btnPrivateMessagesTab.setTextColor(Color.WHITE);
        btnPrivateMessagesTab.setAlpha(1.0f);

        btnChannelsTab.clearAnimation();
        btnChannelsTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_deselect));
        btnChannelsTab.setBackgroundResource(R.drawable.tab_indicator);
        btnChannelsTab.setTextColor(getResources().getColor(R.color.tab_text_inactive));
        btnChannelsTab.setAlpha(0.8f);
    }

    private void animateSectionTransition(View showView, View hideView) {
        showView.setVisibility(View.VISIBLE);
        showView.setAlpha(0f);
        showView.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);

        hideView.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Private Messages";
            String description = "Notifications for private messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(PM_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void showPrivateMessageNotification(String sender, String message) {
        if (isActivityResumed && isViewingPrivateMessages && sender.equalsIgnoreCase(selectedPrivateConversation)) {
            return;
        }

        if (pmSoundEnabled) {
            try {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    new Thread(toneGen::release).start();
                }, 200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pmBarNotificationEnabled) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("OPEN_PM_NICK", sender);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, sender.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PM_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Message From: \"" + sender + "\"")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            try {
                notificationManager.notify(sender.hashCode(), builder.build());
            } catch (SecurityException e) {
                Log.e("ChatActivity", "Missing POST_NOTIFICATIONS permission", e);
            }
        }
    }
    private void animateSectionTransition(FrameLayout showSection, FrameLayout hideSection) {
        hideSection.animate()
                .alpha(0.0f)
                .setDuration(150)
                .withEndAction(() -> hideSection.setVisibility(View.GONE))
                .start();
        showSection.setAlpha(0.0f);
        showSection.setVisibility(View.VISIBLE);
        showSection.animate()
                .alpha(1.0f)
                .setDuration(200)
                .start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.hasExtra("OPEN_PM_NICK")) {
            String targetNick = intent.getStringExtra("OPEN_PM_NICK");
            if (targetNick != null && !targetNick.isEmpty()) {
                Button btnChannelsTab = findViewById(R.id.btnChannelsTab);
                Button btnPrivateMessagesTab = findViewById(R.id.btnPrivateMessagesTab);
                FrameLayout channelsSection = findViewById(R.id.channelsSection);
                FrameLayout privateMessagesSection = findViewById(R.id.privateMessagesSection);

                if (btnChannelsTab != null && btnPrivateMessagesTab != null &&
                        channelsSection != null && privateMessagesSection != null) {
                    switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection);
                }

                if (!privateConversations.contains(targetNick)) {
                    privateConversations.add(0, targetNick);
                    privateConversationAdapter.notifyDataSetChanged();
                }

                loadPrivateConversation(targetNick);
            }
        }
    }
}