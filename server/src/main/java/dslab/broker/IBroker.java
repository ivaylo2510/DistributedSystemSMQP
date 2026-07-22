package dslab.broker;

import dslab.IServer;


/**
 * Interface for the Message Broker server.
 * <p>
 * NOTE: Do not delete this interface as it is part of the test suite and required for this assignment.
 */
public interface IBroker extends IServer {

    /**
     * Implement the logic of the Message Broker server starting from this method.
     * It is recommended to create separate classes for the broker logic to keep the code clean and structured.
     */
    @Override
    void run();

    /**
     * Retrieves the unique identifier of this broker. The ID is used for the leader election process.
     *
     * @return the ID of this broker.
     * @see #initiateElection()
     */
    int getId();

    /**
     * Immediately initiates the leader election process on invocation. The broker will participate
     * in the election to determine the new leader among all other peers of the pool.
     */
    void initiateElection();

    /**
     * Retrieves the ID of the currently elected leader in the pool. If no leader has been elected yet,
     * the method should return -1.
     *
     * @return the positive ID of the elected leader, or -1 if no leader has been elected.
     */
    int getLeader();

    /**
     * Implement a graceful shutdown of the Message Broker server.
     */
    @Override
    void shutdown();
}