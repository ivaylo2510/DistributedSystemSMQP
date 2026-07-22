package dslab.assignment2.dns;

import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.register;
import static dslab.util.CommandBuilder.resolve;
import static dslab.util.CommandBuilder.unregister;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BasicDNSTest extends BaseSingleDNSTest {

    private TelnetClientHelper helper;

    @Override
    protected void initTelnetClientHelpers() throws IOException {
        helper = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        helper.connectAndReadResponse();
    }

    @Override
    protected void closeTelnetClientHelpers() throws IOException {
        helper.disconnect();
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void dns_shutdown_successfully() {
        assertDoesNotThrow(() -> dnsServer.shutdown(), "An unhandled exception occurred during the shutdown of the broker.");
        await().atMost(2, TimeUnit.SECONDS).pollInterval(5, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThat(dnsThread.isAlive()).isFalse());

        TelnetClientHelper helper = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        await().atMost(1, TimeUnit.SECONDS).pollInterval(5, TimeUnit.MILLISECONDS).untilAsserted(() -> assertThrows(IOException.class, () -> {
            helper.connectAndReadResponse();
            helper.disconnect();
        }));
    }

    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void dns_accepts_multiple_connections_successfully() throws IOException {
        final TelnetClientHelper helper1 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        final TelnetClientHelper helper2 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        final TelnetClientHelper helper3 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        assertEquals("ok SDP", helper1.connectAndReadResponse());
        assertEquals("ok SDP", helper2.connectAndReadResponse());
        assertEquals("ok SDP", helper3.connectAndReadResponse());
    }

    @Test
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void register_single_domain_successfully() throws IOException {
        final String toRegister = "192.168.0.1:20000";
        final String name = String.format("domain-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        helper.sendCommandAndReadResponse(register(name, toRegister));

        assertEquals(toRegister, helper.sendCommandAndReadResponse(resolve(name)));
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void change_entry_successfully() throws IOException {
        final String name = String.format("domain-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        helper.sendCommandAndReadResponse(register(name, "192.168.0.1:20000"));
        assertEquals("192.168.0.1:20000", helper.sendCommandAndReadResponse(resolve(name)));

        helper.sendCommandAndReadResponse(register(name, "10.0.0.1:20010"));
        assertEquals("10.0.0.1:20010", helper.sendCommandAndReadResponse(resolve(name)));
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void unregister_single_domain_successfully() throws IOException {
        final String name = String.format("domain-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String host = String.format("192.168.0.1:%d", Global.SECURE_INT_GENERATOR.getInt(1, 2048));

        helper.sendCommandAndReadResponse(register(name, host));
        assertEquals(host, helper.sendCommandAndReadResponse(resolve(name)));

        helper.sendCommandAndReadResponse(unregister(name));
        assertThat(helper.sendCommandAndReadResponse(resolve(name))).startsWith("error");
    }
}
