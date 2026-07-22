package dslab.broker;

import dslab.config.BrokerConfig;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplified Raft leader election.
 *
 * <p>A candidate votes for itself and requests votes from all peers by sending
 * {@code elect <myId>}. Each peer replies with {@code vote <sender-id> <candidate-id>}. A peer
 * grants its vote to the first candidate it hears from (first-come-first-serve) and reports that
 * same choice for any further requests until a leader is declared. If the candidate gathers a
 * majority of votes it declares itself leader. As per the assignment, term handling is reduced:
 * leader declarations are always accepted.</p>
 */
public class RaftElection extends BaseElection {

    // Candidate this broker has voted for in the current election round (-1 = not voted yet).
    private final AtomicInteger votedFor = new AtomicInteger(-1);

    public RaftElection(BrokerConfig config) {
        super(config);
    }

    @Override
    public void initiateElection() {
        if (stopped) return;

        int myId = config.electionId();
        votedFor.set(myId); // vote for self
        int votes = 1;
        int totalNodes = config.electionPeerIds().length + 1;

        for (int i = 0; i < config.electionPeerIds().length; i++) {
            String response = sendMessageAndReadResponse(
                    config.electionPeerHosts()[i], config.electionPeerPorts()[i], "elect " + myId);

            if (response == null) continue;

            String[] parts = response.trim().split("\\s+");
            if (parts.length == 3 && "vote".equals(parts[0])) {
                try {
                    if (Integer.parseInt(parts[2]) == myId) votes++;
                } catch (NumberFormatException ignored) {
                    // malformed vote -> not counted
                }
            }
        }

        if (votes > totalNodes / 2) {
            becomeLeader();
        } else {
            // Did not win -> allow voting for another candidate and try again later.
            votedFor.set(-1);
            resetHeartbeatTimer();
        }
    }

    @Override
    public void handleElect(int candidateId, PrintWriter out) {
        // Grant the vote to the first candidate seen this round; report the choice otherwise.
        votedFor.compareAndSet(-1, candidateId);
        out.println("vote " + config.electionId() + " " + votedFor.get());
        resetHeartbeatTimer();
    }

    @Override
    public void handleDeclare(int leaderId, PrintWriter out) {
        currentLeaderId.set(leaderId);
        votedFor.set(-1); // new leader established -> reset for the next election round
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
