package CyberScope;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.*;

/**
 * Class which represents a packet database, which stores every packet that is captured and collects various stats
 * about the overall traffic.
 *
 * @author Charlie Jones - 100234961
 */
public class PacketDatabase {

    private int packetOrderNo = 1;

    private int count = 0;
    private int linkLayerCount = 0;
    private int networkLayerCount = 0;
    private int applicationLayerCount = 0;
    private int incomingTrafficCount = 0;
    private int outgoingTrafficCount = 0;
    private int internalTrafficCount = 0;

    private final TreeMap<String, Integer> usedPorts = new TreeMap<>();
    private final ArrayList<String> topUsedPorts = new ArrayList<>();
    private final TreeMap<String, Integer> usedProtocols = new TreeMap<>();
    private final ArrayList<String> topUsedProtocols = new ArrayList<>();

    private final ArrayList<String> uniqueIncomingPublicAddresses = new ArrayList<>();
    private final ArrayList<NetworkPacket> uniqueIncomingPublicPackets = new ArrayList<>();
    private final ArrayList<String> uniqueOutgoingPublicAddresses = new ArrayList<>();
    private final ArrayList<NetworkPacket> uniqueOutgoingPublicPackets = new ArrayList<>();

    private final ArrayList<Packet> internalPackets = new ArrayList<>();
    private final ArrayList<NetworkPacket> incomingPackets = new ArrayList<>();
    private final ArrayList<NetworkPacket> outgoingPackets = new ArrayList<>();
    private final ArrayList<Packet> flaggedPackets = new ArrayList<>();

    private final ArrayList<Packet> packets = new ArrayList<>();

    /**
     * This constructor initialises the database with a list of packets, used when a file is being read and there are
     * already packets ready to be added.
     * @param packets list of packets already processed
     */
    public PacketDatabase(ArrayList<Packet> packets){

        // For each packet, add to the database
        for(Packet packet : packets){

            addPacket(packet);

        }

        arrangePortAndProtocolMetrics();

        // Animation function that runs every 2.5seconds, used for when the user is in live mode
        if(Main.isLive) {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(2500), e -> arrangePortAndProtocolMetrics())
            );
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }

    }

    /**
     * This constructor initialises the database with no packets, used when traffic is being captured live and there
     * aren't any packets already available to be added.
     */
    public PacketDatabase(){
        // Animation function that runs every 2.5seconds, used for when the user is in live mode
        if(Main.isLive) {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(2500), e -> arrangePortAndProtocolMetrics())
            );
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }
    }

    /**
     * This method is called when a packet needs to be stored in the database. It takes the packet object and then
     * collects all of it's statistics before adding it to the packet array list.
     * @param packet the packet object that is being added to the database
     */
    public void addPacket(Packet packet){

        // Assign the packet order number of the packet to the current packet order value
        packet.setOrderNo(packetOrderNo);

        collectProtocolStats(packet);

        // If the packet is a network layer packet
        if(packet instanceof NetworkPacket){

            boolean isApplicationLayer = !NetworkPacket.ipProtocolMap.containsValue(((NetworkPacket) packet).getProtocol());

            // If the packet is an application layer packet
            if(isApplicationLayer){

                addApplicationLayerPacket((NetworkPacket) packet);

            } else{

                addNetworkLayerPacket((NetworkPacket) packet);

            }

            // If the packet is incoming
            if(((NetworkPacket) packet).getDirection() == NetworkPacket.TrafficDirection.INCOMING) {
                incomingTrafficCount++;
                collectPortStats((NetworkPacket) packet, NetworkPacket.TrafficDirection.INCOMING);
                checkUniqueAddr(((NetworkPacket) packet), true);
                incomingPackets.add((NetworkPacket) packet);
            }
            // Else, if packet is outgoing
            else if(((NetworkPacket) packet).getDirection() == NetworkPacket.TrafficDirection.OUTGOING) {
                outgoingTrafficCount++;
                collectPortStats((NetworkPacket) packet, NetworkPacket.TrafficDirection.OUTGOING);
                checkUniqueAddr(((NetworkPacket) packet), false);
                outgoingPackets.add((NetworkPacket) packet);
            }
            // Else, packet must be internal traffic
            else {
                internalTrafficCount++;
                internalPackets.add(packet);
            }

        }
        // Else, packet is a link layer packet
        else {

            addLinkLayerPacket(packet);
            internalPackets.add(packet);

        }

        // Increment packet order value
        packetOrderNo++;

    }

    /**
     * This method works out which ports and protocols are most used out of the current dataset
     */
    public void arrangePortAndProtocolMetrics(){

        // Clear current orders
        topUsedProtocols.clear();
        topUsedPorts.clear();

        // Sort each port entry in map by value, and then add it to top used ports list
        usedPorts.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(item -> topUsedPorts.add(item.toString()));

        // Reverse order to descending
        Collections.reverse(topUsedPorts);

        // Sort each protocol entry in map by value, and then add it to top used protocols list
        usedProtocols.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(item -> topUsedProtocols.add(item.toString()));

        // Reverse order to descending
        Collections.reverse(topUsedProtocols);

    }

    /**
     * This method is called whenever a link layer packet is found, it collects the link layer statistics and adds the
     * packet to the database
     * @param packet the packet object to be added to the database
     */
    public void addLinkLayerPacket(Packet packet){
        packets.add(packet);
        count++;
        linkLayerCount++;
        internalTrafficCount++;
    }

    /**
     * This method is called whenever a network layer packet is found, it collects the network layer statistics and then
     * adds the packet to the database
     * @param packet the network packet object to be added to the database
     */
    public void addNetworkLayerPacket(NetworkPacket packet){
        packets.add(packet);
        count++;
        networkLayerCount++;
    }

    /**
     * This method is called whenever an application layer packet is found, it collects the application layer statistics
     * and adds the packet to the database
     * @param packet the application layer packet to be added to the database
     */
    public void addApplicationLayerPacket(NetworkPacket packet){
        packets.add(packet);
        count++;
        applicationLayerCount++;
    }

    /**
     * This method collects the port stats of a packet based on it's direction
     * @param packet the packet that the port stats are to be collected from
     * @param direction the direction of the packet
     */
    public void collectPortStats(NetworkPacket packet, NetworkPacket.TrafficDirection direction){

        // If direction is incoming
        if(direction == NetworkPacket.TrafficDirection.INCOMING){

            String port = packet.getSourcePortString();

            // If port has already been used at least once, increment counter
            if(usedPorts.containsKey(port)){

                int val = usedPorts.get(port) + 1;
                usedPorts.replace(port, val);

            }
            // Else, create new entry for port
            else{

                usedPorts.put(port, 1);

            }

        }
        // Else, direction is outgoing
        else {

            String port = packet.getDestinationPortString();
            if(usedPorts.containsKey(port)){

                int val = usedPorts.get(port) + 1;
                usedPorts.replace(port, val);

            } else{

                usedPorts.put(port, 1);

            }

        }
    }

    /**
     * A packet's protocol stats are collected here
     * @param packet the packet which has it's protocol stats collected from
     */
    public void collectProtocolStats(Packet packet){

        String protocol;

        if(packet instanceof NetworkPacket) protocol = ((NetworkPacket) packet).getProtocol();
        else protocol = packet.getEtherType();

        if(usedProtocols.containsKey(protocol)){

            int val = usedProtocols.get(protocol) + 1;
            usedProtocols.replace(protocol, val);

        } else {

            usedProtocols.put(protocol, 1);

        }

    }

    /* Not used currently

    public void removeLinkLayerPacket(Packet packet){
        packets.remove(packet);
        count--;
        linkLayerCount--;
        internalTrafficCount--;
        internalPackets.remove(packet);
    }

    public void removeNetworkLayerPacket(NetworkPacket packet){
        packets.remove(packet);
        count--;
        networkLayerCount--;
        removePacketDirectionWeight(packet);
    }

    public void removeApplicationLayerPacket(NetworkPacket packet){
        packets.remove(packet);
        count--;
        applicationLayerCount--;
        removePacketDirectionWeight(packet);
    }

    private void removePacketDirectionWeight(NetworkPacket packet){

        if(packet.getDirection() == NetworkPacket.TrafficDirection.INCOMING) {
            incomingTrafficCount--;
            incomingPackets.remove(packet);
        }

        else if(packet.getDirection() == NetworkPacket.TrafficDirection.OUTGOING) {
            outgoingTrafficCount--;
            outgoingPackets.remove(packet);
        }

        else {
            internalTrafficCount--;
            internalPackets.remove(packet);
        }

    }

    public int getLinkLayerCount(){
        return linkLayerCount;
    }

    public int getNetworkLayerCount(){
        return networkLayerCount;
    }

    public int getApplicationLayerCount(){
        return applicationLayerCount;
    }

    */

    /**
     * This method checks if a packet is unique, and adds it to a unique address list if it is.
     * @param packet the packet that is checked if it is unique
     * @param isIncoming the direction of the packet
     */
    private void checkUniqueAddr(NetworkPacket packet, boolean isIncoming){

        if(isIncoming) {
            if (!uniqueIncomingPublicAddresses.contains(packet.getSourceIP())) {
                uniqueIncomingPublicAddresses.add(packet.getSourceIP());
                uniqueIncomingPublicPackets.add(packet);
            }
        } else {
            if (!uniqueOutgoingPublicAddresses.contains(packet.getDestinationIP())) {
                uniqueOutgoingPublicAddresses.add(packet.getDestinationIP());
                uniqueOutgoingPublicPackets.add(packet);
            }
        }
    }

    public int getCount(){
        return count;
    }

    public int getIncomingTrafficCount() { return incomingTrafficCount; }

    public int getOutgoingTrafficCount() { return outgoingTrafficCount; }

    public int getInternalTrafficCount() { return internalTrafficCount; }

    public ArrayList<Packet> getPackets(){
        return packets;
    }

    public ArrayList<String> getUsedProtocols(){
        return new ArrayList<>(usedProtocols.keySet());
    }

    public ArrayList<String> getTopUsedPorts(){
        return topUsedPorts;
    }

    public ArrayList<String> getTopUsedProtocols() {
        return topUsedProtocols;
    }

    public ArrayList<NetworkPacket> getUniqueIncomingPublicPackets() {
        return uniqueIncomingPublicPackets;
    }

    public ArrayList<NetworkPacket> getUniqueOutgoingPublicPackets() {
        return uniqueOutgoingPublicPackets;
    }

    public ArrayList<Packet> getInternalPackets(){
        return internalPackets;
    }

    public ArrayList<Packet> getFlaggedPackets() {
        return flaggedPackets;
    }

    public ArrayList<NetworkPacket> getIncomingPackets() {
        return incomingPackets;
    }

    public ArrayList<NetworkPacket> getOutgoingPackets() {
        return outgoingPackets;
    }

}
