package CyberScope.Maps;

import CyberScope.Main;
import CyberScope.NetworkPacket.TrafficDirection;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Class for controlling the map screen FXML. It also handles and draws all of the geo-IP information of each network
 * layer packet.
 *
 * @author Charlie Jones - 100234961
 */
public class MapScreenController implements Initializable {

    // Referencing elements defined in MapScreen.fxml
    @FXML private VBox loadingBox;
    @FXML private Label loadingLabel;
    @FXML private Label timeElapsedLabel;
    @FXML private ProgressBar progressBar;
    @FXML private VBox mapScreen;
    @FXML private StackPane viewPane;

    // FXML element which contains the web page
    private final WebView view = new WebView();
    // webview engine (JS), used to execute script on backend
    private final WebEngine engine = view.getEngine();

    // Separate thread to indicate to the user how long the map screen is taking to resolve geo-ip data
    private final Task<Void> elapsedTimerTask = new Task<>() {

        private final Instant startTime = Instant.now();

        @Override
        protected Void call() throws InterruptedException {

            // While polyline have not been drawn
            String str;
            while(!isCancelled()){
                str = "Time elapsed: " + Duration.between(startTime, Instant.now()).getSeconds() + "s";
                updateMessage(str);

                // Update label every second
                Thread.sleep(1000);

            }
            return null;
        }

    };

    // Polyline list which all of the worker threads will add to
    private volatile ArrayList<Polyline> polyLines = new ArrayList<>();

    ArrayList<JSONObject> jsonObjects = new ArrayList<>();

    /**
     * This method is run once all of the FXML elements have loaded. It is used to draw all of the poly-lines to the
     * map.
     *
     * @param url JavaFX defined parameter
     * @param resourceBundle JavaFX defined parameter
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        engine.load(getClass().getResource("view.html").toExternalForm());

        viewPane.getChildren().add(view);

        engine.setJavaScriptEnabled(true);

        int totalProgressUnits = Main.getUniquePublicPackets(true).size() +
                Main.getUniquePublicPackets(false).size();


        // Shows how many unique IP addresses need to be looked up
        loadingLabel.setText("Loading... "+totalProgressUnits+" IP addresses to locate.");

        timeElapsedLabel.textProperty().bind(elapsedTimerTask.messageProperty());

        // A new task which acts as the master process to look up the geo ip data
        Task<Void> loader = new Task<>() {
            @Override
            protected Void call() {

                startGeoIPLookup();
                return null;

            }

            // Once all of the IP addresses have been looked up and processed, they must be drawn to the GoogleMaps API
            @Override
            protected void succeeded() {
                super.succeeded();
                loadingLabel.setText("Drawing polylines to map...");

                // Executing script by passing polyline objects as a JSON string to be read by the JS function
                //  'createPolys()'
                for(JSONObject jsonObject : jsonObjects) {
                    engine.executeScript("document.createPolys('" + jsonObject.toJSONString() + "');");
                }

                // Remove loading box when loading has finished
                elapsedTimerTask.cancel();
                loadingBox.setManaged(false);

            }

        };

        progressBar.progressProperty().bind(loader.totalWorkProperty());

        // This listener waits for the webview to load before executing script to set the host IP as a session variable
        //  and then it runs the 'main()' JS function
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue == Worker.State.SUCCEEDED) {
                engine.executeScript("document.hostIP = \""+Main.getHostIP()+"\";");
                engine.executeScript("main();");

                // Starts loader process for looking up geo-ip data once the webview has loaded
                Thread loaderThread = new Thread(loader);
                loaderThread.start();

            }
        });

    }

    /**
     * This method initiates the whole operation of looking up geo-ip data for each NetworkPacket in the packet
     * database. It starts by working out how many packets it is dealing with, then if there are more than 100 packets
     * for incoming or outgoing packets, 10 separate threads will be created with equal load spread out amongst them.
     * This is achieved by the help of a helper method 'taskMaker' which creates a custom thread used for looking up
     * geo-ip data by using the DatabaseHandler class.
     */
    public void startGeoIPLookup() {

        // TreeMap used to store poly-lines as they can be sorted by location
        TreeMap<String, ArrayList<Polyline>> uniqueLocations = new TreeMap<>();

        // Small thread used to notify the user how long the process is taking
        Thread timerThread = new Thread(elapsedTimerTask);
        timerThread.start();

        try {
            ArrayList<Thread> threads = new ArrayList<>();
            int incomingPacketArraySize = Main.getUniquePublicPackets(true).size();
            int outgoingPacketArraySize = Main.getUniquePublicPackets(false).size();

            // If there are more than 100 unique incoming packets
            if (incomingPacketArraySize > 100) {

                // Create 10 chunks of data
                int chunks = 10;
                // Each chunk size should be 10% of the overall load
                int chunkSize = incomingPacketArraySize / chunks;
                // For every chunk
                for (int i = 0; i < chunks; i++) {
                    // If the next chunk is the last, create a normal thread but then create another thread and flag
                    //  it as the last thread to be made
                    if (i + 1 == chunks) {
                        threads.add(new Thread(
                                taskMaker(chunkSize, i, TrafficDirection.INCOMING, false)
                        ));
                        threads.add(new Thread(
                                taskMaker(chunkSize, i + 1, TrafficDirection.INCOMING, true)
                        ));
                    }
                    // Else, keep making a new thread for each chunk
                    else {
                        threads.add(new Thread(
                                taskMaker(chunkSize, i, TrafficDirection.INCOMING, false)
                        ));
                    }
                }
            }
            // Else, only a single thread is needed to handle the load
            else if (incomingPacketArraySize > 0) {
                threads.add(new Thread(
                        taskMaker(incomingPacketArraySize, 0, TrafficDirection.INCOMING, true)
                ));
            }

            // If there are more than 100 unique outgoing packets
            if (outgoingPacketArraySize > 100) {

                // Create 10 chunks of data
                int chunks = 10;
                // Each chunk size should be 10% of the overall load
                int chunkSize = outgoingPacketArraySize / chunks;
                // For every chunk
                for (int i = 0; i < chunks; i++) {
                    // If the next chunk is the last, create a normal thread but then create another thread and flag
                    //  it as the last thread to be made
                    if (i + 1 == chunks) {
                        threads.add(new Thread(
                                taskMaker(chunkSize, i, TrafficDirection.OUTGOING, false)
                        ));
                        threads.add(new Thread(
                                taskMaker(chunkSize, i + 1, TrafficDirection.OUTGOING, true)
                        ));
                    }
                    // Else, keep making a new thread for each chunk
                    else {
                        threads.add(new Thread(
                                taskMaker(chunkSize, i, TrafficDirection.OUTGOING, false)
                        ));
                    }
                }
            }
            // Else, only a single thread is needed to handle the load
            else if (outgoingPacketArraySize > 0) {
                threads.add(new Thread(
                        taskMaker(outgoingPacketArraySize, 0, TrafficDirection.OUTGOING, true)
                ));
            }

            // Start all threads
            for (Thread t : threads) {
                t.start();
            }

            // Wait for every thread to finish before continuing
            for (Thread t : threads) {
                t.join();
            }

            /*
             * This loop will search through all of the polylines to check if there are different polylines that go to
             * the same location. For example there are different IP addresses that will route to the same location but
             * they will appear as different polylines. This method will find the duplicates and add them to a list
             * together.
             */
            for (Polyline polyline : polyLines) {

                // Creates a composite key of the lat, long and direction of polyline
                String latLng = (Arrays.toString(new float[]{polyline.getLatitude(), polyline.getLongitude()}) +
                        "+" + polyline.getDirection());

                // If map does not contain the lat, long and direction key yet, add it with a singleton arraylist
                //  containing just the one polyline object
                if (!uniqueLocations.containsKey(latLng)) {

                    uniqueLocations.put(latLng, new ArrayList<>(Collections.singleton(polyline)));

                }
                // Else key already exists, so add polyline to the list of polylines with the same location and
                //  direction
                else {

                    uniqueLocations.get(latLng).add(polyline);

                }

            }

            // New map to concatenate polylines that are incoming and outgoing to the same destination
            TreeMap<String, ArrayList<Polyline>> uniqueLocationsModified = new TreeMap<>();

            /*
             * This loop searches through each unique location and checks for incoming and outgoing poly lines to the
             * same source/destination. Because a tree map is used, the keys are sorted by the string value, which means
             * a key with the same latitude and longitude but different directions will appear next to each other. This
             * makes it easy to check if two polyline elements next to each other in the key-set are to/from the same
             * destination so they can be merged into one 'bidirectional' polyline and added to the new TreeMap
             * 'uniqueLocationsModified'.
             */
            String[] keys = uniqueLocations.keySet().toArray(new String[0]);
            for (int i = 0; i < keys.length; i++) {

                // Get (composite) key and split it up into lat/long and direction
                String key = keys[i];
                String[] keyComponents = key.split("\\+");

                // If current key and next key have the same lat/long but incoming and outgoing directions
                if (
                    i + 1 < keys.length &&
                    keyComponents[1].equals("INCOMING") &&
                    keys[i + 1].equals(keyComponents[0] + "+OUTGOING")
                ) {

                    // Create a new polyline list and add all of the polylines from both lists
                    ArrayList<Polyline> bidirectionalPolylines = uniqueLocations.get(key);
                    bidirectionalPolylines.addAll(uniqueLocations.get(keyComponents[0] + "+OUTGOING"));

                    // Set direction of first polyline to bi-directional (only first one is needed as every other one
                    //  after that will also be counted at bi-directional)
                    bidirectionalPolylines.get(0).setDirection(TrafficDirection.BIDIRECTIONAL);

                    // Add new entry set to TreeMap with new bi-directional keyword
                    uniqueLocationsModified.put(keyComponents[0] + "+BIDIRECTIONAL", bidirectionalPolylines);

                    // Skips next iter, as this has been merged with current
                    i = i + 1;

                }
                // Else, just add the uni-directional polyline to the map
                else {

                    uniqueLocationsModified.put(key, uniqueLocations.get(key));

                }
            }

            /**
             * This loop converts all of the polylines into JSON objects so they can be passed into a JS function to be
             * drawn on GoogleMaps
             */
            for (Map.Entry<String, ArrayList<Polyline>> unique : uniqueLocationsModified.entrySet()) {

                JSONObject jsonObject = new JSONObject();

                // Gets every IP address that is tied to the polyline location and stores it in a JSON array
                JSONArray ips = new JSONArray();
                for (Polyline polyline : unique.getValue()) {
                    ips.add(polyline.getIp());
                }

                // Add other geographic data to the JSON object
                jsonObject.put("ipAddresses", ips);
                jsonObject.put("direction", unique.getValue().get(0).getDirection().toString());

                // Escapes any single quotes inside a country name
                String country = unique.getValue().get(0).getCountry();
                jsonObject.put("country", country.replaceAll("'", "`"));

                // Escapes any single quotes inside a city name
                String city = unique.getValue().get(0).getCity();
                jsonObject.put("city", city.replaceAll("'", "`"));

                jsonObject.put("latitude", unique.getValue().get(0).getLatitude());
                jsonObject.put("longitude", unique.getValue().get(0).getLongitude());

                jsonObjects.add(jsonObject);

            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * This method creates a custom task (which is used to make a thread) that looks up geo-ip data of a incoming or
     * outgoing packet.
     * @param chunkSize How big the chunk is (used for calculating the upper limit at which the task should stop looking
     *                  up packets from the database)
     * @param index The index at which the task should look up packets from in the packet database
     * @param direction Whether the task is looking up incoming our outgoing packet streams
     * @param isLastThread Indication as to whether the chunk is the last in a sequence, which may mean it will have to
     *                     take the remainder of packets left over from dividing the work up
     * @return A task object that can then be used to initialise a thread to carry out the work specified
     */
    public Task<Void> taskMaker(int chunkSize, int index, TrafficDirection direction, boolean isLastThread){
        return new Task<>() {

            // New instance of database handler, so multiple threads can read from the same database
            private final DatabaseHandler dbHandler = new DatabaseHandler();

            @Override
            protected Void call() throws InterruptedException {

                // Stalls thread to allow GoogleMapsAPI to load properly
                Thread.sleep(500);

                boolean isIncoming;
                isIncoming = (direction != TrafficDirection.OUTGOING);

                // If the current thread should not be the last thread, iterate through the packets until it reaches
                //  it's upper limit of chunk number times index.
                if(!isLastThread) {
                    for (int j = (index * chunkSize); j < (index + 1) * chunkSize; j++) {
                        Polyline polyline;
                        if(isIncoming) {
                            polyline = dbHandler.createPolylineCandidate(
                                    Main.getUniquePublicPackets(true).get(j).getSourceIP(), direction
                            );
                        } else {
                            polyline = dbHandler.createPolylineCandidate(
                                    Main.getUniquePublicPackets(false).get(j).getDestinationIP(), direction
                            );
                        }

                        // If result was found, add poly line to polylines collection
                        if (polyline != null) {
                            polyLines.add(polyline);
                        }

                    }
                }
                // Else, The current thread should be the last thread, iterate through the packets until it reaches
                //  the end of the packet database
                else {
                    for (int j = (index * chunkSize); j < Main.getUniquePublicPackets(isIncoming).size(); j++) {
                        Polyline polyline;
                        if(isIncoming) {
                            polyline = dbHandler.createPolylineCandidate(
                                    Main.getUniquePublicPackets(true).get(j).getSourceIP(), direction
                            );
                        } else {
                            polyline = dbHandler.createPolylineCandidate(
                                    Main.getUniquePublicPackets(false).get(j).getDestinationIP(), direction
                            );
                        }

                        // If result was found, add poly line to polylines collection
                        if (polyline != null) {
                            polyLines.add(polyline);
                        }

                    }
                }

                return null;
            }

        };
    }

}
