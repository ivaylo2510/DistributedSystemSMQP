package dslab.broker;

import dslab.ComponentFactory;
import dslab.broker.BrokerElectionListener;
import dslab.broker.ElectionManager;
import dslab.broker.RingElection;
import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public class Broker implements IBroker {

    private final BrokerConfig config;

    private BrokerConnectionListener smqpListener;
    private Thread smqpListenerThread;

    private BrokerElectionListener electionListener;
    private Thread electionListenerThread;

    private ElectionManager electionManager;

    public Broker(BrokerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        // 1. Start SMQP Listener (message broker functionality)
        smqpListener = new BrokerConnectionListener(config);
        smqpListenerThread = new Thread(smqpListener);
        smqpListenerThread.start();
        System.out.println("Broker SMQP started on port " + config.port());

        // 2. Handle election based on type
        String electionType = config.electionType().toLowerCase();

        if ("none".equals(electionType)) {
            // Assignment 2 behavior: Register immediately with DNS
            registerWithDNS();
        } else {
            // Assignment 3 behavior: Start election system
            initializeElection(electionType);
        }
    }

    private void initializeElection(String electionType) {
        switch (electionType) {
            case "ring":
                electionManager = new RingElection(config);
                break;
            case "bully":
                electionManager = new BullyElection(config);
                break;
            case "raft":
                electionManager = new RaftElection(config);
                break;
            default:
                System.err.println("Unknown election type: " + electionType);
                return;
        }

        // Start the election listener
        electionListener = new BrokerElectionListener(config, electionManager);
        electionListenerThread = new Thread(electionListener);
        electionListenerThread.start();

        // Start the election manager (timers, etc.)
        electionManager.start();

        System.out.println("Election system initialized: " + electionType);
    }

    @Override
    public void shutdown() {
        System.out.println("Broker shutting down...");

        // Stop election system
        if (electionManager != null) {
            electionManager.stop();
        }
        if (electionListener != null) {
            electionListener.stop();
        }

        // Unregister from DNS
        unregisterFromDNS();

        // Stop SMQP listener
        if (smqpListener != null) {
            smqpListener.stop();
        }

        // Wait for threads
        try {
            if (electionListenerThread != null && electionListenerThread.isAlive()) {
                electionListenerThread.join();
            }
            if (smqpListenerThread != null && smqpListenerThread.isAlive()) {
                smqpListenerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Broker shutdown complete.");
    }

    @Override
    public int getId() {
        return config.electionId();
    }

    @Override
    public void initiateElection() {
        if (electionManager != null) {
            electionManager.initiateElection();
        }
    }

    @Override
    public int getLeader() {
        if (electionManager != null) {
            return electionManager.getLeaderId();
        }
        return -1;
    }

    private void registerWithDNS() {
        try {
            ConnectionToDNS connection = connectToDNS();
            if (connection == null) {
                System.err.println("Failed to connect to DNS");
                return;
            }
            connection.out.println("register " + config.domain() + " " + config.host() + ":" + config.port());
            connection.out.flush();
            if (!Objects.equals(connection.in.readLine(), "ok")) {
                System.err.println("DNS registration failed");
            } else {
                System.out.println("Registered with DNS: " + config.domain());
            }
            disconnectFromDNS(connection);
        } catch (Exception e) {
            System.err.println("DNS registration error: " + e.getMessage());
        }
    }

    private void unregisterFromDNS() {
        try {
            ConnectionToDNS connection = connectToDNS();
            if (connection == null) return;

            connection.out.println("unregister " + config.domain());
            connection.out.flush();
            disconnectFromDNS(connection);
        } catch (IOException e) {
            System.err.println("DNS unregistration error: " + e.getMessage());
        }
    }

    private ConnectionToDNS connectToDNS() throws IOException {
        Socket toDNS = new Socket(config.dnsHost(), config.dnsPort());
        BufferedReader in = new BufferedReader(new InputStreamReader(toDNS.getInputStream()));
        PrintWriter out = new PrintWriter(toDNS.getOutputStream(), true);
        String greeting = in.readLine();
        return "ok SDP".equals(greeting) ? new ConnectionToDNS(toDNS, in, out) : null;
    }

    private void disconnectFromDNS(ConnectionToDNS connection) throws IOException {
        connection.in.close();
        connection.out.close();
        connection.toDNS.close();
    }

    private record ConnectionToDNS(Socket toDNS, BufferedReader in, PrintWriter out) {}

    public static void main(String[] args) {
        ComponentFactory.createBroker(args[0]).run();
    }
}
