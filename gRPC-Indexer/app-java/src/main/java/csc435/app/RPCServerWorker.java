package csc435.app;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class RPCServerWorker implements Runnable {
    private Server server;
    private final IndexStore store;
    private final int port;
    private final ServerProcessingEngine engine;
    private volatile boolean running = false; // Ensures state tracking

    public RPCServerWorker(IndexStore store, int port, ServerProcessingEngine engine) {
        this.store = store;
        this.port = port;
        this.engine = engine;
    }

    @Override
    public void run() {
        try {
            System.out.println("Starting gRPC Server on port: " + port);
            server = ServerBuilder.forPort(port)
                    .addService((BindableService) new FileRetrievalEngineService(store, engine))
                    .build()
                    .start();
            running = true;

            // Block and keep the server alive
            server.awaitTermination();

        } catch (IOException e) {
            System.err.println("Error starting gRPC server: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("Server interrupted! Shutting down...");
            Thread.currentThread().interrupt();
        } finally {
            running = false;
        }
    }

    public synchronized void shutdown() {
        if (!running) {
            System.out.println("Shutdown requested but server is already stopped.");
            return;
        }

        System.out.println("Shutting down gRPC Server on port " + port + "...");
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Force shutting down...");
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                System.err.println("Shutdown interrupted!");
                Thread.currentThread().interrupt();
            }
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}