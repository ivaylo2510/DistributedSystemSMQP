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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DNSProtocolTest extends BaseSingleDNSTest {

    private TelnetClientHelper helper;

    @Override
    protected void initTelnetClientHelpers() throws IOException {
        helper = new TelnetClientHelper(Constants.LOCALHOST, config.port());
    }

    @Override
    protected void closeTelnetClientHelpers() throws IOException {
        helper.disconnect();
    }

    @Test
    @Timeout(value = 26000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void dns_protocol_tests() throws IOException {
        dns_accepts_connection_successfully();
        afterEach();

        beforeEach();
        correct_protocol_identifier_successfully();
        afterEach();

        beforeEach();
        unregister_domain_successfully();
        afterEach();

        beforeEach();
        exit_successfully();
        afterEach();

        beforeEach();
        register_domain_successfully();
        afterEach();

        beforeEach();
        resolve_domain_not_found();
        afterEach();

        beforeEach();
        unregister_domain_wrong_syntax("unregister");
        afterEach();

        beforeEach();
        unregister_domain_wrong_syntax("unregister a b");
        afterEach();

        beforeEach();
        register_domain_wrong_syntax("register");
        afterEach();

        beforeEach();
        register_domain_wrong_syntax("register at");
        afterEach();

        beforeEach();
        resolve_domain_wrong_syntax("resolve");
        afterEach();

        beforeEach();
        resolve_domain_wrong_syntax("resolve a b");
    }

    void dns_accepts_connection_successfully() {
        assertDoesNotThrow(() -> helper.connectAndReadResponse());
    }

    void correct_protocol_identifier_successfully() throws IOException {
        assertEquals("ok SDP", helper.connectAndReadResponse());
    }

    void register_domain_successfully() throws IOException {
        helper.connectAndReadResponse();

        final String name = String.format("domain-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String host = String.format("192.168.0.1:%d", Global.SECURE_INT_GENERATOR.getInt(1, 2048));

        String response = helper.sendCommandAndReadResponse(register(name, host));

        assertEquals("ok", response);
    }

    void register_domain_wrong_syntax(String msg) throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse(msg);

        assertThat(response).startsWith("error");
        assertThat(response).contains("register <name> <ip:port>");
    }

    void unregister_domain_successfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse(unregister("domain.at"));

        assertEquals("ok", response);
    }

    void unregister_domain_wrong_syntax(String msg) throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse(msg);

        assertThat(response).startsWith("error");
        assertThat(response).contains("unregister <name>");
    }

    void resolve_domain_not_found() throws IOException {
        helper.connectAndReadResponse();

        String response = helper.sendCommandAndReadResponse(resolve(Global.SECURE_STRING_GENERATOR.getSecureString()));

        assertThat(response).startsWith("error");
        assertThat(response).contains("domain");
    }

    void resolve_domain_wrong_syntax(String msg) throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse(msg);

        assertThat(response).startsWith("error");
        assertThat(response).contains("resolve <name>");
    }

    void exit_successfully() throws IOException {
        helper.connectAndReadResponse();

        assertEquals("ok bye", helper.sendCommandAndReadResponse("exit"));
        assertNull(helper.sendCommandAndReadResponse(register("dslab.at", "192.168.0.1:22")));
    }
}
