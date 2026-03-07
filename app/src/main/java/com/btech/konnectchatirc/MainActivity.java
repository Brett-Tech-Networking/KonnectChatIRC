package com.btech.konnectchatirc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private Spinner channelSpinner;
    private Spinner serverSpinner;
    private ArrayAdapter<String> channelAdapter;
    private ArrayList<String> channels;
    private SharedPreferences sharedPreferences;
    private static final String CHANNELS_KEY = "saved_channels";
    private static final String CUSTOM_SERVERS_KEY = "custom_servers";
    private EditText nickEditText;
    private EditText passwordEditText;
    private CheckBox nickCheckBox;
    private LinearLayout nickPasswordLayout;

    private ArrayList<ServerItem> serverItems;
    private ServerSpinnerAdapter serverAdapter;
    private int previousServerSelection = 0;
    private int previousChannelSelection = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences("com.btech.konnectchatirc", Context.MODE_PRIVATE);

        // Initialize the checkbox and edit text for nickname and password
        nickCheckBox = findViewById(R.id.nickCheckBox);
        nickEditText = findViewById(R.id.nickEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nickPasswordLayout = findViewById(R.id.nickPasswordLayout);

        // Handle checkbox state changes to toggle the nickname and password layout visibility
        nickCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                nickPasswordLayout.setVisibility(View.VISIBLE);
            } else {
                nickPasswordLayout.setVisibility(View.GONE);
            }
        });

        // Initialize server spinner with icons
        serverSpinner = findViewById(R.id.serverSpinner);

        serverItems = new ArrayList<>();
        // Preset servers
        serverItems.add(new ServerItem("KonnectChat IRC", R.drawable.konnectchattrans));
        serverItems.add(new ServerItem("KonnectChat IRC NSFW", R.drawable.nsfw));
        serverItems.add(new ServerItem("ThePlaceToChat IRC", R.drawable.chat));

        // Load user-added custom servers
        loadCustomServers();

        // Add the "Add Server..." entry at the end
        serverItems.add(new ServerItem("✚  Add Server...", 0));

        serverAdapter = new ServerSpinnerAdapter(this, serverItems);
        serverSpinner.setAdapter(serverAdapter);

        // Long-press individual server items to remove them
        serverAdapter.setOnServerLongClickListener((position, item) -> {
            confirmRemoveServer(position, item);
        });

        // Warning message for NSFW server selection + handle "Add Server..."
        serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ServerItem selected = serverItems.get(position);

                // "Add Server..." is the last item
                if (position == serverItems.size() - 1) {
                    showAddServerDialog();
                    return;
                }

                if (position == 1) { // "KonnectChat IRC NSFW" is selected
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("NSFW Server Warning")
                            .setMessage("You have selected a server that contains NSFW (Not Safe For Work) content. Proceed with caution.")
                            .setPositiveButton("Proceed", (dialog, which) -> {
                                previousServerSelection = position;
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                serverSpinner.setSelection(previousServerSelection);
                            })
                            .show();
                } else {
                    previousServerSelection = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Retrieve the preset channels and add user-defined channels
        channels = new ArrayList<>();
        loadPresetChannels();
        loadUserChannels();

        // Add the "Add Channel..." entry at the end
        channels.add("✚  Add Channel...");

        // Initialize channel spinner and adapter
        channelSpinner = findViewById(R.id.channelSpinner);
        channelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, channels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.white));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(0xFF000000);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.white));
                }

                // Long-press to remove channels (skip "Add Channel..." which is last)
                if (position < channels.size() - 1) {
                    final int pos = position;
                    view.setOnLongClickListener(v -> {
                        confirmRemoveChannel(pos);
                        return true;
                    });
                }

                return view;
            }
        };
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(channelAdapter);

        // Handle "Add Channel..." selection
        previousChannelSelection = 0;
        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == channels.size() - 1) {
                    showAddChannelDialog();
                    return;
                }
                previousChannelSelection = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Initialize Remember Me checkbox
        CheckBox rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        
        // Load saved "Remember Me" state and credentials
        boolean rememberMe = sharedPreferences.getBoolean("remember_me", false);
        if (rememberMe) {
            rememberMeCheckBox.setChecked(true);
            nickCheckBox.setChecked(true);
            String savedNick = sharedPreferences.getString("saved_nick", "");
            String savedPass = sharedPreferences.getString("saved_pass", "");
            nickEditText.setText(savedNick);
            passwordEditText.setText(savedPass);
            nickPasswordLayout.setVisibility(View.VISIBLE);
        }

        Button joinButton = findViewById(R.id.joinButton);
        joinButton.setOnClickListener(view -> {
            String selectedChannel = channelSpinner.getSelectedItem().toString();
            ServerItem selectedServerItem = (ServerItem) serverSpinner.getSelectedItem();

            // Don't allow joining with "Add Server..." selected
            if (serverSpinner.getSelectedItemPosition() == serverItems.size() - 1) {
                Toast.makeText(this, "Please select a server first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Don't allow joining with "Add Channel..." selected
            if (channelSpinner.getSelectedItemPosition() == channels.size() - 1) {
                Toast.makeText(this, "Please select a channel first.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Handle Remember Me Logic
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (rememberMeCheckBox.isChecked()) {
                editor.putBoolean("remember_me", true);
                editor.putString("saved_nick", nickEditText.getText().toString());
                editor.putString("saved_pass", passwordEditText.getText().toString());
            } else {
                editor.putBoolean("remember_me", false);
                editor.remove("saved_nick");
                editor.remove("saved_pass");
            }
            editor.apply();

            Intent intent = new Intent(MainActivity.this, ChatActivity.class);

            // Pass the desired nick if the checkbox is checked, regardless of the password
            if (nickCheckBox.isChecked()) {
                if (!nickEditText.getText().toString().trim().isEmpty()) {
                    intent.putExtra("DESIRED_NICK", nickEditText.getText().toString().trim());
                }
                if (!passwordEditText.getText().toString().trim().isEmpty()) {
                    intent.putExtra("DESIRED_PASSWORD", passwordEditText.getText().toString().trim());
                }
            }
            
            intent.putExtra("SELECTED_CHANNEL", selectedChannel);
            intent.putExtra("SELECTED_SERVER", selectedServerItem.getServerName());

            // For custom servers, pass the address and port directly
            if (selectedServerItem.isCustomServer()) {
                intent.putExtra("SELECTED_SERVER_ADDRESS", selectedServerItem.getServerAddress());
                intent.putExtra("SELECTED_SERVER_PORT", selectedServerItem.getServerPort());
            }

            startActivity(intent);
        });

        // Hide the FAB - channel adding is now in the spinner
        FloatingActionButton fab = findViewById(R.id.fabbutton);
        fab.setVisibility(View.GONE);

        // Drawer setup
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                if (getSupportActionBar() != null) getSupportActionBar().hide();
            } else {
                if (getSupportActionBar() != null) getSupportActionBar().show();
            }
        });
    }

    private void loadCustomServers() {
        String json = sharedPreferences.getString(CUSTOM_SERVERS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                String address = obj.getString("address");
                int port = obj.optInt("port", 6667);
                serverItems.add(new ServerItem(name, 0, address, port));
            }
        } catch (JSONException e) {
            Log.e("MainActivity", "Error loading custom servers", e);
        }
    }

    private void saveCustomServers() {
        JSONArray arr = new JSONArray();
        for (ServerItem item : serverItems) {
            if (item.isCustomServer()) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", item.getServerName());
                    obj.put("address", item.getServerAddress());
                    obj.put("port", item.getServerPort());
                    arr.put(obj);
                } catch (JSONException e) {
                    Log.e("MainActivity", "Error saving custom server", e);
                }
            }
        }
        sharedPreferences.edit().putString(CUSTOM_SERVERS_KEY, arr.toString()).apply();
    }

    private void confirmRemoveServer(int position, ServerItem item) {
        String serverName = item.getServerName();

        new AlertDialog.Builder(this)
                .setTitle("Remove \"" + serverName + "\"?")
                .setMessage("Are you sure you want to remove this server?")
                .setPositiveButton("Remove", (d, w) -> {
                    serverItems.remove(position);
                    serverAdapter.notifyDataSetChanged();
                    saveCustomServers();
                    serverSpinner.setSelection(0);
                    previousServerSelection = 0;
                    Toast.makeText(this, "\"" + serverName + "\" removed.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddServerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Custom Server");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Server Name (e.g. Libera Chat)");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(nameInput);

        final EditText addressInput = new EditText(this);
        addressInput.setHint("Server Address (e.g. irc.libera.chat)");
        addressInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(addressInput);

        final EditText portInput = new EditText(this);
        portInput.setHint("Port (default: 6667)");
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setText("6667");
        layout.addView(portInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String address = addressInput.getText().toString().trim();
            String portStr = portInput.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Server name and address are required.", Toast.LENGTH_SHORT).show();
                serverSpinner.setSelection(previousServerSelection);
                return;
            }

            int port = 6667;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                // Use default
            }

            // Insert before the "Add Server..." entry (last item)
            int insertPos = serverItems.size() - 1;
            ServerItem newServer = new ServerItem(name, 0, address, port);
            serverItems.add(insertPos, newServer);
            serverAdapter.notifyDataSetChanged();
            saveCustomServers();

            // Select the newly added server
            serverSpinner.setSelection(insertPos);
            previousServerSelection = insertPos;

            Toast.makeText(this, "Server \"" + name + "\" added!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            serverSpinner.setSelection(previousServerSelection);
            dialog.cancel();
        });

        builder.setOnCancelListener(dialog -> {
            serverSpinner.setSelection(previousServerSelection);
        });

        builder.show();
    }

    private void loadPresetChannels() {
        String[] presetChannels = {"#konnect-chat", "#ThePlaceToChat", "#robz", "#trivia"};
        for (String channel : presetChannels) {
            channels.add(channel);
        }
    }

    private void loadUserChannels() {
        Set<String> savedChannels = sharedPreferences.getStringSet(CHANNELS_KEY, new HashSet<>());
        channels.addAll(savedChannels);
    }

    private void showAddChannelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Channel");

        final EditText input = new EditText(this);
        input.setHint("Enter channel name (e.g., #newchannel)");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newChannel = input.getText().toString().trim();
            if (!newChannel.isEmpty() && !channels.contains(newChannel)) {
                // Insert before "Add Channel..." (last entry)
                int insertPos = channels.size() - 1;
                channels.add(insertPos, newChannel);
                channelAdapter.notifyDataSetChanged();
                saveChannel(newChannel);
                channelSpinner.setSelection(insertPos);
                previousChannelSelection = insertPos;
                Toast.makeText(MainActivity.this, "Channel added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Channel already exists or is invalid", Toast.LENGTH_SHORT).show();
                channelSpinner.setSelection(previousChannelSelection);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            channelSpinner.setSelection(previousChannelSelection);
            dialog.cancel();
        });

        builder.setOnCancelListener(dialog -> {
            channelSpinner.setSelection(previousChannelSelection);
        });

        builder.show();
    }

    private void confirmRemoveChannel(int position) {
        String channelName = channels.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Remove \"" + channelName + "\"?")
                .setMessage("Are you sure you want to remove this channel?")
                .setPositiveButton("Remove", (d, w) -> {
                    channels.remove(position);
                    channelAdapter.notifyDataSetChanged();
                    removeUserChannel(channelName);
                    channelSpinner.setSelection(0);
                    previousChannelSelection = 0;
                    Toast.makeText(this, "\"" + channelName + "\" removed.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveChannel(String channel) {
        Set<String> savedChannels = new HashSet<>(sharedPreferences.getStringSet(CHANNELS_KEY, new HashSet<>()));
        savedChannels.add(channel);
        sharedPreferences.edit().putStringSet(CHANNELS_KEY, savedChannels).apply();
    }

    private void removeUserChannel(String channel) {
        Set<String> savedChannels = new HashSet<>(sharedPreferences.getStringSet(CHANNELS_KEY, new HashSet<>()));
        savedChannels.remove(channel);
        sharedPreferences.edit().putStringSet(CHANNELS_KEY, savedChannels).apply();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
