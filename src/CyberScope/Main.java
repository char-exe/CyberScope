package CyberScope;

import CyberScope.Filter.Filter;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is the main class, which runs when the program starts. It contains all of the relative information about
 * the user, which can be accessed by other controller classes.
 *
 * It is mainly responsible for capturing, dissecting and creating objects out of packets.
 *
 * @author Charlie Jones - 100234961
 *
 */
public class Main extends Application {

    // Runtime object used to execute windows commands
    protected static Runtime rt = Runtime.getRuntime();

    // PacketDatabase reference
    protected static PacketDatabase db;

    // File path to .pcap file being read (if applies)
    protected static String filePath;

    // Stores the user's public IP address
    protected static String ip;

    // DLL byte offset, used for reading hex data of packet
    protected static int dllByteOffset = 0;

    // Indicates wheteher the user is capturing live traffic or not
    protected static boolean isLive = true;

    // Pointer the the current screen that the user is on
    protected static Node centerNodeContentPointer;

    // List containing available interface that the user can capture live traffic on
    protected static final ArrayList<String> availableInterfaces = new ArrayList<>();
    private static int chosenInterfaceIndex = 0;

    // Custom thread which captures live traffic
    protected static PacketCaptureThread liveThread = null;

    /**
     * This is an overridden method from JavaFX application, it is called when the application starts
     *
     * @param primaryStage JavaFX defined variable, references the stage or 'window' that the application displays
     *                     inside
     * @throws IOException in the unlikely event that the system cannot find the WinDump executable file
     */
    @Override
    public void start(Stage primaryStage) throws IOException {

        // Stores all available interfaces
        availableInterfaces.addAll(getAvailableInterfaces());

        // Show splash screen until the user has filled the correct information
        while(filePath == null)
            showSplashScreenAndWait();

        // If the user want to capture live traffic
        if(isLive){

            // Create empty packet database
            db = new PacketDatabase();

            // Store user's public IP address
            ip = getPublicIP();

            // Create & start live capture thread
            liveThread = new PacketCaptureThread();
            liveThread.start();

        }
        // Else, user wants to read packets from a .pcap file
        else {

            // Command to be executed which gets the link layer type.
            String[] commands = {
                    "cmd.exe", "/c", ("cd src/CyberScope/PacketCapture && WinDump.exe -c1 -nr " + filePath)
            };
            Process proc = rt.exec(commands);

            // Retrieves link layer type
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String firstOutputLine = stdInput.readLine();
            Pattern dllPattern = Pattern.compile("(link-type )([A-z0-9_]+)");
            Matcher dllMatcher = dllPattern.matcher(firstOutputLine);

            String dllType = "";
            if(dllMatcher.find())
                dllType = dllMatcher.group(2);

            // If link layer type is 'LINUX_SLL' then byte offset must be set to 2, this is because the link layer will
            //  contain 2 extra bytes than normal which offset all the bytes after it and needs to be ignored.
            if (dllType.equals("LINUX_SLL")){
                dllByteOffset = 2;
            }

            // Gets all of the packet's hex dumps and stores it in a string array list
            ArrayList<String> hexDumps = readPackets(filePath);

            // Converts the hex dumps into packet objects and stores them into an array list
            ArrayList<Packet> packets = readHex(hexDumps);

            // Instantiate packet database with a the array list of packets
            db = new PacketDatabase(packets);

            // Store user's current public IP address
            ip = getPublicIP();

            // Uncomment to print out every packet
//        for(Packet packet : packets){
//            System.out.println(packet);
//        }

        }

        // Loads Main.fxml content and shows the scene to the user
        BorderPane root = FXMLLoader.load(getClass().getResource("FXML/Main.fxml"));
        primaryStage.setTitle("CyberScope");
        primaryStage.setScene(new Scene(root, 1450, 768));
        primaryStage.setMinHeight(500);
        primaryStage.setMinWidth(1100);
        primaryStage.show();

    }

    /**
     * This method is called when the program is first run, it's purpose is to find out whether the user wants to
     * capture live traffic, or read a .pcap file.
     */
    public void showSplashScreenAndWait(){
        final File[] selectedFile = new File[1];

        Dialog<Filter> splashScreen = new Dialog<>();
        splashScreen.setTitle("CyberScope - UEA's finest network visualisation tool!");
        splashScreen.setHeaderText("Welcome.\n\nWould you like to capture live traffic, or read from an existing .pcap file?");

        // Exit application if user selects the close button
        splashScreen.getDialogPane().getScene().getWindow().setOnCloseRequest(e -> System.exit(0));

        // Dialog box content
        RadioButton liveButton = new RadioButton();
        RadioButton offlineButton = new RadioButton();
        ToggleGroup radioGroup = new ToggleGroup();
        liveButton.setToggleGroup(radioGroup);
        offlineButton.setToggleGroup(radioGroup);
        radioGroup.getToggles().get(0).setSelected(true);
        HBox liveRadio = new HBox(new Label("Capture live: "), liveButton);
        HBox offlineRadio = new HBox(new Label("Read file: "), offlineButton);

        HBox radioButtons = new HBox(liveRadio, offlineRadio);
        radioButtons.setAlignment(Pos.CENTER);
        radioButtons.setSpacing(50);

        VBox dialogBox = new VBox();
        dialogBox.setSpacing(25);
        dialogBox.getChildren().add(radioButtons);

        ComboBox<String> availableInterfacesBox = new ComboBox<>();
        availableInterfacesBox.setItems(FXCollections.observableList(availableInterfaces));
        availableInterfacesBox.setVisibleRowCount(10);
        availableInterfacesBox.getSelectionModel().select(0);
        Label selectInterfaceLabel = new Label("Select interface to listen on:");

        // FileChooser to pop up which only allows user to pick .pcap files
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("pcap file", "*.pcap"));
        fileChooser.setTitle("Select .pcap file to read");
        Label selectFileLabel = new Label("Select .pcap file to read:");
        Label filePlaceholderLabel = new Label("File: ");
        Button selectFileButton = new Button("...");

        // Opens file chooser for the user to select a file, then sets 'filePlaceholderLabel' as the file path of the
        //  chosen file
        selectFileButton.setOnAction(e -> {
                    selectedFile[0] = fileChooser.showOpenDialog(splashScreen.getDialogPane().getScene().getWindow());
                    filePlaceholderLabel.setText(selectedFile[0].toString());
        });

        HBox selectFileHbox = new HBox(filePlaceholderLabel, selectFileButton);
        selectFileHbox.setAlignment(Pos.CENTER);
        selectFileHbox.setSpacing(10);

        dialogBox.getChildren().addAll(selectInterfaceLabel, availableInterfacesBox);
        dialogBox.setAlignment(Pos.CENTER);
        splashScreen.getDialogPane().setContent(dialogBox);
        splashScreen.getDialogPane().setPrefSize(800, 250);

        // Listener to listen for the change in the radio button selection from live to offline
        radioGroup.selectedToggleProperty().addListener(e -> {

            // If the 'read .pcap file' button is chosen
            if(radioGroup.getSelectedToggle() == offlineButton){

                // Show options for choosing a file and remove live capture options
                dialogBox.getChildren().removeAll(selectInterfaceLabel, availableInterfacesBox);
                dialogBox.getChildren().addAll(selectFileLabel, selectFileHbox);
            }
            // Else, 'live capture' button was chosen
            else {

                // Show options for live capture and remove file choosing options
                dialogBox.getChildren().removeAll(selectFileLabel, selectFileHbox);
                dialogBox.getChildren().addAll(selectInterfaceLabel, availableInterfacesBox);
            }

        });

        ButtonType buttonTypeOk = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        splashScreen.getDialogPane().getButtonTypes().add(buttonTypeOk);
        Optional<Filter> result = splashScreen.showAndWait();

        // If user has pressed continue
        if(result.isPresent()){

            // Set 'isLive' to whether the the live option was selected or not
            isLive = radioGroup.getToggles().get(0).isSelected();

            // If user selected the live option
            if(isLive) {

                // Set the chosen interface to the one that the user picked on the splash screen
                chosenInterfaceIndex = Integer.parseInt(availableInterfacesBox.getValue().split("\\.")[0]);

                filePath = "";

            }
            // Else if user selected read .pcap file option
            else if (selectedFile[0] != null) {

                // Set file path to the file path of the selected .pcap file
                filePath = selectedFile[0].getPath();

            }

        }

    }

    /**
     * Method to get all interfaces available on the user's machine
     *
     * @return ArrayList of strings containing all available interfaces
     * @throws IOException in the unlikely event that the system cannot find the WinDump executable file
     */
    public ArrayList<String> getAvailableInterfaces() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        String[] commands = {"cmd.exe", "/c", ("cd src/CyberScope/PacketCapture && WinDump.exe -D")};
        Process proc = rt.exec(commands);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String s;
        while((s = stdInput.readLine()) != null){
            result.add(s);
        }
        return result;
    }

    // Runs program
    public static void main(String[] args) {
        launch(args);
    }

    public static ArrayList<String> readPackets(String filePath) throws IOException {

        // Command to be executed by rt which gets all of the hex dump information of each packet in .pcap file
        String[] commands = {"cmd.exe", "/c", ("cd src/CyberScope/PacketCapture && WinDump.exe -nnxxr "+filePath)};
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        ArrayList<String> hexDump = new ArrayList<>();
        StringBuilder packetHex = new StringBuilder();

        // While there is still output from the read file process
        String s;
        while ((s = stdInput.readLine()) != null) {

            // If line doesn't start with '0x' (i.e. not hex dump data) and string builder doesn't contain anything
            if(!s.startsWith("\t0x") && packetHex.length() == 0) {

                // Add timestamp of packet to string builder
                packetHex.append(s, 0, 12);
                packetHex.append("|");

            }
            // If line doesn't start with '0x' (i.e. not hex dump data)
            else if(!s.startsWith("\t0x")){

                // Add string builder to hex dump list
                hexDump.add(packetHex.toString());

                // Reset string builder
                packetHex = new StringBuilder();

                // Add timestamp of packet to string builder
                packetHex.append(s, 0, 12);
                packetHex.append("|");

            }
            // Else, line contains hex dump data
            else {
                // Remove whitespace and unnecessary info and add it to string builder
                String str = s.stripLeading()
                        .substring(9)
                        .replace(" ", "");
                packetHex.append(str);
            }

        }

        // Return list of hex dump data
        return hexDump;

    }

    /**
     * This is a helper method which uses the 'createPacketObj' method to iterate through every hex dump string to be
     * transformed into an object.
     *
     * @param hexDumps ArrayList of Strings containing unformatted hex dumps of every packet
     * @return ArrayList of Packets
     */
    public ArrayList<Packet> readHex(ArrayList<String> hexDumps) {

        ArrayList<Packet> result = new ArrayList<>();

        for(String str : hexDumps) {

            result.add(
                    createPacketObj(str)
            );

        }

        return result;

    }

    /**
     * This is a helper method which uses the 'createPacketObj' method to transform a hex dump string into an object
     *
     * @param hexDump String containing unformatted hex dump of a packet
     * @return Packet object
     */
    public Packet readHex(String hexDump) {

        return createPacketObj(hexDump);

    }

    /**
     * This method takes hex data and translates it into a Packet object.
     * @param str containing the timestamp and hex dump of packet
     * @return Packet object
     */
    public Packet createPacketObj(String str){

        // Split string into time stamp and hex dump
        String[] packetStr = str.split("\\|");
        String timestamp = packetStr[0];

        // Space each hex value out so it can be split
        String hexDump = packetStr[1].replaceAll("(..)", "$0 ").stripTrailing();

        // Split each hex value into a String array
        String[] hexCode = hexDump.split(" ");

        // Get destination MAC from hex code range 0-6
        String[] destinationMACHex = Arrays.copyOfRange(hexCode, 0 + dllByteOffset, 6 + dllByteOffset);

        // Get source MAC from hex code range 6-12
        String[] sourceMACHex = Arrays.copyOfRange(hexCode, 6 + dllByteOffset, 12 + dllByteOffset);

        // Get ether type from hex code range 12-14
        String[] etherTypeHex = Arrays.copyOfRange(hexCode, 12 + dllByteOffset, 14 + dllByteOffset);
        String etherType = etherTypeHex[0] + etherTypeHex[1];

        // Concatenates MAC addresses together and also adding semi-colons to make it look like a MAC address
        StringBuilder destinationMACBuilder = new StringBuilder();
        for (String hex : destinationMACHex) {
            destinationMACBuilder.append(hex).append(":");
        }
        StringBuilder sourceMACBuilder = new StringBuilder();
        for (String hex : sourceMACHex) {
            sourceMACBuilder.append(hex).append(":");
        }
        // Removes extra semi-colons
        String sourceMAC = sourceMACBuilder.substring(0, sourceMACBuilder.length() - 1);
        String destinationMAC = destinationMACBuilder.substring(0, destinationMACBuilder.length() - 1);

        // Total packet size = hex code length in bytes
        int totalLen = hexCode.length;

        // Instantiates packet object
        Packet packet;

        // Hex code is further dissected, but in different ways as the following hex code of the hex dump represents
        //  different things depending on what the ether type value
        switch (etherType) {

            // EtherType is IPv4
            case "0800":

                // Get source IP from hex code range 26-30
                String[] sourceIPHex = Arrays.copyOfRange(hexCode, 26+ dllByteOffset, 30+ dllByteOffset);

                // Get destination IP from hex code range 30-34
                String[] destinationIPHex = Arrays.copyOfRange(hexCode, 30+ dllByteOffset, 34+ dllByteOffset);

                // Get source port from hex code range 34-36
                String[] sourcePortHex = Arrays.copyOfRange(hexCode, 34+ dllByteOffset, 36+ dllByteOffset);

                // Get destination port from hex code range 36-38
                String[] destinationPortHex = Arrays.copyOfRange(hexCode, 36+ dllByteOffset, 38+ dllByteOffset);

                // Get IP protocol from 23rd hex value
                int ipProtocol = hexToNumberConverter(hexCode[23 + dllByteOffset]);

                // Get IP layer length from hex code range 16-18
                int ipLen = hexToNumberConverter(Arrays.copyOfRange(hexCode, 16+ dllByteOffset, 18+ dllByteOffset));

                // If IP protocol translates to TCP
                if(NetworkPacket.ipProtocolMap.getOrDefault(ipProtocol, "").equals("TCP")){

                    // Get length from hex code range 16-18, but take away 16 from final value to exclude IP header
                    //  length
                    int len = hexToNumberConverter(Arrays.copyOfRange(
                            hexCode, 16+ dllByteOffset, 18+ dllByteOffset
                    )) - 16;

                    // If TCP packet has a payload
                    if(Arrays.copyOfRange(hexCode, 53, hexCode.length-1).length > 0) {

                        // Downcast packet object to a TCPPacket, including it's payload
                        packet = new TCPPacket(
                                timestamp,
                                sourceMAC,
                                destinationMAC,
                                totalLen,
                                "IPv4",
                                formatPayload(hexDump),
                                "TCP",
                                hexToIPConverter(sourceIPHex, true),
                                hexToIPConverter(destinationIPHex, true),
                                hexToNumberConverter(sourcePortHex),
                                hexToNumberConverter(destinationPortHex),
                                ipLen,
                                len,
                                hexCode[47 + dllByteOffset],
                                formatPayload(Arrays.copyOfRange(hexCode, 53, hexCode.length-1))
                        );

                    }
                    // Else, packet doesn't have a payload
                    else {

                        // Downcast packet object to a TCPPacket, without any payload
                        packet = new TCPPacket(
                                timestamp,
                                sourceMAC,
                                destinationMAC,
                                totalLen,
                                "IPv4",
                                formatPayload(hexDump),
                                "TCP",
                                hexToIPConverter(sourceIPHex, true),
                                hexToIPConverter(destinationIPHex, true),
                                hexToNumberConverter(sourcePortHex),
                                hexToNumberConverter(destinationPortHex),
                                ipLen,
                                len,
                                hexCode[47 + dllByteOffset],
                                ""
                        );

                    }

                }
                // Else if IP protocol translates to UDP
                else if(NetworkPacket.ipProtocolMap.getOrDefault(ipProtocol, "").equals("UDP")){

                    // Get UDP length from hex code range 38-40
                    int len = hexToNumberConverter(Arrays.copyOfRange(
                            hexCode, 38+ dllByteOffset, 40+ dllByteOffset)
                    );

                    // Downcast Packet object to a UDPPacket object
                    packet = new UDPPacket(
                            timestamp,
                            sourceMAC,
                            destinationMAC,
                            totalLen,
                            "IPv4",
                            formatPayload(hexDump),
                            "UDP",
                            hexToIPConverter(sourceIPHex, true),
                            hexToIPConverter(destinationIPHex, true),
                            hexToNumberConverter(sourcePortHex),
                            hexToNumberConverter(destinationPortHex),
                            ipLen,
                            len
                    );

                }
                // Else, Packet is a Network layer packet
                else{

                    // Downcast Packet object to a NetworkPacket object
                    packet = new NetworkPacket(
                            timestamp,
                            sourceMAC,
                            destinationMAC,
                            totalLen,
                            "IPv4",
                            formatPayload(hexDump),
                            NetworkPacket.ipProtocolMap.get(ipProtocol),
                            hexToIPConverter(sourceIPHex, true),
                            hexToIPConverter(destinationIPHex, true),
                            hexToNumberConverter(sourcePortHex),
                            hexToNumberConverter(destinationPortHex),
                            ipLen
                    );

                }

                break;

            // EtherType is ARP
            case "0806":

                // Create Packet object with an EtherType of "ARP"
                packet = new Packet(
                        timestamp,
                        sourceMAC,
                        destinationMAC,
                        totalLen,
                        "ARP",
                        formatPayload(hexDump)
                );

                break;

            // EtherType is IPv6
            case "86dd":

                // Get source IP address from hex code range 22-38
                String[] sourceIPv6Hex = Arrays.copyOfRange(hexCode, 22+ dllByteOffset, 38+ dllByteOffset);

                // Get destination IP address from hex code range 38-54
                String[] destinationIPv6Hex = Arrays.copyOfRange(hexCode, 38+ dllByteOffset, 54+ dllByteOffset);

                // Get source port from hex code range 54-56
                String[] sourcePortv6Hex = Arrays.copyOfRange(hexCode, 54+ dllByteOffset, 56+ dllByteOffset);

                // Get destination port from hex code range 56-58
                String[] destinationPortv6Hex = Arrays.copyOfRange(
                        hexCode, 56+ dllByteOffset, 58+ dllByteOffset
                );

                // Get IPv6 protocol from 20th hex value
                int ipv6Protocol = hexToNumberConverter(hexCode[20+ dllByteOffset]);

                // Get IPv6 layer length from hex code range 18-20
                int ipv6Len = hexToNumberConverter(
                        Arrays.copyOfRange(hexCode, 18+ dllByteOffset, 20+ dllByteOffset)
                );

                // Get what the IPv6 protocol translates to
                switch (NetworkPacket.ipProtocolMap.getOrDefault(ipv6Protocol, "")) {

                    // IPv6 protocol is ICMPv6
                    case "ICMPv6":

                        // Downcast Packet object to a NetworkPacket object with an ICMPv6 protocol
                        packet = new NetworkPacket(
                                timestamp,
                                sourceMAC,
                                destinationMAC,
                                totalLen,
                                "IPv6",
                                formatPayload(hexDump),
                                "ICMPv6",
                                hexToIPConverter(sourceIPv6Hex, false),
                                hexToIPConverter(destinationIPv6Hex, false),
                                -1,
                                -1,
                                ipv6Len
                        );

                        break;

                    // IPv6 protocol is TCP
                    case "TCP":

                        // Downcast Packet object to a NetworkPacket object with a TCP protocol
                        packet = new NetworkPacket(
                                timestamp,
                                sourceMAC,
                                destinationMAC,
                                totalLen,
                                "IPv6",
                                formatPayload(hexDump),
                                "TCP",
                                hexToIPConverter(sourceIPv6Hex, false),
                                hexToIPConverter(destinationIPv6Hex, false),
                                hexToNumberConverter(sourcePortv6Hex),
                                hexToNumberConverter(destinationPortv6Hex),
                                ipv6Len
                        );

                        break;

                    // IPv6 protocol is UDP
                    case "UDP":

                        // Downcast Packet object to a NetworkPacket object with a UDP protocol
                        packet = new NetworkPacket(
                                timestamp,
                                sourceMAC,
                                destinationMAC,
                                totalLen,
                                "IPv6",
                                formatPayload(hexDump),
                                "UDP",
                                hexToIPConverter(sourceIPv6Hex, false),
                                hexToIPConverter(destinationIPv6Hex, false),
                                hexToNumberConverter(sourcePortv6Hex),
                                hexToNumberConverter(destinationPortv6Hex),
                                ipv6Len
                        );

                        break;

                    // Else IPv6 protocol is unknown (at least within the scope of this project)
                    default:

                        // Downcast Packet object to a NetworkPacket object with an unknown protocol
                        packet = new NetworkPacket(
                                timestamp,
                                sourceMAC,
                                destinationMAC,
                                totalLen,
                                "IPv6",
                                formatPayload(hexDump),
                                "Unknown",
                                hexToIPConverter(sourceIPv6Hex, false),
                                hexToIPConverter(destinationIPv6Hex, false),
                                -1,
                                -1,
                                ipv6Len
                        );

                        break;

                }

                break;

            // EtherType is unknown
            default:

                // Create a packet object with an EtherType of the hex value that was captured in the hex dump
                packet = new Packet(
                        timestamp,
                        sourceMAC,
                        destinationMAC,
                        totalLen,
                        // ether type: 0xfe68 occurs a lot on my home network, as a spanning-tree-(for bridges) protocol
                        ("0x"+etherType),
                        formatPayload(hexDump)
                );

                break;

        }

        return packet;

    }

    /**
     * Converts a list of bytes into an IP address
     * @param hexList List of hex values
     * @param isV4 indicates whether the address is an IPv4 or IPv6 address.
     * @return IP address as a String
     */
    public String hexToIPConverter(String[] hexList, boolean isV4){

        StringBuilder stringBuilder = new StringBuilder();
        if(isV4)
            for (String hex : hexList) {
                stringBuilder.append(Integer.valueOf(hex, 16)).append('.');
            }
        //Formatting IPv6 address, also adding correct notation (e.g. extra ':' if hex segment reads 0)
        else
            for (int i = 0; i < hexList.length; i=i+2) {
                String seg = hexList[i] + hexList[i+1];
                if(!seg.equals("0000"))
                    stringBuilder.append(hexList[i]).append(hexList[i+1]).append(':');

                // Shorten '0000' to an extra ':'
                else
                    if(!stringBuilder.substring(stringBuilder.length()-2, stringBuilder.length()).equals("::"))
                        stringBuilder.append(':');

            }

        // Returns IP address but removing last character (semi-colon)
        return stringBuilder.substring(0, stringBuilder.length()-1);

    }

    /**
     * This method converts a list of Strings into an integer
     * @param hexList list of hex values
     * @return hex values converted from base-16 to base-10
     */
    public int hexToNumberConverter(String[] hexList){
        StringBuilder stringBuilder = new StringBuilder();
        for (String hex : hexList) {
            stringBuilder.append(hex);
        }
        return Integer.valueOf(stringBuilder.toString(), 16);
    }

    /**
     * This method converts a single hex value into an integer
     * @param hex hex value
     * @return have value converted from base-16 to base-10
     */
    public int hexToNumberConverter(String hex){
        return Integer.valueOf(hex, 16);
    }

    /**
     * This method formats the packet payload into a readable format (in ranks of 16). Every 16th byte will have a
     * newline character appended to it.
     * @param hexCode String list of hex values
     * @return StringBuilder containing the formatted hex values
     */
    public String formatPayload(String[] hexCode) {
        StringBuilder out = new StringBuilder();
        int i = 1;
        for (String s : hexCode) {
            if (i == 16) {
                out.append(s).append("\n");
                i = 1;
            } else {
                out.append(s).append(" ");
                i++;
            }
        }
        return out.toString();
    }
    public String formatPayload(String hexCode){
        StringBuilder out = new StringBuilder();
        int i = 1;
        for (String s : hexCode.split(" ")) {
            if(i == 16){
                out.append(s).append("\n");
                i = 1;
            } else{
                out.append(s).append(" ");
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Uses ipify.org to return user's public IP address
     *
     * @return IP as a String
     */
    private static String getPublicIP() {

        try {

            Process proc = rt.exec(new String[] {"cmd.exe", "/c", ("curl https://api.ipify.org")} );
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            return stdInput.readLine();

        } catch (IOException e){

            e.printStackTrace();
            return "n/a";

        }

    }

    /**
     * This methods returns unique public network packets from the packet database.
     * @param isIncoming Indicates whether the method should return incoming or outgoing unique network packets
     * @return unique incoming or outgoing network packets
     */
    public static ArrayList<NetworkPacket> getUniquePublicPackets(boolean isIncoming){
        if(isIncoming)
            return db.getUniqueIncomingPublicPackets();

        return db.getUniqueOutgoingPublicPackets();

    }

    public static String getHostIP(){
        return ip;
    }

    public static PacketDatabase getDb(){
        return db;
    }

    /**
     * This is a custom thread class, used to capture live traffic.
     */
    public class PacketCaptureThread extends Thread {

        private volatile boolean running = true;

        final String[] commands = {
                "cmd.exe", "/c", ("cd src/CyberScope/PacketCapture && WinDump.exe -nnxxli" + chosenInterfaceIndex)
        };
        final Process proc = rt.exec(commands);

        final BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        StringBuilder packetHex = new StringBuilder();

        public PacketCaptureThread() throws IOException {
        }

        @Override
        public void run() {

            // While the thread has not been interrupted
            String s;
            while (running) {

                try {

                    if ((s = stdInput.readLine()) != null) {

                        // If line doesn't start with '0x' (i.e. not hex dump data) and string builder doesn't contain
                        //  anything
                        if (!s.startsWith("\t0x") && packetHex.length() == 0) {

                            // Add timestamp of packet to string builder
                            packetHex.append(s, 0, 12);
                            packetHex.append("|");

                        }
                        // If line doesn't start with '0x' (i.e. not hex dump data)
                        else if (!s.startsWith("\t0x")) {

                            // Send packet hex data to 'readHex' method so it can be converted into a Packet object
                            db.addPacket(readHex(packetHex.toString()));

                            // Reset string builder
                            packetHex = new StringBuilder();

                            // Add timestamp of packet to string builder
                            packetHex.append(s, 0, 12);
                            packetHex.append("|");

                        }
                        // Else, line contains hex dump data
                        else {
                            // Remove whitespace and unnecessary info and add it to string builder
                            String str = s.stripLeading()
                                    .substring(9)
                                    .replace(" ", "");
                            packetHex.append(str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // This method runs when the thread is interrupted, so it can be halted in a thread-safe way
        @Override
        public void interrupt() {
            this.running = false;
        }

        // This method restarts live capture
        public void resetLiveCapture() throws IOException {

            liveThread = new PacketCaptureThread();

            liveThread.start();

        }

    }

}
