package CyberScope;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Class for controlling the home screen FXML
 *
 * @author Charlie Jones - 100234961
 *
 * @version 1.2
 *
 * 1.0 - Dummy page, no content
 * 1.1 - Added nodes
 * 1.2 - Page now updates every 2.5s if the user is capturing live traffic
 */
public class LANController implements Initializable {

    //Referencing elements defined in LANScreen.fxml
    @FXML private VBox lanScreen;
    @FXML private VBox networkNodeContainer;
    @FXML private StackPane hubContainer;
    @FXML private Rectangle hub;

    // Animation function that runs every 2.5seconds, used for when the user is in live mode
    private final Timeline liveUpdater = new Timeline(new KeyFrame(Duration.millis(2500), e -> {

        // If the user is currently on the LAN screen, call 'drawNodes' function
        if(Main.centerNodeContentPointer == lanScreen){
            drawNodes();

        }
        // Else, stop live updating this screen
        else {
            pauseLiveUpdater();

        }

    }));

    // Function called when user pauses live capture, stops the scene from updating itself every 2.5s
    private void pauseLiveUpdater(){
        liveUpdater.stop();
    }

    /**
     * This method is run once all of the FXML elements have loaded. It is used to initialise the screen.
     *
     * @param url JavaFX defined parameter
     * @param resourceBundle JavaFX defined parameter
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Make the network node the same height and width of it's parent container
        hub.widthProperty().bind(hubContainer.widthProperty());
        hub.heightProperty().bind(hubContainer.heightProperty());

        //new Label("Hub").setLabelFor(hub);

        // Calls method to draw all of the nodes connected to the network
        drawNodes();

        // If user is capturing live traffic, rather than just reading a file
        if(Main.isLive) {

            // Run update function forever until it gets cancelled
            liveUpdater.setCycleCount(Animation.INDEFINITE);
            liveUpdater.play();

        }

    }

    /**
     * This method gets every network node and adds a FXML version of each node and adds it to the screen
     */
    private void drawNodes(){

        // For each node in sorted node list
        for(LANTrafficNode node : new LANTrafficNodeCollection().getSortedAddrsByVolume()){

            // Add HBox containing all of the node information to the 'networkNodeContainer'
            networkNodeContainer.getChildren().add(
                    getNetworkNode(node.name, node.incomingCount, node.outgoingCount)
            );

        }

    }

    /**
     * This method is responsible for actually creating the FXML content that represents a network node.
     *
     * @param name the node's MAC/IP address
     * @param incoming the node's incoming traffic volume
     * @param outgoing the node's outgoing traffic volume
     * @return HBox containing all of the information about the network node
     */
    private HBox getNetworkNode(String name, int incoming, int outgoing){

        HBox parent = new HBox();

        parent.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(name);
        nameLabel.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 24px;"
        );

        Label incomingLabel = new Label("Incoming traffic: " + incoming);
        incomingLabel.setStyle(
                "-fx-font-family: Arial;" +
                "-fx-font-size: 16px;"
        );

        Label outgoingLabel = new Label("Outgoing traffic: " + outgoing);
        outgoingLabel.setStyle(
                "-fx-font-family: Arial;" +
                "-fx-font-size: 16px;"
        );

        VBox infoBox = new VBox(nameLabel, incomingLabel, outgoingLabel);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setSpacing(8);
        infoBox.setMinHeight(100);

        HBox.setHgrow(infoBox, Priority.ALWAYS);

        infoBox.setStyle(
                "-fx-background-color: #8787f3;" +
                "-fx-border-color: black;" +
                "-fx-border-width: 2px;"
        );

        Line line = new Line();

        line.setStartX(0d);
        line.setStartY(50d);
        line.setEndX(100d);
        line.setEndY(50d);

        line.setStrokeWidth(5);

        parent.getChildren().addAll(infoBox, line);

        return parent;

    }

    /**
     * This nested class is used as a container for all of the network nodes currently connected to the user's network.
     * Here, the individual nodes will be created but also the sum of all of their traffic will be calculated. On
     * instantiation of this class, it will retrieve all of the packets from the packet database, and count incoming and
     * outgoing traffic from each MAC or IP address depending if the Packet is a NetworkPacket or not.
     */
    private static class LANTrafficNodeCollection {

        // HashMap containing every address as a key, and a LANTrafficNode to store traffic volume statistics
        private final HashMap<String, LANTrafficNode> addrs = new HashMap<>();

        public LANTrafficNodeCollection(){

            // For every Packet in packet database
            for(Packet packet : Main.getDb().getPackets()){

                // If packet is a network pacet
                if(packet instanceof NetworkPacket){

                    // Get direction of packet
                    NetworkPacket.TrafficDirection direction = ((NetworkPacket) packet).getDirection();

                    // If packet is outgoing
                    if(direction == NetworkPacket.TrafficDirection.OUTGOING){

                        // Get the source address of packet
                        String sourceIP = ((NetworkPacket) packet).getSourceIP();

                        // If HashMap already contains address, add outgoing count to it's network node object
                        if(addrs.containsKey(sourceIP)){

                            addrs.get(sourceIP).outgoingCount++;

                        }
                        // Else, add new entry to hashmap, with IP address as they key, and a new network node as the
                        //  value
                        else {

                            addrs.put(sourceIP, new LANTrafficNode(sourceIP, 0, 1));

                        }

                    }
                    // Else if direction is incoming
                    else if(direction == NetworkPacket.TrafficDirection.INCOMING){

                        // Get destination IP address of packet
                        String destinationIP = ((NetworkPacket) packet).getDestinationIP();

                        // If HashMap already contains address, add incoming count to it's network node object
                        if(addrs.containsKey(destinationIP)){

                            addrs.get(destinationIP).incomingCount++;

                        }
                        // Else, add new entry to hashmap, with IP address as they key, and a new network node as the
                        //  value
                        else {

                            addrs.put(destinationIP,new LANTrafficNode(destinationIP,1, 0));

                        }

                    }
                    // Else, two network nodes are interacting with each other, so both source and destination IP
                    //  addresses must be counted
                    else {

                        // Get source and destination IP addresses of packet
                        String sourceIP = ((NetworkPacket) packet).getSourceIP();
                        String destinationIP = ((NetworkPacket) packet).getDestinationIP();

                        // Additional checks to see if non-routed addresses are actually private. If both source and
                        //  destination IPs are private network addresses i.e. not multicast or loopback
                        if(NetworkPacket.isPrivateNetworkAddress(sourceIP) &&
                                NetworkPacket.isPrivateNetworkAddress(destinationIP)){

                            // If HashMap already contains source address, add outgoing count to it's network node
                            if(addrs.containsKey(sourceIP)){

                                addrs.get(sourceIP).outgoingCount++;

                            }
                            // Else, add new entry to hashmap, with IP address as they key, and a new network node as
                            //  the value
                            else {

                                addrs.put(sourceIP, new LANTrafficNode(sourceIP,0,1));

                            }

                            // If HashMap already contains destination address, add incoming count to it's network node
                            if(addrs.containsKey(destinationIP)){

                                addrs.get(destinationIP).incomingCount++;

                            }
                            // Else, add new entry to hashmap, with IP address as they key, and a new network node as
                            //  the value
                            else {

                                addrs.put(destinationIP,new LANTrafficNode(destinationIP,1,0));

                            }


                        // Else if only source IP is a private network address
                        } else if(NetworkPacket.isPrivateNetworkAddress(sourceIP) &&
                                !NetworkPacket.isPrivateNetworkAddress(destinationIP)){

                            // If destination IP is a loopback address, incoming and outgoing count for source IP must
                            //  be incremented.
                            if(NetworkPacket.isLoopbackAddress(destinationIP)){

                                if(addrs.containsKey(sourceIP)){

                                    LANTrafficNode node = addrs.get(sourceIP);

                                    node.incomingCount++;

                                    node.outgoingCount++;

                                } else {

                                    addrs.put(sourceIP, new LANTrafficNode(sourceIP,1,1));

                                }

                            // Destination IP is a multicast address, just increment outgoing count of source IP
                            } else {

                                if(addrs.containsKey(sourceIP)){

                                    addrs.get(sourceIP).outgoingCount++;

                                } else {

                                    addrs.put(sourceIP, new LANTrafficNode(sourceIP,0,1));

                                }

                            }

                        }

                    }

                }
                // Else, packet is an ethernet layer packet, so MAC addresses must be counted for
                else {

                    // Get source and destination MAC addresses of packet
                    String sourceMAC = packet.getSourceMAC();
                    String destinationMAC = packet.getDestinationMAC();

                    // If HashMap already contains source address, add outgoing count to it's network node
                    if(addrs.containsKey(sourceMAC)){

                        addrs.get(sourceMAC).outgoingCount++;

                    }
                    // Else, add new entry to hashmap, with IP address as they key, and a new network node as the value
                    else {

                        addrs.put(sourceMAC, new LANTrafficNode(sourceMAC, 0 ,1));

                    }

                    // If HashMap already contains destination address, add incoming count to it's network node
                    if(addrs.containsKey(destinationMAC)){

                        addrs.get(destinationMAC).incomingCount++;

                    }
                    // Else, add new entry to hashmap, with IP address as they key, and a new network node as the value
                    else {

                        addrs.put(destinationMAC,new LANTrafficNode(destinationMAC,1,0));

                    }


                }

            }

        }

        /**
         * This method gets all network nodes sorted by most volume of traffic, to least volume of traffic.
         *
         * @return LANTrafficNode array, sorted by most volume to least volume
         */
        public LANTrafficNode[] getSortedAddrsByVolume(){

            LANTrafficNode[] result = addrs.values().toArray(new LANTrafficNode[0]);

            Arrays.sort(result);

            return result;

        }

    }

    /**
     * This class represents a connection to the network. It stores the address, incoming traffic volume and outgoing
     * traffic volume.
     *
     * It implements comparable, because a collection of this class must be sorted by total traffic volume.
     */
    private static class LANTrafficNode implements Comparable<LANTrafficNode> {

        private final String name;
        private int incomingCount;
        private int outgoingCount;

        public LANTrafficNode(String name, int incomingCount, int outgoingCount){

            this.name = name;
            this.incomingCount = incomingCount;
            this.outgoingCount = outgoingCount;

        }

        // Comparable method
        @Override
        public int compareTo(LANTrafficNode o) {

            // This node's total traffic volume
            int thisTotal = incomingCount + outgoingCount;

            // Other node's total traffic volume
            int otherTotal = o.incomingCount + o.outgoingCount;

            // If totals are different
            if(thisTotal != otherTotal){

                // Sort by descending order
                return otherTotal - thisTotal;

            }

            // Else, totals are the same, so sort by name
            return this.name.compareTo(o.name);

        }
    }

}
