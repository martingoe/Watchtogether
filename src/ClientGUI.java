import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ClientGUI extends Application {

    //Used to receive data from the server
    static BufferedReader reader;

    //This displays the embedded video
    private static WebView webView = new WebView();

    //Allows the user to give the program a specific second to skip to
    private static TextField skipToField;

    //Displays the duration of the video
    private static Label durationLbl;
    //The duration of the video
    private static int duration;

    //allows the user to play a new video via entering a url
    private static TextField urlField;

    //The current time in seconds
    private static int currentTime;
    //Displays the current time in seconds
    private static Label timeDisplay;

    //Shows the time on a slider
    private static Slider durationSlider;

    //The way to display everything. It is private to reset the Webview
    private static StackPane stackpane;
    private static BorderPane pane;

    //The timer allowing the time to move on
    private static Timer timer;


    //Run a new video by the embedded YouTube url
    static void runURL(String url) {

        //Finds out weather the video is paused, using YouTube's "autoplay" attribute
        boolean isPaused = !url.contains("autoplay=1");

        //Get the duration of the video and display it on the durationLbl
        getDuration(url);
        Platform.runLater(() -> durationLbl.setText(String.valueOf(duration)));

        //Sets the to 0 if there is no specific start time, but if there is one, set it to that
        if (url.contains("start=")) {
            Pattern pattern = Pattern.compile("start=[0-9]+");

            Matcher matcher = pattern.matcher(url);

            if (matcher.find()) {
                String s = matcher.group().replace("start=", "").replace("\"", "");
                Platform.runLater(() -> timeDisplay.setText(s));
                currentTime = Integer.parseInt(s);
                durationSlider.setValue(Double.parseDouble(s));
            }

        } else
            durationSlider.setValue(0);

        //Set the maximum value of the durationSlider to the duration
        durationSlider.setMax(duration);


        //This uses JavaFX, so you have to use Platform.runLater()
        Platform.runLater(() -> {
            //Resets the Webview
            webView.getEngine().loadContent("");
            webView = new WebView();

            //Only start the timer to change the current time when the video is finished loading
            webView.getEngine().getLoadWorker().stateProperty().addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    //Only start the timer when the video is not paused
                    if (!isPaused) {
                            timer.start();
                    }
                    else{
                        timer.stop();
                    }
                }
            });
            //Add the Webview to the screen
            stackpane = new StackPane(webView);
            pane.setCenter(stackpane);
        });

        //Load the video using the Webview
        Platform.runLater(() -> webView.getEngine().load(url));


    }

    private static void getDuration(String urlString) {
        try {
            //Convert the embedded URL to a normal one
            URL url = new URL(urlString.replace("embed/", "watch?v="));

            //BufferedReader to read the YouTube website
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

            //Go through every line of the site
            String line;
            while ((line = br.readLine()) != null) {

                //Only find the duration if the line contains it.
                if (line.contains("\"length_seconds\":")) {
                    //Use RegEx to find the duration
                    Pattern pattern = Pattern.compile("\"length_seconds\":\"[0-9]+\"");

                    Matcher matcher = pattern.matcher(line);

                    //Cut out the duration and save it
                    if (matcher.find()) {
                        line = matcher.group().replace("\"length_seconds\":", "").replace("\"", "");
                        duration = Integer.parseInt(line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //The Socket connects to the Server on localhost
        Socket socket = new Socket("localhost", 5555);

        //define the timer to add set the current time
        defineTimer();

        //Define the writer, that sends to the server
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        //Define the reader to receive data from the server
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //Make the webwiew unable to respond to mouse clicks
        webView.setMouseTransparent(true);

        //Define the urlField as a TextField
        urlField = new TextField();

        //Define the slider and set the min and value to 0
        durationSlider = new Slider();
        durationSlider.setMin(0);
        durationSlider.setValue(0);

        //Define the event listener of the durationSlider
        durationSlider.setOnMouseClicked(event -> {
            //Tell the server to skip to the selected time
            writer.write("SKIP_TO: " + Math.round(durationSlider.getValue()) + "\n");
            writer.flush();
        });

        //Set the durationSlider to the biggest width possible
        StackPane scrollPane = new StackPane(durationSlider);

        //This is the bottom bar
        HBox box = new HBox();

        //the worst pause/play button ever
        Button play = new Button("|> ||");
        //Tell the server to pause at the current time when the button is clicked
        play.setOnAction(event -> {
            writer.write("PAUSE: " + currentTime + "\n");
            writer.flush();
        });

        //Define the durationlbl as a Label
        durationLbl = new Label();
        //This is just a seperator
        Label slashlbl = new Label("/");

        //Define the skipToField as a TextField and set the maxWidth to 50
        skipToField = new TextField();
        skipToField.setMaxWidth(50);
        //Tell the server to skip to the given seconds when the user presses enter
        skipToField.setOnAction(event -> {
            writer.write("SKIP_TO: " + skipToField.getText() + "\n");
            writer.flush();
        });

        //Define the fullScreenButton and set it to make the primaryStage a full screen
        Button fullScreenBtn = new Button("Full");
        fullScreenBtn.setOnAction(event -> {
            if (primaryStage.isFullScreen())
                primaryStage.setFullScreen(false);
            else
                primaryStage.setFullScreen(true);
        });

        //Define the timeDisplay as a new Label
        timeDisplay = new Label("0");

        //Set the spacing of the box
        box.setSpacing(5);
        //Give the scrollPane a priority to be the widest
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        //Add everything to the box
        box.getChildren().addAll(play, timeDisplay, scrollPane, skipToField, slashlbl, durationLbl, fullScreenBtn);

        //Set the webwiev to be the biggest it can be
        stackpane = new StackPane(webView);

        //Define the final borderPane and add everything to it
        pane = new BorderPane();
        pane.setCenter(stackpane);
        pane.setTop(urlField);
        pane.setBottom(box);

        //Allow the user to control the video using the keyboard (While the mouse is in the urlField or the skipToField)
        controlByKeyboard(writer, urlField);
        controlByKeyboard(writer, skipToField);

        //Define the scene and add it to the stage
        Scene scene = new Scene(pane, 400, 300);
        primaryStage.setScene(scene);
        //Show the stage
        primaryStage.show();

        //Call the HandleClient that waits for data from the server
        new Thread(new Handleclient()).start();
    }

    private void defineTimer() {
        //Only do update the time every second
        timer = new Timer(1000, e -> {
            //Add one to the current time
            currentTime++;

            //Update the time on the slider and the label
            Platform.runLater(() -> {
                timeDisplay.setText(String.valueOf(currentTime));
                durationSlider.setValue(durationSlider.getValue() + 1);
            });


        });
    }

    private void controlByKeyboard(PrintWriter writer, TextField field) {
        //Wait for keys to be pressed in the field
        field.setOnKeyPressed(event -> {
            //Get the keycode for the event
            KeyCode code = event.getCode();

            //Tell the server to pause, when the spacebar is pressed
            if (code.getCode() == 32) {
                writer.write("PAUSE\n");
                writer.flush();
            }

            //Update the url when enter is pressed
            if (code.equals(KeyCode.ENTER)) {
                String url = urlField.getText().replace("watch?v=", "embed/") + "?controls=0&autoplay=1&disablekb=1&modestbranding=1&showinfo=0&rel=0";

                writer.write("START_VIDEO: " + url + "\n");
                writer.flush();

            }

            //Set the left and right arrow to be skipping to - or + 15 seconds of the current time
            if (code.equals(KeyCode.LEFT)) {
                writer.write("SKIP_TO: " + (currentTime - 15) + "\n");
                writer.flush();
            }
            if (code.equals(KeyCode.RIGHT)) {
                writer.write("SKIP_TO: " + (currentTime + 15) + "\n");
                writer.flush();
            }

        });
    }


}
