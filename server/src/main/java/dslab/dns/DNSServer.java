package dslab.dns;

import dslab.ComponentFactory;
import dslab.config.DNSServerConfig;

import java.util.HashMap;
import java.util.Map; // Import
import java.util.concurrent.ConcurrentHashMap; // Import
import java.util.concurrent.Semaphore;

public class DNSServer implements IDNSServer {

    private final DNSServerConfig config;
    private final Map<String, String> records;
    private final Semaphore mapLock;

    private DNSConnectionListener listener;
    private Thread listenerThread;

    public DNSServer(DNSServerConfig config) {
        this.config = config;

        this.records = new ConcurrentHashMap<>();

        this.mapLock = new Semaphore(1);

    }

    @Override
    public void shutdown() {
        System.out.println("DNSServer shutting down...");


        if (listener != null) {
            listener.stop();
        }

        if (listenerThread != null) {
            try {
                listenerThread.join();
            } catch (InterruptedException e) {
                System.err.println("Interrupted during shutdown");
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("DNSServer shutdown complete.");
    }

    @Override
    public void run() {
        int port = config.port();

        listener = new DNSConnectionListener(port, records , mapLock);

        listenerThread = new Thread(listener);
        listenerThread.start();

        System.out.println("DNSServer started on port " + port);
    }

    public static void main(String[] args) {
        ComponentFactory.createDNSServer(args[0]).run();
    }
}