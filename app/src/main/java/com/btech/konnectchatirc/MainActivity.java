package com.btech.konnectchatirc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private Spinner channelSpinner;
    private ArrayAdapter<String> channelAdapter;
    private ArrayList<String> channels;
    private SharedPreferences sharedPreferences;
    private static final String CHANNELS_KEY = "saved_channels";
    private EditText nickEditText;
    private EditText passwordEditText;
    private CheckBox nickCheckBox;
    private LinearLayout nickPasswordLayout;

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
        Spinner serverSpinner = findViewById(R.id.serverSpinner);

        List<ServerItem> serverItems = Arrays.asList(
                new ServerItem("KonnectChat IRC", R.drawable.konnectchattrans),
                new ServerItem("KonnectChat IRC NSFW", R.drawable.nsfw),
                new ServerItem("ThePlaceToChat IRC", R.drawable.chat)
        );

        ServerSpinnerAdapter serverAdapter = new ServerSpinnerAdapter(this, serverItems);
        serverSpinner.setAdapter(serverAdapter);

        // Warning message for NSFW server selection
        serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // "KonnectChat IRC NSFW" is selected
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("NSFW Server Warning")
                            .setMessage("You have selected a server that contains NSFW (Not Safe For Work) content. Proceed with caution.")
                            .setPositiveButton("Proceed", (dialog, which) -> {
                                // User confirms to proceed
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                serverSpinner.setSelection(0); // Revert to the first option
                            })
                            .show();
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

        // Initialize channel spinner and adapter
        channelSpinner = findViewById(R.id.channelSpinner);
        channelAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, channels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.white)); // Set the text color to white
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.white)); // Set the text color to white in the dropdown list
                }
                return view;
            }
        };
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(channelAdapter);

        Button joinButton = findViewById(R.id.joinButton);
        joinButton.setOnClickListener(view -> {
            String selectedChannel = channelSpinner.getSelectedItem().toString();
            String selectedServer = serverSpinner.getSelectedItem().toString();
            ServerItem selectedServerItem = (ServerItem) serverSpinner.getSelectedItem();
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
            intent.putExtra("SELECTED_SERVER", selectedServer);
            startActivity(intent);
        });

        FloatingActionButton fab = findViewById(R.id.fabbutton);
        fab.setOnClickListener(view -> showAddChannelDialog());

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
    }

    private void loadPresetChannels() {
        String[] presetChannels = {"#ThePlaceToChat", "#konnect-chat", "#robz", "#trivia"};
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
                channels.add(newChannel);
                channelAdapter.notifyDataSetChanged();
                saveChannel(newChannel);
                Toast.makeText(MainActivity.this, "Channel added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Channel already exists or is invalid", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveChannel(String channel) {
        Set<String> savedChannels = sharedPreferences.getStringSet(CHANNELS_KEY, new HashSet<>());
        savedChannels.add(channel);
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
