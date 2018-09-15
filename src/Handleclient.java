import java.io.IOException;

public class Handleclient extends Thread {
    @Override
    public void run() {
        //Always run this
        while (true) {
            try {
                //Set the url of the video when there is a url to be received
                if (ClientGUI.reader.ready()) {
                    String url = ClientGUI.reader.readLine();
                    ClientGUI.runURL(url);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
