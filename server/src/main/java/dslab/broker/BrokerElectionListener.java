package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ThreadFactory;

public class BrokerElectionListener implements Runnable {

    private final BrokerConfig config;
    private final ElectionManager electionManager;
    private ServerSocket serverSocket;
    private final ThreadFactory threadFactory = Thread.ofVirtual().factory();

    public BrokerElectionListener(BrokerConfig config, ElectionManager electionManager) {
        this.config = config;
        this.electionManager = electionManager;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(config.electionPort());
            System.out.println("Election Listener started on port " + config.electionPort());

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                BrokerElectionHandler handler = new BrokerElectionHandler(socket, electionManager, config);
                threadFactory.newThread(handler).start();
            }
        } catch (SocketException e) {
            if (serverSocket != null && serverSocket.isClosed()) {
                System.out.println("Election Listener: Server socket closed, shutting down.");
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
