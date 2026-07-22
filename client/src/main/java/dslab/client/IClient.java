package dslab.client;

import dslab.cli.ClientCLI;

public interface IClient extends Runnable {

    /**
     * All client related instances should be initialized and started here.
     */
    void run();

    /**
     * This method returns the component id of the client.
     *
     * @return the componentId of the client.
     */
    String getComponentId();

    /**
     * Implement any necessary steps to ensure a correct shutdown of the {@link Client} and {@link ClientCLI}.
     * <p>
     * For example, claimed system resources like streams must be unclaimed.
     * You can obtain more information in the corresponding JavaDoc of the used classes.
     */
    void shutdown();

}
