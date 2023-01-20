package CyberScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class which represents a TCP packet
 *
 * @author Charlie Jones - 100234961
 */
public class TCPPacket extends NetworkPacket {

    private final int len;
    private final ArrayList<String> flags;
    private final String payload;
    private String protocol = "TCP";

    public static Map<Integer, String> portToApplicationMap;

    static {
        try {
            portToApplicationMap = PortMapping.readObject("tcp.txt").getMap();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            portToApplicationMap = new HashMap<>();
        }
    }

    public TCPPacket(String timestamp, String sourceMAC, String destinationMAC, int totalLen, String etherType,
                     String hex, String protocol, String sourceIP, String destinationIP, int sourcePort, int destinationPort,
                     int networkLen, int len, String flagHex, String payload) {
        super(timestamp, sourceMAC, destinationMAC, totalLen, etherType, hex, protocol, sourceIP, destinationIP,
                sourcePort, destinationPort, networkLen);
        this.len = len;
        this.flags = convertHexToTCPFlags(flagHex);
        this.payload = payload;

        String sourceProtocol = portToApplicationMap.get(sourcePort);
        String destinationProtocol = portToApplicationMap.get(destinationPort);

        if(flags.contains("PSH")) {

            if (sourceProtocol != null && destinationProtocol != null) this.protocol = sourceProtocol;

            if (sourceProtocol != null) this.protocol = sourceProtocol;

            if (destinationProtocol != null) this.protocol = destinationProtocol;

        }

    }

    public String getLen(){
        return String.valueOf(len);
    }

    public ArrayList<String> getFlags() {
        return flags;
    }

    public boolean hasPayload() { return !payload.equals(""); }

    public String getPayload() { return payload; }

    @Override
    public String getProtocol() { return protocol; }

    /**
     * This method gathers the flags from the hex data and stores them in the TCPPacket's flags value
     * @param flagHex flags in the form of hex values
     * @return list of flag names
     */
    public ArrayList<String> convertHexToTCPFlags(String flagHex){
        ArrayList<String> flags = new ArrayList<>();

        switch (flagHex.split("")[1]) {
            case "1":
                flags.add("FIN");
                break;
            case "2":
                flags.add("SYN");
                break;
            case "4":
                flags.add("RST");
                break;
            case "8":
                flags.add("PSH");
                break;
        }

        switch (flagHex.split("")[0]) {
            case "1":
                flags.add("ACK");
                break;
            case "2":
                flags.add("URG");
                break;
            case "4":
                flags.add("ECE");
                break;
            case "8":
                flags.add("CWR");
                break;
        }

        return flags;
    }

    @Override
    public String toString() {
        return super.toString() + "\t" + len + "\t" + flags + "\t" + protocol;
    }
}
