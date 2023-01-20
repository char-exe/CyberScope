package CyberScope;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Class for controlling the metric screen FXML
 *
 * @author Charlie Jones - 100234961
 */
public class MetricScreenController implements Initializable {

    // Defining the decimal format used
    private static final DecimalFormat df = new DecimalFormat("#.##");

    //Referencing elements defined in MetricScreen.fxml
    @FXML private VBox metricScreen;
    @FXML private Label noIncomingPackets;
    @FXML private Label noIncomingPacketsSec;
    @FXML private Label noOutgoingPackets;
    @FXML private Label noOutgoingPacketsSec;
    @FXML private VBox topUsedPortsTile;
    @FXML private VBox topUsedProtocolsTile;
    @FXML private BarChart<String, Number> topUsedPortsChart;
    @FXML private BarChart<String, Number> topUsedProtocolsChart;
    @FXML private PieChart trafficRatioPieChart;

    private PieChart.Data trafficRatioSliceInternal;
    private PieChart.Data trafficRatioSliceIncoming;
    private PieChart.Data trafficRatioSliceOutgoing;

    private XYChart.Series<String, Number> topUsedProtocolsSeries;
    private XYChart.Series<String, Number> topUsedPortsSeries;

    // Defining the date format used
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    // Animation function that runs every 2.5seconds, used for when the user is in live mode
    private final Timeline liveUpdater = new Timeline(new KeyFrame(Duration.millis(2500), e -> {

        // If user is currently on metric screen
        if(Main.centerNodeContentPointer == metricScreen){

            // Show/update data
            showData();

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

        initialiseTrafficRatioPieChart();
        initialiseTopUsedPortsChart();
        initialiseTopUsedProtocolsChart();
        showData();

        // If live capture is on, run update function forever until it gets cancelled
        if(Main.isLive) {
            liveUpdater.setCycleCount(Animation.INDEFINITE);
            liveUpdater.play();
        }
    }

    /**
     * This is a controller method that organises how the screen stays up to date
     */
    private void showData(){
        noIncomingPackets.setText(String.valueOf(Main.db.getIncomingTrafficCount()));
        noOutgoingPackets.setText(String.valueOf(Main.db.getOutgoingTrafficCount()));

        noIncomingPacketsSec.setText(getPacketsPerSecValue(true));
        noOutgoingPacketsSec.setText(getPacketsPerSecValue(false));

        setTopUsedPortsList();

        updateLiveChartSeries(topUsedPortsSeries, false);

        setTopUsedProtocolsList();

        updateLiveChartSeries(topUsedProtocolsSeries, true);

        updateTrafficRatioPieChart();

    }

    /**
     * This method calculates the packets per second rate of incoming and outgoing traffic
     * @param isIncoming indicates whether method should work out rate for incoming or outgoing traffic
     * @return Packets per sec value in the form of a String
     */
    private String getPacketsPerSecValue(boolean isIncoming){

        // Gets incoming or outgoing packet count
        int packetCount;
        if(isIncoming) packetCount = Main.db.getIncomingTrafficCount();
        else packetCount = Main.db.getOutgoingTrafficCount();

        // If user is not capturing live traffic
        if(!Main.isLive){

            // Get timestamps of first and last packets
            String firstPacketTime = Main.db.getPackets().get(0).getTimestamp();
            firstPacketTime = firstPacketTime.substring(0, firstPacketTime.length() - 4);

            String lastPacketTime = Main.db.getPackets().get(Main.db.getCount()-1).getTimestamp();
            lastPacketTime = lastPacketTime.substring(0, lastPacketTime.length() - 4);

            // Get difference in seconds between first and last packet times
            int duration;
            try {
                Date start = sdf.parse(firstPacketTime);
                Date end = sdf.parse(lastPacketTime);

                duration = (int) (end.getTime() - start.getTime()) / 1000;

            } catch (ParseException e) {

                return "n/a";

            }

            // Calculate average
            double avg = (double) packetCount / duration;

            return df.format(avg);

        }
        // Else live capture is on, if there are packets in the database
        else if(Main.db.getPackets().size() > 0) {

            // Get timestamp of first packet
            String firstPacketTime = Main.db.getPackets().get(0).getTimestamp();
            firstPacketTime = firstPacketTime.substring(0, firstPacketTime.length() - 4);

            // Get current time
            LocalTime now = LocalTime.now();

            // Get difference in seconds between first packet time and now
            int duration;
            try {
                Date start = sdf.parse(firstPacketTime);
                Date nowDate = sdf.parse(String.valueOf(now));

                duration = (int) (nowDate.getTime() - start.getTime()) / 1000;

            } catch (ParseException e) {

                return "n/a";

            }

            // Calculate average
            double avg = (double) packetCount / duration;

            return df.format(avg);

        }

        return "n/a";

    }

    /**
     * This method works out which ports have been used the most
     */
    private void setTopUsedPortsList(){

        // Gets all content of tile
        ObservableList<Node> tileContent = topUsedPortsTile.getChildren();

        int portListSize = Main.db.getTopUsedPorts().size();

        /*
        Loops through the tile content, ignoring the title and looking for the HBox's that contain each row of the
        list. Each HBox contains two labels and this changes the second label to the respective port number in
        ranking of most used to least used (top 5).
        */
        for (int i = 0; i < tileContent.size(); i++) {

            Node child = tileContent.get(i);

            // Makes sure the child node is not the tile title.
            if(child instanceof HBox){

                // Gets the second label (value placeholder) within the HBox.
                Label label = (Label) ((HBox) child).getChildren().get(1);


                // Sets the text of that label to the corresponding port number in the top used ports list. Offset by
                //  two because of the two labels before the list.
                if(i - 2 < portListSize)
                    label.setText(Main.db.getTopUsedPorts().get(i - 2).split("=")[0]);
                else
                    i = tileContent.size();
            }
        }
    }

    /**
     * This method calculates which protocols have been used the most
     */
    private void setTopUsedProtocolsList(){

        // Gets all content of tile
        ObservableList<Node> tileContent = topUsedProtocolsTile.getChildren();

        int protocolSizeList = Main.db.getTopUsedProtocols().size();

        /*
        Loops through the tile content, ignoring the title and looking for the HBox's that contain each row of the
        list. Each HBox contains two labels and this changes the second label to the respective port number in
        ranking of most used to least used (top 5).
        */
        for (int i = 0; i < tileContent.size(); i++) {

            Node child = tileContent.get(i);

            // Makes sure the child node is not the tile title.
            if(child instanceof HBox){

                // Gets the second label within the HBox.
                Label label = (Label) ((HBox) child).getChildren().get(1);

                // Sets the text of that label to the corresponding port number in the top used ports list. Offset by
                //  one because of the one label before the list.
                if(i - 1 < protocolSizeList)
                    label.setText(Main.db.getTopUsedProtocols().get(i - 1).split("=")[0]);
                else
                    i = tileContent.size();
            }
        }
    }

    /**
     * This method initialises top used ports chart
     */
    private void initialiseTopUsedPortsChart(){

        topUsedPortsSeries = new XYChart.Series<>();

        initialiseChartSeries(topUsedPortsSeries, false);

        topUsedPortsChart.getData().add(topUsedPortsSeries);

        topUsedPortsChart.setAnimated(false);

    }

    /**
     * This method initialises top used protocols chart
     */
    private void initialiseTopUsedProtocolsChart(){

        topUsedProtocolsSeries = new XYChart.Series<>();

        initialiseChartSeries(topUsedProtocolsSeries, true);

        topUsedProtocolsChart.getData().add(topUsedProtocolsSeries);

        topUsedProtocolsChart.setAnimated(false);

    }

    /**
     * This method is used when initialising any of the charts, to initialise the series of either the port or protocol
     * series. If there are less than 10 ports or protocols, it will show all of the data, otherwise it will just get
     * the top 10 ports or protocols.
     * @param series the series chart that is being updated
     * @param isProtocolSeries indicator as to whether the method is updating the protocol or port series
     */
    private void initialiseChartSeries(XYChart.Series<String, Number> series, boolean isProtocolSeries){

        ArrayList<String> dataPoints;

        if(isProtocolSeries) dataPoints = Main.db.getTopUsedProtocols();
        else dataPoints = Main.db.getTopUsedPorts();

        if (dataPoints.size() < 10) {

            for (String port : dataPoints) {
                String[] temp = port.split("=");
                series.getData().add(new XYChart.Data<>(temp[0], Integer.valueOf(temp[1])));
            }

        } else {

            for (int i = 0; i < 10; i++) {
                String[] temp = dataPoints.get(i).split("=");
                series.getData().add(new XYChart.Data<>(temp[0], Integer.valueOf(temp[1])));
            }

        }

    }

    /**
     * This method is used when the user is capturing live traffic. It takes either the port chart or protocol chart
     * series on the metrics screen, and updates it's data based on the current traffic in the packet database.
     * @param series the series chart that is being updated
     * @param isProtocolSeries indicator as to whether the method is updating the protocol or port series
     */
    private void updateLiveChartSeries(XYChart.Series<String, Number> series, boolean isProtocolSeries){

        // Clears old data
        series.getData().clear();

        if(series.getChart() == topUsedProtocolsChart)
            topUsedProtocolsChart.layout();
        else
            topUsedPortsChart.layout();

        // Gets the data points from either port or protocol statistics
        ArrayList<String> dataPoints;
        if(isProtocolSeries) dataPoints = Main.db.getTopUsedProtocols();
        else dataPoints = Main.db.getTopUsedPorts();

        // If there is any data to show
        if(dataPoints.size() > 0) {

            // If there are less than 10 ports or protocols used, show all of the data
            if (dataPoints.size() < 10) {

                for (String dataPoint : dataPoints) {
                    String[] temp = dataPoint.split("=");
                    series.getData().add(new XYChart.Data<>(temp[0], Integer.valueOf(temp[1])));
                }

            }
            // Else, there is more than 10 ports or protocols used, just choose the top 10
            else {

                for (int i = 0; i < 10; i++) {
                    String[] temp = dataPoints.get(i).split("=");
                    series.getData().add(new XYChart.Data<>(temp[0], Integer.valueOf(temp[1])));
                }

            }

        }

        // Resize the charts to accommodate the new data
        if(series.getChart() == topUsedProtocolsChart)
            topUsedProtocolsChart.autosize();
        else
            topUsedPortsChart.autosize();

    }

    /**
     * This method initialises the traffic volume ratio pie chart
     */
    private void initialiseTrafficRatioPieChart(){

        // Creating chart data
        trafficRatioSliceInternal = new PieChart.Data("Internal", Main.db.getInternalTrafficCount());
        trafficRatioSliceIncoming = new PieChart.Data("Incoming", Main.db.getIncomingTrafficCount());
        trafficRatioSliceOutgoing = new PieChart.Data("Outgoing", Main.db.getOutgoingTrafficCount());

        // Add chart data to pie chart
        trafficRatioPieChart.getData().add(trafficRatioSliceInternal);
        trafficRatioPieChart.getData().add(trafficRatioSliceIncoming);
        trafficRatioPieChart.getData().add(trafficRatioSliceOutgoing);

    }

    /**
     * This method labels each pie chart data with the value it represents to make it clearer to the user.
     */
    private void updateTrafficRatioPieChart(){

        trafficRatioSliceInternal.setPieValue(Main.db.getInternalTrafficCount());
        String internalNameTag = "Internal: "+trafficRatioSliceInternal.getPieValue();
        trafficRatioSliceInternal.nameProperty().set(internalNameTag.substring(0, internalNameTag.length()-2));

        trafficRatioSliceIncoming.setPieValue(Main.db.getIncomingTrafficCount());
        String incomingNameTag = "Incoming: "+trafficRatioSliceIncoming.getPieValue();
        trafficRatioSliceIncoming.nameProperty().set(incomingNameTag.substring(0, incomingNameTag.length()-2));

        trafficRatioSliceOutgoing.setPieValue(Main.db.getOutgoingTrafficCount());
        String outgoingNameTag = "Outgoing: "+trafficRatioSliceOutgoing.getPieValue();
        trafficRatioSliceOutgoing.nameProperty().set(outgoingNameTag.substring(0, outgoingNameTag.length()-2));

    }

}
