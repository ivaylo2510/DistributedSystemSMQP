package dslab.cli;

import dslab.client.IClient;
import dslab.config.Config;
import dslab.connection.Channel;
import dslab.connection.Subscription;
import dslab.connection.types.ExchangeType;

import java.io.*;

public class ClientCLI implements IClientCLI {

    private final IClient client;
    private final Config config;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private Channel channel;
    private Thread subscriptionThread;

    public ClientCLI(IClient client, Config config, InputStream in, OutputStream out) {
        this.client = client;
        this.config = config;
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.writer = new PrintWriter(out, true);
    }

    @Override
    public void run() {
        while (true) {
            printPrompt();
            try {
                String line = reader.readLine();
                if (line == null) break;  // EOF (Ctrl+D)
                if (line.isBlank()) continue;

                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();

                switch (cmd) {
                    case "channel" -> handleChannel(parts);
                    case "subscribe" -> handleSubscribe(parts);
                    case "publish" -> handlePublish(parts);
                    case "shutdown" -> {
                        handleShutdown(parts);
                    }
                    default -> {
                        writer.println("error");
                    }
                }

            } catch (Exception e) {
                writer.println("error");
            }
        }
    }

    @Override
    public void printPrompt() {
        writer.print(client.getComponentId() + "> ");
        writer.flush();
    }

    /**
     * A channel is an instance which is used to multiplex connections on a single TCP connection.
     * Please read the documentation of the {@link Channel} class for more information.
     * Attention should be paid if the channel is already connected to a broker, it should be disconnected first.
     *
     * @param broker the broker to which the channel should be created
     * @return the channel to which a connection is established with the specified broker
     */
    private Channel createChannel(String broker) {
        try {
            String host = config.getString(broker + ".host");
            int port = config.getInt(broker + ".port");

            Channel newChannel = new Channel(host, port);
            if (newChannel.connect()) {
                return newChannel;
            }
        } catch (Exception e) {
            throw new IllegalStateException();
        }
        return null;
    }

    private void handleChannel(String[] parts) throws IOException {
        if (parts.length != 2) {
            writer.println("error");
            return;
        }

        String broker = parts[1];

        // Disconnect old channel if needed
        if (channel != null) {
            channel.disconnect();
        }

        channel = createChannel(broker);
        writer.println(channel != null ? "ok" : "error");
    }

    private void handleSubscribe(String[] parts) {
        if (parts.length != 5 || channel == null) {
            throw new IllegalStateException();
        }

        String exchange = parts[1];
        String type = parts[2];
        String queue = parts[3];
        String binding = parts[4];

        try {
            boolean okExchange = channel.exchangeDeclare(ExchangeType.valueOf(type.toUpperCase()), exchange);
            boolean okQueueBind = channel.queueBind(queue, binding);

            if (!okExchange || !okQueueBind) {
                throw new IllegalStateException();
            }

            // Start background subscription
            subscriptionThread = channel.subscribe(writer::println);

            // ✅ Let test/user know subscription is active
            writer.println("ok");
            printPrompt();

            // ✅ Wait for user (or test) input to end subscription
            reader.readLine();// blocks until test sends termination line

            // ✅ Stop the background listener safely
            if (subscriptionThread.isAlive()) {
                ((Subscription) subscriptionThread).kill();
            }

            writer.println("ok");
            printPrompt();

        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }


    private void handlePublish(String[] parts) throws IOException {
        if (parts.length != 5 || channel == null) {
            throw new IllegalStateException();
        }

        String exchange = parts[1];
        String type = parts[2];
        String routingKey = parts[3];
        String message = parts[4];


        boolean ok = channel.exchangeDeclare(ExchangeType.valueOf(type.toUpperCase()), exchange)
                && channel.publish(routingKey, message);
        if (ok) {
            writer.println("ok");
        } else {
            throw new IllegalStateException();
        }

    }

    private void handleShutdown(String[] parts) throws IOException {
        if (parts.length != 1) {
            throw new IllegalStateException();
        }

        if (channel != null) {
            channel.disconnect();
        }

        client.shutdown();
        writer.println("ok");
    }


}
