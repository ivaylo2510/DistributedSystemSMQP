package dslab.connection;

import java.io.IOException;
import java.util.function.Consumer;

public class Subscription extends Thread {

    private final IChannel channel;
    private final Consumer<String> callback;

    public Subscription(IChannel channel, Consumer<String> callback) {
        this.channel = channel;
        this.callback = callback;
        setName("subscription -thread");
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            String msg = channel.getFromSubscription(); // blocks
            if (msg == null) break;                     // socket closed / error
            callback.accept(msg);
        }

    }

    public void kill() throws IOException{
        interrupt();          // set interrupt flag
        channel.subscribe(null); // unblocks getFromSubscription()
    }
}
