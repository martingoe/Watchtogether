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

    static BufferedReader reader;
    //This displays the embedded video
    private static WebView webView = new WebView();
    //Allows the user to give the program a specific second to skip to
    private static TextField skipToField;
    //Displays the duration of the video
    private static Label durationLbl;
    //Makes sure some things only happen once
    private static boolean isFirst = true;

    private static TextField urlField;
    private static int duration;
    private static int currentTime;
    private static Label timeDisplay;
    private static boolean isPaused;
    private static Slider scrollBar;
    private static StackPane stackpane;
    private static BorderPane pane;
    private static Timer timer;

    //Run a new video by the embedded YouTube url
    static void runURL(String url) {

        //Finds out weather the video is paused, using YouTube's "autoplay" attribute
        isPaused = !url.contains("autoplay=1");

        //Get the duration of the video and display it on the durationLbl
        getDuration(url);
        Platform.runLater(() -> durationLbl.setText(String.valueOf(duration)));




        if (url.contains("start=")) {
            Pattern pattern = Pattern.compile("start=[0-9]+");

            Matcher matcher = pattern.matcher(url);

            if (matcher.find()) {
                String s = matcher.group().replace("start=", "").replace("\"", "");
                Platform.runLater(() -> timeDisplay.setText(s));
                currentTime = Integer.parseInt(s);
                scrollBar.setValue(Double.parseDouble(s));
            }

        } else
            scrollBar.setValue(0);
        scrollBar.setMax(duration);


        Platform.runLater(() -> {
            webView.getEngine().loadContent("");
            webView = new WebView();
            webView.getEngine().getLoadWorker().stateProperty().addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    if (!isPaused) {
                            timer.start();
                    }
                    else{
                        timer.stop();
                    }
                }
            });
            stackpane = new StackPane(webView);
            pane.setCenter(stackpane);
        });

        Platform.runLater(() -> webView.getEngine().load(url));


    }

    static void getDuration(String urlString) {
        try {
            URL url = new URL(urlString.replace("embed/", "watch?v="));

            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;

            while ((line = br.readLine()) != null) {

                if (line.contains("\"length_seconds\":")) {
                    Pattern pattern = Pattern.compile("\"length_seconds\":\"[0-9]+\"");

                    Matcher matcher = pattern.matcher(line);

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
        Socket socket = new Socket("localhost", 5555);

        timer = new Timer(1000, e -> {
            currentTime++;

            Platform.runLater(() -> {
                timeDisplay.setText(String.valueOf(currentTime));
                scrollBar.setValue(scrollBar.getValue() + 1);
            });


        });

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        webView.setMouseTransparent(true);

        urlField = new TextField();

        scrollBar = new Slider();
        scrollBar.setMin(0);
        scrollBar.setValue(0);

        scrollBar.setOnMouseClicked(event -> {
            writer.write("SKIP_TO: " + Math.round(scrollBar.getValue()) + "\n");
            writer.flush();
        });

        StackPane scrollPane = new StackPane(scrollBar);

        HBox box = new HBox();

        Button play = new Button("|> ||");
        play.setOnAction(event -> {

            writer.write("PAUSE: " + currentTime + "\n");
            writer.flush();
        });

        durationLbl = new Label();
        Label slashlbl = new Label("/");

        skipToField = new TextField();
        skipToField.setMaxWidth(50);
        skipToField.setOnAction(event -> {
            writer.write("SKIP_TO: " + skipToField.getText() + "\n");
            writer.flush();

        });

        Button fullScreenBtn = new Button("Full");
        fullScreenBtn.setOnAction(event -> {
            if (primaryStage.isFullScreen())
                primaryStage.setFullScreen(false);
            else
                primaryStage.setFullScreen(true);
        });

        timeDisplay = new Label("0");
        box.setSpacing(5);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        box.getChildren().addAll(play, timeDisplay, scrollPane, skipToField, slashlbl, durationLbl, fullScreenBtn);

        stackpane = new StackPane(webView);

        pane = new BorderPane();
        pane.setCenter(stackpane);
        pane.setTop(urlField);
        pane.setBottom(box);

        controlByKeyboard(writer, durationLbl, urlField);

        controlByKeyboard(writer, durationLbl, skipToField);


        Scene scene = new Scene(pane, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(new Handleclient()).start();
    }

    private void controlByKeyboard(PrintWriter writer, Label durationLbl, TextField skipToField) {
        skipToField.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            if (code.getCode() == 32) {
                writer.write("PAUSE\n");
                writer.flush();
                System.out.println("pause");
            }

            if (code.equals(KeyCode.ENTER)) {
                String url = urlField.getText().replace("watch?v=", "embed/") + "?controls=0&autoplay=1&disablekb=1&modestbranding=1&showinfo=0&rel=0";

                writer.write("START_VIDEO: " + url + "\n");
                writer.flush();

            }

            if (code.equals(KeyCode.LEFT)) {
                writer.write("ADD15\n");
                writer.flush();
            }
            if (code.equals(KeyCode.RIGHT)) {
                writer.write("BACK15\n");
                writer.flush();
            }

        });
    }


}
