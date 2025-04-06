package csc435.app;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class DocFreqPair {
    public int clientId;
    public long documentNumber;
    public long wordFrequency;

    public DocFreqPair(int clinetId, long documentNumber, long wordFrequency) {
        this.clientId = clinetId;
        this.documentNumber = documentNumber;
        this.wordFrequency = wordFrequency;
    }
}

public class IndexStore {
    private final HashMap<String, Long> documentMap = new HashMap<>();
    private final HashMap<Long, String> reverseDocumentMap = new HashMap<>();
    private final HashMap<Long, String> documentClientMap = new HashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<DocFreqPair>> termInvertedIndex = new ConcurrentHashMap<>();
    private long documentCounter = 1;

    public synchronized long putDocument(String clientId, String documentPath) {
        return documentMap.computeIfAbsent(documentPath, k -> {
            long docId = documentCounter++;
            reverseDocumentMap.put(docId, documentPath);
            documentClientMap.put(docId, clientId);
            return docId;
        });
    }

    public String getDocument(long documentNumber) {
        return reverseDocumentMap.getOrDefault(documentNumber, null);
    }

    public String getClientId(long documentNumber) {
        return documentClientMap.getOrDefault(documentNumber, null);
    }

    public void updateIndex(int clientId, long documentNumber, HashMap<String, Long> wordFrequencies) {
        wordFrequencies.forEach((term, frequency) -> {
            termInvertedIndex.computeIfAbsent(term, k -> new CopyOnWriteArrayList<>())
                    .add(new DocFreqPair(clientId,documentNumber, frequency));
        });
    }

    public CopyOnWriteArrayList<DocFreqPair> lookupIndex(String term) {
        return termInvertedIndex.getOrDefault(term, new CopyOnWriteArrayList<>());
    }
}