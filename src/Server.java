import javax.swing.*;
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

    static ArrayList<Socket> clients = new ArrayList<>();
    static ServerSocket serverSocket;
    static boolean isPaused = false;
    static int timeInSecs = 0;
    static String url = "";
    boolean isMuted = false;

    public static void main(String[] args) {

        {
            try {
                serverSocket = new ServerSocket(5555);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        new Thread(new HandleServer()).start();
        receiveContent();

    }


    static void receiveContent() {
        Runnable runnable = () -> {
            while (true) {

                Socket[] sockets = clients.toArray(new Socket[clients.size()]);
                for (Socket socket : sockets)

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        if (reader.ready()) {
                            String receivedString = reader.readLine();
                            System.out.println("Received: " + receivedString);

                            if (receivedString.startsWith("START_VIDEO: "))
                                startVideo(receivedString);

                            else if (receivedString.startsWith("ADD15"))
                                skipTo(timeInSecs - 15);

                            else if (receivedString.startsWith("BACK15"))
                                skipTo(timeInSecs + 15);

                            else if (receivedString.startsWith("SKIP_TO"))
                                skipTo(Integer.parseInt(receivedString.replace("SKIP_TO:", "")));

                            else if (receivedString.equals("PAUSE"))
                                unpauseOrPauseVideo();

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

        timeInSecs = secs;
        if (url.contains("&start=")) {
            Pattern p = Pattern.compile("&start=[0-9]+");

            Matcher m = p.matcher(url);
            url = m.replaceAll("&start=" + timeInSecs);
        } else {
            url = url + "&start=" + timeInSecs;
        }
    }

    static void startVideo(String text) {
        timeInSecs = 0;
        url = text.replace("START_VIDEO: ", "");


        Timer timerTask = new Timer(1000, e -> {
            if (!isPaused) {
                timeInSecs++;
                System.out.println("Secs + 1");
            }
        });
        timerTask.start();

    }


    static void unpauseOrPauseVideo() {
        System.out.println("PAUSE");
        if (!isPaused)
            pause();
        else
            unpause();


    }

    private static void unpause() {
        Pattern p = Pattern.compile("&start=[0-9]+");

        Matcher m = p.matcher(url);
        url = m.replaceAll("&start=" + timeInSecs);
        url = url.replace("autoplay=0", "autoplay=1");
        isPaused = false;
    }

    private static void pause() {
        if (url.contains("&start=")) {

            url = url.replace("autoplay=1", "autoplay=0");

        } else
            url = url.replace("autoplay=1", "autoplay=0") + "&start=" + timeInSecs;
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
