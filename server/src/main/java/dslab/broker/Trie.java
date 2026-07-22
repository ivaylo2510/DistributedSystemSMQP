package dslab.broker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Trie {
    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    public void insert(String bindingKey, BrokerQueue queue) {
        String[] tokens = bindingKey.split("\\.");
        root.add(tokens, 0, queue);
    }

    public List<BrokerQueue> getMatchingQueues(String routingKey) {
        List<BrokerQueue> matches = new ArrayList<>();

        String[] tokens = routingKey.split("\\.");
        root.collectMatches(tokens, 0, matches);

        return matches;
    }
}



class TrieNode {
    private final ConcurrentHashMap<String, TrieNode> children = new ConcurrentHashMap<>();

    private final List<BrokerQueue> subscribers = Collections.synchronizedList(new ArrayList<>());

    public void add(String[] tokens, int index, BrokerQueue queue) {
        if (index == tokens.length) {
            synchronized (subscribers) {
                if (!subscribers.contains(queue)) {
                    subscribers.add(queue);
                }
            }
            return;
        }

        String token = tokens[index];
        children.computeIfAbsent(token, k -> new TrieNode())
                .add(tokens, index + 1, queue);
    }

    public void collectMatches(String[] tokens, int index, List<BrokerQueue> results) {
        if (index == tokens.length) {
            synchronized (subscribers) {
                results.addAll(this.subscribers);
            }

            TrieNode hashChild = children.get("#");
            if (hashChild != null) {
                hashChild.collectMatches(tokens, index, results);
            }
            return;
        }

        String token = tokens[index];

        TrieNode exactChild = children.get(token);
        if (exactChild != null) {
            exactChild.collectMatches(tokens, index + 1, results);
        }

        TrieNode starChild = children.get("*");
        if (starChild != null) {
            starChild.collectMatches(tokens, index + 1, results);
        }

        TrieNode hashChild = children.get("#");
        if (hashChild != null) {
            for (int k = index; k <= tokens.length; k++) {
                hashChild.collectMatches(tokens, k, results);
            }
        }
    }
}
