package dslab.assignment2.broker;

import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BrokerProtocolTest extends BaseSingleBrokerTest {

    private final String suffix = Global.SECURE_STRING_GENERATOR.getSecureString();
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
    @Timeout(value = 32000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void broker_protocol_tests() throws IOException {
        broker_accepts_connection_successfully();
        afterEach();

        beforeEach();
        correct_protocol_identifier_successfully();
        afterEach();

        beforeEach();
        declare_exchange_default_and_redeclare_fails();
        afterEach();

        beforeEach();
        create_exchange_direct_successfully();
        afterEach();

        beforeEach();
        create_exchange_fanout_successfully();
        afterEach();

        beforeEach();
        create_exchange_topic_successfully();
        afterEach();

        beforeEach();
        create_queue_successfully();
        afterEach();

        beforeEach();
        bind_queue_to_exchange_no_routing_key_needed_successfully();
        afterEach();

        beforeEach();
        bind_queue_to_exchange_with_binding_key_successfully();
        afterEach();

        beforeEach();
        broker_accepts_multiple_connections_successfully();
        afterEach();

        beforeEach();
        publish_message_to_exchange_no_routing_key_needed_successfully();
        afterEach();

        beforeEach();
        publish_message_to_exchange_with_routing_key_successfully();
        afterEach();

        beforeEach();
        exit_successfully();
        afterEach();

        beforeEach();
        try_bind_before_declaring_exchange_unsuccessfully();
        afterEach();

        beforeEach();
        try_bind_before_declaring_queue_unsuccessfully();
        afterEach();

        beforeEach();
        try_subscribe_before_declaring_queue_unsuccessfully();
        afterEach();

        beforeEach();
        try_publish_before_declaring_exchange_unsuccessfully();
    }

    void broker_accepts_connection_successfully() {
        assertDoesNotThrow(() -> helper.connectAndReadResponse());
    }

    void correct_protocol_identifier_successfully() throws IOException {
        assertEquals("ok SMQP", helper.connectAndReadResponse());
    }

    void declare_exchange_default_and_redeclare_fails() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("exchange default default");
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("exchange direct default");
        assertThat(response).contains("error");

        response = helper.sendCommandAndReadResponse("exchange fanout default");
        assertThat(response).contains("error");

        response = helper.sendCommandAndReadResponse("exchange topic default");
        assertThat(response).contains("error");
    }

    void create_exchange_direct_successfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("exchange direct direct-%s".formatted(suffix));
        assertEquals("ok", response);
    }

    void create_exchange_fanout_successfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("exchange fanout fanout-%s".formatted(suffix));
        assertEquals("ok", response);
    }

    void create_exchange_topic_successfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("exchange topic topic-%s".formatted(suffix));
        assertEquals("ok", response);
    }

    void create_queue_successfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("queue queue-%s".formatted(suffix));
        assertEquals("ok", response);
    }

    void bind_queue_to_exchange_no_routing_key_needed_successfully() throws IOException {
        helper.connectAndReadResponse();

        String response;

        response = helper.sendCommandAndReadResponse("exchange fanout fanout-%s".formatted(suffix));
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("queue queue-%s".formatted(suffix));
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("bind none");
        assertEquals("ok", response);
    }

    void bind_queue_to_exchange_with_binding_key_successfully() throws IOException {
        helper.connectAndReadResponse();

        String response;

        response = helper.sendCommandAndReadResponse("exchange direct direct-%s".formatted(suffix));
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("queue queue-%s".formatted(suffix));
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("bind key-%s".formatted(suffix));
        assertEquals("ok", response);
    }

    void broker_accepts_multiple_connections_successfully() throws IOException {
        final int NUM_HELPERS = 100;
        TelnetClientHelper[] helpers = new TelnetClientHelper[NUM_HELPERS];
        String response;

        for (int i = 0; i < NUM_HELPERS; i++) {
            helpers[i] = new TelnetClientHelper(Constants.LOCALHOST, config.port());
            response = helpers[i].connectAndReadResponse();
            assertEquals("ok SMQP", response);
        }

        for (int i = 0; i < NUM_HELPERS; i++) {
            helpers[i].disconnect();
        }
    }

    void publish_message_to_exchange_no_routing_key_needed_successfully() throws IOException {
        helper.connectAndReadResponse();

        String response = helper.sendCommandAndReadResponse("exchange fanout fanout-%s".formatted(suffix));
        assertEquals("ok", response);
        response = helper.sendCommandAndReadResponse("queue queue-%s".formatted(suffix));
        assertEquals("ok", response);
        response = helper.sendCommandAndReadResponse("bind none");
        assertEquals("ok", response);
        response = helper.sendCommandAndReadResponse("publish key-%s %s".formatted(suffix, suffix));
        assertEquals("ok", response);
    }

    void publish_message_to_exchange_with_routing_key_successfully() throws IOException {
        final String routingKey = "rk-%s".formatted(suffix);

        helper.connectAndReadResponse();

        String response = helper.sendCommandAndReadResponse("exchange direct direct-%s".formatted(suffix));
        assertEquals("ok", response);
        response = helper.sendCommandAndReadResponse("queue queue-%s".formatted(suffix));
        assertEquals("ok", response);
        response = helper.sendCommandAndReadResponse("bind %s".formatted(routingKey));
        assertEquals("ok", response);
        response = helper.sendCommandAndReadResponse("publish key-%s %s".formatted(routingKey, suffix));
        assertEquals("ok", response);
    }

    void exit_successfully() throws IOException {
        helper.connectAndReadResponse();
        assertEquals("ok bye", helper.sendCommandAndReadResponse("exit"));
        assertNull(helper.sendCommandAndReadResponse("exchange fanout fanout-%s".formatted(suffix)));
    }

    void try_bind_before_declaring_exchange_unsuccessfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("bind key");
        assertThat(response).startsWith("error");
        assertThat(response).contains("exchange");
    }

    void try_bind_before_declaring_queue_unsuccessfully() throws IOException {
        helper.connectAndReadResponse();

        String response = helper.sendCommandAndReadResponse("exchange direct direct-%s".formatted(suffix));
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("bind key");
        assertThat(response).startsWith("error");
        assertThat(response).contains("queue");
    }

    void try_subscribe_before_declaring_queue_unsuccessfully() throws IOException {
        helper.connectAndReadResponse();

        String response = helper.sendCommandAndReadResponse("exchange direct direct-%s".formatted(suffix));
        assertEquals("ok", response);

        response = helper.sendCommandAndReadResponse("subscribe");
        assertThat(response).startsWith("error");
        assertThat(response).contains("queue");
    }

    void try_publish_before_declaring_exchange_unsuccessfully() throws IOException {
        helper.connectAndReadResponse();
        String response = helper.sendCommandAndReadResponse("publish routing.key this-message-should-never-arrive-at-any-subscriber");
        assertThat(response).startsWith("error");
        assertThat(response).contains("exchange");
    }
}
