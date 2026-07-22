package dslab.broker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DirectExchange extends Exchange {

    private final Map<String, List<BrokerQueue>> bindings;

    public DirectExchange(String name) {
        super(name);
        bindings = new ConcurrentHashMap<>();
    }

    @Override
    public void bind(BrokerQueue queue, String bindingKey) {
        bindings.computeIfAbsent(bindingKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(queue);
    }

    @Override
    public void publish(String routingKey, String message) {
        List<BrokerQueue> targetQueues = bindings.get(routingKey);

        if (targetQueues != null) {
            synchronized (targetQueues) {
                for (BrokerQueue queue : targetQueues) {
                    queue.enqueue(message);
                }
            }
        }
    }
}
