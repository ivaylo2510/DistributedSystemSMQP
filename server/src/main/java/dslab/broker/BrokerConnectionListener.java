package dslab.broker;


import dslab.config.BrokerConfig;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

public class BrokerConnectionListener implements Runnable {

    private ServerSocket socket;
    private final ThreadFactory threadFactory = Thread.ofVirtual().factory();
    private final BrokerConfig config;
    private final ConcurrentHashMap<String, Exchange> exchanges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BrokerQueue> queues = new ConcurrentHashMap<>();
    private final DefaultExchange defaultExchange = new DefaultExchange("");
    private DatagramSocket monitoringSocket;
    private String brokerHostPort;

    public BrokerConnectionListener(BrokerConfig config) {
        this.config = config;
        exchanges.put(defaultExchange.getName(), defaultExchange);

        // Only attempt to connect if the port is valid (> 0)
        if (config.monitoringPort() > 0) {
            try {
                this.monitoringSocket = new DatagramSocket();
                this.monitoringSocket.connect(
                        InetAddress.getByName(config.monitoringHost()),
                        config.monitoringPort()
                );
            } catch (SocketException | UnknownHostException | IllegalArgumentException e) {
                // Log but DO NOT crash. Monitoring is optional/secondary.
                System.err.println("Warning: Failed to setup monitoring socket: " + e.getMessage());
                this.monitoringSocket = null;
            }
        } else {
            // Monitoring disabled or not configured
            this.monitoringSocket = null;
        }
    }


    @Override
    public void run() {
        try {
            socket = new ServerSocket(config.port());
            this.brokerHostPort = config.host() + ":" + config.port();
            while (!socket.isClosed()) {
                Socket conn = socket.accept();

                BrokerConnectionHandler handler = new BrokerConnectionHandler(
                        conn,
                        exchanges,
                        queues,
                        defaultExchange,
                        monitoringSocket,
                        brokerHostPort
                );

                Thread t = threadFactory.newThread(handler);
                t.start();
            }

        }catch (SocketException e) {
            if (socket != null && socket.isClosed()) {
                System.out.println("BrokerConnectionListener: Server socket closed, shutting down.");
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
