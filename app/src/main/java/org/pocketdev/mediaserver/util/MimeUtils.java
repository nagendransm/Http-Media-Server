package org.pocketdev.mediaserver.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MimeUtils {
    private static final Map<String, String> MIME_MAP = new HashMap<>();

    static {
        // Video
        MIME_MAP.put("mp4", "video/mp4");
        MIME_MAP.put("m4v", "video/mp4");
        MIME_MAP.put("mkv", "video/x-matroska");
        MIME_MAP.put("avi", "video/x-msvideo");
        MIME_MAP.put("mov", "video/quicktime");
        MIME_MAP.put("wmv", "video/x-ms-wmv");
        MIME_MAP.put("flv", "video/x-flv");
        MIME_MAP.put("webm", "video/webm");
        MIME_MAP.put("3gp", "video/3gpp");
        MIME_MAP.put("ts", "video/mp2t");
        MIME_MAP.put("mpg", "video/mpeg");
        MIME_MAP.put("mpeg", "video/mpeg");
        MIME_MAP.put("vob", "video/dvd");

        // Audio
        MIME_MAP.put("mp3", "audio/mpeg");
        MIME_MAP.put("m4a", "audio/mp4");
        MIME_MAP.put("aac", "audio/aac");
        MIME_MAP.put("flac", "audio/flac");
        MIME_MAP.put("ogg", "audio/ogg");
        MIME_MAP.put("wav", "audio/wav");
        MIME_MAP.put("wma", "audio/x-ms-wma");
        MIME_MAP.put("opus", "audio/opus");
        MIME_MAP.put("mid", "audio/midi");

        // Images
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("webp", "image/webp");
        MIME_MAP.put("bmp", "image/bmp");
        MIME_MAP.put("svg", "image/svg+xml");

        // Documents & Subtitles
        MIME_MAP.put("srt", "text/plain");
        MIME_MAP.put("vtt", "text/vtt");
        MIME_MAP.put("txt", "text/plain");
        MIME_MAP.put("html", "text/html");
        MIME_MAP.put("htm", "text/html");
        MIME_MAP.put("css", "text/css");
        MIME_MAP.put("js", "application/javascript");
        MIME_MAP.put("json", "application/json");
        MIME_MAP.put("xml", "text/xml");
        MIME_MAP.put("pdf", "application/pdf");
    }

    public static String getMimeType(String filename) {
        if (filename == null) return "application/octet-stream";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            String mime = MIME_MAP.get(ext);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }

    public static String getUpnpClass(String mimeType) {
        if (mimeType.startsWith("video/")) {
            return "object.item.videoItem";
        } else if (mimeType.startsWith("audio/")) {
            return "object.item.audioItem.musicTrack";
        } else if (mimeType.startsWith("image/")) {
            return "object.item.imageItem.photo";
        }
        return "object.item";
    }

    public static boolean isMedia(String mimeType) {
        return mimeType.startsWith("video/") || mimeType.startsWith("audio/") || mimeType.startsWith("image/");
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
