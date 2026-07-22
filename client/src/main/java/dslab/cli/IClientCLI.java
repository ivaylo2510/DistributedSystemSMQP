package dslab.cli;


public interface IClientCLI extends Runnable {

    /**
     * Implement the logic to handle the user input in the CLI.
     */
    @Override
    void run();

    /**
     * Print the prompt to the Command Line Interface of the {@link ClientCLI}.
     * <p>
     * The required pattern of the prompt is: "componentId" + "> "
     */
    void printPrompt();

}
