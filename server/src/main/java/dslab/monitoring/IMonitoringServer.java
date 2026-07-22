package dslab.monitoring;

import dslab.IServer;

public interface IMonitoringServer extends IServer {

    /**
     * Retrieves the total number of messages received by the Monitoring Server..
     *
     * @return the total count of messages.
     */
    int receivedMessages();

    /**
     * Provides a detailed representation of the message statistics collected by the Monitoring Server..
     * The statistics include a breakdown of each Message Server, identified by its hostname and port,
     * along with the routing keys and their respective published messages count.
     * <p>
     * The String should be in the following format:<br>
     * Server &lt;hostname&gt;:&lt;port&gt;<br>
     * &nbsp;&lt;routing-key&gt; &lt;count&gt;<br>
     * &nbsp;&lt;routing-key&gt; &lt;count&gt;<br>
     * Server &lt;hostname&gt;:&lt;port&gt;<br>
     * &nbsp;&lt;routing-key&gt; &lt;count&gt;<br>
     * ...
     * <br>
     * <br>
     * Example:<br>
     * Server 194.232.104.142:16501<br>
     * &nbsp;austria.vienna 27<br>
     * &nbsp;austria.linz 3<br>
     * Server 142.251.36.238:16503<br>
     * &nbsp;usa.texas.houston 5099<br>
     * ...
     * <br>
     *
     * @return a formatted string representing the number of messages received for each routing-key by each Message Broker.
     */
    String getStatistics();
}
