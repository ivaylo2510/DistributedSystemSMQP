package dslab.assignment2.dns;

import dslab.ComponentFactory;
import dslab.config.DNSServerConfig;
import dslab.dns.IDNSServer;
import dslab.util.ConfigFactory;
import dslab.util.Constants;
import dslab.util.Util;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class BaseSingleDNSTest {

    protected static final DNSServerConfig config = ConfigFactory.createDNSServerConfig();

    protected IDNSServer dnsServer;
    protected Thread dnsThread;

    protected abstract void initTelnetClientHelpers() throws IOException;

    protected abstract void closeTelnetClientHelpers() throws IOException;

    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @BeforeEach
    public void beforeEach() throws IOException {
        dnsServer = ComponentFactory.createDNSServer(config);

        dnsThread = new Thread(dnsServer);
        dnsThread.start();

        TelnetClientHelper waitForConnHelper = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        waitForConnHelper.waitForInitConnection();
        waitForConnHelper.disconnect();

        initTelnetClientHelpers();
    }

    @AfterEach
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void afterEach() {
        try {
            closeTelnetClientHelpers();
        } catch (IOException e) {
            // ignored
        }

        if (dnsServer != null) {
            dnsServer.shutdown();
        }

        try {
            if (dnsThread != null && dnsThread.isAlive()) {
                dnsThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Util.waitForTcpPortsToClose(config.port());
    }
}
