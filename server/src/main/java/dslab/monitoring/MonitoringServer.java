package dslab.monitoring;

import dslab.ComponentFactory;
import dslab.config.MonitoringServerConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitoringServer implements IMonitoringServer {

    private final MonitoringServerConfig config;
    private DatagramSocket socket;
    private volatile boolean running = false;

    // Track total messages received
    private final AtomicInteger totalMessages = new AtomicInteger(0);

    // Store statistics: Map<"host:port", Map<"routing-key", count>>
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> statistics =
            new ConcurrentHashMap<>();

    public MonitoringServer(MonitoringServerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(config.monitoringPort());
            running = true;
            System.out.println("MonitoringServer started on UDP port " + config.monitoringPort());

            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet);

                    // Extract the message
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();

                    // Process the message
                    processMessage(message);

                } catch (SocketException e) {
                    if (!running) {
                        // Socket was closed during shutdown
                        break;
                    }
                    throw e;
                }
            }

        } catch (SocketException e) {
            if (!running) {
                System.out.println("MonitoringServer socket closed during shutdown.");
            } else {
                System.err.println("MonitoringServer socket error: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("MonitoringServer IO error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("MonitoringServer stopped.");
        }
    }

    /**
     * Process incoming UDP message in format: <ip>:<port> <routing-key>
     */
    private void processMessage(String message) {
        // Expected format: "194.232.104.142:16501 austria.vienna"
        String[] parts = message.split(" ", 2);

        if (parts.length != 2) {
            // Invalid format, discard
            return;
        }

        String hostPort = parts[0];
        String routingKey = parts[1];

        // Validate format: should have at least one colon for host:port
        if (!hostPort.contains(":")) {
            // Invalid format, discard
            return;
        }

        // Increment total message count
        totalMessages.incrementAndGet();

        // Update statistics
        statistics.computeIfAbsent(hostPort, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(routingKey, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    @Override
    public int receivedMessages() {
        return totalMessages.get();
    }

    @Override
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, ConcurrentHashMap<String, AtomicInteger>> serverEntry : statistics.entrySet()) {
            String hostPort = serverEntry.getKey();
            Map<String, AtomicInteger> routingKeys = serverEntry.getValue();

            sb.append("Server ").append(hostPort).append("\n");

            for (Map.Entry<String, AtomicInteger> routingEntry : routingKeys.entrySet()) {
                String routingKey = routingEntry.getKey();
                int count = routingEntry.getValue().get();

                sb.append(routingKey).append(" ").append(count).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public void shutdown() {
        System.out.println("MonitoringServer shutting down...");
        running = false;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        ComponentFactory.createMonitoringServer(args[0]).run();
    }
}
