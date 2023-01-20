package CyberScope;

import java.util.Map;

import static java.util.Map.entry;

/**
 * Class which represents a network packet
 *
 * @author Charlie Jones - 100234961
 */
public class NetworkPacket extends Packet{

    private final String protocol;
    private final String sourceIP;
    private final String destinationIP;
    private final int sourcePort;
    private final int destinationPort;
    private final TrafficDirection trafficDirectionIndicator;
    private final int networkLen;

    // Map which is used by Main.java to convert hex values into protocol names
    public static final Map<Integer, String> ipProtocolMap = Map.ofEntries(
            entry(1, "ICMP"),
            entry(2, "IGMP"),
            entry(6, "TCP"),
            entry(17, "UDP"),
            entry(27, "RDP"),
            entry(50, "ESP"),
            entry(58, "ICMPv6")
    );

    // Enum representing the direction of the network packet
    public enum TrafficDirection {
        INCOMING,OUTGOING,INTERNAL,BIDIRECTIONAL
    }

    public NetworkPacket(String timestamp, String sourceMAC, String destinationMAC, int totalLen, String etherType,
                         String hex, String protocol, String sourceIP, String destinationIP, int sourcePort,
                         int destinationPort, int networkLen) {
        super(timestamp, sourceMAC, destinationMAC, totalLen, etherType, hex);
        this.protocol = protocol;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.trafficDirectionIndicator = getDirection(sourceIP, destinationIP);
        this.networkLen = networkLen;

    }

    public String getSourceIP() {
        return sourceIP;
    }

    public String getDestinationIP() {
        return destinationIP;
    }

    public String getSourcePortString() {
        return String.valueOf(sourcePort);
    }

    public int getSourcePort(){
        return sourcePort;
    }

    public String getDestinationPortString() {
        return String.valueOf(destinationPort);
    }

    public int getDestinationPort(){
        return destinationPort;
    }

    public String getProtocol() { return protocol; }

    public String getNetworkLen() { return String.valueOf(networkLen); }

    public TrafficDirection getDirection() { return trafficDirectionIndicator; }

    /**
     * This method calculates which direction the network packet is heading
     * @param src packet's source address
     * @param dst packet's destination address
     * @return direction of the network packet
     */
    private static TrafficDirection getDirection(String src, String dst){
        if(isNonRoutedAddress(src) && !isNonRoutedAddress(dst)) return TrafficDirection.OUTGOING;

        else if(!isNonRoutedAddress(src) && isNonRoutedAddress(dst)) return TrafficDirection.INCOMING;

        return TrafficDirection.INTERNAL;

    }

    /**
     * Checks if IP address is routed or not
     * @param ip network packet's source or destination address
     * @return boolean depending on whether address is routed or not
     */
    private static boolean isNonRoutedAddress(String ip){
        return ip.startsWith("10.") || ip.startsWith("192.168.") ||
                ip.matches("^(172.)((1[6-9].)|(2\\d.)|(3[01].))(\\d{1,3}.){2}") || ip.startsWith("127.") ||

                // Multicast addresses
                ip.matches("^((22[4-9]|23\\d|24\\d|25[0-5])(.\\d{1,3}.\\d{1,3}.\\d{1,3}))");
    }

    /**
     * Checks if IP address is private or not (not including multi-casting addresses)
     * @param ip network packet's source or destination address
     * @return boolean depending on whether address is private or not
     */
    public static boolean isPrivateNetworkAddress(String ip){
        return ip.startsWith("10.") || ip.startsWith("192.168.") ||
                ip.matches("^(172.)((1[6-9].)|(2\\d.)|(3[01].))(\\d{1,3}.){2}");
    }

    /**
     * Checks if IP address is a loopback address or not
     * @param ip network packet's source or destination address
     * @return boolean depending on whether address is loopback or not
     */
    public static boolean isLoopbackAddress(String ip){
        return ip.startsWith("127.");
    }

    @Override
    public String toString() {
        return super.toString() + "\t" + sourceIP+":"+sourcePort + " ---> " + destinationIP+":"+destinationPort + "\t"
                + protocol;
    }

}
