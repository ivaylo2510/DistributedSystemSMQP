package dslab.assignment2.dns;

import dslab.ComponentFactory;
import dslab.broker.IBroker;
import dslab.config.BrokerConfig;
import dslab.config.DNSServerConfig;
import dslab.dns.IDNSServer;
import dslab.util.ConfigFactory;
import dslab.util.Constants;
import dslab.util.Util;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DNSIntegrationTest {

    private final BrokerConfig brokerConfig = ConfigFactory.createBrokerConfigA2();
    private IBroker broker;
    private Thread brokerThread;

    private final DNSServerConfig dnsConfig = ConfigFactory.createDNSServerConfig();
    private IDNSServer dnsServer;
    private Thread dnsThread;
    private TelnetClientHelper dnsHelper;

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @BeforeEach
    public void beforeEach() throws IOException {
        dnsServer = ComponentFactory.createDNSServer(dnsConfig);
        dnsThread = new Thread(dnsServer);
        dnsThread.start();

        TelnetClientHelper waitForDnsConnHelper = new TelnetClientHelper(Constants.LOCALHOST, dnsConfig.port());
        waitForDnsConnHelper.waitForInitConnection();
        waitForDnsConnHelper.disconnect();

        broker = ComponentFactory.createBroker(brokerConfig);
        brokerThread = new Thread(broker);
        brokerThread.start();

        TelnetClientHelper waitForBrokerConnHelper = new TelnetClientHelper(Constants.LOCALHOST, brokerConfig.port());
        waitForBrokerConnHelper.waitForInitConnection();
        waitForBrokerConnHelper.disconnect();

        dnsHelper = new TelnetClientHelper(Constants.LOCALHOST, dnsConfig.port());
    }

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @AfterEach
    public void afterEach() {
        if (dnsServer != null) dnsServer.shutdown();
        broker.shutdown();

        try {
            if (dnsThread != null && dnsThread.isAlive()) dnsThread.join();
            if (brokerThread != null && brokerThread.isAlive()) brokerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Util.waitForTcpPortsToClose(dnsConfig.port());
        Util.waitForTcpPortsToClose(brokerConfig.port());
    }

    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @Test
    void message_broker_registers_itself_on_startup() throws IOException {
        dnsHelper.connectAndReadResponse();
        String host = dnsHelper.waitForDnsRegistration(brokerConfig.domain());

        String[] parts = host.split(":");
        String hostname = parts[0];
        int port = Integer.parseInt(parts[1]);

        TelnetClientHelper brokerHelper = new TelnetClientHelper(hostname, port);
        assertEquals("ok SMQP", brokerHelper.connectAndReadResponse());
        brokerHelper.disconnect();
    }
}
