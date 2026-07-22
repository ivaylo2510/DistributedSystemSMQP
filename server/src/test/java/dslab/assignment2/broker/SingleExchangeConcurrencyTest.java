package dslab.assignment2.broker;

import dslab.util.Constants;
import dslab.util.Global;
import dslab.util.helper.TelnetClientHelper;
import dslab.util.threads.PublisherThread;
import dslab.util.threads.SubscriberThread;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static dslab.util.CommandBuilder.bind;
import static dslab.util.CommandBuilder.exchange;
import static dslab.util.CommandBuilder.queue;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SingleExchangeConcurrencyTest extends BaseSingleBrokerTest {

    @Override
    protected void initTelnetClientHelpers() throws IOException {
        // Not used
    }

    @Override
    protected void closeTelnetClientHelpers() throws IOException {
        // Not used
    }

    @Test
    @Timeout(value = 6000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_direct_exchange() throws InterruptedException, IOException {
        final int numberOfSubscribers = 2;
        final int numberOfPublishers = 4;
        final int messagesPerPublisher = 100;
        final int numOfMsgPerSubscriber = numberOfPublishers * messagesPerPublisher / numberOfSubscribers;
        final String exchangeName = String.format("exchange-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String queueName1 = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String queueName2 = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key1 = String.format("rk-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String key2 = String.format("rk-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        final TelnetClientHelper helper = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        helper.connectAndReadResponse();
        helper.sendCommandAndReadResponse(exchange("direct", exchangeName));
        helper.sendCommandAndReadResponse(queue(queueName1));
        helper.sendCommandAndReadResponse(bind(key1));
        helper.sendCommandAndReadResponse(queue(queueName2));
        helper.sendCommandAndReadResponse(bind(key2));
        helper.disconnect();

        final Set<SubscriberThread> subscribers = new HashSet<>();
        subscribers.add(new SubscriberThread(config.port(), "direct", exchangeName, queueName1, numOfMsgPerSubscriber));
        subscribers.add(new SubscriberThread(config.port(), "direct", exchangeName, queueName2, numOfMsgPerSubscriber));

        List<PublisherThread> publishers = new ArrayList<>();
        for (int i = 0; i < numberOfPublishers; i++) {
            publishers.add(new PublisherThread(config.port(), exchangeName, "direct", new String[]{key1, key2}, messagesPerPublisher));
        }

        for (PublisherThread p : publishers) p.start();
        for (SubscriberThread s : subscribers) s.start();
        for (SubscriberThread s : subscribers) s.join();
        for (SubscriberThread s : subscribers) {
            assertThat(s.getReceivedMessages().size()).isEqualTo(numOfMsgPerSubscriber);
        }
    }

    @Test
    @Timeout(value = 6000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void publish_to_fanout_exchange() throws InterruptedException, IOException {
        final int numberOfPublishers = 2;
        final int messagesPerPublisher = 100;
        final int numOfMsgPerSubscriber = numberOfPublishers * messagesPerPublisher;
        final String exchangeName = String.format("exchange-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String queueName1 = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());
        final String queueName2 = String.format("queue-%s", Global.SECURE_STRING_GENERATOR.getSecureString());

        final TelnetClientHelper helper = new TelnetClientHelper(Constants.LOCALHOST, config.port());
        helper.connectAndReadResponse();
        helper.sendCommandAndReadResponse(exchange("fanout", exchangeName));
        helper.sendCommandAndReadResponse(queue(queueName1));
        helper.sendCommandAndReadResponse(bind("whatever"));
        helper.sendCommandAndReadResponse(queue(queueName2));
        helper.sendCommandAndReadResponse(bind("whatever"));
        helper.disconnect();

        final Set<SubscriberThread> subscribers = new HashSet<>();
        subscribers.add(new SubscriberThread(config.port(), "fanout", exchangeName, queueName1, numOfMsgPerSubscriber));
        subscribers.add(new SubscriberThread(config.port(), "fanout", exchangeName, queueName2, numOfMsgPerSubscriber));

        List<PublisherThread> publishers = new ArrayList<>();
        for (int i = 0; i < numberOfPublishers; i++) {
            publishers.add(new PublisherThread(config.port(), exchangeName, "fanout", new String[]{"whatever"}, messagesPerPublisher));
        }

        for (PublisherThread p : publishers) p.start();
        for (SubscriberThread s : subscribers) s.start();
        for (SubscriberThread s : subscribers) s.join();
        for (SubscriberThread s : subscribers) {
            assertThat(s.getReceivedMessages().size()).isEqualTo(numOfMsgPerSubscriber);
        }
    }
}
