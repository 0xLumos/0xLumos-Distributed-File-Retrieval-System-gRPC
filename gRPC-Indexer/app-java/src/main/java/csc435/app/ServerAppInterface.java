package csc435.app;

import java.util.List;
import java.util.Scanner;

public class ServerAppInterface {
    private final ServerProcessingEngine engine;

    public ServerAppInterface(ServerProcessingEngine engine) {
        this.engine = engine;
    }

    public void readCommands() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("---------Server---------");

            while (true) {
                System.out.print("Server> ");
                String command = sc.nextLine().trim().toLowerCase();

                if (command.isEmpty()) continue;

                switch (command) {
                    case "list" -> {
                        List<Integer> clients = engine.getConnectedWorkers();
                        System.out.println( clients.size()+ " Total connected clients:");
                        for (int client : clients){
                        System.out.println("Client ID: " + client);
                        }
                    }
                    case "quit" -> {
                        System.out.println("Shutting down server...");
                        engine.shutdown();
                        return;
                    }
                    default -> System.out.println("Unrecognized command!");
                }
            }
        }
    }
}