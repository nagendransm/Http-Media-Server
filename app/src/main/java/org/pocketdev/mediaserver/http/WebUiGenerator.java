package org.pocketdev.mediaserver.http;

import org.pocketdev.mediaserver.util.MimeUtils;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WebUiGenerator {

    public static String renderDirectory(String serverName, File rootDir, File currentDir, String relPath) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("<title>").append(escapeHtml(serverName)).append("</title>\n")
                .append("<style>\n")
                .append(":root { --bg: #121820; --card: #1E2632; --card-hover: #2A3647; --text: #E2E8F0; --text-dim: #94A3B8; --accent: #38BDF8; --accent-hover: #0EA5E9; --border: #334155; --badge-video: #F43F5E; --badge-audio: #A855F7; --badge-img: #10B981; }\n")
                .append("* { box-sizing: border-box; margin: 0; padding: 0; }\n")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: var(--bg); color: var(--text); padding: 20px; line-height: 1.5; }\n")
                .append(".container { max-width: 1000px; margin: 0 auto; }\n")
                .append("header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); flex-wrap: wrap; gap: 12px; }\n")
                .append(".title-wrap h1 { font-size: 24px; font-weight: 700; color: #FFFFFF; display: flex; align-items: center; gap: 10px; }\n")
                .append(".badge { font-size: 11px; font-weight: 600; padding: 3px 8px; border-radius: 999px; text-transform: uppercase; letter-spacing: 0.5px; }\n")
                .append(".badge-dlna { background: rgba(56,189,248,0.15); color: var(--accent); border: 1px solid var(--accent); }\n")
                .append(".breadcrumbs { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-bottom: 20px; font-size: 15px; color: var(--text-dim); }\n")
                .append(".breadcrumbs a { color: var(--accent); text-decoration: none; font-weight: 500; padding: 4px 8px; border-radius: 6px; background: rgba(56,189,248,0.08); transition: background 0.2s; }\n")
                .append(".breadcrumbs a:hover { background: rgba(56,189,248,0.2); }\n")
                .append(".file-list { display: flex; flex-direction: column; gap: 8px; }\n")
                .append(".item-card { display: flex; align-items: center; justify-content: space-between; background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 12px 16px; transition: all 0.2s ease; text-decoration: none; color: inherit; }\n")
                .append(".item-card:hover { background: var(--card-hover); transform: translateY(-1px); border-color: var(--accent); }\n")
                .append(".item-left { display: flex; align-items: center; gap: 14px; overflow: hidden; }\n")
                .append(".icon { font-size: 24px; min-width: 30px; text-align: center; }\n")
                .append(".meta { display: flex; flex-direction: column; overflow: hidden; }\n")
                .append(".name { font-weight: 600; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 550px; }\n")
                .append(".details { font-size: 12px; color: var(--text-dim); display: flex; gap: 12px; margin-top: 2px; }\n")
                .append(".actions { display: flex; align-items: center; gap: 8px; }\n")
                .append(".btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 6px; font-size: 13px; font-weight: 600; text-decoration: none; cursor: pointer; transition: 0.2s; }\n")
                .append(".btn-play { background: var(--accent); color: #0F172A; }\n")
                .append(".btn-play:hover { background: var(--accent-hover); }\n")
                .append(".btn-dl { background: rgba(255,255,255,0.08); color: var(--text); border: 1px solid var(--border); }\n")
                .append(".btn-dl:hover { background: rgba(255,255,255,0.15); }\n")
                .append(".player-box { margin-bottom: 20px; background: #000; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5); display: none; }\n")
                .append("video, audio { width: 100%; outline: none; display: block; }\n")
                .append(".empty-msg { text-align: center; padding: 40px; color: var(--text-dim); background: var(--card); border-radius: 12px; border: 1px dashed var(--border); }\n")
                .append("@media (max-width: 600px) { .name { max-width: 200px; } .btn span { display: none; } }\n")
                .append("</style>\n")
                .append("<script>\n")
                .append("function playMedia(url, isVideo) {\n")
                .append("  var box = document.getElementById('playerBox');\n")
                .append("  box.style.display = 'block';\n")
                .append("  if (isVideo) {\n")
                .append("    box.innerHTML = '<video controls autoplay src=\"' + url + '\"></video>';\n")
                .append("  } else {\n")
                .append("    box.innerHTML = '<audio controls autoplay style=\"padding:20px;\" src=\"' + url + '\"></audio>';\n")
                .append("  }\n")
                .append("  box.scrollIntoView({behavior: 'smooth'});\n")
                .append("}\n")
                .append("</script>\n")
                .append("</head>\n<body>\n")
                .append("<div class=\"container\">\n")
                .append("<header>\n")
                .append("<div class=\"title-wrap\">\n")
                .append("<h1>📡 ").append(escapeHtml(serverName)).append(" <span class=\"badge badge-dlna\">DLNA & HTTP</span></h1>\n")
                .append("</div>\n")
                .append("</header>\n");

        // Player placeholder
        html.append("<div id=\"playerBox\" class=\"player-box\"></div>\n");

        // Breadcrumbs
        html.append("<div class=\"breadcrumbs\">\n");
        html.append("<a href=\"/\">🏠 Root</a>");
        if (relPath != null && !relPath.isEmpty()) {
            String[] segments = relPath.split("/");
            StringBuilder currSeg = new StringBuilder();
            for (String seg : segments) {
                if (seg.isEmpty()) continue;
                if (currSeg.length() > 0) currSeg.append("/");
                currSeg.append(seg);
                try {
                    String enc = URLEncoder.encode(currSeg.toString(), "UTF-8");
                    html.append(" <span>/</span> <a href=\"/?path=").append(enc).append("\">").append(escapeHtml(seg)).append("</a>");
                } catch (Exception ignored) {
                }
            }
        }
        html.append("</div>\n");

        // Files & Folders
        File[] files = currentDir.listFiles();
        List<File> dirList = new ArrayList<>();
        List<File> fileList = new ArrayList<>();

        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(".")) continue;
                if (f.isDirectory()) {
                    dirList.add(f);
                } else {
                    fileList.add(f);
                }
            }
        }

        Comparator<File> nameComp = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
        Collections.sort(dirList, nameComp);
        Collections.sort(fileList, nameComp);

        html.append("<div class=\"file-list\">\n");

        // Parent folder link
        if (relPath != null && !relPath.isEmpty()) {
            File parent = currentDir.getParentFile();
            String parentRel = "";
            if (parent != null && parent.getAbsolutePath().startsWith(rootDir.getAbsolutePath())) {
                String p = parent.getAbsolutePath().substring(rootDir.getAbsolutePath().length());
                if (p.startsWith("/")) p = p.substring(1);
                parentRel = p;
            }
            try {
                String parentEnc = URLEncoder.encode(parentRel, "UTF-8");
                html.append("<a href=\"/?path=").append(parentEnc).append("\" class=\"item-card\">\n")
                        .append("<div class=\"item-left\">\n")
                        .append("<div class=\"icon\">📁</div>\n")
                        .append("<div class=\"meta\"><div class=\"name\">.. (Up one level)</div></div>\n")
                        .append("</div>\n</a>\n");
            } catch (Exception ignored) {
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        // List directories
        for (File dir : dirList) {
            String dirRel = (relPath == null || relPath.isEmpty()) ? dir.getName() : relPath + "/" + dir.getName();
            File[] subs = dir.listFiles();
            int subCount = (subs != null) ? subs.length : 0;
            try {
                String enc = URLEncoder.encode(dirRel, "UTF-8");
                html.append("<a href=\"/?path=").append(enc).append("\" class=\"item-card\">\n")
                        .append("<div class=\"item-left\">\n")
                        .append("<div class=\"icon\">📁</div>\n")
                        .append("<div class=\"meta\">\n")
                        .append("<div class=\"name\">").append(escapeHtml(dir.getName())).append("</div>\n")
                        .append("<div class=\"details\"><span>").append(subCount).append(" items</span></div>\n")
                        .append("</div>\n</div>\n")
                        .append("<div class=\"actions\"><span style=\"color:var(--text-dim);font-size:18px;\">›</span></div>\n")
                        .append("</a>\n");
            } catch (Exception ignored) {
            }
        }

        // List files
        for (File f : fileList) {
            String fRel = (relPath == null || relPath.isEmpty()) ? f.getName() : relPath + "/" + f.getName();
            String mime = MimeUtils.getMimeType(f.getName());
            String sizeStr = MimeUtils.formatFileSize(f.length());
            String dateStr = sdf.format(new Date(f.lastModified()));

            String icon = "📄";
            boolean isVideo = mime.startsWith("video/");
            boolean isAudio = mime.startsWith("audio/");
            boolean isImage = mime.startsWith("image/");

            if (isVideo) icon = "🎬";
            else if (isAudio) icon = "🎵";
            else if (isImage) icon = "🖼️";

            try {
                String enc = URLEncoder.encode(fRel, "UTF-8");
                String mediaUrl = "/media?path=" + enc;
                html.append("<div class=\"item-card\">\n")
                        .append("<div class=\"item-left\">\n")
                        .append("<div class=\"icon\">").append(icon).append("</div>\n")
                        .append("<div class=\"meta\">\n")
                        .append("<div class=\"name\">").append(escapeHtml(f.getName())).append("</div>\n")
                        .append("<div class=\"details\">\n")
                        .append("<span>").append(sizeStr).append("</span>")
                        .append("<span>•</span>")
                        .append("<span>").append(dateStr).append("</span>")
                        .append("</div>\n</div>\n</div>\n")
                        .append("<div class=\"actions\">\n");

                if (isVideo || isAudio) {
                    html.append("<button class=\"btn btn-play\" onclick=\"playMedia('")
                            .append(mediaUrl).append("', ").append(isVideo).append(")\">▶ <span>Play</span></button>\n");
                }
                html.append("<a class=\"btn btn-dl\" href=\"").append(mediaUrl).append("\" download>⬇ <span>Get</span></a>\n");
                html.append("</div>\n</div>\n");
            } catch (Exception ignored) {
            }
        }

        if (dirList.isEmpty() && fileList.isEmpty()) {
            html.append("<div class=\"empty-msg\">📂 This directory is empty.</div>\n");
        }

        html.append("</div>\n") // end file-list
                .append("<footer style=\"margin-top:30px; text-align:center; font-size:12px; color:var(--text-dim);\">\n")
                .append("<p>Pocket Media Server • Stream to Smart TV, Browser, VLC or Kodi</p>\n")
                .append("</footer>\n")
                .append("</div>\n</body>\n</html>");

        return html.toString();
    }

    private static String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
