import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {

    //Stores all of the connected Sockets
    static ArrayList<Socket> clients = new ArrayList<>();
    //The actual server
    static ServerSocket serverSocket;
    //Saves, if the video is paused
    private static boolean isPaused = false;
    //The current url
    static String url = "";

    public static void main(String[] args) {
        {
            try {
                //Setup the new Server
                serverSocket = new ServerSocket(5555);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //handles all the incoming Sockets
        new Thread(new HandleServer()).start();
        //Handles the incoming requests
        receiveContent();
    }


    private static void receiveContent() {
        Runnable runnable = () -> {
            while (true) {

                Socket[] sockets = clients.toArray(new Socket[0]);
                for (Socket socket : sockets)

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        if (reader.ready()) {
                            String receivedString = reader.readLine();
                            System.out.println("Received: " + receivedString);

                            if (receivedString.startsWith("START_VIDEO: "))
                                startVideo(receivedString);

                            else if (receivedString.startsWith("SKIP_TO"))
                                skipTo(Integer.parseInt(receivedString.replace("SKIP_TO: ", "")));

                            else if (receivedString.startsWith("PAUSE"))
                                unpauseOrPauseVideo(receivedString.replace("PAUSE: ", ""));

                            sendURL();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        };


        runnable.run();
    }

    private static void skipTo(int secs) {

        if (url.contains("&start=")) {
            Pattern p = Pattern.compile("&start=[0-9]+");

            Matcher m = p.matcher(url);
            url = m.replaceAll("&start=" + secs);
        } else {
            url = url + "&start=" + secs;
        }
    }

    private static void startVideo(String text) {
        url = text.replace("START_VIDEO: ", "") + "&start=0";
    }


    private static void unpauseOrPauseVideo(String replace) {
        int timeInSecs = Integer.parseInt(replace);
        System.out.println("PAUSE");
        if (!isPaused)
            pause(timeInSecs);
        else
            unpause(timeInSecs);


    }

    private static void unpause(int timeInSecs) {
        Pattern p = Pattern.compile("&start=[0-9]+");

        Matcher m = p.matcher(url);
        url = m.replaceAll("&start=" + timeInSecs);
        url = url.replace("autoplay=0", "autoplay=1");
        isPaused = false;
    }

    private static void pause(int timeInSecs) {
        Pattern p = Pattern.compile("start=[0-9]+");

        Matcher m = p.matcher(url);
        url = m.replaceAll("start=" + timeInSecs);
        if(url.contains("autoplay=1"))
            url = url.replace("autoplay=1", "autoplay=0");
        else
            url += "&autoplay=0";
        isPaused = true;
    }

    private static void sendURL() {
        clients.forEach(socket1 -> {

            try {
                PrintWriter writer = new PrintWriter(socket1.getOutputStream());
                writer.write(url + "\n");
                writer.flush();

                System.out.println("Sent " + url + " back.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}
