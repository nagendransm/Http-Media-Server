package org.pocketdev.mediaserver.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {

    public static String getLocalIpAddress(Context context) {
        // First try via WifiManager if connected to Wi-Fi
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                if (ipInt != 0) {
                    return String.format(java.util.Locale.US, "%d.%d.%d.%d",
                            (ipInt & 0xff),
                            (ipInt >> 8 & 0xff),
                            (ipInt >> 16 & 0xff),
                            (ipInt >> 24 & 0xff));
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback: enumerate non-loopback IPv4 addresses (useful for Hotspot, Ethernet, etc.)
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "127.0.0.1";
    }

    public static NetworkInterface getActiveNetworkInterface() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            // Prioritize wlan/ap/eth interfaces
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                String name = intf.getName().toLowerCase();
                if (name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("eth") || name.startsWith("rndis")) {
                    Enumeration<InetAddress> addrs = intf.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return intf;
                        }
                    }
                }
            }
            // Fallback: any up, non-loopback interface with IPv4
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return intf;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getNetworkName(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null && info.getSSID() != null && !info.getSSID().equals("<unknown ssid>")) {
                    String ssid = info.getSSID();
                    if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 2) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    return ssid;
                }
            }
        } catch (Exception ignored) {
        }
        return "Local Network / Hotspot";
    }
}
