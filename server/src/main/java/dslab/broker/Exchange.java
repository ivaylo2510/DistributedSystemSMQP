package dslab.broker;

import java.util.Queue;

public abstract class Exchange {

    private final String name;

    public Exchange(String name) {
        this.name = name;
    }

    public String getName() { return name; }


    public abstract void bind(BrokerQueue queue, String bindingKey);


    public abstract void publish(String routingKey, String message);
}