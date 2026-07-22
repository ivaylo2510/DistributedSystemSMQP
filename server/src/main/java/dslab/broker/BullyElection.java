package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.PrintWriter;

/**
 * Bully leader election.
 *
 * <p>On timeout (or upon receiving an {@code elect} from a lower id) a broker sends
 * {@code elect <myId>} to all peers with a strictly higher id. If none of them answers with
 * {@code ok}, the broker wins and declares itself leader to all peers. If a higher-id peer
 * responds, the broker defers and lets the higher ids continue the election.</p>
 */
public class BullyElection extends BaseElection {

    public BullyElection(BrokerConfig config) {
        super(config);
    }

    @Override
    public void initiateElection() {
        if (stopped) return;

        int myId = config.electionId();
        boolean higherResponded = false;

        for (int i = 0; i < config.electionPeerIds().length; i++) {
            if (config.electionPeerIds()[i] <= myId) continue;

            String response = sendMessageAndReadResponse(
                    config.electionPeerHosts()[i], config.electionPeerPorts()[i], "elect " + myId);

            if (response != null && "ok".equals(response.trim())) {
                higherResponded = true;
            }
        }

        if (higherResponded) {
            // A higher id took over the election -> wait for its declare / heartbeats.
            resetHeartbeatTimer();
        } else {
            becomeLeader();
        }
    }

    @Override
    public void handleElect(int candidateId, PrintWriter out) {
        // Always acknowledge the election request.
        out.println("ok");

        // A lower id started an election -> bully it by starting our own.
        if (candidateId < config.electionId() && !stopped) {
            new Thread(this::initiateElection).start();
        }
    }

    @Override
    public void handleDeclare(int leaderId, PrintWriter out) {
        currentLeaderId.set(leaderId);
        out.println("ack " + config.electionId());
        resetHeartbeatTimer();
    }

    private void becomeLeader() {
        if (stopped) return;

        int myId = config.electionId();
        currentLeaderId.set(myId);

        for (int i = 0; i < config.electionPeerIds().length; i++) {
            sendMessageAndReadResponse(
                    config.electionPeerHosts()[i], config.electionPeerPorts()[i], "declare " + myId);
        }

        startLeaderHeartbeats();
        registerLeaderWithDNS();
    }
}
