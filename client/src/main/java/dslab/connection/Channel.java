package dslab.connection;

import dslab.connection.types.ExchangeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class Channel implements IChannel {

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private boolean hasExchange = false;
    private boolean hasQueue = false;


    public Channel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean connect() throws IOException {
        socket = new Socket(host, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        String greeting = in.readLine();
        return "ok SMQP".equals(greeting);


    }

    @Override
    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            // 1️⃣ tell broker we’re leaving
            if (out != null) {
                out.println("exit");
                out.flush();
            }

            if (in != null) in.close();
            if (out != null) out.close();

            in = null;
            out = null;
            socket = null;
        }
    }

    @Override
    public boolean exchangeDeclare(ExchangeType exchangeType, String exchangeName) throws IOException {

        if (socket == null || socket.isClosed() || out == null || in == null) {
            return false;
        }

        String cmd = "exchange " + exchangeType.name().toLowerCase() + " " + exchangeName;
        out.println(cmd);


        boolean ok = "ok".equalsIgnoreCase(in.readLine());
        if (ok) {
            hasExchange = true;
            hasQueue = false; // reset any old queue
        }
        return ok;

    }

    @Override
    public boolean queueBind(String queueName, String bindingKey) throws IOException {
        if (!hasExchange) {
            return false;
        }


        if (socket == null || socket.isClosed() || in == null || out == null) {
            return false;
        }

        out.println("queue " + queueName);
        String resp1 = in.readLine();
        if (!"ok".equalsIgnoreCase(resp1)) return false;

        out.println("bind " + bindingKey);
        String resp2 = in.readLine();
        if (!"ok".equalsIgnoreCase(resp2)) return false;

        hasQueue = true;
        return true;

    }

    @Override
    public Thread subscribe(Consumer<String> callback) throws IOException {


        // Must have exchange + queue before subscribing
        if (!hasExchange || !hasQueue) return null;
        if (socket == null || socket.isClosed() || in == null || out == null) return null;


        if (callback == null) {
            socket.close();
            return null;
        }
        // 1) Tell broker we want to subscribe
        out.println("subscribe");

        // 2) Expect immediate "ok"
        String resp = in.readLine();
        if (!"ok".equalsIgnoreCase(resp)) throw new IllegalStateException();

        // 3) Start background reader thread that will call the callback
        Thread sub = new Subscription(this, callback);
        sub.start();
        return sub;


    }

    @Override
    public String getFromSubscription() {
        // Block until next line from broker; null on EOF/error
        try {
            return (in != null) ? in.readLine() : null;
        } catch (IOException e) {
            return null;
        }


    }

    @Override
    public boolean publish(String routingKey, String message) throws IOException {

        if (!hasExchange) {
            return false;
        }

            if (socket == null || socket.isClosed() || in == null || out == null) {
                return false;
            }

            out.println("publish " + routingKey + " " + message);

            String response = in.readLine();

            return "ok".equalsIgnoreCase(response);

    }

}
