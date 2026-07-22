package dslab.broker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FanoutExchange extends Exchange {

    private final List<BrokerQueue> targetQueues;

    public FanoutExchange(String name) {
        super(name);
        targetQueues = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void bind(BrokerQueue queue, String bindingKey) {
        synchronized (targetQueues) {
            if (!targetQueues.contains(queue)) {
                targetQueues.add(queue);
            }
        }
    }

    @Override
    public void publish(String routingKey, String message) {
        synchronized (targetQueues) {
            for (BrokerQueue queue : targetQueues) {
                queue.enqueue(message);
            }
        }
    }
}
