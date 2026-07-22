package dslab.broker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TopicExchange extends Exchange {

    private final Trie trie;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public TopicExchange(String name) {
        super(name);
        this.trie = new Trie();
    }

    @Override
    public void bind(BrokerQueue queue, String bindingKey) {
        lock.writeLock().lock();
        try {
            trie.insert(bindingKey, queue);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void publish(String routingKey, String message) {
        List<BrokerQueue> rawMatches;

        lock.readLock().lock();
        try {
            rawMatches = trie.getMatchingQueues(routingKey);
        } finally {
            lock.readLock().unlock();
        }

        Set<BrokerQueue> uniqueQueues = new HashSet<>(rawMatches);

        for (BrokerQueue queue : uniqueQueues) {
            queue.enqueue(message);
        }
    }
}
