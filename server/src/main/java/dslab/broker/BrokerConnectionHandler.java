package dslab.broker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Handles all communication with a single connected client.
 * This class is stateful and keeps track of the client's
 * selected exchange and queue.
 */
public class BrokerConnectionHandler implements Runnable {

    private final Socket socket;


    private final ConcurrentHashMap<String, Exchange> allExchanges;
    private final ConcurrentHashMap<String, BrokerQueue> allQueues;
    private final DefaultExchange defaultExchange;


    private Exchange selectedExchange = null;
    private BrokerQueue selectedQueue = null;


    private Thread subscriptionThread = null;

    private DatagramSocket monitoringSocket;
    private String brokerHostPort;

    /**
     * Constructor to inject the shared broker state.
     */
    public BrokerConnectionHandler(Socket socket,
                                   ConcurrentHashMap<String, Exchange> exchanges,
                                   ConcurrentHashMap<String, BrokerQueue> queues,
                                   DefaultExchange defaultExchange,
                                   DatagramSocket monitoringSocket,
                                   String brokerHostPort) {
        this.socket = socket;
        this.allExchanges = exchanges;
        this.allQueues = queues;
        this.defaultExchange = defaultExchange;
        this.monitoringSocket = monitoringSocket;
        this.brokerHostPort = brokerHostPort;
    }

    @Override
    public void run() {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("ok SMQP");

            String line;
            loop: while ((line = in.readLine()) != null) {

                String[] parts = line.split(" ", 3);

                try {
                    switch (parts[0]) {
                        case "exchange":
                            handleExchange(parts, out);
                            break;
                        case "queue":
                            handleQueue(parts, out);
                            break;
                        case "bind":
                            handleBind(parts, out);
                            break;
                        case "publish":
                            handlePublish(parts, out);
                            break;
                        case "subscribe":
                            handleSubscribe(parts, out);
                            break;
                        case "exit":
                            if (parts.length != 1) {
                                out.println("error invalid arguments");
                            } else {
                                out.println("ok bye");
                                break loop;
                            }
                            break;
                        case "stop":
                            handleStop(parts, out);
                            break;

                        default:
                            out.println("error unknown command");
                    }
                } catch (Exception e) {

                    out.println("error " + e.getMessage());
                }
            }

        } catch (IOException e) {

            System.out.println("Client handler IO error: " + e.getMessage());
        } finally {

            if (subscriptionThread != null) {
                subscriptionThread.interrupt();
            }

            closeSocket();

        }
    }

    private void handleExchange(String[] parts, PrintWriter out) {
        if (parts.length != 3 ) {
            out.println("error usage: exchange <type> <name>");
            return;
        }

        if(!parts[1].equals("default")) {
            if(parts[2].equals("default")) {
                out.println("error usage: exchange <type> <name>");
                return;
            }
        }
        String type = parts[1];
        String name = parts[2];

        if (type.equals("default")) {
            this.selectedExchange = this.defaultExchange;
            out.println("ok");
            return;
        }

        if (!type.equals("fanout") && !type.equals("topic") && !type.equals("direct")) {
            out.println("error unknown exchange type: " + type);
            return;
        }

        this.selectedExchange = allExchanges.computeIfAbsent(name, k -> {
            if (type.equals("fanout")) {
                return new FanoutExchange(k);
            }
            if (type.equals("topic")) {
                return new TopicExchange(k);
            }
            return new DirectExchange(k);
        });

        out.println("ok");
    }

    private void handleQueue(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("error usage: queue <name>");
            return;
        }
        String name = parts[1];


        this.selectedQueue = allQueues.computeIfAbsent(name, k -> {

            BrokerQueue newQueue = new BrokerQueue();
            defaultExchange.bind(newQueue, k);

            return newQueue;
        });

        out.println("ok");
    }

    private void handleBind(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("error usage: bind <bindingKey>");
            return;
        }

        if (selectedExchange == null) {
            out.println("error no exchange selected");
            return;
        }
        if (selectedQueue == null) {
            out.println("error no queue selected");
            return;
        }

        String bindingKey = parts[1];
        selectedExchange.bind(selectedQueue, bindingKey);
        out.println("ok");
    }

    private void handlePublish(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("error usage: publish <routingKey> <message>");
            return;
        }
        if (selectedExchange == null) {
            out.println("error no exchange selected");
            return;
        }

        String routingKey = parts[1];
        String message = parts[2];
        selectedExchange.publish(routingKey, message);

        // **ADD THIS**: Send monitoring data via UDP
        sendMonitoringData(routingKey);

        out.println("ok");
    }

    // **ADD THIS METHOD** to your BrokerConnectionHandler class
    private void sendMonitoringData(String routingKey) {
        if (monitoringSocket == null || monitoringSocket.isClosed()) return;

        try {
            String monitoringMessage = brokerHostPort + " " + routingKey;
            byte[] data = monitoringMessage.getBytes();

            // Constructor doesn't need address/port because socket is connected!
            DatagramPacket packet = new DatagramPacket(data, data.length);

            monitoringSocket.send(packet);

        } catch (Exception e) {
            // ignore
        }
    }


    private void handleSubscribe(String[] parts, PrintWriter out) {
        if (parts.length != 1) {
            out.println("error usage: subscribe");
            return;
        }
        if (selectedQueue == null) {
            out.println("error no queue selected");
            return;
        }
        if (subscriptionThread != null) {
            out.println("error already subscribed");
            return;
        }


        final BrokerQueue queueToWatch = this.selectedQueue;
        final PrintWriter clientWriter = out;

        this.subscriptionThread = new Thread(() -> {
            try {

                while (!subscriptionThread.isInterrupted()) {
                    String msg = queueToWatch.blockingGet();
                    clientWriter.println(msg);
                }
            } catch (InterruptedException e) {
                System.out.println("Subscription thread for " + socket.getPort() + " stopped.");
            }
        });

        this.subscriptionThread.start();
        out.println("ok");
    }

    private void handleStop(String[] parts, PrintWriter out) {
        if (parts.length != 1) {
            out.println("error usage: stop");
            return;
        }

        if (subscriptionThread != null) {

            subscriptionThread.interrupt();
            try {
                subscriptionThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            subscriptionThread = null;
            out.println("ok");
        } else {
            out.println("error not subscribed");
        }
    }


    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}