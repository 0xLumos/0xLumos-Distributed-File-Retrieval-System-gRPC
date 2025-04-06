package csc435.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerProcessingEngine {
    private final List<RPCServerWorker> workers = new ArrayList<>();
    private final IndexStore store;
    private final ExecutorService workerPool;
    private final Map<Integer, String> registeredClients = new ConcurrentHashMap<>();
    private int nextClientID = 1;
    private boolean running = true;

    public ServerProcessingEngine(IndexStore store) {
        this.store = store;
        this.workerPool = Executors.newFixedThreadPool(1);
    }


    public void initialize(int serverPort) {
        for (int i = 0; i < 1; i++) {
            RPCServerWorker worker = new RPCServerWorker(store, serverPort + i, this);
            workers.add(worker);
            workerPool.submit(worker);
        }
    }

    public int registerClient(String clientAddress) {
        int clientId;
        synchronized (this) {
            clientId = nextClientID++;
            registeredClients.put(clientId, clientAddress);
        }
        return clientId;
    }

    public void deregisterClient(int clientId) {
        synchronized (this) {
            registeredClients.remove(clientId);
            System.out.println("Client ID " + clientId + " disconnected.");
        }
    }

    public List<Integer> getConnectedWorkers() {
        return new ArrayList<>(registeredClients.keySet());
    }

    public void shutdown() {
        running = false;
        workerPool.shutdown();
        workers.forEach(RPCServerWorker::shutdown);
        try {
            workerPool.awaitTermination(10, TimeUnit.SECONDS);
            System.out.println("Server shut down.");
        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted.");
        }
    }
}