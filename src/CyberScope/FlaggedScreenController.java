package CyberScope;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Class for controlling the flagged screen FXML
 *
 * @author Charlie Jones - 100234961
 *
 * @version 1.2
 *
 * 1.0 - Dummy page, no content
 * 1.1 - Added tables for incoming, outgoing and internal traffic
 * 1.2 - Added malicious traffic table
 * 1.3 - Page now updates every 2.5s if the user is capturing live traffic
 */
public class FlaggedScreenController implements Initializable {

    // Referencing FXML elements
    @FXML private VBox flaggedScreen;

    @FXML private TableView<Packet> internalTrafficTable;
    @FXML private TableColumn<Packet, String> internalTrafficSource;
    @FXML private TableColumn<Packet, String> internalTrafficDestination;
    @FXML private TableColumn<Packet, String> internalTrafficProtocol;
    @FXML private TableColumn<Packet, Integer> internalTrafficSize;

    @FXML private TableView<Packet> incomingTrafficTable;
    @FXML private TableColumn<Packet, String> incomingTrafficSource;
    @FXML private TableColumn<Packet, String> incomingTrafficDestination;
    @FXML private TableColumn<Packet, String> incomingTrafficProtocol;
    @FXML private TableColumn<Packet, Integer> incomingTrafficSize;

    @FXML private TableView<Packet> outgoingTrafficTable;
    @FXML private TableColumn<Packet, String> outgoingTrafficSource;
    @FXML private TableColumn<Packet, String> outgoingTrafficDestination;
    @FXML private TableColumn<Packet, String> outgoingTrafficProtocol;
    @FXML private TableColumn<Packet, Integer> outgoingTrafficSize;

    @FXML private TableView<Packet> flaggedTrafficTable;
    @FXML private TableColumn<Packet, String> flaggedTrafficNo;
    @FXML private TableColumn<Packet, String> flaggedTrafficTime;
    @FXML private TableColumn<Packet, String> flaggedTrafficSource;
    @FXML private TableColumn<Packet, String> flaggedTrafficProtocol;
    @FXML private TableColumn<Packet, Integer> flaggedTrafficSize;
    @FXML private TableColumn<Packet, String> flaggedTrafficDestination;
    @FXML private TableColumn<Packet, String> flaggedTrafficNotes;

    // Animation function that runs every 2.5seconds, used for when the user is in live mode
    private final Timeline liveUpdater = new Timeline(new KeyFrame(Duration.millis(2500), e -> {

        // If user is currently on flagged screen
        if(Main.centerNodeContentPointer == flaggedScreen){

            // Initialise and populate tables
            populateInternalTrafficTable();
            populateIncomingTrafficTable();
            populateOutgoingTrafficTable();
            populateFlaggedTrafficTable();

        }
        // Else, stop live updating of this screen
        else {

            pauseLiveUpdater();

        }

    }));

    // Function called when user pauses live capture, stops the scene from updating itself every 2.5s
    private void pauseLiveUpdater(){
        liveUpdater.stop();
    }

    /**
     * This method is run once all of the FXML elements have loaded. It is used to populate all of the tables.
     *
     * @param url JavaFX defined parameter
     * @param resourceBundle JavaFX defined parameter
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        populateInternalTrafficTable();

        populateIncomingTrafficTable();

        populateOutgoingTrafficTable();

        populateFlaggedTrafficTable();

        // If live capture is on, run update function forever until it gets cancelled
        if(Main.isLive) {

            liveUpdater.setCycleCount(Animation.INDEFINITE);
            liveUpdater.play();

        }

    }

    /**
     * This method initialises the flagged traffic table and sets the columns of it so that a Packet object can be
     * inserted into the table. Once it has done this it then populates the table with all of the packets from the
     * database.
     */
    private void populateFlaggedTrafficTable() {

        // Placeholder text shows when there is no data in the table
        flaggedTrafficTable.setPlaceholder(new Label("No flagged traffic yet..."));

        // Sets 'flaggedTrafficNo' column to show the order number of each Packet
        flaggedTrafficNo.setCellValueFactory(new PropertyValueFactory<>("orderNo"));

        // Sets 'flaggedTrafficTime' column to show the timestamp of each packet
        flaggedTrafficTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Sets 'flaggedTrafficSource' column to show either the source MAC address or source IP address and port of the
        //  packet depending on the type of packet
        flaggedTrafficSource.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getSourceIP()+":"+
                        ((NetworkPacket) packet).getSourcePort());
            }

            return new SimpleStringProperty(packet.getSourceMAC());

        });

        // Sets 'flaggedTrafficProtocol' column to show either the EtherType or protocol of the packet depending on the
        //  type of packet
        flaggedTrafficProtocol.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getProtocol());
            }

            return new SimpleStringProperty(packet.getEtherType());

        });

        // Sets 'flaggedTrafficSize' column to show the packet size in bytes of each packet
        flaggedTrafficSize.setCellValueFactory(new PropertyValueFactory<>("totalLen"));

        // Sets 'flaggedTrafficSize' column to show either the destination MAC address or destination IP address and
        //  port of the packet depending on the type of packet
        flaggedTrafficDestination.setCellValueFactory(p -> {
            Packet packet = p.getValue();
            if (packet instanceof NetworkPacket) {
                return new SimpleStringProperty(((NetworkPacket) packet).getDestinationIP() + ":" +
                        ((NetworkPacket) packet).getDestinationPortString());
            }
            return new SimpleStringProperty(packet.getDestinationMAC());

        });

        // Sets 'flaggedTrafficNotes' column to show the notes of each packet
        flaggedTrafficNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Sets 'flaggedTrafficTable' table items to all of the flagged packets in Main's packet database
        flaggedTrafficTable.setItems(FXCollections.observableArrayList(Main.db.getFlaggedPackets()));

    }

    /**
     * This method initialises the outgoing traffic table and sets the columns so that a Packet object can be inserted
     * into the table. Once it has done this it then populates the table with all of the packets from the database
     */
    private void populateOutgoingTrafficTable() {

        // Placeholder text shows when there is no data in the table
        outgoingTrafficTable.setPlaceholder(new Label("No outgoing traffic yet..."));

        // Calls method to initialise each column (in parameter) so it can show the Packet's data
        setCellValueFactories(
                outgoingTrafficSource,
                outgoingTrafficDestination,
                outgoingTrafficProtocol,
                outgoingTrafficSize
        );

        // Sets 'outgoingTrafficTable' table items to all of the outgoing packets in Main's packet database
        outgoingTrafficTable.setItems(FXCollections.observableArrayList(Main.db.getOutgoingPackets()));

    }

    /**
     * This method initialises the incoming traffic table and sets the columns so that a Packet object can be inserted
     * into the table. Once it has done this it then populates the table with all of the packets from the database
     */
    private void populateIncomingTrafficTable() {

        // Placeholder text shows when there is no data in the table
        incomingTrafficTable.setPlaceholder(new Label("No incoming traffic yet..."));

        // Calls method to initialise each column (in parameter) so it can show the Packet's data
        setCellValueFactories(
                incomingTrafficSource,
                incomingTrafficDestination,
                incomingTrafficProtocol,
                incomingTrafficSize
        );

        // Sets 'incomingTrafficTable' table items to all of the incoming packets in Main's packet database
        incomingTrafficTable.setItems(FXCollections.observableArrayList(Main.db.getIncomingPackets()));

    }

    /**
     * This method initialises the internal traffic table and sets the columns so that a Packet object can be inserted
     * into the table. Once it has done this it then populates the table with all of the packets from the database
     */
    private void populateInternalTrafficTable() {

        // Placeholder text shows when there is no data in the table
        internalTrafficTable.setPlaceholder(new Label("No internal traffic yet..."));

        // Calls method to initialise each column (in parameter) so it can show the Packet's data
        setCellValueFactories(
                internalTrafficSource,
                internalTrafficDestination,
                internalTrafficProtocol,
                internalTrafficSize
        );

        // Sets 'internalTrafficTable' table items to all of the internal packets in Main's packet database
        internalTrafficTable.setItems(FXCollections.observableArrayList(Main.db.getInternalPackets()));

    }

    /**
     * Method which initialises the columns of a table which are passed in as parameters.
     *
     * @param sourceColumn The column which shows the Packet's source MAC/IP and Port
     * @param destinationColumn The column which shows the Packet's destination MAC/IP and Port
     * @param protocolColumn The column which shows the Packet's EtherType/Protocol
     * @param sizeColumn The column which shows the Packet's size
     */
    private void setCellValueFactories(
            TableColumn<Packet, String> sourceColumn,
            TableColumn<Packet, String> destinationColumn,
            TableColumn<Packet, String> protocolColumn,
            TableColumn<Packet, Integer> sizeColumn
    ){

        // Sets 'sourceColumn' to show either the source MAC address or source IP address and port of the
        //  packet depending on the type of packet
        sourceColumn.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getSourceIP()+":"+
                        ((NetworkPacket) packet).getSourcePort());
            }

            return new SimpleStringProperty(packet.getSourceMAC());

        });

        // Sets 'protocolColumn' to show either the EtherType or Protocol of the packet depending on the type of packet
        protocolColumn.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getProtocol());
            }

            return new SimpleStringProperty(packet.getEtherType());

        });

        // Sets 'sizeColumn' column to show the packet size in bytes of each packet
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("totalLen"));

        // Sets 'destinationColumn' column to show either the destination MAC address or destination IP address and
        //  port of the packet depending on the type of packet
        destinationColumn.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if (packet instanceof NetworkPacket) {
                return new SimpleStringProperty(((NetworkPacket) packet).getDestinationIP() + ":" +
                        ((NetworkPacket) packet).getDestinationPortString());
            }

            return new SimpleStringProperty(packet.getDestinationMAC());

        });

    }

}
