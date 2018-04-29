import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    public static BufferedReader reader;
    static WebView webview = new WebView();
    private static TextField skipToField;
    private static Label durationLbl;
    private static boolean isFirst = true;
    Socket socket;
    private static TextField uRLfield;
    private static int duration;
    private static int currentTime;
    static Label timeDisplay;
    private static boolean isPaused;

    static void updateURL(String url) {

        isPaused = !url.contains("autoplay=1");

        getDuration(url);
        Platform.runLater(() -> durationLbl.setText(String.valueOf(duration)));

        if(isFirst) {
            Runnable runnable = () -> {
                Timer timer = new Timer(1000, e -> {
                    if (!isPaused) {
                        currentTime++;
                        Platform.runLater(() -> timeDisplay.setText(String.valueOf(currentTime)));
                    }
                });
                timer.start();
            };
            new Thread(runnable).start();
            isFirst = false;
        }

        if(url.contains("start=")) {
            Pattern pattern = Pattern.compile("start=[0-9]+");

            Matcher matcher = pattern.matcher(url);

            if (matcher.find()) {
                Platform.runLater(() -> timeDisplay.setText(matcher.group().replace("start=", "").replace("\"", "")));
                currentTime = Integer.parseInt(matcher.group().replace("start=", "").replace("\"", ""));
            }

        }
        Platform.runLater(() -> webview.getEngine().load(url));

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        socket = new Socket("localhost", 5555);

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        webview.setMouseTransparent(true);

        uRLfield = new TextField();

        HBox box = new HBox();

        Button play = new Button("|> ||");
        play.setOnAction(event -> {

            writer.write("PAUSE\n");
            writer.flush();
        });

        durationLbl = new Label();
        Label slashlbl = new Label("/");

        skipToField = new TextField();
        skipToField.setMaxWidth(50);
        skipToField.setOnAction(event -> {
            writer.write("SKIP_TO:" + skipToField.getText() + "\n");
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
        box.getChildren().addAll(play, timeDisplay,skipToField, slashlbl, durationLbl, fullScreenBtn);

        StackPane stackPane = new StackPane(webview);

        BorderPane pane = new BorderPane();
        pane.setCenter(stackPane);
        pane.setTop(uRLfield);
        pane.setBottom(box);

        controlByKeyboard(writer, durationLbl, uRLfield);

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
                String url = uRLfield.getText().replace("watch?v=", "embed/") + "?controls=0&autoplay=1&disablekb=1&modestbranding=1&showinfo=0&rel=0";

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


}
