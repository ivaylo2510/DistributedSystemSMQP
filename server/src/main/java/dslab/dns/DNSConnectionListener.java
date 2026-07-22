package dslab.dns;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

public class DNSConnectionListener implements Runnable {

    private final ThreadFactory threadFactory = Thread.ofVirtual().factory();

    private final Map<String,String> records;

    private final Semaphore semaphore;

    private final int port;
    private ServerSocket socket;

    public DNSConnectionListener(int port,Map<String, String> records, Semaphore semaphore) {
        this.records = records;
        this.port=port;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try {
            socket = new ServerSocket(port);
            while (!socket.isClosed()) {
                Socket conn = socket.accept();
                Thread t = threadFactory.newThread(new DNSConnectionHandler(conn,records, semaphore));
                t.start();
            }

        }catch (SocketException e) {
            if (socket != null && socket.isClosed()) {
                System.out.println("DNSConnectionListener: Server socket closed, shutting down.");
            } else {
                e.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
