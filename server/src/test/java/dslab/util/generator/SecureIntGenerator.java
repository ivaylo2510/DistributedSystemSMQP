package dslab.util.generator;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.TreeSet;

public class SecureIntGenerator {
    private final SecureRandom random = new SecureRandom();

    public int getInt(int upperBound) {
        return random.nextInt(upperBound);
    }

    public int getInt(int lowerBound, int upperBound) {
        return random.nextInt(lowerBound, upperBound);
    }

    public TreeSet<Integer> getInts(int size, int lowerBound, int upperBound) {
        TreeSet<Integer> values = new TreeSet<>();


        Iterator<Integer> it = random.ints(lowerBound,upperBound).iterator();

        int i = 0;

        while (i < size) {
            if (values.add(it.next()))
                i++;
        }

        return values;
    }
}
