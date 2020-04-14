package mypc.clientremote;

import java.net.Socket;

public class KeepAliveThread extends Thread {
    private Socket socket;
    private boolean go;

    public KeepAliveThread(Socket socket) {
        this.socket = socket;
        this.go = true;
    }

    @Override
    public void run() {
        try {
            while (go) {
                Thread.sleep(60000); // one ping per minute
                socket.getOutputStream().write(13); // carriage return
            }
        } catch (Exception e) {
            // Do nothing, just die
        }
    }
}
