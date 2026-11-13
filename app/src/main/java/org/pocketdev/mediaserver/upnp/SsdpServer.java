package org.pocketdev.mediaserver.upnp;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.pocketdev.mediaserver.model.ServerConfig;
import org.pocketdev.mediaserver.util.NetworkUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SsdpServer {
    private static final String TAG = "PocketSsdpServer";
    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final int SSDP_PORT = 1900;

    private final Context context;
    private final ServerConfig config;
    private final String hostIp;
    private final int httpPort;
    private final SsdpLogListener logListener;

    private MulticastSocket multicastSocket;
    private WifiManager.MulticastLock multicastLock;
    private volatile boolean isRunning = false;
    private Thread listenThread;
    private Thread announceThread;

    public interface SsdpLogListener {
        void onLog(String message);
    }

    public SsdpServer(Context context, ServerConfig config, String hostIp, int httpPort, SsdpLogListener logListener) {
        this.context = context.getApplicationContext();
        this.config = config;
        this.hostIp = hostIp;
        this.httpPort = httpPort;
        this.logListener = logListener;
    }

    public synchronized void start() throws IOException {
        if (isRunning) return;

        // Acquire Android MulticastLock
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("PocketMediaServer_Multicast");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        }

        multicastSocket = new MulticastSocket(SSDP_PORT);
        multicastSocket.setReuseAddress(true);
        multicastSocket.setTimeToLive(4);

        InetAddress group = InetAddress.getByName(SSDP_ADDRESS);
        NetworkInterface netIf = NetworkUtils.getActiveNetworkInterface();
        if (netIf != null) {
            multicastSocket.joinGroup(new InetSocketAddress(group, SSDP_PORT), netIf);
        } else {
            multicastSocket.joinGroup(group);
        }

        isRunning = true;

        // Listener thread for M-SEARCH requests
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buf = new byte[2048];
                while (isRunning && !multicastSocket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(packet);
                        handlePacket(packet);
                    } catch (IOException e) {
                        if (!isRunning) break;
                    }
                }
            }
        }, "SSDP-Listener");
        listenThread.start();

        // Announcement thread (sends ssdp:alive every 60s)
        announceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        sendAliveBroadcast();
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending SSDP alive", e);
                    }
                }
            }
        }, "SSDP-Announce");
        announceThread.start();

        logMessage("DLNA/UPnP SSDP discovery service active");
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;

        // Send byebye announcement before shutting down
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendByebyeBroadcast();
                } catch (Exception ignored) {
                }
            }
        }).start();

        if (listenThread != null) {
            listenThread.interrupt();
        }
        if (announceThread != null) {
            announceThread.interrupt();
        }

        try {
            if (multicastSocket != null && !multicastSocket.isClosed()) {
                multicastSocket.close();
            }
        } catch (Exception ignored) {
        }

        if (multicastLock != null && multicastLock.isHeld()) {
            try {
                multicastLock.release();
            } catch (Exception ignored) {
            }
        }

        logMessage("SSDP discovery stopped");
    }

    private void handlePacket(DatagramPacket packet) {
        String data = new String(packet.getData(), 0, packet.getLength());
        if (data.startsWith("M-SEARCH")) {
            String st = extractHeader(data, "ST");
            if (st != null) {
                st = st.trim();
                boolean match = "ssdp:all".equalsIgnoreCase(st) ||
                        "upnp:rootdevice".equalsIgnoreCase(st) ||
                        "urn:schemas-upnp-org:device:MediaServer:1".equalsIgnoreCase(st) ||
                        "urn:schemas-upnp-org:service:ContentDirectory:1".equalsIgnoreCase(st) ||
                        "urn:schemas-upnp-org:service:ConnectionManager:1".equalsIgnoreCase(st) ||
                        ("uuid:" + config.getUuid()).equalsIgnoreCase(st);

                if (match) {
                    logMessage("SSDP M-SEARCH query from " + packet.getAddress().getHostAddress() + " for " + st);
                    sendSearchResponse(packet.getAddress(), packet.getPort(), st);
                }
            }
        }
    }

    private void sendSearchResponse(InetAddress targetAddress, int targetPort, String st) {
        try {
            String uuid = config.getUuid();
            String location = "http://" + hostIp + ":" + httpPort + "/description.xml";
            String date = getHttpDate();

            String[] targets;
            if ("ssdp:all".equalsIgnoreCase(st)) {
                targets = new String[]{
                        "upnp:rootdevice",
                        "uuid:" + uuid,
                        "urn:schemas-upnp-org:device:MediaServer:1",
                        "urn:schemas-upnp-org:service:ContentDirectory:1",
                        "urn:schemas-upnp-org:service:ConnectionManager:1"
                };
            } else {
                targets = new String[]{st};
            }

            for (String target : targets) {
                String usn;
                if (target.startsWith("uuid:")) {
                    usn = target;
                } else if ("upnp:rootdevice".equals(target)) {
                    usn = "uuid:" + uuid + "::upnp:rootdevice";
                } else {
                    usn = "uuid:" + uuid + "::" + target;
                }

                String response = "HTTP/1.1 200 OK\r\n" +
                        "CACHE-CONTROL: max-age=1800\r\n" +
                        "DATE: " + date + "\r\n" +
                        "EXT:\r\n" +
                        "LOCATION: " + location + "\r\n" +
                        "SERVER: Android/14.0 UPnP/1.0 PocketMediaServer/1.0\r\n" +
                        "ST: " + target + "\r\n" +
                        "USN: " + usn + "\r\n" +
                        "\r\n";

                byte[] bytes = response.getBytes("UTF-8");
                DatagramPacket respPacket = new DatagramPacket(bytes, bytes.length, targetAddress, targetPort);
                multicastSocket.send(respPacket);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SSDP response", e);
        }
    }

    private void sendAliveBroadcast() {
        sendNotifyBroadcast("ssdp:alive");
    }

    private void sendByebyeBroadcast() {
        sendNotifyBroadcast("ssdp:byebye");
    }

    private void sendNotifyBroadcast(String nts) {
        try {
            String uuid = config.getUuid();
            String location = "http://" + hostIp + ":" + httpPort + "/description.xml";
            InetAddress group = InetAddress.getByName(SSDP_ADDRESS);

            String[] ntList = new String[]{
                    "upnp:rootdevice",
                    "uuid:" + uuid,
                    "urn:schemas-upnp-org:device:MediaServer:1",
                    "urn:schemas-upnp-org:service:ContentDirectory:1",
                    "urn:schemas-upnp-org:service:ConnectionManager:1"
            };

            for (String nt : ntList) {
                String usn;
                if (nt.startsWith("uuid:")) {
                    usn = nt;
                } else if ("upnp:rootdevice".equals(nt)) {
                    usn = "uuid:" + uuid + "::upnp:rootdevice";
                } else {
                    usn = "uuid:" + uuid + "::" + nt;
                }

                StringBuilder notify = new StringBuilder();
                notify.append("NOTIFY * HTTP/1.1\r\n")
                        .append("HOST: ").append(SSDP_ADDRESS).append(":").append(SSDP_PORT).append("\r\n")
                        .append("NT: ").append(nt).append("\r\n")
                        .append("NTS: ").append(nts).append("\r\n")
                        .append("USN: ").append(usn).append("\r\n");

                if ("ssdp:alive".equals(nts)) {
                    notify.append("LOCATION: ").append(location).append("\r\n")
                            .append("CACHE-CONTROL: max-age=1800\r\n")
                            .append("SERVER: Android/14.0 UPnP/1.0 PocketMediaServer/1.0\r\n");
                }
                notify.append("\r\n");

                byte[] bytes = notify.toString().getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, SSDP_PORT);
                multicastSocket.send(packet);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed sending notify broadcast: " + e.getMessage());
        }
    }

    private String extractHeader(String text, String headerName) {
        String[] lines = text.split("\r?\n");
        String prefix = headerName.toLowerCase(Locale.ROOT) + ":";
        for (String line : lines) {
            if (line.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private String getHttpDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    private void logMessage(String msg) {
        Log.i(TAG, msg);
        if (logListener != null) {
            logListener.onLog(msg);
        }
    }
}
