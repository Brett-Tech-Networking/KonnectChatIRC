package com.btech.konnectchatirc;

import static androidx.core.util.TypedValueCompat.dpToPx;

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
import org.pircbotx.UserLevel;
import java.util.Set;
import org.pircbotx.UserLevel;
import org.pircbotx.cap.EnableCapHandler;
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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
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
import org.pircbotx.hooks.Listener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import android.text.util.Linkify;
import androidx.recyclerview.widget.LinearLayoutManager;


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
    private static final String IMGUR_CLIENT_ID = "4968ca92805f1b2"; // Replace with your Imgur client ID
    private ConnectivityManager connectivityManager;
    private DrawerLayout drawerLayout;
    private ChannelAdapter channelAdapter;
    private List<ChannelItem> channelList = new ArrayList<>(); // List to hold channel items
    private Map<String, List<ChatMessage>> channelMessagesMap = new HashMap<>(); // Stores messages for each channel
    private TextView unreadBadge;
    private int totalUnreadMessages = 0; // Tracks channel unread messages
    private String desiredPassword;
    private ProgressDialog progressDialog; // Declare ProgressDialog
    private String desiredNick;  // Declare desiredNick as a class-level variable
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
                connectToIrcServer();  // Reconnect when network is back
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d("NetworkCallback", "Network lost.");
            // Optionally notify the user, but do not disconnect the bot right away
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
    private static final long TYPING_SEND_INTERVAL = 3000; // 3 seconds debounce for sending
    private Set<String> typingUsers = new HashSet<>();
    private ChannelStorage channelStorage;
    private boolean rainbowNicks = false; // Rainbow Nicks setting
    
    private final Handler rainbowHandler = new Handler(Looper.getMainLooper());
    private final Runnable rainbowRunnable = new Runnable() {
        @Override
        public void run() {
            if (rainbowNicks && chatAdapter != null) {
                // Notify adapter to rebind views, which triggers RainbowSpan.updateDrawState
                chatAdapter.notifyDataSetChanged();
                rainbowHandler.postDelayed(this, 20); // 50 FPS for smooth color animation
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
                if (count >= 3) break; // Limit to 3 names
            }
            if (typingUsers.size() > 3) {
                sb.append(" and ").append(typingUsers.size() - 3).append(" others");
            }
            sb.append(typingUsers.size() == 1 ? " is typing..." : " are typing...");
            typingIndicator.setText(sb.toString());
            
            // Auto-hide after 6 seconds if no updates
            typingHandler.removeCallbacksAndMessages(null);
            typingHandler.postDelayed(() -> {
                typingUsers.clear();
                updateTypingIndicator();
            }, 6000);
        }
    }

    private void sendTyping(boolean active) {
        if (bot != null && bot.isConnected() && activeChannel != null) {
            long now = System.currentTimeMillis();
            if (active) {
                if (now - lastTypingSent > TYPING_SEND_INTERVAL) {
                    new Thread(() -> bot.sendRaw().rawLine("@+typing=active TAGMSG " + activeChannel)).start();
                    lastTypingSent = now;
                }
            } else {
                new Thread(() -> bot.sendRaw().rawLine("@+typing=done TAGMSG " + activeChannel)).start();
                lastTypingSent = 0; // Reset to allow immediate active status next time
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register this activity as the active bot provider
        BotManager.setActiveBotProvider(this);
        
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_chat);
// Inside onCreate() in ChatActivity
        acquireWakeLock();
        acquireWifiLock();
        
        // Start rainbow animation if enabled
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

        String desiredNick = getIntent().getStringExtra("DESIRED_NICK");
        desiredPassword = getIntent().getStringExtra("DESIRED_PASSWORD"); // Retrieve password
        // Initialize ConnectivityManager here
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        client = new OkHttpClient(); // Initialize OkHttpClient

        // Initialize the RecyclerView for chat messages
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        // Initialize Server Notices channel
        ChannelItem serverNotices = new ChannelItem("Server Notices");
        channelList.add(serverNotices);
        channelMessagesMap.put("Server Notices", new ArrayList<>());
        
        chatAdapter = new ChatAdapter((BotProvider) this, chatMessages);  // Pass ChatActivity instance
        
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);

        showTimestamps = prefs.getBoolean("show_timestamps", false);
        boolean use24HrFormat = prefs.getBoolean("use_24hr_format", true);
        rainbowNicks = prefs.getBoolean("rainbow_nicks", false); // Load setting
        chatAdapter.setShowTimestamps(showTimestamps);
        chatAdapter.setUse24HrFormat(use24HrFormat);
        chatAdapter.setRainbowEnabled(rainbowNicks);

        // Initialize PrivateMessageStorage
        messageStorage = new PrivateMessageStorage(prefs);
        channelStorage = new ChannelStorage(prefs);
        
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);  // Ensure the adapter is set here

        // Add layout change listener for keyboard handling
        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                // Keyboard likely opened (height decreased)
                // In this case, we almost always want to ensure the last message is visible
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
        // Initialize the user list adapter
        userListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);

        // Assuming you have a ListView for the user list in your layout
        ListView userListView = findViewById(R.id.userListView);
        userListView.setAdapter(userListAdapter);

        String selectedChannel = getIntent().getStringExtra("SELECTED_CHANNEL");

        channelNameTextView = findViewById(R.id.ChannelName);
        chatEditText = findViewById(R.id.chatEditText);

        initializeCommandPopup();
        initializeMentionPopup();

        typingIndicator = findViewById(R.id.typingIndicator);

        // Add TextWatcher to chatEditText
        chatEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int cursorPosition = chatEditText.getSelectionStart();
                if (cursorPosition < 0) return;

                String text = s.toString();
                
                // Typing Indicator Logic
                if (!text.startsWith("/")) { // Don't send typing for commands
                    if (text.length() > 0) {
                        sendTyping(true);
                        // Schedule stop typing if user pauses
                        if (stopTypingRunnable != null) typingHandler.removeCallbacks(stopTypingRunnable);
                        stopTypingRunnable = () -> sendTyping(false);
                        typingHandler.postDelayed(stopTypingRunnable, 3000);
                    } else {
                        sendTyping(false);
                    }
                }

                if (cursorPosition > text.length()) return;

                // **Detect if the cursor is currently typing a command** (starts with `/`)
                int slashIndex = text.lastIndexOf('/', cursorPosition - 1);
                if (slashIndex != -1 && (slashIndex == 0 || Character.isWhitespace(text.charAt(slashIndex - 1)))) {
                    // Extract the command text after the '/'
                    String commandText = text.substring(slashIndex + 1, cursorPosition);
                    if (!commandText.contains(" ")) { // No space within the command
                        isCommandActive = true;
                        commandStartIndex = slashIndex;
                        currentQuery = text.substring(commandStartIndex + 1, cursorPosition);
                        showCommandPopup(currentQuery);
                    }
                } else {
                    // If no active command is detected, dismiss the command popup
                    if (isCommandActive) {
                        isCommandActive = false;
                        commandStartIndex = -1;
                        dismissCommandPopup();
                    }
                }

                // **Detect if the cursor is currently typing a mention** (starts with `@`)
                int atIndex = text.lastIndexOf('@', cursorPosition - 1);
                if (atIndex != -1 && (atIndex == 0 || Character.isWhitespace(text.charAt(atIndex - 1)))) {
                    // Extract the mention text after the '@'
                    String mentionText = text.substring(atIndex + 1, cursorPosition);
                    if (!mentionText.contains(" ")) { // No space within the mention
                        isMentionActive = true;
                        mentionStartIndex = atIndex;
                        currentQuery = text.substring(mentionStartIndex + 1, cursorPosition);
                        showMentionPopup(currentQuery);
                    }
                } else {
                    // If no active mention is detected, dismiss the mention popup
                    if (isMentionActive) {
                        isMentionActive = false;
                        mentionStartIndex = -1;
                        dismissMentionPopup();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });


        adminButton = findViewById(R.id.adminButton);
        ImageButton sendButton = findViewById(R.id.sendButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);

        if (desiredNick != null && !desiredNick.isEmpty()) {
            userNick = desiredNick;  // Use the custom nickname if provided
        } else {
            userNick = "Guest" + (1000 + (int) (Math.random() * 9000));  // Fallback to Guest nick if no custom nick is provided
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
        Button btnNickRegister = hoverPanel.findViewById(R.id.btnNickRegister);
        Button btnChanRegister = hoverPanel.findViewById(R.id.btnChanRegister);
        Button btnSOP = hoverPanel.findViewById(R.id.btnSOP);
        Button btnAOP = hoverPanel.findViewById(R.id.btnAOP);
        Button btnBan = hoverPanel.findViewById(R.id.btnBan);
        Button btnUnban = hoverPanel.findViewById(R.id.btnUnban);
        operatorButton = hoverPanel.findViewById(R.id.btnOperator);

        // Initialize buttons from operator panel
        btnKill = operatorPanel.findViewById(R.id.btnKill);
        btnOperLogin = operatorPanel.findViewById(R.id.btnOperLogin);
        btnSajoin = operatorPanel.findViewById(R.id.btnSajoin);
        Button btnSapart = operatorPanel.findViewById(R.id.btnSapart);
        Button btnOSLogin = operatorPanel.findViewById(R.id.btnOSLogin);
        Button btnZline = operatorPanel.findViewById(R.id.btnZline);
        ImageButton btnOperatorBack = operatorPanel.findViewById(R.id.btnOperatorBack);

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

        btnNickRegister.setOnClickListener(v -> Toast.makeText(this, "Nick Registration: Use /msg NickServ REGISTER <pass> <email>", Toast.LENGTH_LONG).show());
        btnChanRegister.setOnClickListener(v -> Toast.makeText(this, "Chan Registration: Use /msg ChanServ REGISTER #chan <pass> <desc>", Toast.LENGTH_LONG).show());
        btnSOP.setOnClickListener(v -> Toast.makeText(this, "SOP: Use /msg ChanServ SOP #chan ADD <nick>", Toast.LENGTH_SHORT).show());
        btnAOP.setOnClickListener(v -> Toast.makeText(this, "AOP: Use /msg ChanServ AOP #chan ADD <nick>", Toast.LENGTH_SHORT).show());

        btnKill.setOnClickListener(v -> new Kill(this, bot, this).startKillProcess());

        btnOperLogin.setOnClickListener(v -> new OperLogin(this, this, bot).startOperLoginProcess());

        btnSajoin.setOnClickListener(v -> new Sajoin(this, bot, this).startSajoinProcess());
        btnSapart.setOnClickListener(v -> new Sajoin(this, bot, this).startSapartProcess());

        // Back navigation
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
        
        // Initial selection highlight
        if (activeChannel != null) {
            channelAdapter.setSelectedChannelName(activeChannel);
        }

        // Initialize private messaging
        initializePrivateMessaging();

        btnOSLogin.setOnClickListener(v -> Toast.makeText(this, "OS Login not yet implemented", Toast.LENGTH_SHORT).show());
        btnZline.setOnClickListener(v -> new Kill(this, bot, this).startKillProcess()); // Reusing Kill for now, can be specialized later

        sendButton.setOnClickListener(v -> {
            String message = chatEditText.getText().toString();
            if (!message.isEmpty()) {
                if (isViewingPrivateMessages && selectedPrivateConversation != null) {
                    // Send private message
                    sendPrivateMessage(message);
                } else if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    addChatMessage(userNick + ": " + message);
                    chatEditText.setText("");
                    sendTyping(false); // Reset typing status
                    
                    // Dismiss mention popup if active
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
                        bot.sendIRC().quitServer("https://www.BrettTechCoding.com, A BrettTech Client, Goodbye!");
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
        
        // Initialize Private Message Receiver
        privateMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("private_message".equals(intent.getAction())) {
                    String sender = intent.getStringExtra("sender");
                    String message = intent.getStringExtra("message");
                    
                    if (sender != null && message != null) {
                        // Store message
                        if (messageStorage != null) {
                            messageStorage.saveMessage(sender, userNick, message, false);
                        }
                        
                        // If viewing this conversation, add to UI
                        if (isViewingPrivateMessages && sender.equalsIgnoreCase(selectedPrivateConversation)) {
                            ChatMessage chatMsg = new ChatMessage(sender + ": " + message, System.currentTimeMillis());
                            Log.d("ChatActivity", "Adding private message to list: " + sender + ": " + message);
                            chatMessages.add(chatMsg);
                            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                        } else {
                            // Increment unread count if not viewing this conversation
                            if (messageStorage != null) {
                                messageStorage.incrementUnreadCount(userNick, sender);
                            }
                            // Refresh conversation list if visible
                            if (privateConversationAdapter != null) {
                                privateConversationAdapter.notifyDataSetChanged();
                            }
                            updateGlobalUnreadCount();
                        }
                    }
                }
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(privateMessageReceiver, new IntentFilter("private_message"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(privateMessageReceiver, new IntentFilter("private_message"));
        }
            Log.d("ChatActivity", "Selected Server: " + selectedServer);

            if (selectedServer == null || selectedServer.isEmpty()) {
                selectedServer = "KonnectChat IRC"; // Default or fallback server
            }


            Configuration.Builder configurationBuilder = new Configuration.Builder()
                    .setName(userNick)
                    .setLogin("KCIRC")
                    .setAutoNickChange(true)
                    .setRealName("TPTC IRC Client")
                    .addAutoJoinChannel(selectedChannel)
                    .addListener(new Listeners(this))
                    .addCapHandler(new EnableCapHandler("extended-join"))
                    .addCapHandler(new EnableCapHandler("account-notify"))
                    .addCapHandler(new EnableCapHandler("message-tags"))
                    .setAutoSplitMessage(true)
                    .setAutoReconnect(true)
                    .addCapHandler(new EnableCapHandler("multi-prefix"))

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
                                updateCurrentNick(userNick);  // Apply the custom or guest nickname
                            });

                            // Apply the custom or guest nickname immediately
                            bot.sendRaw().rawLine("NICK " + userNick);

                            // Condition 1: Custom Nick, No Password
                            if (desiredNick != null && !desiredNick.isEmpty() && (desiredPassword == null || desiredPassword.isEmpty())) {
                                runOnUiThread(() -> addChatMessage("Nick changed to: " + userNick + ". No password provided, skipping identification."));
                            }

                            // Condition 2: Custom Nick and Password
                            if (desiredNick != null && !desiredNick.isEmpty() && desiredPassword != null && !desiredPassword.isEmpty()) {
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    String identifyCommand = "PRIVMSG NickServ :IDENTIFY " + userNick + " " + desiredPassword;
                                    bot.sendRaw().rawLine(identifyCommand);
                                    runOnUiThread(() -> addChatMessage("Identifying user: " + userNick));
                                }, 500); // Short delay before identification
                            }

                            updateChannelListAfterDelay();

                            // Start the custom ping thread after the bot is connected
                            new Thread(() -> {
                                while (true) {
                                    try {
                                        bot.sendRaw().rawLine("PING " + bot.getServerInfo().getServerName());
                                        Thread.sleep(60 * 1000); // Send PING every 60 seconds
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        break; // Exit the loop if something goes wrong
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
                            Thread.sleep(2000); // Wait before retrying
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (IOException | IrcException e) {
                    retries++;
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error connecting to IRC server. Retrying...", Toast.LENGTH_LONG).show());

                    try {
                        Thread.sleep(5000); // Wait before retrying
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
        // Add the message to the list as a ChatMessage object
        // We pass the raw String so the ChatAdapter can handle formatting (Rank colors, Mentions, etc.)
        ChatMessage chatMsg = new ChatMessage(message, System.currentTimeMillis());
        chatMessages.add(chatMsg);
        
        // Note: We do NOT add to channelMessagesMap here because processServerMessage() 
        // and sendMessage() already call storeMessageForChannel() to handle history.
        // This prevents duplicate messages.

        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatMessages.isEmpty()) return;

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
    
    // Helper to store messages safely
    void storeMessageForChannel(String channel, String message) {
        if (channelMessagesMap.containsKey(channel)) {
            // Create a temporary ChatMessage with current time for storage
            // Note: This matches the one displayed in UI
            ChatMessage chatMsg = new ChatMessage(message, System.currentTimeMillis());
            channelMessagesMap.get(channel).add(chatMsg);
        }
    }
    
    // Overloaded method to store actual ChatMessage objects
    void storeMessageForChannel(String channel, ChatMessage message) {
        if (channelMessagesMap.containsKey(channel)) {
            channelMessagesMap.get(channel).add(message);
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        // Create a layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Timestamps Switch
        final CheckBox timestampCheck = new CheckBox(this);
        timestampCheck.setText("Show Timestamps");
        timestampCheck.setChecked(showTimestamps);
        timestampCheck.setTextSize(16);
        
        // 24hr Format Switch
        final CheckBox formatCheck = new CheckBox(this);
        formatCheck.setText("Use 24-hour format");
        formatCheck.setChecked(chatAdapter.isUse24HrFormat()); // Use getter
        formatCheck.setTextSize(16);
        
        final CheckBox rainbowCheck = new CheckBox(this);
        rainbowCheck.setText("Rainbow Nicks");
        rainbowCheck.setChecked(rainbowNicks);
        rainbowCheck.setTextSize(16);
        rainbowCheck.setTextColor(Color.MAGENTA); // Formatting flair for the option itself

        layout.addView(timestampCheck);
        layout.addView(formatCheck);
        layout.addView(rainbowCheck);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            boolean newShowTimestamps = timestampCheck.isChecked();
            boolean newUse24HrFormat = formatCheck.isChecked();
            boolean newRainbowNicks = rainbowCheck.isChecked();
            
            SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            boolean changesMade = false;

            if (showTimestamps != newShowTimestamps) {
                showTimestamps = newShowTimestamps;
                editor.putBoolean("show_timestamps", showTimestamps);
                chatAdapter.setShowTimestamps(showTimestamps);
                changesMade = true;
            }
            
            // We don't have a local member for use24HrFormat in Activity, so we check against value in adapter or prefs would be needed
            // But simplify: always save if checked state matches what we want
            if (chatAdapter.isUse24HrFormat() != newUse24HrFormat) {
                editor.putBoolean("use_24hr_format", newUse24HrFormat);
                chatAdapter.setUse24HrFormat(newUse24HrFormat);
                changesMade = true;
            }

            if (rainbowNicks != newRainbowNicks) {
                rainbowNicks = newRainbowNicks;
                editor.putBoolean("rainbow_nicks", rainbowNicks);
                chatAdapter.setRainbowEnabled(rainbowNicks);
                if (rainbowNicks) {
                    rainbowHandler.post(rainbowRunnable); // Start animation
                } else {
                    rainbowHandler.removeCallbacks(rainbowRunnable); // Stop animation
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

    public String getActiveChannel() {
        return activeChannel;
    }

    public void setActiveChannel(String channel) {
        this.activeChannel = channel;
        updateChannelName(channel);
        updateUserCount(); // Update user count for new channel
        
        // Reset unread count for this channel
        if (channelStorage != null) {
            channelStorage.resetUnreadCount(channel);
        }
        // Also update the item in the list immediately to reflect the change visually
        for (ChannelItem item : channelList) {
            if (item.getChannelName().equalsIgnoreCase(channel)) {
                item.setUnreadCount(0);
                break;
            }
        }
        if (channelAdapter != null) {
             channelAdapter.notifyDataSetChanged();
        }

        chatMessages.clear();
        if (channelMessagesMap.containsKey(channel)) {
            chatMessages.addAll(channelMessagesMap.get(channel));
        } else {
            channelMessagesMap.put(channel, new ArrayList<>());
        }
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        
        // Update channel selection highlight in sidebar
        if (channelAdapter != null) {
            channelAdapter.setSelectedChannelName(channel);
        }
        
        // Disable input for Server Notices
        if (channel.equalsIgnoreCase("Server Notices")) {
            chatEditText.setEnabled(false);
            chatEditText.setHint("Server Notices (Read-Only)");
        } else {
            chatEditText.setEnabled(true);
            chatEditText.setHint("Message " + channel);
        }
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
    }

    public void joinChannel(String channelName) {
        if (channelName.isEmpty()) {
            addChatMessage("Usage: /join <channel>");
            chatEditText.setText("");
            return;
        }

        // Convert the channel name to lowercase
        final String lowerCaseChannelName = channelName.toLowerCase();

        new Thread(() -> {
            try {
                if (isNetworkAvailable()) {
                    if (bot.isConnected()) {
                        bot.sendIRC().joinChannel(lowerCaseChannelName);
                        runOnUiThread(() -> {
                            // Check if the channel is already in the list before adding
                            if (!isChannelInList(lowerCaseChannelName)) {
                                channelList.add(new ChannelItem(lowerCaseChannelName));
                                channelMessagesMap.put(lowerCaseChannelName, new ArrayList<>());
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
        currentNickTextView.setText(newNick);
        updateUserCount(); // Update user count when nick changes
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
        // The user specified: name#numbers followed by @ Discord/Konnect-Chat Bot name IRCRELAY
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
            
            // Highlight Discord Users visually
            if (isDiscordUser(realName)) {
                avatarView.setImageResource(android.R.drawable.ic_lock_idle_lock); // Shield/Lock icon for Discord
                avatarView.setBackgroundResource(R.drawable.circle_badge);
                avatarView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#7289DA"))); // Discord Blurple
            }
            
            realNameView.setText("Name: " + (realName != null ? realName : "N/A"));
            identView.setText("User: " + (ident != null ? ident : "N/A"));
            hostView.setText("Host: " + (host != null ? host : "N/A"));
            serverView.setText("Server: " + (server != null ? server : "N/A"));
            
            // Handle clickable channels
            if (channels != null && !channels.isEmpty()) {
                SpannableStringBuilder builderChannels = new SpannableStringBuilder("Channels: ");
                // Channels string is usually "[#chan1, @#chan2, ...]" or "#chan1 #chan2" depending on PircBotX conversion
                // The toString() of a list is usually [#chan, @#chan]
                String listContent = channels;
                if (listContent.startsWith("[") && listContent.endsWith("]")) {
                    listContent = listContent.substring(1, listContent.length() - 1);
                }
                
                String[] channelArray = listContent.split(",?\\s+");
                for (int i = 0; i < channelArray.length; i++) {
                    final String rawChan = channelArray[i].trim();
                    if (rawChan.isEmpty()) continue;
                    
                    // Remove prefixes like @, +, %, etc. to get the pure channel name for joining
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
                                ds.setColor(Color.parseColor("#4FC3F7")); // Light blue for links
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
                awayView.setTextColor(Color.parseColor("#FF9800")); // Orange for away
            } else {
                awayView.setText("Status: Online");
            }

            AlertDialog dialog = builder.create();
            dialogView.findViewById(R.id.btnWhoisClose).setOnClickListener(v -> dialog.dismiss());
            dialog.show();

            // Adjust dialog size and center
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
        
        // Recalculate channel unread counts
        totalUnreadMessages = 0;
        // Recalculate channel unread counts from storage + active list (sync if needed)
        totalUnreadMessages = 0;
        
        // If we have storage, use it as source of truth for unread counts, but only for active channels
        if (channelStorage != null) {
             // Only count unread messages for channels we are actually in
             for (ChannelItem item : channelList) {
                 String key = item.getChannelName(); // Use channel name as key
                 int count = channelStorage.getUnreadCount(key);
                 item.setUnreadCount(count); // Sync memory object with storage
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
        // Only resets the badge visibility if there are no unread messages total
        updateGlobalUnreadCount();
    }

    public void processServerMessage(String sender, String message, String requestChannel) {
        runOnUiThread(() -> {
            // Never add channel messages while viewing private messages
            if (isViewingPrivateMessages) {
                // Still store the message for the channel, but don't add to current display
                storeMessageForChannel(requestChannel, sender + ": " + message);
                return;
            }

            String channel = requestChannel;
            if (channel == null) {
                // Handle null case if necessary, e.g., skip or set to a default channel
                channel = getActiveChannel(); // Set to active channel as fallback
            }

            String prefix = "";
            User senderUser = null;

            if (bot != null && bot.isConnected()) {
                Channel channelObj = null;
                try {
                    channelObj = bot.getUserChannelDao().getChannel(channel);
                } catch (Exception e) {
                    // Channel might not be tracked yet or invalid
                    Log.w("processServerMessage", "Channel not found in DAO: " + channel);
                }

                if (channelObj != null) {
                    // Retrieve the senderUser from the channel's user list
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

            // Since we are now on the UI thread, isActivityResumed reading is safe and accurate
            if (isActiveChannel && !isViewingPrivateMessages && isActivityResumed) {
                addChatMessage(formattedMessage);
            } else {
                updateGlobalUnreadCount();  // Updates the global unread message badge
                updateUnreadCountForChannel(channel); // Update unread count for the specific channel
            }
        });
    }

    private void updateUnreadCountForChannel(String channel) {
        Log.d("NotificationDebug", "ChatActivity: Updating unread count for channel: " + channel);
        // Always update storage first to ensure it's persisted, using case-insensitive key internally
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
        
        // Broadcast the new message event for other activities (like PrivateChatActivity)
        Intent intent = new Intent("com.btech.konnectchatirc.CHANNEL_MESSAGE");
        intent.putExtra("channel", channel);
        intent.setPackage(getPackageName()); // Explicitly target our own app
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
                registerReceiver(privateMessageReceiver, new IntentFilter("private_message"), Context.RECEIVER_EXPORTED);
            } catch (Exception e) {
                // Already registered
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rainbowNicks) {
            rainbowHandler.post(rainbowRunnable); // Start animation
        }
        isActivityResumed = true;
        // Resume updates and refresh UI
        refreshChat();
        // Always refresh private conversations when resuming, in case PrivateChatActivity added new ones
        loadPrivateConversationList();
        updateGlobalUnreadCount();
    }

    @Override
    protected void onPause() {
        super.onPause();
        rainbowHandler.removeCallbacks(rainbowRunnable); // Stop animation to save battery
        isActivityResumed = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (privateMessageReceiver != null) {
            try {
                unregisterReceiver(privateMessageReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear the bot provider registration
        BotManager.clearActiveBotProvider();
        
        dismissMentionPopup();
        disconnectFromServer();
        releaseWakeLock();
        releaseWifiLock();
        
        if (privateMessageReceiver != null) {
            try {
                unregisterReceiver(privateMessageReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered
            }
        }
    }

    private void disconnectFromServer() {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendIRC().quitServer("https://www.BrettTechCoding.com, A BrettTech Client, Goodbye!");
                    bot.stopBotReconnect(); // Stop auto-reconnect attempts
                    bot.close(); // Properly close the connection
                    Log.d("ChatActivity", "IRC connection terminated.");
                } catch (Exception e) {
                    Log.e("ChatActivity", "Error while disconnecting from IRC", e);
                }
            }).start();
        }
    }

    private void initializePrivateMessaging() {
        // Initialize message storage
        SharedPreferences prefs = getSharedPreferences("konnect_chat", MODE_PRIVATE);
        messageStorage = new PrivateMessageStorage(prefs);

        // Get UI elements
        privateConversationListView = findViewById(R.id.privateConversationListView);
        Button btnChannelsTab = findViewById(R.id.btnChannelsTab);
        Button btnPrivateMessagesTab = findViewById(R.id.btnPrivateMessagesTab);
        FrameLayout channelsSection = findViewById(R.id.channelsSection);
        FrameLayout privateMessagesSection = findViewById(R.id.privateMessagesSection);

        // Set up adapter for conversations using custom adapter with delete button
        privateConversationAdapter = new PrivateConversationAdapter(this, privateConversations, messageStorage, userNick);
        privateConversationListView.setAdapter(privateConversationAdapter);

        // Set delete listener for conversations
        privateConversationAdapter.setOnConversationDeleteListener(nick -> {
            // Delete all messages for this conversation
            messageStorage.clearConversation(userNick, nick);
        });

        // Set selection listener for conversations
        privateConversationAdapter.setOnConversationSelectListener((nick, position) -> {
            selectedPrivateConversation = nick;
            privateConversationAdapter.setSelectedPosition(position);
            loadPrivateConversation(selectedPrivateConversation);
            // Close sidebar when selecting a conversation
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Handle conversation selection
        privateConversationListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedPrivateConversation = privateConversations.get(position);
            loadPrivateConversation(selectedPrivateConversation);
            // Close sidebar when selecting a conversation
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Set up tab switching with smooth animations
        btnChannelsTab.setOnClickListener(v -> switchToChannelsTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));

        btnPrivateMessagesTab.setOnClickListener(v -> switchToPrivateMessagesTab(btnChannelsTab, btnPrivateMessagesTab, channelsSection, privateMessagesSection));

        // Set up broadcast receiver for private messages
        privateMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String sender = intent.getStringExtra("sender");
                String message = intent.getStringExtra("message");
                
                // Don't process messages from ourselves
                if (sender != null && !sender.equalsIgnoreCase(userNick) && !message.isEmpty()) {
                    // Save message to storage (only from other users)
                    messageStorage.saveMessage(userNick, sender, message, false);
                    
                    // Move/Add to conversation list (always move to top)
                    runOnUiThread(() -> {
                        privateConversations.remove(sender);
                        privateConversations.add(0, sender);
                        
                        // If this conversation is selected and we are viewing it, update display
                        if (sender.equalsIgnoreCase(selectedPrivateConversation) && isViewingPrivateMessages) {
                            loadPrivateConversation(sender);
                        } else {
                            // Otherwise, increment unread count
                            messageStorage.incrementUnreadCount(userNick, sender);
                            updateGlobalUnreadCount();
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

    private void loadPrivateConversation(String nick) {
        selectedPrivateConversation = nick;
        isViewingPrivateMessages = true;
        channelNameTextView.setText("Private: " + nick);
        
        chatMessages.clear();
        
        // Load ONLY messages from this private conversation, NOT from channels
        List<PrivateMessageStorage.PrivateMessage> messages = messageStorage.getMessages(userNick, nick);
        for (PrivateMessageStorage.PrivateMessage msg : messages) {
            String content = msg.sender + ": " + msg.message;
            chatMessages.add(new ChatMessage(content, msg.timestamp));
        }
        

        
        // Reset unread count for this conversation
        messageStorage.resetUnreadCount(userNick, nick);
        updateGlobalUnreadCount();
        chatAdapter.notifyDataSetChanged();
        if (chatMessages.size() > 0) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }

    private void sendPrivateMessage(String message) {
        if (selectedPrivateConversation == null || selectedPrivateConversation.isEmpty()) {
            Toast.makeText(this, "No conversation selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bot != null && bot.isConnected()) {
            // Move/Add to top of conversation list
            privateConversations.remove(selectedPrivateConversation);
            privateConversations.add(0, selectedPrivateConversation);
            privateConversationAdapter.notifyDataSetChanged();
            
            // Re-sync selection position to the top
            privateConversationAdapter.setSelectedPosition(0);
            
            // Save message immediately to storage
            messageStorage.saveMessage(userNick, selectedPrivateConversation, message, true);
            
            // Update UI immediately to show sent message
            loadPrivateConversation(selectedPrivateConversation);
            chatEditText.setText("");
            
            // Send via IRC in background
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
                    String prefix = getUserPrefix(user, activeChannelObj);
                    userList.add(prefix + user.getNick());
                }
            }
        } catch (DaoException e) {
            // Handle the case where the channel is not found
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Channel not found or not yet joined.", Toast.LENGTH_SHORT).show());
        }

        return userList;
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
    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }


    private void initializeMentionPopup() {
        // Inflate the suggestion list layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_mentions, null);
        suggestionRecyclerView = popupView.findViewById(R.id.suggestionRecyclerView);
        suggestionRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list
        // After:
        suggestionAdapter = new MentionSuggestionAdapter(new ArrayList<>(), suggestion -> {
            insertMention(suggestion, mentionStartIndex);
            dismissMentionPopup();
        });

        suggestionRecyclerView.setAdapter(suggestionAdapter);

        // Measure the height of the RecyclerView to adjust the PopupWindow height dynamically
        suggestionRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int height = suggestionRecyclerView.getHeight();
            if (height > 600) { // Set a maximum height (e.g., 600 pixels)
                height = 600;
            }
            mentionPopupWindow.setHeight(height);
        });

        mentionPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(200), // Set maximum height to 200dp
                true
        );

        // Allow the PopupWindow to adjust its position based on the keyboard
        mentionPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Ensure the PopupWindow doesn't overlap the EditText
        mentionPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background));
        mentionPopupWindow.setOutsideTouchable(true);
        mentionPopupWindow.setFocusable(false); // Allow EditText to retain focus
    }
    // Method to convert dp to pixels
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
            // Show all users if no query is present
            filteredList = new ArrayList<>(userList);
        } else {
            // Filter users based on the current query
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

        // Update the adapter with the new list
        suggestionAdapter.updateSuggestions(filteredList);

        // Calculate the position to show the popup above the EditText
        int[] location = new int[2];
        chatEditText.getLocationOnScreen(location);
        int yOffset = location[1] - chatEditText.getHeight() - mentionPopupWindow.getHeight();

        // Ensure yOffset is not negative to prevent the popup from appearing off-screen
        if (yOffset < 0) {
            yOffset = 0;
        }

        // Show the popup at the calculated position
        mentionPopupWindow.showAtLocation(chatEditText, Gravity.NO_GRAVITY, 0, yOffset);
    }




    private void updateMentionSuggestions() {
        int cursorPosition = chatEditText.getSelectionStart();
        if (mentionStartIndex < 0 || cursorPosition < mentionStartIndex) {
            dismissMentionPopup();
            return;
        }

        // Ensure that mentionStartIndex + 1 <= cursorPosition to prevent IndexOutOfBounds
        if (mentionStartIndex + 1 > cursorPosition) {
            dismissMentionPopup();
            return;
        }

        String query = chatEditText.getText().toString().substring(mentionStartIndex + 1, cursorPosition).toLowerCase();

        if (query.isEmpty()) {
            // If there's nothing typed after '@', show all users
            List<String> userList = getUserListFromActiveChannel();
            suggestionAdapter.updateSuggestions(userList);
        } else {
            // Filter the user list based on the query
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
        // Enhanced implementation with safety checks...
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

        // Ensure that mentionStartIndex + 1 <= cursorPosition to prevent IndexOutOfBounds
        if (mentionStartIndex + 1 > cursorPosition) {
            Log.e("MentionFeature", "mentionStartIndex + 1 (" + (mentionStartIndex + 1) + ") exceeds cursorPosition (" + cursorPosition + ").");
            dismissMentionPopup();
            return;
        }

        // Remove the '@' and partial text
        try {
            editable.delete(mentionStartIndex, cursorPosition);
        } catch (IndexOutOfBoundsException e) {
            Log.e("MentionFeature", "Error deleting text for mention insertion.", e);
            dismissMentionPopup();
            return;
        }

        // Insert the selected nickname with '@' and a space
        editable.insert(mentionStartIndex, "@" + nickname + " ");

        // Reset mention tracking
        isMentionActive = false;
        mentionStartIndex = -1;

        Log.d("MentionFeature", "Inserted mention: @" + nickname + " at index: " + mentionStartIndex);
    }
    public void onRemoveChannel(String channelName) {
        partChannel(channelName); // Part the channel
        removeChannel(channelName); // Remove from the list

        if (!channelList.isEmpty()) {
            setActiveChannel(channelList.get(channelList.size() - 1).getChannelName());
        } else {
            // No channels left, so do not set any active channel
            activeChannel = null;
            updateChannelName(""); // Clear the displayed channel name or handle it accordingly
            chatMessages.clear(); // Clear chat messages since no channel is active
            chatAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onChannelClick(ChannelItem channel) {
        switchChannel(channel); // Adjust this method as needed for your channel switching logic
    }

    @Override
    public void onLeaveChannelClick(ChannelItem channel) {
        onRemoveChannel(channel.getChannelName()); // Use onRemoveChannel instead
    }

    public interface OnChannelClickListener {
        void onChannelClick(ChannelItem channel);
        void onLeaveChannelClick(ChannelItem channel);
    }

    private void initializeCommandPopup() {
        // Inflate the layout for the command suggestions
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_commands, null);
        commandSuggestionRecyclerView = popupView.findViewById(R.id.suggestionRecyclerView);
        commandSuggestionRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter with an empty list
        commandSuggestionAdapter = new MentionSuggestionAdapter(new ArrayList<>(), suggestion -> {
            insertCommand(suggestion, commandStartIndex);
            dismissCommandPopup();
        });

        commandSuggestionRecyclerView.setAdapter(commandSuggestionAdapter);

        // Create the PopupWindow
        commandPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        // Adjust the popup window to work with the soft keyboard
        commandPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        commandPopupWindow.setOutsideTouchable(true);
        commandPopupWindow.setFocusable(false); // Allow EditText to retain focus
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

        // Update the adapter with filtered commands
        commandSuggestionAdapter.updateSuggestions(filteredCommands);

        // Display the popup below the EditText
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

        // Insert the selected command with a space
        editable.insert(commandStartIndex, command + " ");
    }

    public void updateUserList() {
        // Get the active channel
        String activeChannel = getActiveChannel();
        if (activeChannel != null && bot != null) {
            Channel channel = bot.getUserChannelDao().getChannel(activeChannel);
            if (channel != null) {
                userList.clear();
                for (User user : channel.getUsers()) {
                    String prefix = String.valueOf(IrcUtils.getUserPrefix(user, channel));
                    userList.add(prefix + user.getNick());
                }
                // Notify the adapter of the changes
                runOnUiThread(() -> userListAdapter.notifyDataSetChanged());
            }
        }
    }

    /**
     * Switch to Channels tab with smooth animation
     */
    private void switchToChannelsTab(Button btnChannelsTab, Button btnPrivateMessagesTab, 
                                     FrameLayout channelsSection, FrameLayout privateMessagesSection) {
        isViewingPrivateMessages = false;
        selectedPrivateConversation = null;
        chatMessages.clear();
        if (activeChannel != null && channelMessagesMap.containsKey(activeChannel)) {
            chatMessages.addAll(channelMessagesMap.get(activeChannel));
        }
        chatAdapter.notifyDataSetChanged();
        animateSectionTransition(channelsSection, privateMessagesSection);
        
        // Animate selected tab
        btnChannelsTab.clearAnimation();
        btnChannelsTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_select));
        btnChannelsTab.setBackgroundResource(R.drawable.tab_indicator_active);
        btnChannelsTab.setTextColor(Color.WHITE);
        btnChannelsTab.setAlpha(1.0f);
        
        // Animate deselected tab
        btnPrivateMessagesTab.clearAnimation();
        btnPrivateMessagesTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_deselect));
        btnPrivateMessagesTab.setBackgroundResource(R.drawable.tab_indicator);
        btnPrivateMessagesTab.setTextColor(getResources().getColor(R.color.tab_text_inactive));
        btnPrivateMessagesTab.setAlpha(0.8f);
    }

    /**
     * Switch to Private Messages tab with smooth animation
     */
    private void switchToPrivateMessagesTab(Button btnChannelsTab, Button btnPrivateMessagesTab,
                                            FrameLayout channelsSection, FrameLayout privateMessagesSection) {
        isViewingPrivateMessages = true;
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        // Reload conversations from storage to pick up any new ones from other activities
        loadPrivateConversationList();
        animateSectionTransition(privateMessagesSection, channelsSection);
        
        // Animate selected tab
        btnPrivateMessagesTab.clearAnimation();
        btnPrivateMessagesTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_select));
        btnPrivateMessagesTab.setBackgroundResource(R.drawable.tab_indicator_active);
        btnPrivateMessagesTab.setTextColor(Color.WHITE);
        btnPrivateMessagesTab.setAlpha(1.0f);
        
        // Animate deselected tab
        btnChannelsTab.clearAnimation();
        btnChannelsTab.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.tab_deselect));
        btnChannelsTab.setBackgroundResource(R.drawable.tab_indicator);
        btnChannelsTab.setTextColor(getResources().getColor(R.color.tab_text_inactive));
        btnChannelsTab.setAlpha(0.8f);
    }

    /**
     * Animate section transitions with fade effect
     */
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

}