package CyberScope;

import CyberScope.Filter.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Class for controlling the home screen FXML
 *
 * @author Charlie Jones - 100234961
 *
 * @version 1.6
 *
 * 1.0 - Dummy page, no content
 * 1.1 - Added main table
 * 1.2 - Added detailed Packet view feature
 * 1.3 - Added ability to apply a filter
 * 1.4 - Added ability to remove a filter
 * 1.5 - Optimised filtering, including having multiple filters
 * 1.6 - Page now updates every 2.5s if the user is capturing live traffic
 */
public class HomeScreenController implements Initializable {

    // Referencing elements defined in HomeScreen.fxml
    @FXML private VBox homeScreen;
    @FXML private TableView<Packet> mainTable;
    @FXML private TableColumn<Packet, String> mainTableNo;
    @FXML private TableColumn<Packet, String> mainTableTime;
    @FXML private TableColumn<Packet, String> mainTableSource;
    @FXML private TableColumn<Packet, String> mainTableProtocol;
    @FXML private TableColumn<Packet, Integer> mainTableSize;
    @FXML private TableColumn<Packet, String> mainTableDestination;
    @FXML private TableColumn<Packet, Boolean> mainTableFlagged;
    @FXML private TableColumn<Packet, String> mainTableNotes;

    @FXML private Label tableLabel;
    @FXML private Label ipLabel;
    @FXML private TreeView<Label> packetFocus;
    @FXML private TreeView<Label> packetFocusHexDump;

    @FXML private TableView<Filter> filterTableView;
    @FXML private TableColumn<Filter, String> filterTypeColumn;
    @FXML private TableColumn<Filter, String> filterFromColumn;
    @FXML private TableColumn<Filter, String> filterToColumn;
    @FXML private TableColumn<Filter, String> filterNegatedColumn;

    @FXML private Button addFilterButton;
    @FXML private Button removeFilterButton;

    // List which contains current active filters
    private final ObservableList<Filter> filters = FXCollections.observableArrayList();

    // Animation function that runs every 2.5seconds, used for when the user is in live mode
    private final Timeline liveUpdater = new Timeline(new KeyFrame(Duration.millis(2500), e -> {

        // If user is currently on flagged screen
        if(Main.centerNodeContentPointer == homeScreen) {

            // If there is at least one filter applied
            if (filters.size() > 0) {
                applyFilters();

            }
            // Else, show every packet (non-filtered) in the main table
            else {
                mainTable.setItems(FXCollections.observableArrayList(Main.db.getPackets()));

            }


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
     * This method is run once all of the FXML elements have loaded. It is used to populate all of the tables.
     *
     * @param url JavaFX defined parameter
     * @param resourceBundle JavaFX defined parameter
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Set 'ipLabel' to show the user's current public IP address
        ipLabel.setText("IP: " + Main.ip);

        populateMainTable();

        initialiseFilterTable();

        // Call 'addFilter' function if user clicks on the add filter button
        addFilterButton.setOnAction(e -> addFilter());

        // Call 'removeFilter' function if the user has selected a filter and then clicked the remove filter button
        removeFilterButton.setOnAction(e -> removeFilter(filterTableView.getSelectionModel().getSelectedItem()));

    }

    /**
     * This method initialises the main table and sets the columns of it so that a Packet object can be
     * inserted into the table. Once it has done this it then populates the table with all of the packets from the
     * database.
     */
    private void populateMainTable(){

        // Placeholder text shows when there is no data in the table
        mainTable.setPlaceholder(new Label("No traffic yet.."));

        // Sets 'mainTableNo' column to show the order number of each Packet
        mainTableNo.setCellValueFactory(new PropertyValueFactory<>("orderNo"));

        // Sets 'mainTableTime' column to show the timestamp of each packet
        mainTableTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Sets 'mainTableSource' column to show either the source MAC address or source IP address and port of the
        //  packet depending on the type of packet
        mainTableSource.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getSourceIP()+":"+
                        ((NetworkPacket) packet).getSourcePort());
            }

            return new SimpleStringProperty(packet.getSourceMAC());

        });

        // Sets 'mainTableProtocol' column to show either the EtherType or protocol of the packet depending on the
        //  type of packet
        mainTableProtocol.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getProtocol());
            }

            return new SimpleStringProperty(packet.getEtherType());

        });

        // Sets 'mainTableSize' column to show the packet size in bytes of each packet
        mainTableSize.setCellValueFactory(new PropertyValueFactory<>("totalLen"));

        // Sets 'mainTableDestination' column to show either the destination MAC address or destination IP address and
        //  port of the packet depending on the type of packet
        mainTableDestination.setCellValueFactory(p -> {
            Packet packet = p.getValue();

            if(packet instanceof NetworkPacket){
                return new SimpleStringProperty(((NetworkPacket) packet).getDestinationIP()+":"+
                        ((NetworkPacket) packet).getDestinationPortString());
            }

            return new SimpleStringProperty(packet.getDestinationMAC());

        });

        // Sets 'mainTableFlagged' column to show whether the Packet has been flagged as malicious traffic or not
        mainTableFlagged.setCellValueFactory(new PropertyValueFactory<>("flagged"));

        // Sets 'mainTableNotes' column to show the notes of each packet
        mainTableNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Sets 'mainTable' table items to all of the packets in Main's packet database
        mainTable.setItems(FXCollections.observableArrayList(Main.db.getPackets()));

        // A change listener which will run 'setPacketFocus' function whenever the user selects a different packet in
        //  the main table
        mainTable.getFocusModel().focusedCellProperty().addListener((ob, oldSelect, newSelect) -> {
            if(mainTable.getSelectionModel().getSelectedItem() != null)
                    setPacketFocus(mainTable.getSelectionModel().getSelectedItem());
        });

        // If user is capturing live traffic, rather than just reading a file
        if(Main.isLive) {
            tableLabel.setText("Reading live data");

            // Run update function forever until it gets cancelled
            liveUpdater.setCycleCount(Animation.INDEFINITE);
            liveUpdater.play();

        } else
            tableLabel.setText("Reading data from: " + Main.filePath);

    }

    /**
     * This method is called whenever a change in Packet selection occurs on the main table. It will get all the
     * information about this packet and display it in a tree-like view just below the table. It will also show the
     * entire hex dump of a packet on the right hand side of the section.
     *
     * @param packet The packet that is currently focused on
     */
    private void setPacketFocus(Packet packet){

        // Adds the main branch called frame
        Label rootLabel = new Label("Frame");
        rootLabel.setStyle("-fx-font-weight: bold;");
        TreeItem<Label> root = new TreeItem<>(rootLabel);

        // Adds a sub branch called ethernet
        Label ethernetLabel = new Label("Ethernet");
        ethernetLabel.setStyle("-fx-font-weight: bold;");
        TreeItem<Label> ethernet = new TreeItem<>(ethernetLabel);

        // Creates a tree item containing the packet's source MAC
        Label srcMACLabel = new Label("source MAC addr:      "+packet.getSourceMAC());
        TreeItem<Label> srcMAC = new TreeItem<>(srcMACLabel);

        // Creates a tree item containing the packet's destination MAC
        Label dstMACLabel = new Label("destination MAC addr: "+packet.getDestinationMAC());
        TreeItem<Label> dstMAC = new TreeItem<>(dstMACLabel);

        // Creates a tree item containing the packet's ether type, with a prefix
        Label eTypeLabel = new Label("ether type: "+packet.getEtherType());
        TreeItem<Label> eType = new TreeItem<>(eTypeLabel);

        // Creates a tree item containing the packet's ether type
        Label eTypeNoPrefixLabel = new Label(packet.getEtherType());
        eTypeNoPrefixLabel.setStyle("-fx-font-weight: bold;");
        TreeItem<Label> eTypeNoPrefix = new TreeItem<>(eTypeNoPrefixLabel);

        // Adds source MAC, destination MAC and ether type tree items to the ethernet sub branch
        ethernet.getChildren().addAll(srcMAC, dstMAC, eType);
        ethernet.setExpanded(true);

        // Adds the ethernet sub branch and ether type tree items to the root
        root.getChildren().addAll(ethernet, eTypeNoPrefix);
        root.setExpanded(true);

        // If the packet includes a network layer
        if(packet instanceof NetworkPacket){

            // Creates a tree item containing the packet's IPv4 layer length
            Label ipLenLabel = new Label("IPv4 length: "+((NetworkPacket) packet).getNetworkLen());
            TreeItem<Label> ipLen = new TreeItem<>(ipLenLabel);

            // Creates a tree item containing the packet's source IP
            Label srcIPLabel = new Label("source IP addr:      "+((NetworkPacket) packet).getSourceIP());
            TreeItem<Label> srcIP = new TreeItem<>(srcIPLabel);

            // Creates a tree item containing the packet's destination IP
            Label dstIPLabel = new Label("destination IP addr: "+((NetworkPacket) packet).getDestinationIP());
            TreeItem<Label> dstIP = new TreeItem<>(dstIPLabel);

            // Creates a tree item containing the packet's protocol
            Label protoLabel = new Label("protocol: "+((NetworkPacket) packet).getProtocol());
            TreeItem<Label> proto = new TreeItem<>(protoLabel);

            // Adds source IP, destination IP, IPv4 length, and protocol tree items to the EtherType sub branch
            eTypeNoPrefix.getChildren().addAll(ipLen, srcIP, dstIP, proto);
            eTypeNoPrefix.setExpanded(true);

            // Adds a sub branch called the packet's protocol
            Label transLayerLabel = new Label(((NetworkPacket) packet).getProtocol());
            transLayerLabel.setStyle("-fx-font-weight: bold;");
            TreeItem<Label> transLayer = new TreeItem<>(transLayerLabel);

            // If the packet's protocol is not ICMPv6
            if(!((NetworkPacket) packet).getProtocol().equals("ICMPv6")) {

                // Creates a tree item containing the packet's source port
                Label srcPortLabel = new Label("source port: " + ((NetworkPacket) packet).getSourcePort());
                TreeItem<Label> srcPort = new TreeItem<>(srcPortLabel);

                // Creates a tree item containing the packet's destination port
                Label dstPortLabel = new Label("destination port: " + ((NetworkPacket) packet).getDestinationPortString());
                TreeItem<Label> dstPort = new TreeItem<>(dstPortLabel);

                // Adds source port and destination port tree items to the protocol sub branch
                transLayer.getChildren().addAll(srcPort, dstPort);
                transLayer.setExpanded(true);
            }

            // If network layer contains a TCP transport layer
            TreeItem<Label> p = null;
            if(packet instanceof TCPPacket){

                // Creates a tree item containing the packet's protocol layer length
                Label lenLabel = new Label("protocol length: "+((TCPPacket) packet).getLen());
                TreeItem<Label> len = new TreeItem<>(lenLabel);

                // Creates a tree item containing the packet's TCP flags
                ArrayList<String> rawFlags = ((TCPPacket) packet).getFlags();
                Label flagsLabel = new Label("flags: "+rawFlags.toString());
                TreeItem<Label> flags = new TreeItem<>(flagsLabel);

                // Adds protocol layer length and TCP flag tree items to the transport layer sub branch
                transLayer.getChildren().addAll(len, flags);

                // If the TCP packet has a payload, add the payload as a tree item to the transport layer sub branch
                if(((TCPPacket) packet).hasPayload()){
                    Label pLabel = new Label("Payload");
                    p = new TreeItem<>(pLabel);
                    p.getChildren().add(new TreeItem<>(new Label(((TCPPacket) packet).getPayload())));
                }

            }
            // Else if the network layer contains a UDP transport layer
            else if(packet instanceof UDPPacket){

                // Creates a tree item containing the packet's UDP protocol length
                Label lenLabel = new Label("protocol length: "+((UDPPacket) packet).getLen());
                TreeItem<Label> len = new TreeItem<>(lenLabel);

                // Adds UDP protocol length tree item to transport layer sub branch
                transLayer.getChildren().add(len);
            }

            // 'p' has tree items, add them to root
            if(p != null) root.getChildren().addAll(transLayer, p);
            // Else, just add the transport layer sub branch to root
            else root.getChildren().add(transLayer);

        }

        // Sets 'packetFocus' tree content to 'root'
        packetFocus.setRoot(root);

        // Creates a tree item containing the packet's hex dump
        Label dumpRootLabel = new Label("Hex Dump");
        dumpRootLabel.setStyle("-fx-font-weight: bold;");
        TreeItem<Label> dumpRoot = new TreeItem<>(dumpRootLabel);
        dumpRoot.setExpanded(true);
        dumpRoot.getChildren().add(new TreeItem<>(new Label(packet.getHex())));

        // Add the hex dump content to 'packetFocusHexDump'
        packetFocusHexDump.setRoot(dumpRoot);

    }

    /**
     * This method is called when the user clicks on the add filter button. It creates and displays a new separate
     * window, or 'dialog box' so that the user can enter the details of whatever filter they want to apply
     */
    private void addFilter(){

        // Creates a dialog box and sets it title and header
        Dialog<Filter> dialog = new Dialog<>();
        dialog.setTitle("Add filter");
        dialog.setHeaderText("Apply filters to "+Main.filePath);

        // Setting filter form input vectors
        Label filterLabel = new Label("Choose filter type: ");
        Label lowerBoundLabel = new Label("Lower bound (>=): ");
        Label upperBoundLabel = new Label("Upper bound (<=): ");
        Spinner<Integer> lowerBound = new Spinner<>();
        lowerBound.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,65535));
        lowerBound.setEditable(true);
        lowerBound.setMaxWidth(100);
        Spinner<Integer> upperBound = new Spinner<>();
        upperBound.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,65535));
        upperBound.setEditable(true);
        upperBound.setMaxWidth(100);

        RadioButton srcAddress = new RadioButton();
        RadioButton dstAddress = new RadioButton();
        ToggleGroup radioGroup = new ToggleGroup();
        srcAddress.setToggleGroup(radioGroup);
        dstAddress.setToggleGroup(radioGroup);
        radioGroup.getToggles().get(0).setSelected(true);
        HBox srcRadio = new HBox(new Label("Source address: "), srcAddress);
        HBox dstRadio = new HBox(new Label("Destination address: "), dstAddress);

        Label ipLabel = new Label("IP address: ");
        TextField ipTextField = new TextField();

        Label macLabel = new Label("MAC address: ");
        TextField macTextField = new TextField();

        Label protocolLabel = new Label("Protocol: ");
        ComboBox<String> availableProtocols = new ComboBox<>();
        availableProtocols.setItems(FXCollections.observableList(Main.db.getUsedProtocols()));
        availableProtocols.setVisibleRowCount(10);


        Label negatedLabel = new Label("Negated? ");
        CheckBox negatedCheckBox = new CheckBox();

        GridPane dialogGrid = new GridPane();
        dialogGrid.setHgap(10);
        dialogGrid.setVgap(10);
        dialogGrid.add(filterLabel, 1,1);

        ChoiceBox<Filter.FilterType> filterChoices = new ChoiceBox<>();
        filterChoices.setItems(FXCollections.observableList(Arrays.asList(Filter.FilterType.values())));
        filterChoices.getSelectionModel().clearSelection();
        dialogGrid.add(filterChoices,2,1);

        // Must be a final variable when referenced in lambda expression, array used as a workaround
        final Filter.FilterType[] selected = {null};

        /*
         * This listener lambda will wait for a change in selection of filter type on the add filter popout window. This
         * is because the window will need to change which contents it shows based on what filter the user chose. For
         * example, a protocol drop-down list will need to show if the user selects the protocol filter option, and an
         * IP address text input field will need to show it the user selects the IP address filter option.
         */
        filterChoices.getSelectionModel().selectedItemProperty().addListener((obs, old, new_) -> {

            // Remove all previous window content and then add the filter choice box back in again, so the user can
            //  select another filter type if they want to
            dialogGrid.getChildren().clear();
            dialogGrid.add(filterLabel, 1,1);
            dialogGrid.add(filterChoices,2,1);

            // This switch statement decides which content to show based on the currently selected filter option
            switch(filterChoices.getSelectionModel().getSelectedItem()){

                // If user has selected a MAC filter, add MAC filter input fields
                case MAC:
                    dialogGrid.add(macLabel, 1,2);
                    dialogGrid.add(macTextField, 2,2);

                    dialogGrid.add(srcRadio,1,3);
                    dialogGrid.add(dstRadio,2,3);

                    dialogGrid.add(negatedLabel,1,4);
                    dialogGrid.add(negatedCheckBox,2,4);

                    selected[0] = Filter.FilterType.MAC;

                    break;

                // If user has selected an IP filter, add IP filter input fields
                case IP:
                    dialogGrid.add(ipLabel, 1,2);
                    dialogGrid.add(ipTextField,2,2);

                    dialogGrid.add(srcRadio,1,3);
                    dialogGrid.add(dstRadio,2,3);

                    dialogGrid.add(negatedLabel,1,4);
                    dialogGrid.add(negatedCheckBox,2,4);

                    selected[0] = Filter.FilterType.IP;

                    break;

                // If user has selected a protocol filter, add protocol filter input fields
                case Protocol:
                    dialogGrid.add(protocolLabel, 1,2);
                    dialogGrid.add(availableProtocols,2,2);

                    dialogGrid.add(negatedLabel,1,3);
                    dialogGrid.add(negatedCheckBox,2,3);

                    selected[0] = Filter.FilterType.Protocol;

                    break;

                // If user has selected a port filter, add port filter input fields
                case Port:
                    dialogGrid.add(lowerBoundLabel, 1,2);
                    dialogGrid.add(lowerBound,2,2);

                    dialogGrid.add(upperBoundLabel,1,3);
                    dialogGrid.add(upperBound,2,3);

                    dialogGrid.add(srcRadio,1,4);
                    dialogGrid.add(dstRadio,2,4);

                    selected[0] = Filter.FilterType.Port;

                    break;

                // If user has selected a packet size filter, add size filter input fields
                case Size:
                    dialogGrid.add(lowerBoundLabel, 1,2);
                    dialogGrid.add(lowerBound,2,2);

                    dialogGrid.add(upperBoundLabel,1,3);
                    dialogGrid.add(upperBound,2,3);

                    selected[0] = Filter.FilterType.Size;

                    break;

                // If user has selected a flagged filter, add flagged filter input fields
                case Flagged:
                    dialogGrid.add(new Label("Not flagged?: "), 1,2);
                    dialogGrid.add(negatedCheckBox,2,2);

                    selected[0] = Filter.FilterType.Flagged;

                    break;

            }

        });

        // Format size and alignment of popout window
        dialogGrid.setAlignment(Pos.TOP_CENTER);
        dialog.getDialogPane().setContent(dialogGrid);
        dialog.getDialogPane().setPrefSize(300, 300);

        ButtonType buttonTypeOk = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);

        // Show screen and wait for result
        Optional<Filter> result = dialog.showAndWait();

        // If user has closed/continued from window and has selected a type of filter
        if(result.isPresent() && selected[0] != null){

            // If user selected a MAC filter
            if(selected[0] == Filter.FilterType.MAC){

                // Get MAC address that user entered
                String input = macTextField.getText();

                // If user didn't leave blank input
                if(!input.equals("")){

                    MACFilter macFilter;

                    // If user wanted to filter anything that wasn't the address they entered
                    if(negatedCheckBox.isSelected()){

                        macFilter = new MACFilter(
                                input,
                                radioGroup.getToggles().get(0).isSelected(),
                                true
                        );

                    }
                    // Else, user wanted to filter just the address they specified
                    else {

                        macFilter = new MACFilter(
                                input,
                                radioGroup.getToggles().get(0).isSelected(),
                                false
                        );

                    }

                    // Add MAC filter to list of active filters
                    filters.add(macFilter);

                    applyFilters();

                }
                // Else, show popout window that notifies they entered a bad input
                else {

                    showInputError();

                }

            }
            // Else if user selected an IP filter
            else if(selected[0] == Filter.FilterType.IP) {

                // Get IP address that user entered
                String input = ipTextField.getText();

                // If user didn't leave blank input
                if(!input.equals("")){

                    IPFilter ipFilter;

                    // If user wanted to filter anything that wasn't the address they entered
                    if(negatedCheckBox.isSelected()){

                        ipFilter = new IPFilter(
                                input,
                                radioGroup.getToggles().get(0).isSelected(),
                                true
                        );

                    }
                    // Else, user wanted to filter just the address they specified
                    else {

                        ipFilter = new IPFilter(
                                input,
                                radioGroup.getToggles().get(0).isSelected(),
                                false
                        );

                    }

                    // Add IP filter to list of active filters
                    filters.add(ipFilter);

                    applyFilters();

                }
                // Else, show popout window that notifies they entered a bad input
                else {

                    showInputError();

                }

            }
            // Else if user selected a protocol filter
            else if(selected[0] == Filter.FilterType.Protocol) {

                // If user didn't leave blank input
                if(!availableProtocols.getSelectionModel().isEmpty()){

                    // Get protocol that user selected
                    String protocol = availableProtocols.getValue();

                    // If user wanted to filter any protocol that wasn't the one they selected
                    if(negatedCheckBox.isSelected()){

                        filters.add(new ProtocolFilter(true, protocol));

                    }
                    // Else, user wanted to filter just the protocol they specified
                    else {

                        filters.add(new ProtocolFilter(false, protocol));

                    }

                    applyFilters();

                }
                // Else, show popout window that notifies they entered a bad input
                else {

                    showInputError();

                }

            }
            // Else if user selected a port filter
            else if(selected[0] == Filter.FilterType.Port) {

                // Get lower and upper bounds of ports the user wants to filter
                int lower = lowerBound.getValue();
                int upper = upperBound.getValue();

                // If lower bound is lower than upper bound
                if(lower <= upper){

                    // Add port filter to list of active filters
                    filters.add(new PortFilter(
                            lower,
                            upper,
                            radioGroup.getToggles().get(0).isSelected()
                    ));

                    applyFilters();

                }
                // Else, show popout window that notifies they entered a bad input
                else {

                    showInputError();

                }

            }
            // Else if user selected a size filter
            else if(selected[0] == Filter.FilterType.Size) {

                // Get lower and upper bounds of the packet size that the user wants to filter
                int lower = lowerBound.getValue();
                int upper = upperBound.getValue();

                // If lower bound is lower than upper bound
                if(lower <= upper){

                    // Add size filter to list of active filters
                    filters.add(new SizeFilter(lower,upper));

                    applyFilters();

                }
                // Else, show popout window that notifies they entered a bad input
                else {

                    showInputError();

                }

            }
            // Else if user selected a flagged filter
            else if(selected[0] == Filter.FilterType.Flagged) {

                // If user wants to see all traffic that is NOT flagged
                if(negatedCheckBox.isSelected()){

                    filters.add(new FlaggedFilter(true));

                }
                // Else user wants to see all flagged traffic
                else {

                    filters.add(new FlaggedFilter(false));

                }

                applyFilters();

            }

        }

    }

    /**
     * This method shows an alert to the user that an error occurred when they tried to apply a new filter. The alert
     * will show whenever they enter something that is not allowed, or is a 'bad input'. This is to prevent the user
     * from making a mistake or adding a filter that doesn't work.
     */
    private void showInputError(){

        Alert badInput = new Alert(Alert.AlertType.ERROR);

        badInput.setTitle("");

        badInput.setHeaderText("Invalid input!");

        badInput.setContentText("Please check that you filled out all of the inputs correctly.");

        badInput.show();

    }

    /**
     * This method is called when the user clicks on the remove filter button. For it to work, the user must select a
     * filter in the filter table before clicking the button otherwise it won't do anything.
     * @param filter
     */
    private void removeFilter(Filter filter){

        // If a filter has been selected
        if(filter != null) {

            // Remove selected filter from active filter list
            filters.remove(filter);

            // Reapply active filters
            applyFilters();

        }

    }

    /**
     * This filter gets all the filters to gather the packets that applies to them, removes packets that don't apply to
     * every filter, and then shows the results in the main table.
     */
    private void applyFilters(){

        // If user has applied at least one filter
        if(!(filters.size() == 0)) {

            ArrayList<Packet> packets = new ArrayList<>();

            // For each active filter
            for(Filter filter : filters){

                // For each packet that applies to the filter
                for(Packet packet : filter.getFilteredPackets()){

                    // If packet list doesn't already contain the packet, add it
                    if(!packets.contains(packet)){

                        packets.add(packet);

                    }

                }

            }

            // If there is more than one filters, packets must be removed if they don't apply to all active filters
            if(filters.size() > 1) {

                // For each active filter
                for (Filter filter : filters) {

                    // Get packets that apply to filter
                    ArrayList<Packet> filteredPackets = filter.getFilteredPackets();

                    // Remove packet from main list if it doesn't exist in current filters packet list
                    packets.removeIf(packet -> !filteredPackets.contains(packet));

                }

            }

            // Populate main table with only packets that apply to all filters
            populateFilteredTable(packets);

        }
        // Else, no filters applied, show all packets
        else {

            mainTable.setItems(FXCollections.observableList(Main.db.getPackets()));

        }

        // Show all active filters in the filter table
        filterTableView.setItems(FXCollections.observableArrayList(filters));

    }

    /**
     * This method initialises the filter table and the filter table columns.
     */
    private void initialiseFilterTable(){

        filterTableView.setPlaceholder(new Label("No filters applied."));

        // Sets the appropriate label for the filter type column based on filter type
        filterTypeColumn.setCellValueFactory(p -> {

            Filter filter = p.getValue();

            // If filter is a MAC filter
            if(filter instanceof MACFilter){

                // Sets filter type label to source or destination MAC address
                if(((MACFilter) filter).getIsSourceAddress())
                    return new SimpleStringProperty("Source MAC Address");

                return new SimpleStringProperty("Destination MAC Address");

            }
            // Else if filter is an address filter
            else if(filter instanceof AddressFilter) {

                // Sets filter type label to source or destination IP address
                if(((IPFilter) filter).getIsSourceAddress())
                    return new SimpleStringProperty("Source IP Address");

                return new SimpleStringProperty("Destination IP Address");

            }
            // Else if filter is a protocol filter
            else if(filter instanceof ProtocolFilter) {

                // Sets filter type label to 'Protocol'
                return new SimpleStringProperty("Protocol");

            }
            // Else if filter is a port filter
            else if(filter instanceof PortFilter) {

                // Sets filter type label to source or destination port
                if(((PortFilter) filter).isSourcePort())
                    return new SimpleStringProperty("Source Port");

                return new SimpleStringProperty("Destination Port");

            }
            // Else if filter is a size filter
            else if(filter instanceof SizeFilter) {

                // Sets filter type label to 'Size'
                return new SimpleStringProperty("Size");

            }

            // Else filter is a flagged filter, set filter type label to 'Flagged'
            return new SimpleStringProperty("Flagged");

        });

        // Sets the appropriate label for the filter 'from' column based on filter type
        filterFromColumn.setCellValueFactory(p -> {

            Filter filter = p.getValue();

            // If filter is an address filter
            if(filter instanceof AddressFilter) {

                // Set 'from' value to filter address
                return new SimpleStringProperty(((AddressFilter) filter).getAddr());

            }
            // Else if filter is a protocol filter
            else if(filter instanceof ProtocolFilter) {

                // Set 'from' value to protocol
                return new SimpleStringProperty(((ProtocolFilter) filter).getProtocol());

            }
            // Else if filter is a range filter
            else if(filter instanceof RangeFilter) {

                // Set 'from' value to lower bound
                return new SimpleStringProperty(String.valueOf(((RangeFilter) filter).getLowerBound()));

            }

            // For all other filters set value to '-'
            return new SimpleStringProperty("-");

        });

        // Sets the appropriate label for the filter 'to' column based on filter type
        filterToColumn.setCellValueFactory(p -> {

            Filter filter = p.getValue();

            // If filter is a range filter
            if(filter instanceof RangeFilter){

                // Set 'to' value to upper bound
                return new SimpleStringProperty(String.valueOf(((RangeFilter) filter).getUpperBound()));

            }

            // For all other filters set 'to' value to '-'
            return new SimpleStringProperty("-");

        });

        // Sets the appropriate label for the filter 'negated?' column based on filter type
        filterNegatedColumn.setCellValueFactory(p -> {

            Filter filter = p.getValue();

            // If filter is a specified filter
            if(filter instanceof SpecifiedFilter) {

                // Set value to whether it is negated or not
                return new SimpleStringProperty(String.valueOf(((SpecifiedFilter) filter).isNegated()));

            }
            // If filter is an address filter
            else if(filter instanceof AddressFilter) {

                // Set value to whether it is negated or not
                return new SimpleStringProperty(String.valueOf(((AddressFilter) filter).getIsNegated()));

            }

            // For all other filters set value to 'n/a'
            return new SimpleStringProperty("n/a");

        });

    }

    /**
     * This method is called when the applyFilters() method has finished calculating which packets to show, and this
     * method will show the filtered packets
     * @param packets complete list of packets which comply to all active filters
     */
    private void populateFilteredTable(ArrayList<Packet> packets){

        // If user is reading data from a .pcap file
        if(!Main.isLive)
            tableLabel.setText("Reading data from: "+Main.filePath+" (filters applied)");

        // Else, user is capturing live traffic data
        else
            tableLabel.setText("Capturing live data (filters applied)");

        // If filters show no results, set placeholder of main table to say no results found
        mainTable.setPlaceholder(new Label("Search criteria found no results."));

        // Add all of the filtered packets to the main table
        mainTable.setItems(FXCollections.observableList(packets));

    }

}
