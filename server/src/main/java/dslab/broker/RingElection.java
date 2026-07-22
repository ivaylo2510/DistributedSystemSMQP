package dslab.broker;

import dslab.config.BrokerConfig;
import java.io.PrintWriter;

public class RingElection extends BaseElection {

    public RingElection(BrokerConfig config) {
        super(config);
    }

    @Override
    public void initiateElection() {
        if (stopped) return;
        int myId = config.electionId();
        if (!sendToNeighbor("elect " + myId)) {
            becomeLeader();
        } else {
            resetHeartbeatTimer();
        }
    }

    @Override
    public void handleElect(int candidateId, PrintWriter out) {
        int myId = config.electionId();
        if (candidateId > myId) {
            sendToNeighbor("elect " + candidateId);
        } else if (candidateId < myId) {
            sendToNeighbor("elect " + myId);
        } else {
            becomeLeader();
        }
        resetHeartbeatTimer();
        out.println("ok");
    }

    @Override
    public void handleDeclare(int winnerId, PrintWriter out) {
        int myId = config.electionId();
        currentLeaderId.set(winnerId);

        if (winnerId != myId) {
            sendToNeighbor("declare " + winnerId);
            resetHeartbeatTimer();
        } else {
            startLeaderHeartbeats();
        }
        out.println("ack " + myId);
    }

    private void becomeLeader() {
        int myId = config.electionId();
        currentLeaderId.set(myId);
        sendToNeighbor("declare " + myId);
        startLeaderHeartbeats();

        // Use the base class method with your signature style
        registerLeaderWithDNS();
    }
}
