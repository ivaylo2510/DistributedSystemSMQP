package dslab;

import dslab.annotations.GitHubClassroomGrading;
import dslab.client.Client;
import dslab.client.IClient;
import dslab.config.Config;
import dslab.grading.LocalGradingExtension;
import dslab.mock.MessageBrokerServer;
import dslab.util.RandomStringGenerator;
import dslab.util.streams.TestInputStream;
import dslab.util.streams.TestOutputStream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;

@ExtendWith(LocalGradingExtension.class)
public class ClientCLITest {

    private final String exchangeName = "exchange-%s".formatted(RandomStringGenerator.getSecureString());
    private final String queueName = "queue-%s".formatted(RandomStringGenerator.getSecureString());
    private final String routingKey = "key-%s".formatted(RandomStringGenerator.getSecureString());
    private final String bindingKey = "key-%s".formatted(RandomStringGenerator.getSecureString());
    private final String message1 = "message-%s".formatted(RandomStringGenerator.getSecureString(32));
    private final String message2 = "message-%s".formatted(RandomStringGenerator.getSecureString(32));
    private final String message3 = "message-%s".formatted(RandomStringGenerator.getSecureString(32));
    final String[] strings = RandomStringGenerator.getSecureStrings((int) (Math.random() * 16 + 1));
    final int loop = (int) (Math.random() * 5) + 1;

    @BeforeAll
    public static void beforeAll() {
        Awaitility.setDefaultPollDelay(1, MILLISECONDS);
        Awaitility.setDefaultTimeout(1000, MILLISECONDS);
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void displays_correct_prompt(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).isEqualTo("%s> ".formatted(componentId));
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void empty_newline_command(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            for (int i = 0; i < loop; i++) {
                in.addLine("");
                await().untilAsserted(() -> {
                    assertThat(readNextLine(out)).isEqualTo("%s> ".formatted(componentId));
                });
            }
        }
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @Test
    public void unknown_command() throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread("client-0", in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, strings[0]);
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            writeToCLI(in, strings[0]);
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @CsvSource({"client-0", "client-1", "client-2"})
    public void shutdown_basic_command_validation(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "shutdown ");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            writeToCLI(in, "shutdown " + String.join(" ", strings));
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 1)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void shutdown_command_sends_exit(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream();
                MessageBrokerServer mb = new MessageBrokerServer(new Config(componentId))
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            writeToCLI(in, "channel broker-0");
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("CONNECTED");
            });
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            writeToCLI(in, "shutdown");

            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("exit");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 4)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void channel_command(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream();
                MessageBrokerServer mb = new MessageBrokerServer(new Config(componentId))
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            writeToCLI(in, "channel broker-0");

            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("CONNECTED");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void channel_basic_command_validation(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "channel");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            writeToCLI(in, "channel broker-0 " + String.join(" ", strings));
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void channel_invalid_config_validation(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "channel ");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            writeToCLI(in, "channel " + String.join(" ", strings));
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void channel_error_handling(String componentId) throws IOException {
        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            // Broker is not running -> should print error
            writeToCLI(in, "channel broker-0");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            // Invalid config properties -> should print error
            writeToCLI(in, "channel " + strings[0]);
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);
        }
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Timeout(value = 2500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({
            "fanout, client-0", "fanout, client-1", "fanout, client-2",
            "direct, client-0", "direct, client-1", "direct, client-2",
            "default, client-0", "default, client-1", "default, client-2",
            "topic, client-0", "topic, client-1", "topic, client-2"
    })
    public void subscribe_command(String exchangeType, String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream();
                MessageBrokerServer mb = new MessageBrokerServer(new Config(componentId))
        ) {

            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            writeToCLI(in, "channel broker-0");
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("CONNECTED");
            });
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            // Test non-existing exchange type
            writeToCLI(in, "subscribe %s %s %s %s".formatted(exchangeName, strings[0], queueName, bindingKey));
            await().untilAsserted(() -> {
                String lineSubscribe = readNextLine(out);
                assertThat(lineSubscribe).contains("error");
            });

            writeToCLI(in, "subscribe %s %s %s %s".formatted(exchangeName, exchangeType, queueName, bindingKey));
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).containsExactly(
                        "exchange %s %s".formatted(exchangeType, exchangeName),
                        "queue %s".formatted(queueName), "bind %s".formatted(bindingKey),
                        "subscribe"
                );
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({
            "fanout, client-0", "fanout, client-1", "fanout, client-2",
            "direct, client-0", "direct, client-1", "direct, client-2",
            "default, client-0", "default, client-1", "default, client-2",
            "topic, client-0", "topic, client-1", "topic, client-2"
    })
    public void subscribe_command_without_channel(String exchangeType, String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "subscribe %s %s %s %s".formatted(exchangeName, exchangeType, queueName, bindingKey));
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void subscribe_basic_command_validation(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "subscribe");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            writeToCLI(in, "subscribe ");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            for (int i = 0; i < loop; i++) {
                writeToCLI(in, "subscribe " + String.join(" ", Arrays.copyOf(strings, i)));
                await().untilAsserted(() -> {
                    assertThat(readNextLine(out)).contains("error");
                });
            }
        }
    }

    @GitHubClassroomGrading(maxScore = 4)
    @Timeout(value = 2500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({
            "fanout, client-0, client-1", "fanout, client-1,  client-2",
            "direct, client-0, client-1", "direct, client-1,  client-2",
            "default, client-0, client-1", "default, client-1,  client-2",
            "topic, client-0, client-1", "topic, client-1,  client-2"
    })
    public void subscription_correct_callback_and_thread_behaviour(String exchangeType, String componentId1, String componentId2) throws IOException {

        try (
                // Subscriber
                TestInputStream in0 = new TestInputStream();
                TestOutputStream out0 = new TestOutputStream();
                // Publisher
                TestInputStream in1 = new TestInputStream();
                TestOutputStream out1 = new TestOutputStream();
                MessageBrokerServer mb = new MessageBrokerServer(new Config(componentId1))
        ) {

            prepareClientThread(componentId1, in0, out0).start();
            awaitCLIPromptReady(out0);
            clearOut(mb.getLogs());

            writeToCLI(in0, "channel broker-0");
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("CONNECTED");
            });
            awaitCLIPromptReady(out0);
            clearOut(mb.getLogs());

            writeToCLI(in0, "subscribe %s %s %s %s".formatted(exchangeName, exchangeType, queueName, bindingKey));

            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines().toArray()).satisfiesAnyOf(
                        a -> assertThat(a).containsExactly("exchange %s %s".formatted(exchangeType, exchangeName), "queue %s".formatted(queueName), "bind %s".formatted(bindingKey), "subscribe"),
                        b -> assertThat(b).containsExactly("queue %s".formatted(queueName), "exchange %s %s".formatted(exchangeType, exchangeName), "bind %s".formatted(bindingKey), "subscribe"));
            });
            clearOut(out0);

            prepareClientThread(componentId2, in1, out1).start();
            awaitCLIPromptReady(out1);
            clearOut(mb.getLogs());

            writeToCLI(in1, "channel broker-0");
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("CONNECTED");
            });
            awaitCLIPromptReady(out1);
            clearOut(mb.getLogs());

            writeToCLI(in1, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message1));
            writeToCLI(in1, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message2));
            writeToCLI(in1, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message3));

            await().untilAsserted(() -> {
                assertThat(readNextLine(out0)).contains("%s".formatted(message1));
                assertThat(readNextLine(out0)).contains("%s".formatted(message2));
                assertThat(readNextLine(out0)).contains("%s".formatted(message3));
            });
            clearOut(out0);

            // Terminate the subscription
            writeToCLI(in0, "arbitrary input to terminate the subscription");
            awaitCLIPromptReady(out0);

            // Publish more messages
            writeToCLI(in1, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message1));
            writeToCLI(in1, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message2));

            // No messages should be received
            await().untilAsserted(() -> {
                assertThat(out0.getLines()).isEqualTo(new ArrayList<>());
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 5)
    @Timeout(value = 2500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({
            "fanout, client-0", "fanout, client-1", "fanout, client-2",
            "direct, client-0", "direct, client-1", "direct, client-2",
            "default, client-0", "default, client-1", "default, client-2",
            "topic, client-0", "topic, client-1", "topic, client-2"
    })
    public void publish_command(String exchangeType, String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream();
                MessageBrokerServer mb = new MessageBrokerServer(new Config(componentId))
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            writeToCLI(in, "channel broker-0");
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).contains("CONNECTED");
            });
            awaitCLIPromptReady(out);
            clearOut(mb.getLogs());

            // Test non-existing exchange type
            writeToCLI(in, "publish %s %s %s %s".formatted(exchangeName, strings[0], routingKey, message1));
            await().untilAsserted(() -> {
                String linePublish = readNextLine(out);
                assertThat(linePublish).contains("error");
            });

            writeToCLI(in, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message1));
            await().untilAsserted(() -> {
                assertThat(mb.getLogs().getLines()).containsExactly(
                        "exchange %s %s".formatted(exchangeType, exchangeName),
                        "publish %s %s".formatted(routingKey, message1)
                );
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({
            "fanout, client-0", "fanout, client-1", "fanout, client-2",
            "direct, client-0", "direct, client-1", "direct, client-2",
            "default, client-0", "default, client-1", "default, client-2",
            "topic, client-0", "topic, client-1", "topic, client-2"
    })
    public void publish_command_without_channel(String exchangeType, String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "publish %s %s %s %s".formatted(exchangeName, exchangeType, routingKey, message1));

            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
        }
    }

    @GitHubClassroomGrading(maxScore = 2)
    @Timeout(value = 1500, unit = MILLISECONDS, threadMode = SEPARATE_THREAD)
    @ParameterizedTest
    @CsvSource({"client-0", "client-1", "client-2"})
    public void publish_basic_command_validation(String componentId) throws IOException {

        try (
                TestInputStream in = new TestInputStream();
                TestOutputStream out = new TestOutputStream()
        ) {
            prepareClientThread(componentId, in, out).start();
            awaitCLIPromptReady(out);

            writeToCLI(in, "publish");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            writeToCLI(in, "publish ");
            await().untilAsserted(() -> {
                assertThat(readNextLine(out)).contains("error");
            });
            clearOut(out);

            for (int i = 0; i < loop; i++) {
                writeToCLI(in, "publish " + String.join(" ", Arrays.copyOf(strings, i)));
                await().untilAsserted(() -> {
                    assertThat(readNextLine(out)).contains("error");
                });
            }
        }
    }

    /* =========================== HELPER METHODS =========================== */
    private Thread prepareClientThread(String componentId, TestInputStream in, TestOutputStream out) {
        IClient client = new Client(componentId, in, out);
        return new Thread(client);
    }

    private void writeToCLI(TestInputStream in, String message) {
        in.addLine(message);
    }

    private void clearOut(TestOutputStream out) {
        out.clear();
        await().until(() -> out.getLines().isEmpty());
    }

    private String readNextLine(TestOutputStream target) {
        try {
            return target.readNextLine(1000, MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitCLIPromptReady(TestOutputStream out) {
        await().until(() -> readNextLine(out) != null);
        clearOut(out);
    }
}
