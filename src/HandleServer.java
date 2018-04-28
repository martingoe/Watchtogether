import java.io.IOException;
import java.net.Socket;

public class HandleServer extends Thread {
    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = Server.serverSocket.accept();
                Server.clients.add(socket);
                System.out.println("New Client");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
