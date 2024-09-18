package com.btech.konnectchatirc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import java.io.IOException;

public class IrcForegroundService extends Service {

    private static final String CHANNEL_ID = "IrcServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private PircBotX bot;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification("IRC service is running..."));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String channelName = intent.getStringExtra("SELECTED_CHANNEL");
            String serverAddress = intent.getStringExtra("SELECTED_SERVER");
            if (channelName != null && serverAddress != null) {
                Log.d("IrcForegroundService", "Connecting to channel: " + channelName);
                keepBotConnected(serverAddress, channelName);
            }
        } else {
            Log.e("IrcForegroundService", "Received null Intent, stopping service.");
            stopSelf();
        }
        return START_STICKY;
    }

    private void keepBotConnected(String serverAddress, String channelName) {
        if (bot == null || !bot.isConnected()) {
            new Thread(() -> {
                try {
                    Configuration.Builder config = new Configuration.Builder()
                            .setName("GuestUser") // Use default guest nickname if not provided
                            .addServer(serverAddress)
                            .addAutoJoinChannel(channelName)
                            .setAutoNickChange(true)
                            .addListener(new Listeners(null)); // Add relevant listeners here

                    bot = new PircBotX(config.buildConfiguration());
                    bot.startBot();
                } catch (IOException | IrcException e) {
                    Log.e("IrcForegroundService", "Error connecting bot", e);
                }
            }).start();
        }
    }

    @Override
    public void onDestroy() {
        if (bot != null && bot.isConnected()) {
            new Thread(() -> {
                try {
                    bot.sendIRC().quitServer("Service stopped");
                    bot.close();
                } catch (Exception e) {
                    Log.e("IrcForegroundService", "Error disconnecting bot", e);
                }
            }).start();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used since it's a foreground service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "IRC Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IRC Connection")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification)  // Set your notification icon
                .build();
    }
}
