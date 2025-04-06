package csc435.app;

import java.util.Scanner;

public class ClientAppInterface {
    private final ClientProcessingEngine engine;

    public ClientAppInterface(ClientProcessingEngine engine) {
        this.engine = engine;
    }

    public void readCommands() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("---------Client---------");
        while (true) {
            System.out.print("Client> ");
            String command = scanner.nextLine().trim();
            String[] parts = command.split(" ", 2);

            switch (parts[0]) {
                case "connect" -> {
                    System.out.println("Connecting...");

                    if (parts.length < 2) {
                        System.out.println("Invalid Command, Usage: connect <server_ip>:<port>");
                        break;
                    }
                    String[] params = parts[1].split(" ");
                    if (params.length != 2) {
                        System.out.println("Invalid Command, Usage: connect <server_ip> <port>");
                        break;
                    }
                    String serverIP = params[0];
                    int serverPort = Integer.parseInt(params[1]);
                    engine.connect(serverIP, serverPort);
                }
                case "get_info" -> {
                    long clientID = engine.getInfo();
                    System.out.println("Client ID: " + clientID);
                }
                case "index" -> {
                    System.out.println("Indexing...");

                    if (parts.length < 2) {
                        System.out.println("Invalid Command, Usage: index <folder_path>");
                        break;
                    }
                    engine.indexFolder(parts[1]);
                }
                case "search" -> {

                    if (parts.length < 2) {
                        System.out.println("Invalid Command, Usage: search <query>");
                        break;
                    }
                    engine.searchFiles(parts[1]);
                }
                case "quit" -> {
                    System.out.println("Exiting...");

                    engine.disconnect();
                    scanner.close();
                    System.exit(0);
                }
                default -> System.out.println("Unrecognized command! Use: connect, index, search, get_info or quit.");
            }
        }
    }

}