package com.btech.konnectchatirc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class IrcForegroundService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "IRC_CHANNEL";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "IRC Connection Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IRC Connection")
                .setContentText("Connected to IRC Server")
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists in your drawable resources
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // You can initialize your bot connection here or pass the bot from your activity

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup resources or disconnect from the server
    }
}
