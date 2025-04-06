package csc435.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BenchmarkWorker implements Runnable {
    private final ClientProcessingEngine clientEngine;
    private final String datasetPath;
    private final CountDownLatch latch;
    private String serverIP;
    private int serverPort;

    public BenchmarkWorker(String serverIP, int serverPort, String datasetPath, CountDownLatch latch) {
        this.clientEngine = new ClientProcessingEngine();
        this.datasetPath = datasetPath;
        this.latch = latch;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            clientEngine.connect(serverIP, serverPort);
            clientEngine.indexFolder(datasetPath);
        } finally {
            latch.countDown();
        }
    }

    public void search(String query) {
        clientEngine.searchFiles(query);
    }

    public void disconnect() {
        clientEngine.disconnect();
    }
}

public class FileRetrievalBenchmark {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java FileRetrievalBenchmark <serverIP> <serverPort> <numClients> <datasetPaths...>");
            return;
        }

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);
        int numberOfClients = Integer.parseInt(args[2]);
        List<String> clientsDatasetPaths = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            clientsDatasetPaths.add(args[i]);
        }

        if (clientsDatasetPaths.size() < numberOfClients) {
            System.out.println("Error: Not enough dataset paths for the number of clients.");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        List<BenchmarkWorker> workers = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfClients; i++) {
            BenchmarkWorker worker = new BenchmarkWorker(serverIP, serverPort, clientsDatasetPaths.get(i), latch);
            workers.add(worker);
            executor.execute(worker);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();

        System.out.printf("Indexing completed in %.3f seconds\n", (endTime - startTime) / 1000.0);

        if (!workers.isEmpty()) {
            System.out.println("Performing benchmark search query: distortion AND adaptation'");
            workers.get(0).search("distortion AND adaptation");
        }

        workers.forEach(BenchmarkWorker::disconnect);
        executor.shutdown();
        System.out.println("Benchmark completed.");
    }
}