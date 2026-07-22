package dslab.broker;

import java.io.PrintWriter;

public interface ElectionManager {
    void handleElect(int id, PrintWriter out);
    void handleDeclare(int id, PrintWriter out);
    void handlePing(PrintWriter out);
    void handleVote(int senderId, int candidateId, PrintWriter out);

    // For manual triggering (e.g. from tests or timeouts)
    void initiateElection();
    int getLeaderId();
    void start(); // Start timers/threads
    void stop();  // Cleanup
}
