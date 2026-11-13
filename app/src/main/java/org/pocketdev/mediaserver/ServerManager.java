package org.pocketdev.mediaserver;

import android.content.Context;
import android.util.Log;

import org.pocketdev.mediaserver.http.HttpServer;
import org.pocketdev.mediaserver.model.ServerConfig;
import org.pocketdev.mediaserver.upnp.SsdpServer;
import org.pocketdev.mediaserver.util.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ServerManager {
    private static final String TAG = "ServerManager";
    private static ServerManager sInstance;

    private final Context context;
    private final ServerConfig config;
    private HttpServer httpServer;
    private SsdpServer ssdpServer;
    private boolean isRunning = false;
    private String currentIp = "127.0.0.1";

    private final List<ServerStatusListener> statusListeners = new ArrayList<>();
    private final LinkedList<String> activityLogs = new LinkedList<>();
    private static final int MAX_LOGS = 100;

    public interface ServerStatusListener {
        void onServerStatusChanged(boolean running, String ip, int port);
        void onLogAdded(String logEntry);
    }

    public static synchronized ServerManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ServerManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private ServerManager(Context context) {
        this.context = context;
        this.config = new ServerConfig(context);
    }

    public synchronized void startServer() throws IOException {
        if (isRunning) return;

        currentIp = NetworkUtils.getLocalIpAddress(context);
        int port = config.getPort();

        addLog("Starting server on " + currentIp + ":" + port + "...");

        httpServer = new HttpServer(config, currentIp, port, new HttpServer.LogListener() {
            @Override
            public void onLog(String message) {
                addLog(message);
            }
        });
        httpServer.start();

        try {
            ssdpServer = new SsdpServer(context, config, currentIp, port, new SsdpServer.SsdpLogListener() {
                @Override
                public void onLog(String message) {
                    addLog(message);
                }
            });
            ssdpServer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed starting SSDP server (UPnP discovery may be limited)", e);
            addLog("Warning: Multicast SSDP startup note: " + e.getMessage());
        }

        isRunning = true;
        config.setServerEnabled(true);
        notifyStatusChanged();
        addLog("Ready! Open http://" + currentIp + ":" + port + " on your TV or browser.");
    }

    public synchronized void stopServer() {
        if (!isRunning) return;

        if (ssdpServer != null) {
            ssdpServer.stop();
            ssdpServer = null;
        }
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }

        isRunning = false;
        config.setServerEnabled(false);
        addLog("Media server stopped.");
        notifyStatusChanged();
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    public int getCurrentPort() {
        return config.getPort();
    }

    public ServerConfig getConfig() {
        return config;
    }

    public synchronized List<String> getLogs() {
        return Collections.unmodifiableList(new ArrayList<>(activityLogs));
    }

    public synchronized void clearLogs() {
        activityLogs.clear();
        notifyLogAdded("Logs cleared.");
    }

    private synchronized void addLog(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
        String entry = "[" + time + "] " + message;
        activityLogs.addFirst(entry);
        if (activityLogs.size() > MAX_LOGS) {
            activityLogs.removeLast();
        }
        notifyLogAdded(entry);
    }

    public synchronized void addListener(ServerStatusListener listener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
            listener.onServerStatusChanged(isRunning, currentIp, config.getPort());
        }
    }

    public synchronized void removeListener(ServerStatusListener listener) {
        statusListeners.remove(listener);
    }

    private synchronized void notifyStatusChanged() {
        for (ServerStatusListener l : statusListeners) {
            l.onServerStatusChanged(isRunning, currentIp, config.getPort());
        }
    }

    private synchronized void notifyLogAdded(String entry) {
        for (ServerStatusListener l : statusListeners) {
            l.onLogAdded(entry);
        }
    }
}
