package dslab.assignment3.monitoring;

import dslab.ComponentFactory;
import dslab.config.MonitoringServerConfig;
import dslab.monitoring.IMonitoringServer;
import dslab.util.ConfigFactory;
import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.Util;
import dslab.util.grading.LocalGradingExtension;
import dslab.util.grading.annotations.GitHubClassroomGrading;
import dslab.util.helper.UdpClientHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static dslab.util.monitoring.MonitoringUtil.waitForMonitoringServerToUpdateDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the {@link IMonitoringServer} component using the UDP protocol.
 *
 * <p>This test class verifies the correct behavior of the monitoring server when receiving
 * messages from different hosts over the UDP protocol.</p>
 *
 * <p>Tests involve creating a monitoring server instance, sending messages to it, and
 * checking that the server correctly records the messages and maintains the expected statistics.</p>
 */
@ExtendWith(LocalGradingExtension.class)
public class MonitoringServerProtocolTest {

    private final MonitoringServerConfig config = ConfigFactory.createMonitoringServerConfig();

    private IMonitoringServer monitoringServer;
    private Thread monitoringServerThread;

    private UdpClientHelper helper;

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @BeforeEach
    public void beforeEach() throws IOException {
        monitoringServer = ComponentFactory.createMonitoringServer(config);

        // Run the monitoring server in a background thread since it blocks the test process
        monitoringServerThread = new Thread(monitoringServer);
        monitoringServerThread.start();

        // Setup helper for tcp communication
        helper = new UdpClientHelper(Constants.LOCALHOST, config.monitoringPort());

        await()
                .atMost(1, TimeUnit.SECONDS)                // Maximum wait time (adjust as needed)
                .pollInterval(10, TimeUnit.MILLISECONDS)   // Poll every 50 milliseconds
                .until(() -> Util.isUdpPortListening(Constants.LOCALHOST, config.monitoringPort()));
    }

    @AfterEach
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void afterEach() {
        helper.disconnect();

        if (monitoringServer != null) monitoringServer.shutdown();

        try {
            // Wait for the monitoring server thread to shut down
            if (monitoringServerThread != null && monitoringServerThread.isAlive()) {
                monitoringServerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @GitHubClassroomGrading(maxScore = 10)
    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void monitoring_sendingMessages_returnsCorrectStatistics() throws IOException {
        final int numOfMsg = 5;
        TreeSet<Integer> ports = Global.SECURE_INT_GENERATOR.getInts(2, 1, 20000);

        // 3 Hosts to check that different ips are possible & same ip with different port is possible
        String host1 = String.format("127.0.0.1:%d", ports.first());
        String host2 = String.format("127.0.0.1:%d", ports.last());
        String host3 = String.format("142.251.36.238:%d", ports.last());

        // Host 1
        helper.send(String.format("%s key.one", host1));
        helper.send(String.format("%s key.two", host1));

        // Host 2
        helper.send(String.format("%s key.special", host2));

        // Host 3
        helper.send(String.format("%s key.one", host3));

        // Host 1 again
        helper.send(String.format("%s key.one", host1));

        waitForMonitoringServerToUpdateDatabase(monitoringServer, numOfMsg);

        assertEquals(numOfMsg, monitoringServer.receivedMessages());
        assertThat(monitoringServer.getStatistics()).contains(
                host1, "key.one 2", "key.two 1",
                host2, "key.special 1",
                host3, "key.one 1");
    }
}
