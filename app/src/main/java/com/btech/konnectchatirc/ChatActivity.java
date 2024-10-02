package com.btech.konnectchatirc;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageButton;
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
import org.pircbotx.hooks.events.WhoEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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


public class ChatActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_chat);
// Inside onCreate() in ChatActivity
        acquireWakeLock();
        acquireWifiLock();

        unreadBadge = findViewById(R.id.unreadBadge);
        drawerLayout = findViewById(R.id.drawerLayout);
        RelativeLayout rootLayout = findViewById(R.id.rootLayout);
        String desiredNick = getIntent().getStringExtra("DESIRED_NICK");
        desiredPassword = getIntent().getStringExtra("DESIRED_PASSWORD"); // Retrieve password
        // Initialize ConnectivityManager here
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        client = new OkHttpClient(); // Initialize OkHttpClient

        // Initialize the RecyclerView for chat messages
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessages);  // Pass ChatActivity instance
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

        btnOperLogin.setOnClickListener(v -> new OperLogin(this, this, bot).startOperLoginProcess());

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
        channelAdapter = new ChannelAdapter(channelList, this);
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
                    .setAutoSplitMessage(true)
                    .setAutoReconnect(true)
                    .addCapHandler(new EnableCapHandler("multi-prefix"))
                    .addCapHandler(new EnableCapHandler("userhost-in-names"))
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
        SpannableStringBuilder spannableMessage = new SpannableStringBuilder(message);

        // Apply default white color to the entire message
        ForegroundColorSpan defaultColorSpan = new ForegroundColorSpan(Color.WHITE);
        spannableMessage.setSpan(defaultColorSpan, 0, spannableMessage.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        // Apply clickable red color only to @nick mentions
        Pattern mentionPattern = Pattern.compile("@\\w+");
        Matcher mentionMatcher = mentionPattern.matcher(message);

        while (mentionMatcher.find()) {
            final String mention = mentionMatcher.group();
            final String nickWithoutAt = mention.substring(1); // Extract nick without the '@' character


            // Apply the color to @nick
            ForegroundColorSpan mentionColorSpan = new ForegroundColorSpan(Color.RED);
            spannableMessage.setSpan(mentionColorSpan, mentionMatcher.start(), mentionMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Make @nick clickable
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (bot != null && bot.isConnected()) {
                        // Get the active channel
                        String activeChannelName = getActiveChannel();
                        Channel activeChannel = bot.getUserChannelDao().getChannel(activeChannelName);

                        if (activeChannel != null) {
                            User clickedUser = null;

                            // Iterate over users in the active channel to find the matching nick
                            for (User user : activeChannel.getUsers()) {
                                if (user.getNick().equalsIgnoreCase(nickWithoutAt)) {
                                    clickedUser = user;
                                    break;
                                }
                            }

                            if (clickedUser != null) {
                                final User finalClickedUser = clickedUser; // Declare final variable
                                runOnUiThread(() -> {
                                    UserOptionsDialog userOptionsDialog = new UserOptionsDialog(ChatActivity.this, finalClickedUser, ChatActivity.this);
                                    userOptionsDialog.show();
                                });
                            } else {
                                // User not in the current channel
                                Toast.makeText(widget.getContext(), "User not found in the current channel.", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            // Active channel is null
                            Toast.makeText(widget.getContext(), "Active channel not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(widget.getContext(), "Not connected to a server.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.RED); // Ensure the @nick remains red
                    ds.setUnderlineText(false); // Remove underline if needed
                }
            };


            spannableMessage.setSpan(clickableSpan, mentionMatcher.start(), mentionMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        chatMessages.add(spannableMessage);
        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        markMessageAsProcessed(message);
    }


    void storeMessageForChannel(String channel, String message) {
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

    public boolean hasMessageBeenProcessed(String message) {
        return processedMessages.contains(message);
    }

    public void markMessageAsProcessed(String message) {
        processedMessages.add(message);
    }

    void incrementUnreadCount() {
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
        if (channel == null) {
            // Handle null case if necessary, e.g., skip or set to a default channel
            channel = getActiveChannel(); // Set to active channel as fallback
        }

        String prefix = "";
        User senderUser = null;

        if (bot != null && bot.isConnected()) {
            Channel channelObj = bot.getUserChannelDao().getChannel(channel);
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
                System.out.println("Channel object is null for channel: " + channel);
            }
        } else {
            System.out.println("Bot is null or not connected.");
        }

        String formattedMessage = prefix + " " +  sender + ": " + message;
        storeMessageForChannel(channel, formattedMessage);

        boolean isActiveChannel = channel.equalsIgnoreCase(getActiveChannel());

        if (isActiveChannel) {
            runOnUiThread(() -> addChatMessage(formattedMessage));
        } else {
            final String finalChannel = channel;
            runOnUiThread(() -> {
                incrementUnreadCount();  // Increments the unread message badge
                updateUnreadCountForChannel(finalChannel); // Update unread count for the specific channel
            });
        }
    }

    private void updateUnreadCountForChannel(String channel) {
        for (ChannelItem channelItem : channelList) {
            if (channelItem.getChannelName().equals(channel)) {
                channelItem.incrementUnreadCount();
                break;
            }
        }
        channelAdapter.notifyDataSetChanged();
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
        dismissMentionPopup();
        disconnectFromServer();
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
        // Inside onCreate or a similar initialization method
        Button btnZline = findViewById(R.id.btnZline);
        btnZline.setOnClickListener(v -> {
            Zline zline = new Zline(this, bot, this);
            zline.startZlineProcess();
        });
    }
    public ChatAdapter getChatAdapter() {
        return chatAdapter;
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Do not disconnect from the server on pause
        // You may want to release locks here if needed but maintain the connection
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Do not disconnect from the server on stop, unless the user manually initiates disconnection
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
}