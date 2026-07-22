package dslab.assignment2.broker;

import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.helper.TelnetClientHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.exchange;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SingleExchangeScenarioTest extends BaseSingleBrokerTest {

    private final String exchangeName = String.format("exchange-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
    private final String queue1Name = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
    private final String queue2Name = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
    private TelnetClientHelper publisher1;
    private TelnetClientHelper publisher2;
    private TelnetClientHelper subscriber1;
    private TelnetClientHelper subscriber2;

    @Override
    protected void initTelnetClientHelpers() throws IOException {
        publisher1 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        publisher2 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        subscriber1 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        subscriber2 = new TelnetClientHelper(Constants.LOCALHOST, config.port());
    }

    @Override
    protected void closeTelnetClientHelpers() throws IOException {
        publisher1.disconnect();
        publisher2.disconnect();
        subscriber1.disconnect();
        subscriber2.disconnect();
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_fanout_exchange_two_subscribers_successfully() throws IOException {
        subscriber1.connectAndReadResponse();
        subscriber2.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "fanout", queue1Name, "none");
        subscriber2.subscribe(exchangeName, "fanout", queue2Name, "none");

        publisher1.sendCommandAndReadResponse(String.format("exchange fanout %s", exchangeName));
        publisher2.sendCommandAndReadResponse(String.format("exchange fanout %s", exchangeName));

        publisher1.publish("none", "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-1", subscriber2.readResponse());

        publisher2.publish("none", "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-2", subscriber2.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_two_binding_keys_for_single_queue_successfully() throws IOException {
        final String key1 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key2 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        subscriber1.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "direct", queue1Name, key1, key2);

        publisher1.sendCommandAndReadResponse(String.format("exchange direct %s", exchangeName));
        publisher2.sendCommandAndReadResponse(String.format("exchange direct %s", exchangeName));

        publisher1.publish(key1, "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        publisher2.publish(key2, "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber1.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_single_binding_key_for_two_queues_successfully() throws IOException {
        final String key = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        subscriber1.connectAndReadResponse();
        subscriber2.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "direct", queue1Name, key);
        subscriber2.subscribe(exchangeName, "direct", queue2Name, key);

        publisher1.sendCommandAndReadResponse(String.format("exchange direct %s", exchangeName));
        publisher2.sendCommandAndReadResponse(String.format("exchange direct %s", exchangeName));

        publisher1.publish(key, "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-1", subscriber2.readResponse());
        publisher2.publish(key, "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-2", subscriber2.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange_only_routes_correct_successfully() throws IOException {
        final String key1 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key2 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String keyBoth = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        subscriber1.connectAndReadResponse();
        subscriber2.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "direct", queue1Name, key1, keyBoth);
        subscriber2.subscribe(exchangeName, "direct", queue2Name, key2, keyBoth);

        publisher1.sendCommandAndReadResponse(String.format("exchange direct %s", exchangeName));
        publisher2.sendCommandAndReadResponse(String.format("exchange direct %s", exchangeName));

        publisher1.publish(key1 + key2, "THIS-SHOULD-BE-DISCARDED");
        publisher1.publish(key2 + key1, "THIS-SHOULD-BE-DISCARDED");
        publisher1.publish(String.format("%s.", key1), "THIS-SHOULD-BE-DISCARDED");

        publisher2.publish("key", "THIS-SHOULD-BE-DISCARDED");
        publisher2.publish(String.format("%s.", key2), "THIS-SHOULD-BE-DISCARDED");
        publisher2.publish(String.format(keyBoth + key2), "THIS-SHOULD-BE-DISCARDED");

        publisher1.publish(key1, "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        publisher1.publish(key2, "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber2.readResponse());
        publisher2.publish(keyBoth, "VERIFICATION-MESSAGE-3");
        assertEquals("VERIFICATION-MESSAGE-3", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-3", subscriber2.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_topic_exchange_multiple_binding_keys_for_single_queue() throws IOException {
        final String key1 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key2 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        subscriber1.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "topic", queue1Name,
                String.format("*.%s.*", key1), String.format("#.%s.#", key2));

        publisher1.sendCommandAndReadResponse(exchange("topic", exchangeName));
        publisher2.sendCommandAndReadResponse(exchange("topic", exchangeName));

        publisher1.publish(String.format("www.%s.at", key1), "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        publisher2.publish(String.format("eee.www.%s.de.at", key2), "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber1.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_topic_exchange_single_binding_key_for_two_queues() throws IOException {
        final String key = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        subscriber1.connectAndReadResponse();
        subscriber2.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "topic", queue1Name, String.format("%s.*", key));
        subscriber2.subscribe(exchangeName, "topic", queue2Name, String.format("%s.*", key));

        publisher1.sendCommandAndReadResponse(exchange("topic", exchangeName));
        publisher2.sendCommandAndReadResponse(exchange("topic", exchangeName));

        publisher1.publish(String.format("%s.at", key), "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-1", subscriber2.readResponse());

        publisher2.publish(String.format("%s.at", key), "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-2", subscriber2.readResponse());
    }

    @Test
    @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_topic_exchange_two_subscribers_only_routes_correct_message_successfully() throws IOException {
        final String key1 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key2 = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String keyBoth = String.format("key-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        subscriber1.connectAndReadResponse();
        subscriber2.connectAndReadResponse();
        publisher1.connectAndReadResponse();
        publisher2.connectAndReadResponse();

        subscriber1.subscribe(exchangeName, "topic", queue1Name, String.format("%s.*", key1), String.format("#.%s.#", keyBoth));
        subscriber2.subscribe(exchangeName, "topic", queue2Name, String.format("%s.#", key2), String.format("#.%s.#", keyBoth));

        publisher1.sendCommandAndReadResponse(exchange("topic", exchangeName));
        publisher2.sendCommandAndReadResponse(exchange("topic", exchangeName));

        publisher1.publish(key1 + key2, "THIS-SHOULD-BE-DISCARDED");
        publisher1.publish(key2 + key1, "THIS-SHOULD-BE-DISCARDED");
        publisher1.publish(key1 + ".", "THIS-SHOULD-BE-DISCARDED");
        publisher1.publish("key", "THIS-SHOULD-BE-DISCARDED");
        publisher1.publish(key1 + keyBoth, "THIS-SHOULD-BE-DISCARDED");

        publisher1.publish(String.format("%s.at", key1), "VERIFICATION-MESSAGE-1");
        assertEquals("VERIFICATION-MESSAGE-1", subscriber1.readResponse());
        publisher1.publish(String.format("%s.com.at", key2), "VERIFICATION-MESSAGE-2");
        assertEquals("VERIFICATION-MESSAGE-2", subscriber2.readResponse());
        publisher2.publish(String.format("www.%s.xyz.at", keyBoth), "VERIFICATION-MESSAGE-3");
        assertEquals("VERIFICATION-MESSAGE-3", subscriber1.readResponse());
        assertEquals("VERIFICATION-MESSAGE-3", subscriber2.readResponse());
    }
}
