package dslab.mock;

import dslab.config.Config;
import dslab.util.streams.TestOutputStream;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Mocks a message broker for testing purposes. Response is always "ok" except on client connection establishment.
 */
public final class MessageBrokerServer extends Thread implements AutoCloseable {

    private final Config config;
    private final TestOutputStream logs;
    private final Set<Socket> sockets = new HashSet<>();
    private final Set<Socket> subscribers = new HashSet<>();

    private ServerSocket serverSocket;

    public MessageBrokerServer(Config config) {
        this.config = config;
        this.logs = new TestOutputStream();
        this.start();
    }

    @Override
    public void run() {

        try {
            serverSocket = new ServerSocket(this.config.getInt("broker-0.port"));

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                sockets.add(socket);
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            // Ignored
        }
        close();
    }

    private void logConnection() {
        try {
            logs.write("CONNECTED".getBytes());
            logs.flush();
        } catch (IOException e) {
            // Ignored
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter socketOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            logConnection();

            socketOut.write("ok SMQP\n");
            socketOut.flush();

            String line;
            while ((line = socketIn.readLine()) != null) {
                logs.write(line.getBytes());
                logs.flush();

                if (line.startsWith("subscribe")) {
                    subscribers.add(socket);
                } else if (line.startsWith("publish")) {
                    String[] parts = line.split(" ");

                    if (parts.length != 3) continue;

                    for (Socket s : subscribers) {
                        if (s.isClosed()) continue;

                        s.getOutputStream().write("%s\n".formatted(parts[2]).getBytes());
                        s.getOutputStream().flush();
                    }
                } else if (line.startsWith("exit")) {
                    socketOut.write("ok bye\n");
                    socketOut.flush();
                    sockets.remove(socket);
                    subscribers.remove(socket);
                    socket.close();
                    break;
                }

                socketOut.write("ok\n");
                socketOut.flush();
            }
        } catch (IOException e) {
            // Ignored
        }
    }

    @Override
    public void close() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) {
            // Ignored
        }

        for (Socket socket : sockets) {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                // Ignored
            }
        }
    }

    public TestOutputStream getLogs() {
        return logs;
    }
}
