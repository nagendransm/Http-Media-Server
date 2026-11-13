package org.pocketdev.mediaserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MediaServerService extends Service implements ServerManager.ServerStatusListener {
    private static final String TAG = "MediaServerService";
    public static final String CHANNEL_ID = "pocket_media_server_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "org.pocketdev.mediaserver.ACTION_START";
    public static final String ACTION_STOP = "org.pocketdev.mediaserver.ACTION_STOP";

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private ServerManager serverManager;

    @Override
    public void onCreate() {
        super.onCreate();
        serverManager = ServerManager.getInstance(this);
        serverManager.addListener(this);

        createNotificationChannel();
        acquireLocks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_STOP.equals(action)) {
            stopServerAndSelf();
            return START_NOT_STICKY;
        }

        // Start foreground immediately
        startInForeground();

        // Start server in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverManager.startServer();
                    updateNotification(true, serverManager.getCurrentIp(), serverManager.getCurrentPort());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start server in service", e);
                    stopServerAndSelf();
                }
            }
        }).start();

        return START_STICKY;
    }

    private void startInForeground() {
        Notification notification = buildNotification(serverManager.isRunning(), serverManager.getCurrentIp(), serverManager.getCurrentPort());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            }
            startForeground(NOTIFICATION_ID, notification, serviceType);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(boolean running, String ip, int port) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(running, ip, port));
        }
    }

    private Notification buildNotification(boolean running, String ip, int port) {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent stopIntent = new Intent(this, MediaServerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title = running ? "Pocket Media Server is RUNNING" : "Starting Media Server...";
        String text = running ? "http://" + ip + ":" + port + " (DLNA / UPnP active)" : "Initializing network services...";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_upload_you_tube)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_delete, "Stop Server", stopPendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pocket Media Server";
            String description = "Persistent notification for running DLNA/HTTP Media Server";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void acquireLocks() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PocketMediaServer::WakeLock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(24 * 60 * 60 * 1000L); // 24 hours max fallback
            }

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PocketMediaServer::WifiLock");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error acquiring system locks: " + e.getMessage());
        }
    }

    private void releaseLocks() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {
        }
        try {
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
            }
        } catch (Exception ignored) {
        }
    }

    private void stopServerAndSelf() {
        serverManager.stopServer();
        releaseLocks();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        serverManager.removeListener(this);
        serverManager.stopServer();
        releaseLocks();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onServerStatusChanged(boolean running, String ip, int port) {
        updateNotification(running, ip, port);
    }

    @Override
    public void onLogAdded(String logEntry) {
    }
}
