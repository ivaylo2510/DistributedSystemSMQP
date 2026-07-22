package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseElection implements ElectionManager {

    protected final BrokerConfig config;
    protected final AtomicInteger currentLeaderId = new AtomicInteger(-1);
    protected Timer heartbeatTimer;
    protected Timer leaderHeartbeatTimer;

    // Set once the election manager is stopped. Guards against timer tasks
    // re-arming new timers after shutdown, which would leak threads and send
    // stray election messages to ports later reused by other brokers/tests.
    protected volatile boolean stopped = false;

    public BaseElection(BrokerConfig config) {
        this.config = config;
    }

    // ... (getLeaderId, start, stop, handlePing, handleVote, resetHeartbeatTimer, etc. - SAME AS BEFORE) ...

    @Override
    public int getLeaderId() {
        return currentLeaderId.get();
    }

    @Override
    public void start() {
        resetHeartbeatTimer();
    }

    @Override
    public void stop() {
        stopped = true;
        if (heartbeatTimer != null) heartbeatTimer.cancel();
        if (leaderHeartbeatTimer != null) leaderHeartbeatTimer.cancel();
    }

    @Override
    public void handlePing(PrintWriter out) {
        resetHeartbeatTimer();
        out.println("pong");
    }

    @Override
    public void handleVote(int senderId, int candidateId, PrintWriter out) {
        out.println("error command not supported");
    }

    protected synchronized void resetHeartbeatTimer() {
        if (stopped) return;
        if (heartbeatTimer != null) heartbeatTimer.cancel();
        heartbeatTimer = new Timer(true);
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (stopped) return;
                System.out.println("Heartbeat timeout! Initiating election...");
                initiateElection();
            }
        }, config.electionHeartbeatTimeoutMs());
    }

    protected synchronized void startLeaderHeartbeats() {
        if (stopped) return;
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
        if (leaderHeartbeatTimer != null) leaderHeartbeatTimer.cancel();

        leaderHeartbeatTimer = new Timer(true);
        leaderHeartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (stopped) return;
                sendPingToAll();
            }
        }, 0, config.electionHeartbeatTimeoutMs() / 2);
    }

    protected boolean sendToNeighbor(String message) {
        for (int i = 0; i < config.electionPeerIds().length; i++) {
            String host = config.electionPeerHosts()[i];
            int port = config.electionPeerPorts()[i];
            try (Socket s = new Socket(host, port);
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                if ("ok LEP".equals(in.readLine())) {
                    out.println(message);
                    return true;
                }
            } catch (IOException e) { /* ignore */ }
        }
        return false;
    }

    /**
     * Opens a short-lived connection to a single peer, sends {@code message} and returns the
     * peer's response line (after the {@code ok LEP} greeting). Returns {@code null} if the peer
     * is unreachable or does not speak LEP.
     */
    protected String sendMessageAndReadResponse(String host, int port, String message) {
        try (Socket s = new Socket(host, port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            if (!"ok LEP".equals(in.readLine())) return null;
            out.println(message);
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    protected void sendPingToAll() {
        for (int i = 0; i < config.electionPeerIds().length; i++) {
            String host = config.electionPeerHosts()[i];
            int port = config.electionPeerPorts()[i];
            try (Socket s = new Socket(host, port);
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                if ("ok LEP".equals(in.readLine())) out.println("ping");
            } catch (IOException e) { /* ignore */ }
        }
    }

    // --- YOUR SIGNATURE DNS CONNECTION STYLE ---

    protected void registerLeaderWithDNS() {
        new Thread(() -> {
            try {
                ConnectionToDNS connection = connectToDNS();
                if (connection != null) {
                    connection.out.println("register " + config.electionDomain() + " " + config.host() + ":" + config.port());
                    connection.out.flush();

                    String response = connection.in.readLine();
                    if ("ok".equals(response)) {
                        System.out.println("Registered leader with DNS: " + config.electionDomain());
                    }
                    disconnectFromDNS(connection);
                }
            } catch (IOException e) {
                System.err.println("DNS registration failed: " + e.getMessage());
            }
        }).start();
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

    protected record ConnectionToDNS(Socket toDNS, BufferedReader in, PrintWriter out) {}
}
