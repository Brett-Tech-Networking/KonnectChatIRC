package com.btech.konnectchatirc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
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
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private Spinner channelSpinner;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> channels;
    private SharedPreferences sharedPreferences;
    private static final String CHANNELS_KEY = "saved_channels";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = getSharedPreferences("com.btech.konnectchatirc", Context.MODE_PRIVATE);

        // Retrieve the preset channels and add user-defined channels
        channels = new ArrayList<>();
        loadPresetChannels();
        loadUserChannels();

        // Initialize spinner and adapter
        channelSpinner = findViewById(R.id.channelSpinner);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, channels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(adapter);

        // Apply color programmatically for selected item
        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Assuming this is around line 69 in MainActivity.java
                TextView textView = findViewById(R.id.textView); // Use the correct ID
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.white)); // Use the correct color
                } else {
                    Log.e("MainActivity", "TextView is null, cannot set text color.");
                }

            }

                @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        Button joinButton = findViewById(R.id.joinButton);
        joinButton.setOnClickListener(view -> {
            String selectedChannel = channelSpinner.getSelectedItem().toString();
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("SELECTED_CHANNEL", selectedChannel);
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
                adapter.notifyDataSetChanged();
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
