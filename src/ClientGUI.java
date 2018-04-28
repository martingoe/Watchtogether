import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ClientGUI extends Application {
    public static BufferedReader reader;
    static WebView webview = new WebView();
    Socket socket;
    boolean startVideo = false;

    static void updateURL(String url) {
        Platform.runLater(() -> webview.getEngine().load(url));

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        socket = new Socket("localhost", 5555);

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        webview.setMouseTransparent(true);

        TextField uRLfield = new TextField();

        StackPane stackPane = new StackPane(webview);

        BorderPane pane = new BorderPane();
        pane.setCenter(stackPane);
        pane.setTop(uRLfield);

        uRLfield.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            if (code.getCode() == 32) {
                writer.write("PAUSE\n");
                writer.flush();
                System.out.println("pause");
            }

            if (code.equals(KeyCode.ENTER)) {
                String url = uRLfield.getText().replace("watch?v=", "embed/") + "?controls=0&autoplay=1&disablekb=1&modestbranding=1&showinfo=0&rel=0";

                writer.write("START_VIDEO: " + url + "\n");
                writer.write("START_VIDEO: " + url + "\n");
                writer.flush();

            }
            if(code.equals(KeyCode.LEFT)){
                writer.write("ADD15\n");
                writer.flush();
            }
            if(code.equals(KeyCode.RIGHT)){
                writer.write("BACK15\n");
                writer.flush();
            }

        });


        Platform.setImplicitExit(false);
        Scene scene = new Scene(pane, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(new Handleclient()).start();
    }


}
