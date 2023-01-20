package CyberScope;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Class for controlling the main screen FXML
 *
 * @author Charlie Jones - 100234961
 */
public class MainController implements Initializable {

    //Referencing elements defined in Main.fxml
    @FXML private Label statusLabel;
    @FXML private Button playButton;
    @FXML private Button pauseButton;

    @FXML private Button mapScreenButton;
    @FXML private Button metricScreenButton;
    @FXML private Button LANScreenButton;
    @FXML private Button flaggedScreenButton;
    @FXML private Button homeScreenButton;
    @FXML private BorderPane main;

    // Pointer to the last button that the user clicked on
    private Button lastUsedButton = new Button();

    /**
     * This method is run once all of the FXML elements have loaded. It is used to populate all of the tables.
     *
     * @param url JavaFX defined parameter
     * @param resourceBundle JavaFX defined parameter
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        if(Main.isLive)
            playButton.setDisable(true);
        else
            main.setTop(null);

        try {
            homeScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method loads up the home screen and sets the center of the screen to it's content
     * @throws IOException if FXMl file cannot be found
     */
    @FXML
    private void homeScreen() throws IOException {
        VBox vBox = FXMLLoader.load(getClass().getResource("FXML/HomeScreen.fxml"));
        main.setCenter(vBox);
        toggleButtonFocus(homeScreenButton);
        Main.centerNodeContentPointer = vBox;
    }

    /**
     * This method loads up the map screen and sets the center of the screen to it's content
     * @throws IOException if FXMl file cannot be found
     */
    @FXML
    private void mapScreen() throws IOException {
        VBox vBox = FXMLLoader.load(getClass().getResource("Maps/MapScreen.fxml"));
        main.setCenter(vBox);
        toggleButtonFocus(mapScreenButton);
        Main.centerNodeContentPointer = vBox;
    }

    /**
     * This method loads up the metric screen and sets the center of the screen to it's content
     * @throws IOException if FXMl file cannot be found
     */
    @FXML
    private void metricScreen() throws IOException {
        VBox vBox = FXMLLoader.load(getClass().getResource("FXML/MetricScreen.fxml"));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(vBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);

        main.setCenter(scrollPane);
        toggleButtonFocus(metricScreenButton);
        Main.centerNodeContentPointer = vBox;
    }

    /**
     * This method loads up the LAN screen and sets the center of the screen to it's content
     * @throws IOException if FXMl file cannot be found
     */
    @FXML
    private void LANScreen() throws IOException {
        VBox vBox = FXMLLoader.load(getClass().getResource("FXML/LANScreen.fxml"));
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(vBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);

        main.setCenter(scrollPane);
        toggleButtonFocus(LANScreenButton);
        Main.centerNodeContentPointer = vBox;
    }

    /**
     * This method loads up the flagged screen and sets the center of the screen to it's content
     * @throws IOException if FXMl file cannot be found
     */
    @FXML
    private void flaggedScreen() throws IOException {
        VBox vBox = FXMLLoader.load(getClass().getResource("FXML/FlaggedScreen.fxml"));
        main.setCenter(vBox);
        toggleButtonFocus(flaggedScreenButton);
        Main.centerNodeContentPointer = vBox;
    }

    /**
     * This method changes the button focus when the user clicks on a button, and alters the styles of the selected and
     * deselected button
     * @param selectedButton The button that has been clicked on by the user
     */
    private void toggleButtonFocus(Button selectedButton){
        lastUsedButton.setDisable(false);

        //Setting temporary background colour for selected button
        lastUsedButton.setBackground(new Background(
                new BackgroundFill(Color.rgb(169,169,169), null, null)));

        lastUsedButton = selectedButton;
        selectedButton.setDisable(true);

        //Restoring original background colour for de-selected button
        selectedButton.setBackground(new Background(
                new BackgroundFill(Color.rgb(120,120,120), null, null)));

    }

    /**
     * This method restarts live capturing when the user clicks on the 'play' button
     * @throws IOException when it can't find the WinDump executable file
     */
    public void startLiveUpdates() throws IOException {
        pauseButton.setDisable(false);
        playButton.setDisable(true);

        statusLabel.setText("Capturing");
        statusLabel.setStyle("-fx-background-color: #5CA800;");

        Main.liveThread.resetLiveCapture();
    }

    /**
     * This method pauses live capturing when the user clicks on the 'pause' button
     */
    public void stopLiveUpdates() {
        playButton.setDisable(false);
        pauseButton.setDisable(true);

        statusLabel.setText("Paused");
        statusLabel.setStyle("-fx-background-color: #E89003;");

        Main.liveThread.interrupt();
    }
}
