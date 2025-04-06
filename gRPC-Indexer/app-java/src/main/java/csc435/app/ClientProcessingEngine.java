package csc435.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.protobuf.Empty;

import csc435.app.FileRetrievalEngineGrpc.FileRetrievalEngineBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

class IndexResult {
    public double executionTime;
    public long totalBytesRead;

    public IndexResult(double executionTime, long totalBytesRead) {
        this.executionTime = executionTime;
        this.totalBytesRead = totalBytesRead;
    }
}

class DocPathFreqPair {
    public String documentPath;
    public long wordFrequency;

    public DocPathFreqPair(String documentPath, long wordFrequency) {
        this.documentPath = documentPath;
        this.wordFrequency = wordFrequency;
    }
}

class SearchResult {
    public double executionTime;
    public int totalResults;
    public ArrayList<DocPathFreqPair> documentFrequencies;

    public SearchResult(double executionTime, int totalResults, ArrayList<DocPathFreqPair> documentFrequencies) {
        this.executionTime = executionTime;
        this.totalResults = totalResults;
        this.documentFrequencies = documentFrequencies;
    }
}

public class ClientProcessingEngine {
    private ManagedChannel channel;
    private FileRetrievalEngineBlockingStub stub;
    private long clientId;

    public ClientProcessingEngine() {}




    public void connect(String serverIP, int serverPort) {
        channel = ManagedChannelBuilder.forAddress(serverIP, serverPort).usePlaintext().build();
        stub = FileRetrievalEngineGrpc.newBlockingStub(channel);

        RegisterRep response = stub.register(Empty.newBuilder().build());
        clientId = response.getClientId();
        System.out.println("Connected to server. Client ID: " + clientId);
    }


    public void indexFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid directory: " + folderPath);
            return;
        }
    
        long totalBytesRead = crawlDirectory(folder);
        System.out.println("Indexing completed. Total bytes read: " + totalBytesRead);
    }
    

    // Create a seperate directory crawling method to ease debugging, and make code more readable
    private long crawlDirectory(File folder) {
        long bytesRead = 0;
    
        File[] files = folder.listFiles();
        if (files == null) return bytesRead;
    
        for (File file : files) {
            if (file.isDirectory()) {
                // Recursive call for subdirectories
                bytesRead += crawlDirectory(file);
            } else if (file.isFile()) {
                try {
                    bytesRead += file.length();
                    HashMap<String, Long> wordFrequencies = computeWordFrequencies(file);
                    IndexReq request = IndexReq.newBuilder()
                            .setClientId((int) clientId)
                            .setDocumentPath(file.getAbsolutePath())
                            .putAllWordFrequencies(wordFrequencies)
                            .build();
    
                    IndexRep response = stub.computeIndex(request);
                    System.out.println(response.getMessage());
                } catch (IOException e) {
                    System.err.println("Error reading file: " + file.getName());
                }
            }
        }
    
        return bytesRead;
    }
    

private HashMap<String, Long> computeWordFrequencies(File file) throws IOException {
    HashMap<String, Long> wordFrequencies = new HashMap<>();
    Pattern wordPattern = Pattern.compile("[a-zA-Z0-9_-]{4,}"); // Only terms 4+ characters long

    try (Stream<String> lines = Files.lines(Paths.get(file.getAbsolutePath()))) {
        lines.forEach(line -> {
            Matcher matcher = wordPattern.matcher(line.toLowerCase());
            while (matcher.find()) {
                String word = matcher.group();
                wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0L) + 1);
            }
        });
    }

    return wordFrequencies;
}

public void searchFiles(String query) {
    List<String> queryList = Arrays.stream(query.split("(?i)\\bAND\\b"))
                               .map(String::trim)
                               .filter(term -> !term.isEmpty())
                               .toList();

    System.out.println("Performing AND search for: " + queryList);

    SearchReq request = SearchReq.newBuilder().addAllTerms(queryList).build();
    SearchRep response = stub.computeSearch(request);

    List<SearchRep.SearchResult> sortedResults = response.getSearchResultsList().stream()
        .sorted(Comparator.comparingLong(SearchRep.SearchResult::getFrequency).reversed()) 
        .limit(10) // Only take top 10 results
        .toList();

    System.out.printf("Search completed in %.3f seconds%n", response.getTimeTaken());
    System.out.printf("Search results (top %d out of %d):%n", sortedResults.size(), response.getTotalResults());

    sortedResults.forEach(result ->
        System.out.println(result.getClientId() + ":" + result.getDocumentPath() + ":" + result.getFrequency()) 
    );
}

    

    public long getInfo() {
        if (stub == null) {
            System.out.println("Not connected to any server.");
            return -1;
        }
        return clientId;
    }



    public void disconnect() {
        if (stub != null && clientId > 0) {
            try {
                DeregisterReq request = DeregisterReq.newBuilder().setClientId((int) clientId).build();
                stub.deregister(request);
            } catch (StatusRuntimeException e) {
                System.out.println(e);
            }
        }
        shutdown();
        System.out.println("Disconnected...");
    }

    public void shutdown() {
        if (channel != null) {
            channel.shutdown();
            try {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.out.println("Error while shutting down channel: " + e.getMessage());
            }
        }
    }
}