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

import org.pircbotx.PircBotX;

public class IrcForegroundService extends Service {

    private static final String CHANNEL_ID = "IrcServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private PircBotX bot; // Ensure this is properly initialized if used

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification("Service is running..."));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check for null intent
        if (intent == null) {
            Log.e("IrcForegroundService", "Received null Intent");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Check for required extra data
        String channelName = intent.getStringExtra("CHANNEL_NAME");
        if (channelName == null) {
            Log.e("IrcForegroundService", "Missing required channel name");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Use channelName or other data as needed
        Log.d("IrcForegroundService", "Connecting to channel: " + channelName);

        // Example of starting bot or other operations
        // startBot(channelName); // Your method to start the bot with the channel

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Properly disconnect bot if it was initialized
        if (bot != null) {
            new Thread(() -> {
                try {
                    bot.sendIRC().quitServer("Service stopped");
                    bot.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
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
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IRC Foreground Service")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your own icon
                .build();
    }
}
