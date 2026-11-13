package org.pocketdev.mediaserver.upnp;

public class UpnpDescriptions {

    public static String getDeviceDescription(String friendlyName, String uuid, String hostIp, int port) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<root xmlns=\"urn:schemas-upnp-org:device-1-0\" xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">\n" +
                "  <specVersion>\n" +
                "    <major>1</major>\n" +
                "    <minor>0</minor>\n" +
                "  </specVersion>\n" +
                "  <device>\n" +
                "    <deviceType>urn:schemas-upnp-org:device:MediaServer:1</deviceType>\n" +
                "    <friendlyName>" + escapeXml(friendlyName) + "</friendlyName>\n" +
                "    <manufacturer>PocketDev</manufacturer>\n" +
                "    <manufacturerURL>http://localhost</manufacturerURL>\n" +
                "    <modelDescription>Android DLNA UPnP and HTTP Media Server</modelDescription>\n" +
                "    <modelName>PocketMediaServer</modelName>\n" +
                "    <modelNumber>1.0</modelNumber>\n" +
                "    <modelURL>http://localhost</modelURL>\n" +
                "    <serialNumber>100000000001</serialNumber>\n" +
                "    <UDN>uuid:" + uuid + "</UDN>\n" +
                "    <dlna:X_DLNADOC xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">DMS-1.50</dlna:X_DLNADOC>\n" +
                "    <serviceList>\n" +
                "      <service>\n" +
                "        <serviceType>urn:schemas-upnp-org:service:ContentDirectory:1</serviceType>\n" +
                "        <serviceId>urn:upnp-org:serviceId:ContentDirectory</serviceId>\n" +
                "        <SCPDURL>/upnp/cd_scpd.xml</SCPDURL>\n" +
                "        <controlURL>/upnp/control/cd</controlURL>\n" +
                "        <eventSubURL>/upnp/event/cd</eventSubURL>\n" +
                "      </service>\n" +
                "      <service>\n" +
                "        <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>\n" +
                "        <serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId>\n" +
                "        <SCPDURL>/upnp/cm_scpd.xml</SCPDURL>\n" +
                "        <controlURL>/upnp/control/cm</controlURL>\n" +
                "        <eventSubURL>/upnp/event/cm</eventSubURL>\n" +
                "      </service>\n" +
                "    </serviceList>\n" +
                "  </device>\n" +
                "</root>";
    }

    public static String getContentDirectoryScpd() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n" +
                "  <specVersion>\n" +
                "    <major>1</major>\n" +
                "    <minor>0</minor>\n" +
                "  </specVersion>\n" +
                "  <actionList>\n" +
                "    <action>\n" +
                "      <name>GetSearchCapabilities</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>SearchCaps</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>SearchCapabilities</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "    <action>\n" +
                "      <name>GetSortCapabilities</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>SortCaps</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>SortCapabilities</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "    <action>\n" +
                "      <name>GetSystemUpdateID</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>Id</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>SystemUpdateID</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "    <action>\n" +
                "      <name>Browse</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>ObjectID</name>\n" +
                "          <direction>in</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_ObjectID</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>BrowseFlag</name>\n" +
                "          <direction>in</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_BrowseFlag</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>Filter</name>\n" +
                "          <direction>in</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Filter</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>StartingIndex</name>\n" +
                "          <direction>in</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Index</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>RequestedCount</name>\n" +
                "          <direction>in</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>SortCriteria</name>\n" +
                "          <direction>in</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_SortCriteria</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>Result</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Result</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>NumberReturned</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>TotalMatches</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>UpdateID</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_UpdateID</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "  </actionList>\n" +
                "  <serviceStateTable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_ObjectID</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_Result</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_BrowseFlag</name>\n" +
                "      <dataType>string</dataType>\n" +
                "      <allowedValueList>\n" +
                "        <allowedValue>BrowseMetadata</allowedValue>\n" +
                "        <allowedValue>BrowseDirectChildren</allowedValue>\n" +
                "      </allowedValueList>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_Filter</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_SortCriteria</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_Index</name>\n" +
                "      <dataType>ui4</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_Count</name>\n" +
                "      <dataType>ui4</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_UpdateID</name>\n" +
                "      <dataType>ui4</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>SearchCapabilities</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>SortCapabilities</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"yes\">\n" +
                "      <name>SystemUpdateID</name>\n" +
                "      <dataType>ui4</dataType>\n" +
                "    </stateVariable>\n" +
                "  </serviceStateTable>\n" +
                "</scpd>";
    }

    public static String getConnectionManagerScpd() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n" +
                "  <specVersion>\n" +
                "    <major>1</major>\n" +
                "    <minor>0</minor>\n" +
                "  </specVersion>\n" +
                "  <actionList>\n" +
                "    <action>\n" +
                "      <name>GetProtocolInfo</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>Source</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>SourceProtocolInfo</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>Sink</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>SinkProtocolInfo</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "    <action>\n" +
                "      <name>GetCurrentConnectionIDs</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>ConnectionIDs</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>CurrentConnectionIDs</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "    <action>\n" +
                "      <name>GetCurrentConnectionInfo</name>\n" +
                "      <argumentList>\n" +
                "        <argument>\n" +
                "          <name>RcsID</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_RcsID</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>AVTransportID</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_AVTransportID</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>ProtocolInfo</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_ProtocolInfo</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>PeerConnectionManager</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_ConnectionManager</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>PeerConnectionID</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>Direction</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_Direction</relatedStateVariable>\n" +
                "        </argument>\n" +
                "        <argument>\n" +
                "          <name>Status</name>\n" +
                "          <direction>out</direction>\n" +
                "          <relatedStateVariable>A_ARG_TYPE_ConnectionStatus</relatedStateVariable>\n" +
                "        </argument>\n" +
                "      </argumentList>\n" +
                "    </action>\n" +
                "  </actionList>\n" +
                "  <serviceStateTable>\n" +
                "    <stateVariable sendEvents=\"yes\">\n" +
                "      <name>SourceProtocolInfo</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"yes\">\n" +
                "      <name>SinkProtocolInfo</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"yes\">\n" +
                "      <name>CurrentConnectionIDs</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_ConnectionStatus</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_ConnectionID</name>\n" +
                "      <dataType>i4</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_Direction</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_ProtocolInfo</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_ConnectionManager</name>\n" +
                "      <dataType>string</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_AVTransportID</name>\n" +
                "      <dataType>i4</dataType>\n" +
                "    </stateVariable>\n" +
                "    <stateVariable sendEvents=\"no\">\n" +
                "      <name>A_ARG_TYPE_RcsID</name>\n" +
                "      <dataType>i4</dataType>\n" +
                "    </stateVariable>\n" +
                "  </serviceStateTable>\n" +
                "</scpd>";
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
