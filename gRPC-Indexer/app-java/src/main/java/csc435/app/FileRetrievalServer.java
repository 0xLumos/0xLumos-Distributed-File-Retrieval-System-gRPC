package csc435.app;

public class FileRetrievalServer {
    public static void main(String[] args) {
        // Use a non-privileged port from args[0]
        if (args.length < 1) {
            System.out.println("Usage: java FileRetrievalServer <port>");
            return;
        }
        int serverPort = Integer.parseInt(args[0]);

        IndexStore store = new IndexStore();
        ServerProcessingEngine engine = new ServerProcessingEngine(store);
        ServerAppInterface appInterface = new ServerAppInterface(engine);

        // Start gRPC server workers
        engine.initialize(serverPort);

        // Read user commands
        appInterface.readCommands();
    }
}