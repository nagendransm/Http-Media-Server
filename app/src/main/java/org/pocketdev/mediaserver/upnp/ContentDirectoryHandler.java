package org.pocketdev.mediaserver.upnp;

import org.pocketdev.mediaserver.model.ServerConfig;
import org.pocketdev.mediaserver.util.MimeUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentDirectoryHandler {

    private final ServerConfig config;
    private final String hostIp;
    private final int port;

    public ContentDirectoryHandler(ServerConfig config, String hostIp, int port) {
        this.config = config;
        this.hostIp = hostIp;
        this.port = port;
    }

    public String handleSoapAction(String action, String soapBody) {
        if (action != null && action.contains("GetSortCapabilities")) {
            return buildGetSortCapabilitiesResponse();
        } else if (action != null && action.contains("GetSearchCapabilities")) {
            return buildGetSearchCapabilitiesResponse();
        } else if (action != null && action.contains("GetSystemUpdateID")) {
            return buildGetSystemUpdateIdResponse();
        } else if (action != null && action.contains("Browse")) {
            return handleBrowse(soapBody);
        }

        // Generic fault response
        return buildSoapFault("401", "Invalid Action");
    }

    private String handleBrowse(String soapBody) {
        String objectId = extractTagValue(soapBody, "ObjectID");
        String browseFlag = extractTagValue(soapBody, "BrowseFlag");
        int startingIndex = parseInt(extractTagValue(soapBody, "StartingIndex"), 0);
        int requestedCount = parseInt(extractTagValue(soapBody, "RequestedCount"), 0);

        if (objectId == null || objectId.isEmpty()) {
            objectId = "0";
        }
        if (browseFlag == null || browseFlag.isEmpty()) {
            browseFlag = "BrowseDirectChildren";
        }

        File rootDir = new File(config.getSharedPath());
        File targetFile;

        if ("0".equals(objectId)) {
            targetFile = rootDir;
        } else {
            // objectId is relative path
            targetFile = new File(rootDir, objectId);
        }

        if (!targetFile.exists()) {
            return buildSoapFault("701", "No Such Object");
        }

        StringBuilder didl = new StringBuilder();
        didl.append("<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" ")
                .append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ")
                .append("xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" ")
                .append("xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\">");

        int numberReturned = 0;
        int totalMatches = 0;

        if ("BrowseMetadata".equalsIgnoreCase(browseFlag)) {
            totalMatches = 1;
            numberReturned = 1;
            if (targetFile.isDirectory()) {
                String parentId = "0".equals(objectId) ? "-1" : getParentId(rootDir, targetFile);
                didl.append(buildContainerXml(targetFile, objectId, parentId, rootDir));
            } else {
                String parentId = getParentId(rootDir, targetFile);
                didl.append(buildItemXml(targetFile, objectId, parentId));
            }
        } else {
            // BrowseDirectChildren
            if (targetFile.isDirectory()) {
                File[] list = targetFile.listFiles();
                List<File> fileList = new ArrayList<>();
                if (list != null) {
                    for (File f : list) {
                        // ignore hidden files
                        if (!f.getName().startsWith(".")) {
                            fileList.add(f);
                        }
                    }
                }

                // Sort: directories first, then files alphabetically
                Collections.sort(fileList, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        if (o1.isDirectory() && !o2.isDirectory()) return -1;
                        if (!o1.isDirectory() && o2.isDirectory()) return 1;
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    }
                });

                totalMatches = fileList.size();
                int endIndex = totalMatches;
                if (requestedCount > 0 && (startingIndex + requestedCount) < totalMatches) {
                    endIndex = startingIndex + requestedCount;
                }

                for (int i = startingIndex; i < endIndex && i < totalMatches; i++) {
                    File item = fileList.get(i);
                    String itemRelPath = getRelativePath(rootDir, item);
                    if (item.isDirectory()) {
                        didl.append(buildContainerXml(item, itemRelPath, objectId, rootDir));
                    } else {
                        didl.append(buildItemXml(item, itemRelPath, objectId));
                    }
                    numberReturned++;
                }
            }
        }

        didl.append("</DIDL-Lite>");

        String escapedDidl = escapeXml(didl.toString());

        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "  <s:Body>\n" +
                "    <u:BrowseResponse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">\n" +
                "      <Result>" + escapedDidl + "</Result>\n" +
                "      <NumberReturned>" + numberReturned + "</NumberReturned>\n" +
                "      <TotalMatches>" + totalMatches + "</TotalMatches>\n" +
                "      <UpdateID>1</UpdateID>\n" +
                "    </u:BrowseResponse>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";
    }

    private String buildContainerXml(File folder, String id, String parentId, File rootDir) {
        String title = "0".equals(id) ? config.getServerName() : folder.getName();
        int childCount = 0;
        File[] children = folder.listFiles();
        if (children != null) {
            for (File c : children) {
                if (!c.getName().startsWith(".")) childCount++;
            }
        }
        return "<container id=\"" + escapeXml(id) + "\" parentID=\"" + escapeXml(parentId) +
                "\" restricted=\"1\" searchable=\"1\" childCount=\"" + childCount + "\">" +
                "<dc:title>" + escapeXml(title) + "</dc:title>" +
                "<upnp:class>object.container.storageFolder</upnp:class>" +
                "</container>";
    }

    private String buildItemXml(File file, String id, String parentId) {
        String mime = MimeUtils.getMimeType(file.getName());
        String upnpClass = MimeUtils.getUpnpClass(mime);
        long size = file.length();
        String streamUrl;
        try {
            streamUrl = "http://" + hostIp + ":" + port + "/media?path=" + URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            streamUrl = "http://" + hostIp + ":" + port + "/media?path=" + id;
        }

        String dlnaProfile = "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
        String protocolInfo = "http-get:*:" + mime + ":" + dlnaProfile;

        return "<item id=\"" + escapeXml(id) + "\" parentID=\"" + escapeXml(parentId) + "\" restricted=\"1\">" +
                "<dc:title>" + escapeXml(file.getName()) + "</dc:title>" +
                "<upnp:class>" + upnpClass + "</upnp:class>" +
                "<res protocolInfo=\"" + protocolInfo + "\" size=\"" + size + "\">" +
                escapeXml(streamUrl) +
                "</res>" +
                "</item>";
    }

    private String getRelativePath(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootPath)) {
            String rel = filePath.substring(rootPath.length());
            if (rel.startsWith("/")) rel = rel.substring(1);
            return rel;
        }
        return file.getName();
    }

    private String getParentId(File root, File file) {
        File parent = file.getParentFile();
        if (parent == null || parent.equals(root) || !parent.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            return "0";
        }
        return getRelativePath(root, parent);
    }

    private String buildGetSortCapabilitiesResponse() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "  <s:Body>\n" +
                "    <u:GetSortCapabilitiesResponse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">\n" +
                "      <SortCaps>dc:title</SortCaps>\n" +
                "    </u:GetSortCapabilitiesResponse>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";
    }

    private String buildGetSearchCapabilitiesResponse() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "  <s:Body>\n" +
                "    <u:GetSearchCapabilitiesResponse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">\n" +
                "      <SearchCaps></SearchCaps>\n" +
                "    </u:GetSearchCapabilitiesResponse>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";
    }

    private String buildGetSystemUpdateIdResponse() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "  <s:Body>\n" +
                "    <u:GetSystemUpdateIDResponse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">\n" +
                "      <Id>1</Id>\n" +
                "    </u:GetSystemUpdateIDResponse>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";
    }

    private String buildSoapFault(String code, String desc) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <s:Body>\n" +
                "    <s:Fault>\n" +
                "      <faultcode>s:Client</faultcode>\n" +
                "      <faultstring>UPnPError</faultstring>\n" +
                "      <detail>\n" +
                "        <UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">\n" +
                "          <errorCode>" + code + "</errorCode>\n" +
                "          <errorDescription>" + desc + "</errorDescription>\n" +
                "        </UPnPError>\n" +
                "      </detail>\n" +
                "    </s:Fault>\n" +
                "  </s:Body>\n" +
                "</s:Envelope>";
    }

    private String extractTagValue(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<(?:\\w+:)?(" + Pattern.quote(tagName) + ")>(.*?)</(?:\\w+:)?\\1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "";
    }

    private int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
    }

    private static String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
