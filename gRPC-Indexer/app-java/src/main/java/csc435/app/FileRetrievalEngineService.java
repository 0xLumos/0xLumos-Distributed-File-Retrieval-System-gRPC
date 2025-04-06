package csc435.app;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

public class FileRetrievalEngineService extends FileRetrievalEngineGrpc.FileRetrievalEngineImplBase {
    private final IndexStore store;
    private final ServerProcessingEngine engine;

    public FileRetrievalEngineService(IndexStore store, ServerProcessingEngine engine) {
        this.store = store;
        this.engine = engine;
    }

    @Override
    public void register(Empty request, StreamObserver<RegisterRep> responseObserver) {
        int clientId = engine.registerClient(Integer.toString(request.hashCode()));
        RegisterRep response = RegisterRep.newBuilder().setClientId(clientId).build();
        System.out.println("+ Connection accepted");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void computeIndex(IndexReq request, StreamObserver<IndexRep> responseObserver) {
        int clientId = request.getClientId();
        String documentPath = request.getDocumentPath();
        HashMap<String, Long> wordFrequencies = new HashMap<>(request.getWordFrequenciesMap());

        long documentNumber = store.putDocument(String.valueOf(clientId), documentPath);
        store.updateIndex(clientId,documentNumber, wordFrequencies);

        IndexRep response = IndexRep.newBuilder()
                .setMessage("Indexing successful for document: " + documentPath + " By client " + clientId)
                .setIndexedBytes(wordFrequencies.size())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

@Override
public void computeSearch(SearchReq request, StreamObserver<SearchRep> responseObserver) {
    List<String> terms = request.getTermsList();
    SearchRep.Builder responseBuilder = SearchRep.newBuilder();

    // Map to store documents containing all terms and their accumulated frequencies
    Map<String, SearchRep.SearchResult.Builder> documentFrequencyMap = new HashMap<>();

    for (String term : terms) {
        CopyOnWriteArrayList<DocFreqPair> results = store.lookupIndex(term);

        for (DocFreqPair result : results) {
            String documentPath = store.getDocument(result.documentNumber);
            long frequency = result.wordFrequency;
            int indexingClientId = result.clientId; // Ensure DocFreqPair stores clientId

            documentFrequencyMap.compute(documentPath, (key, existingResult) -> {
                if (existingResult == null) {
                    return SearchRep.SearchResult.newBuilder()
                            .setDocumentPath(documentPath)
                            .setFrequency(frequency)
                            .setClientId(indexingClientId); // Store correct client ID
                } else {
                    return existingResult.setFrequency(existingResult.getFrequency() + frequency);
                }
            });
        }
    }

    // Filter: Keep only documents containing **all** terms
    List<SearchRep.SearchResult> finalResults = documentFrequencyMap.values().stream()
        .filter(entry -> terms.stream().allMatch(term -> store.lookupIndex(term).stream()
            .anyMatch(df -> store.getDocument(df.documentNumber).equals(entry.getDocumentPath()))
        ))
        .sorted(Comparator.comparingLong(SearchRep.SearchResult.Builder::getFrequency).reversed()) // Sort by frequency
        .limit(10) // Take top 10
        .map(SearchRep.SearchResult.Builder::build)
        .toList();

    responseBuilder.addAllSearchResults(finalResults);
    responseBuilder.setTimeTaken(0.01);
    responseBuilder.setTotalResults(finalResults.size());

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
}


    @Override
    public void deregister(DeregisterReq request, StreamObserver<Empty> responseObserver) {
        engine.deregisterClient(request.getClientId());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void shutdown(ServerShutdownReq request, StreamObserver<ServerShutdownReq> responseObserver) {
        System.out.println("Server is shutting down upon request: " + request.getMessage());
        engine.shutdown();

        ServerShutdownReq response = ServerShutdownReq.newBuilder()
                .setMessage("Server shutting down.")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}