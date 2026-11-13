package org.pocketdev.mediaserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.pocketdev.mediaserver.model.ServerConfig;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "PocketBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "Boot broadcast received: " + action);

        ServerConfig config = new ServerConfig(context);
        if (config.isStartOnBoot()) {
            Log.i(TAG, "Start on boot is ENABLED. Launching MediaServerService...");
            Intent serviceIntent = new Intent(context, MediaServerService.class);
            serviceIntent.setAction(MediaServerService.ACTION_START);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            Log.i(TAG, "Start on boot is disabled in settings. Skipping auto-start.");
        }
    }
}
