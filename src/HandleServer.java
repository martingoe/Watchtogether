import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class HandleServer extends Thread {
    @Override
    public void run() {
        //Always run
        while (true) {
            try {
                //Accept any new connecting Sockets
                Socket socket = Server.serverSocket.accept();
                Server.clients.add(socket);
                //Send the new Socket the current url
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
                printWriter.write(Server.url + "\n");
                printWriter.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
