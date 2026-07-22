package dslab.connection;

import dslab.connection.types.ExchangeType;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * A channel establishes a shareable connection to the message broker and provides methods to interact with it.
 */
public interface IChannel {

    /**
     * Connect to the specified host and port and establish a connection via {@link Socket}.
     *
     * @return true if the connection to the broker was successful, false otherwise.
     * @throws IOException if an I/O error occurs when creating the socket.
     */
    boolean connect() throws IOException;

    /**
     * Disconnect from the broker by closing the connection established in the {@link #connect()} method.
     */
    void disconnect() throws IOException;

    /**
     * Handles the protocol specific exchange declaration
     * @param exchangeType the type of the exchange to declare
     * @param exchangeName the name of the exchange to declare
     *
     * @return true if the exchange declaration was successful, false otherwise
     */
    boolean exchangeDeclare(ExchangeType exchangeType, String exchangeName) throws IOException;

    /**
     * Binds a queue to the exchange with the specified binding key
     *
     * @param queueName the name of the queue to bind
     * @param bindingKey the binding key to use for the binding
     *                   (for the fanout exchange, any binding key can be specified, but is ignored)
     *
     * @return true if the binding between the queue and the exchange was successful, false otherwise
     */
    boolean queueBind(String queueName, String bindingKey) throws IOException;

    /**
     * Subscribes to the specified queue and registers the callback to be called when a message is received.
     * The subscription should be non-blocking, therefore the logic and callback needs to be called in a dedicated background-thread.
     *
     * @param callback the callback to be called when a message is received.
     * @return the background-thread that is used to subscribe to the queue
     */
    Thread subscribe(Consumer<String> callback) throws IOException;

    /**
     * Returns the next message from the subscribed queue.
     * This method should block until a message is available.
     *
     * @return the next message from the subscribed queue
     */
    String getFromSubscription();

        /**
         * Publishes a message with the specified routing key to the exchange.
         *
         * @param routingKey the routing key to use for the message
         * @param message the message to publish
         *
         * @return true if the message was successfully published, false otherwise
         */
    boolean publish(String routingKey, String message) throws IOException;

}
