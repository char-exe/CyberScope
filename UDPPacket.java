package CyberScope;

import java.io.IOException;
import java.util.Map;

/**
 * Class which represents a UDP packet
 *
 * @author Charlie Jones - 100234961
 */
public class UDPPacket extends NetworkPacket {

    private final int len;
    private String protocol = "UDP";

    public static Map<Integer, String> portToApplicationMap;

    static {
        try {
            portToApplicationMap = PortMapping.readObject("udp.txt").getMap();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public UDPPacket(String timestamp, String sourceMAC, String destinationMAC, int totalLen, String etherType,
                     String hex, String protocol, String sourceIP, String destinationIP, int sourcePort,
                     int destinationPort, int networkLen, int len) {
        super(timestamp, sourceMAC, destinationMAC, totalLen, etherType, hex, protocol, sourceIP, destinationIP,
                sourcePort, destinationPort, networkLen);
        this.len = len;

        String sourceProtocol = portToApplicationMap.get(sourcePort);
        String destinationProtocol = portToApplicationMap.get(destinationPort);

        if(sourceProtocol != null && destinationProtocol != null) this.protocol = sourceProtocol;

        if(sourceProtocol != null) this.protocol = sourceProtocol;

        if(destinationProtocol != null) this.protocol = destinationProtocol;

    }

    public String getLen(){
        return String.valueOf(len);
    }

    @Override
    public String getProtocol() { return protocol; }

    @Override
    public String toString() {
        return super.toString() + "\t" + len + "\t" + protocol;
    }
}