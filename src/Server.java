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
    //The current url
    static String url = "";
    //Saves, if the video is paused
    private static boolean isPaused = false;

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
            //Always run this
            while (true) {

                //Run this for every connected sockets
                Socket[] sockets = clients.toArray(new Socket[0]);
                for (Socket socket : sockets)

                    try {
                        //Find out if the socket sent any data
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        if (reader.ready()) {
                            //If there is data to be received, get it
                            String receivedString = reader.readLine();

                            //Start a new video if the socket tells the server to
                            if (receivedString.startsWith("START_VIDEO: "))
                                startVideo(receivedString.replace("START_VIDEO: ", ""));

                                //Skip to a specific time if the socket tells the server to
                            else if (receivedString.startsWith("SKIP_TO: "))
                                skipTo(Integer.parseInt(receivedString.replace("SKIP_TO: ", "")));

                                //Pause or unpause the video if the socket wants the server to
                            else if (receivedString.startsWith("PAUSE: "))
                                unpauseOrPauseVideo(receivedString.replace("PAUSE: ", ""));

                            //Send the new URL to everyone
                            sendURL();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        };

        //Run the new Thread
        runnable.run();
    }

    private static void skipTo(int secs) {
        startAttribute(secs);
    }

    //Add the start attribute to the URL
    private static void startVideo(String text) {
        url = text + "&start=0";
    }


    private static void unpauseOrPauseVideo(String replace) {
        //Find the current time
        int timeInSecs = Integer.parseInt(replace);
        System.out.println("PAUSE");
        //Pause the video if it is not yet paused and the other way around
        if (!isPaused)
            pause(timeInSecs);
        else
            unPause(timeInSecs);


    }

    private static void unPause(int timeInSecs) {
        startAttribute(timeInSecs);

        //Change the autoplay attribute to 1 and change isPaused to false
        url = url.replace("autoplay=0", "autoplay=1");
        isPaused = false;
    }

    private static void pause(int timeInSecs) {
        startAttribute(timeInSecs);

        //Change the autoplay attribute to 1 and change isPaused to false
        if (url.contains("autoplay=1"))
            url = url.replace("autoplay=1", "autoplay=0");
        else
            url += "&autoplay=0";

        isPaused = true;
    }

    private static void startAttribute(int timeInSecs) {
        //Change the start attribute to the new one
        Pattern p = Pattern.compile("start=[0-9]+");
        Matcher m = p.matcher(url);
        url = m.replaceAll("start=" + timeInSecs);
    }

    //Sends the video URL to every Socket
    private static void sendURL() {
        //Do this for every connected Socket
        Socket[] sockets = clients.toArray(new Socket[0]);
        for (Socket socket : sockets) {
            try {
                //Send the current URL to the Socket
                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.write(url + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
