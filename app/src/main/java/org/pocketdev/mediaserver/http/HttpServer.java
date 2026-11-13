package org.pocketdev.mediaserver.http;

import android.util.Log;

import org.pocketdev.mediaserver.model.ServerConfig;
import org.pocketdev.mediaserver.upnp.ContentDirectoryHandler;
import org.pocketdev.mediaserver.upnp.UpnpDescriptions;
import org.pocketdev.mediaserver.util.MimeUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private static final String TAG = "PocketHttpServer";
    private final ServerConfig config;
    private final String hostIp;
    private final int port;
    private final LogListener logListener;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean isRunning = false;
    private ContentDirectoryHandler cdHandler;

    public interface LogListener {
        void onLog(String message);
    }

    public HttpServer(ServerConfig config, String hostIp, int port, LogListener logListener) {
        this.config = config;
        this.hostIp = hostIp;
        this.port = port;
        this.logListener = logListener;
        this.cdHandler = new ContentDirectoryHandler(config, hostIp, port);
    }

    public synchronized void start() throws IOException {
        if (isRunning) return;
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        threadPool = Executors.newFixedThreadPool(24);
        isRunning = true;

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                logMessage("HTTP & DLNA Server listening on port " + port);
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSocket.setSoTimeout(30000);
                        threadPool.execute(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (!isRunning) break;
                    }
                }
            }
        });
    }

    public synchronized void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        logMessage("Server stopped.");
    }

    private void logMessage(String msg) {
        Log.i(TAG, msg);
        if (logListener != null) {
            logListener.onLog(msg);
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream rawIn = socket.getInputStream();
                OutputStream rawOut = socket.getOutputStream();
                BufferedInputStream in = new BufferedInputStream(rawIn);
                BufferedOutputStream out = new BufferedOutputStream(rawOut);

                // Read request line and headers
                String requestLine = readLine(in);
                if (requestLine == null || requestLine.trim().isEmpty()) {
                    socket.close();
                    return;
                }

                String[] reqParts = requestLine.split(" ");
                if (reqParts.length < 2) {
                    socket.close();
                    return;
                }

                String method = reqParts[0].toUpperCase();
                String uri = reqParts[1];

                Map<String, String> headers = new HashMap<>();
                String headerLine;
                int contentLength = 0;
                while ((headerLine = readLine(in)) != null && !headerLine.isEmpty()) {
                    int colon = headerLine.indexOf(':');
                    if (colon > 0) {
                        String key = headerLine.substring(0, colon).trim().toLowerCase();
                        String val = headerLine.substring(colon + 1).trim();
                        headers.put(key, val);
                        if ("content-length".equals(key)) {
                            try {
                                contentLength = Integer.parseInt(val);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                // If POST, read body
                byte[] bodyBytes = new byte[0];
                if (contentLength > 0) {
                    bodyBytes = new byte[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int r = in.read(bodyBytes, totalRead, contentLength - totalRead);
                        if (r == -1) break;
                        totalRead += r;
                    }
                }

                String clientIp = socket.getInetAddress().getHostAddress();
                handleRequest(method, uri, headers, bodyBytes, out, clientIp);
                out.flush();

            } catch (Exception e) {
                // connection reset / pipe broken during streaming is normal when client cancels
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void handleRequest(String method, String uri, Map<String, String> headers,
                                   byte[] body, BufferedOutputStream out, String clientIp) throws IOException {

            String rawPath = uri;
            Map<String, String> queryParams = new HashMap<>();
            int qIndex = uri.indexOf('?');
            if (qIndex >= 0) {
                rawPath = uri.substring(0, qIndex);
                String qStr = uri.substring(qIndex + 1);
                String[] pairs = qStr.split("&");
                for (String p : pairs) {
                    int eq = p.indexOf('=');
                    if (eq > 0) {
                        String k = URLDecoder.decode(p.substring(0, eq), "UTF-8");
                        String v = URLDecoder.decode(p.substring(eq + 1), "UTF-8");
                        queryParams.put(k, v);
                    }
                }
            }

            // UPnP Device Description
            if ("/description.xml".equals(rawPath) || "/upnp/rootdevice.xml".equals(rawPath)) {
                logMessage("DLNA Discovery from " + clientIp);
                String xml = UpnpDescriptions.getDeviceDescription(config.getServerName(), config.getUuid(), hostIp, port);
                sendXmlResponse(out, xml);
                return;
            }

            // ContentDirectory SCPD
            if ("/upnp/cd_scpd.xml".equals(rawPath)) {
                String xml = UpnpDescriptions.getContentDirectoryScpd();
                sendXmlResponse(out, xml);
                return;
            }

            // ConnectionManager SCPD
            if ("/upnp/cm_scpd.xml".equals(rawPath)) {
                String xml = UpnpDescriptions.getConnectionManagerScpd();
                sendXmlResponse(out, xml);
                return;
            }

            // ContentDirectory Control (SOAP POST)
            if ("/upnp/control/cd".equals(rawPath)) {
                String soapAction = headers.get("soapaction");
                String soapBody = new String(body, "UTF-8");
                logMessage("DLNA Browse request from " + clientIp);
                String resp = cdHandler.handleSoapAction(soapAction, soapBody);
                sendSoapResponse(out, resp);
                return;
            }

            // ConnectionManager Control (SOAP POST)
            if ("/upnp/control/cm".equals(rawPath)) {
                String soapAction = headers.get("soapaction");
                String resp = handleConnectionManagerSoap(soapAction);
                sendSoapResponse(out, resp);
                return;
            }

            // Media Streaming: /media?path=...
            if ("/media".equals(rawPath)) {
                String reqRelPath = queryParams.get("path");
                if (reqRelPath == null || reqRelPath.isEmpty()) {
                    sendError(out, 400, "Bad Request: Missing path parameter");
                    return;
                }
                serveFile(reqRelPath, method, headers, out, clientIp);
                return;
            }

            // Web UI: / or /browse
            if ("/".equals(rawPath) || "/index.html".equals(rawPath)) {
                String relPath = queryParams.get("path");
                if (relPath == null) relPath = "";
                serveWebUi(relPath, out);
                return;
            }

            sendError(out, 404, "Not Found");
        }

        private void serveFile(String relPath, String method, Map<String, String> headers,
                               BufferedOutputStream out, String clientIp) throws IOException {
            File root = new File(config.getSharedPath());
            File target = new File(root, relPath);

            // Prevent path traversal
            if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                sendError(out, 403, "Forbidden");
                return;
            }

            if (!target.exists() || !target.isFile()) {
                sendError(out, 404, "File Not Found");
                return;
            }

            long fileLength = target.length();
            String mimeType = MimeUtils.getMimeType(target.getName());
            String rangeHeader = headers.get("range");

            long start = 0;
            long end = fileLength - 1;
            boolean isPartial = false;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String rangeVal = rangeHeader.substring(6).trim();
                int dash = rangeVal.indexOf('-');
                if (dash >= 0) {
                    String startStr = rangeVal.substring(0, dash).trim();
                    String endStr = rangeVal.substring(dash + 1).trim();

                    if (!startStr.isEmpty()) {
                        start = Long.parseLong(startStr);
                    }
                    if (!endStr.isEmpty()) {
                        end = Long.parseLong(endStr);
                    }
                }
                if (start > end || start >= fileLength) {
                    sendRangeNotSatisfiable(out, fileLength);
                    return;
                }
                if (end >= fileLength) {
                    end = fileLength - 1;
                }
                isPartial = true;
            }

            long contentLength = end - start + 1;

            if (isPartial) {
                logMessage("Stream [" + target.getName() + "] (" + start + "-" + end + "/" + fileLength + ") -> " + clientIp);
            } else {
                logMessage("Stream [" + target.getName() + "] (full " + MimeUtils.formatFileSize(fileLength) + ") -> " + clientIp);
            }

            StringBuilder respHeader = new StringBuilder();
            if (isPartial) {
                respHeader.append("HTTP/1.1 206 Partial Content\r\n");
                respHeader.append("Content-Range: bytes ").append(start).append("-").append(end).append("/").append(fileLength).append("\r\n");
            } else {
                respHeader.append("HTTP/1.1 200 OK\r\n");
            }

            respHeader.append("Content-Type: ").append(mimeType).append("\r\n");
            respHeader.append("Content-Length: ").append(contentLength).append("\r\n");
            respHeader.append("Accept-Ranges: bytes\r\n");
            respHeader.append("transferMode.dlna.org: Streaming\r\n");
            respHeader.append("contentFeatures.dlna.org: DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000\r\n");
            respHeader.append("Connection: keep-alive\r\n");
            respHeader.append("\r\n");

            out.write(respHeader.toString().getBytes("UTF-8"));
            out.flush();

            if ("HEAD".equalsIgnoreCase(method)) {
                return;
            }

            // Stream content
            try (RandomAccessFile raf = new RandomAccessFile(target, "r")) {
                raf.seek(start);
                byte[] buffer = new byte[64 * 1024];
                long remaining = contentLength;
                while (remaining > 0 && isRunning) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = raf.read(buffer, 0, toRead);
                    if (read == -1) break;
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
                out.flush();
            }
        }

        private void serveWebUi(String relPath, BufferedOutputStream out) throws IOException {
            File root = new File(config.getSharedPath());
            File current = root;
            if (relPath != null && !relPath.isEmpty()) {
                current = new File(root, relPath);
                if (!current.getCanonicalPath().startsWith(root.getCanonicalPath()) || !current.exists()) {
                    current = root;
                    relPath = "";
                }
            }

            String html = WebUiGenerator.renderDirectory(config.getServerName(), root, current, relPath);
            byte[] bytes = html.getBytes("UTF-8");

            StringBuilder resp = new StringBuilder();
            resp.append("HTTP/1.1 200 OK\r\n");
            resp.append("Content-Type: text/html; charset=utf-8\r\n");
            resp.append("Content-Length: ").append(bytes.length).append("\r\n");
            resp.append("Connection: close\r\n");
            resp.append("\r\n");

            out.write(resp.toString().getBytes("UTF-8"));
            out.write(bytes);
            out.flush();
        }

        private void sendXmlResponse(BufferedOutputStream out, String xml) throws IOException {
            byte[] bytes = xml.getBytes("UTF-8");
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/xml; charset=\"utf-8\"\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(header.getBytes("UTF-8"));
            out.write(bytes);
            out.flush();
        }

        private void sendSoapResponse(BufferedOutputStream out, String xml) throws IOException {
            byte[] bytes = xml.getBytes("UTF-8");
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/xml; charset=\"utf-8\"\r\n" +
                    "EXT:\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(header.getBytes("UTF-8"));
            out.write(bytes);
            out.flush();
        }

        private void sendError(BufferedOutputStream out, int code, String msg) throws IOException {
            String body = "<h1>" + code + " " + msg + "</h1>";
            byte[] bytes = body.getBytes("UTF-8");
            String header = "HTTP/1.1 " + code + " " + msg + "\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(header.getBytes("UTF-8"));
            out.write(bytes);
            out.flush();
        }

        private void sendRangeNotSatisfiable(BufferedOutputStream out, long fileLen) throws IOException {
            String header = "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                    "Content-Range: bytes */" + fileLen + "\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(header.getBytes("UTF-8"));
            out.flush();
        }

        private String handleConnectionManagerSoap(String soapAction) {
            if (soapAction != null && soapAction.contains("GetProtocolInfo")) {
                return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                        "  <s:Body>\n" +
                        "    <u:GetProtocolInfoResponse xmlns:u=\"urn:schemas-upnp-org:service:ConnectionManager:1\">\n" +
                        "      <Source>http-get:*:video/mp4:*,http-get:*:video/x-matroska:*,http-get:*:audio/mpeg:*,http-get:*:image/jpeg:*</Source>\n" +
                        "      <Sink></Sink>\n" +
                        "    </u:GetProtocolInfoResponse>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";
            } else if (soapAction != null && soapAction.contains("GetCurrentConnectionIDs")) {
                return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                        "  <s:Body>\n" +
                        "    <u:GetCurrentConnectionIDsResponse xmlns:u=\"urn:schemas-upnp-org:service:ConnectionManager:1\">\n" +
                        "      <ConnectionIDs>0</ConnectionIDs>\n" +
                        "    </u:GetCurrentConnectionIDsResponse>\n" +
                        "  </s:Body>\n" +
                        "</s:Envelope>";
            }

            return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "  <s:Body>\n" +
                    "    <s:Fault><faultcode>s:Client</faultcode><faultstring>Invalid Action</faultstring></s:Fault>\n" +
                    "  </s:Body>\n" +
                    "</s:Envelope>";
        }

        private String readLine(InputStream in) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\r') {
                    int next = in.read();
                    if (next == '\n' || next == -1) break;
                    baos.write(c);
                    baos.write(next);
                } else if (c == '\n') {
                    break;
                } else {
                    baos.write(c);
                }
            }
            if (baos.size() == 0 && c == -1) return null;
            return baos.toString("UTF-8");
        }
    }
}
