package dslab.dns;

import dslab.IServer;

public interface IDNSServer extends IServer {

    /**
     * Implement the main logic of the DNS server.
     * This method should start the DNS server and listen for incoming DNS requests.
     * It is recommended to create separate classes for the broker logic to keep the code clean and structured.
     */
    @Override
    void run();

    /**
     * Implement a graceful shutdown of the DNS server.
     * That means all system resources should be released and running threads should be stopped gracefully.
     */
    @Override
    void shutdown();
}
