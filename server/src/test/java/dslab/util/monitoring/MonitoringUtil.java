package dslab.util.monitoring;

import dslab.monitoring.IMonitoringServer;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class MonitoringUtil {
    public static void waitForMonitoringServerToUpdateDatabase(IMonitoringServer monitoringServer, int numberOfMsg) {
        await()
                .atMost(1, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.MILLISECONDS)
                .until(() -> monitoringServer.receivedMessages() >= numberOfMsg);
    }

}
