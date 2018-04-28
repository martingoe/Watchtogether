import java.io.IOException;

public class Handleclient extends Thread {
    @Override
    public void run() {
        while (true) {
            try {
                if (ClientGUI.reader.ready()) {

                    String url = ClientGUI.reader.readLine();
                    ClientGUI.updateURL(url);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
