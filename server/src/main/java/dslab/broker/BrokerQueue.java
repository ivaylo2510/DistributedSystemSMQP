package dslab.broker;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class BrokerQueue {


    private final Queue<String> messages = new ConcurrentLinkedQueue<>();
    private final Semaphore itemsAvailable = new Semaphore(0);


    public void enqueue(String message) {
        messages.add(message);
        itemsAvailable.release();
    }

    // This is the ONLY way to get a message
    public String blockingGet() throws InterruptedException {
        itemsAvailable.acquire();
        return messages.poll();
    }
}