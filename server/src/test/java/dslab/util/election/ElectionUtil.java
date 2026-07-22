package dslab.util.election;


import dslab.broker.Broker;
import dslab.broker.IBroker;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class ElectionUtil {
    public static void waitForElectionToEnd(IBroker[] brokers, long initialDelay) {
        await()
            .atMost(5, TimeUnit.SECONDS) // Timeout after 10 seconds (adjust as needed)
            .with()
            .pollDelay(initialDelay, TimeUnit.MILLISECONDS)
            .and()
            .pollInterval(10, TimeUnit.MILLISECONDS)  // Poll every 10 milliseconds
            .until(() -> {
                // Check if all brokers have a leader
                for (IBroker broker : brokers) {
                    if (broker.getLeader() < 0) {
                        return false;  // If any broker doesn't have a leader, the election is not complete
                    }
                }
                return true;  // All brokers have a leader
            });
    }

    public static void waitForElectionToEnd(IBroker[] brokers, int excludedId, long initialDelay) {
        await()
            .atMost(5, TimeUnit.SECONDS) // Timeout after 10 seconds (adjust as needed)
            .with()
            .pollDelay(initialDelay, TimeUnit.MILLISECONDS)
            .and()
            .pollInterval(10, TimeUnit.MILLISECONDS)  // Poll every 10 milliseconds
            .until(() -> {
                // Check if all brokers have a valid leader and their leader is not excluded
                for (IBroker broker : brokers) {
                    // Election still ongoing if the leader is invalid or the leader is the excluded ID
                    if (broker.getLeader() < 0 || broker.getLeader() == excludedId) {
                        return false;  // Not finished yet, continue waiting
                    }
                }
                return true;  // All brokers have a valid leader (none with excluded ID)
            });
    }

    public static int shutdownLeader(IBroker[] brokers, int leaderId) {
        for (int i = 0; i < brokers.length; i++) {
            if (brokers[i].getId() == leaderId) {
                brokers[i].shutdown();
                return i;
            }
        }
        return -1; // leaderId not in brokers.getId()
    }

    public static IBroker[] getAliveBrokers(IBroker[] brokers, int deadLeaderIndex) {
        IBroker[] aliveBrokers = new Broker[brokers.length - 1];
        int aliveBrokerIndex = 0;

        for (int i = 0; i < brokers.length; i++) {
            if (i == deadLeaderIndex) continue;
            aliveBrokers[aliveBrokerIndex++] = brokers[i];
        }

        return aliveBrokers;
    }
}
