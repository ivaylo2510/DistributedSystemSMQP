package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BrokerElectionHandler implements Runnable {

    private final Socket socket;
    private final ElectionManager electionManager;
    private final BrokerConfig config;

    public BrokerElectionHandler(Socket socket, ElectionManager electionManager, BrokerConfig config) {
        this.socket = socket;
        this.electionManager = electionManager;
        this.config = config;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // LEP Protocol Greeting
            out.println("ok LEP");

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");

                if (parts.length == 0) continue;

                String command = parts[0];

                try {
                    switch (command) {
                        case "elect":
                            if (parts.length != 2) {
                                out.println("error usage: elect <id>");
                            } else {
                                electionManager.handleElect(Integer.parseInt(parts[1]), out);
                            }
                            break;

                        case "declare":
                            if (parts.length != 2) {
                                out.println("error usage: declare <id>");
                            } else {
                                electionManager.handleDeclare(Integer.parseInt(parts[1]), out);
                            }
                            break;

                        case "vote":
                            if (parts.length != 3) {
                                out.println("error usage: vote <sender-id> <candidate-id>");
                            } else {
                                electionManager.handleVote(
                                        Integer.parseInt(parts[1]),
                                        Integer.parseInt(parts[2]),
                                        out
                                );
                            }
                            break;

                        case "ping":
                            electionManager.handlePing(out);
                            break;

                        default:
                            out.println("error protocol error");
                            return;
                    }
                } catch (NumberFormatException e) {
                    out.println("error invalid id format");
                } catch (Exception e) {
                    out.println("error " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Connection closed
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
