package org.pocketdev.mediaserver.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;
import java.util.UUID;

public class ServerConfig {
    private static final String PREF_NAME = "pocket_media_server_prefs";
    private static final String KEY_PORT = "server_port";
    private static final String KEY_SERVER_NAME = "server_name";
    private static final String KEY_SHARED_PATH = "shared_path";
    private static final String KEY_START_ON_BOOT = "start_on_boot";
    private static final String KEY_SERVER_UUID = "server_uuid";
    private static final String KEY_SERVER_ENABLED = "server_enabled";

    private final SharedPreferences prefs;
    private final Context context;

    public ServerConfig(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getPort() {
        return prefs.getInt(KEY_PORT, 8080);
    }

    public void setPort(int port) {
        prefs.edit().putInt(KEY_PORT, port).apply();
    }

    public String getServerName() {
        return prefs.getString(KEY_SERVER_NAME, "Pocket Media Server (" + android.os.Build.MODEL + ")");
    }

    public void setServerName(String name) {
        prefs.edit().putString(KEY_SERVER_NAME, name).apply();
    }

    public String getSharedPath() {
        String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String saved = prefs.getString(KEY_SHARED_PATH, defaultPath);
        File f = new File(saved);
        if (!f.exists() || !f.canRead()) {
            File ext = context.getExternalFilesDir(null);
            if (ext != null && ext.exists()) {
                return ext.getAbsolutePath();
            }
        }
        return saved;
    }

    public void setSharedPath(String path) {
        prefs.edit().putString(KEY_SHARED_PATH, path).apply();
    }

    public boolean isStartOnBoot() {
        return prefs.getBoolean(KEY_START_ON_BOOT, false);
    }

    public void setStartOnBoot(boolean enable) {
        prefs.edit().putBoolean(KEY_START_ON_BOOT, enable).apply();
    }

    public boolean isServerEnabled() {
        return prefs.getBoolean(KEY_SERVER_ENABLED, false);
    }

    public void setServerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SERVER_ENABLED, enabled).apply();
    }

    public String getUuid() {
        String uuid = prefs.getString(KEY_SERVER_UUID, null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_SERVER_UUID, uuid).apply();
        }
        return uuid;
    }
}
