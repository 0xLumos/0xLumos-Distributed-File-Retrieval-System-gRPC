# Distributed File Retrieval System (gRPC)

This project implements a gRPC-based distributed document indexing and retrieval system in Java. Clients register with a central server, index documents, and perform keyword-based searches. The server maintains an in-memory inverted index and supports concurrent processing.

## Features

gRPC-based client-server communication

Document indexing with word frequency tracking

Keyword search with support for logical queries (e.g., term1 AND term2)

In-memory inverted index (IndexStore)

Thread-safe, concurrent design

Performance benchmarking tool

## Requirements

Java 17+

gRPC Java libraries

Protocol Buffers (protoc) for compiling .proto definitions

## Build Instructions

Compile .proto (if needed)

If you have a .proto file, use:

protoc --java_out=. --grpc-java_out=. file_retrieval.proto

## Compile Java Sources

javac -cp ".:grpc-netty-shaded.jar:grpc-stub.jar:grpc-protobuf.jar:protobuf-java.jar" csc435/app/*.java

On Windows, replace : with ; in the classpath.

## Running the System

Start the Server

java -cp ".:<your-classpath>" csc435.app.FileRetrievalServer <PORT>

This initializes the gRPC server and processing engine.

Start a Client

Update the main() method in ClientProcessingEngine.java with a valid folder path:

client.indexFolder("path/to/documents");
client.searchFiles("example AND query");
client.disconnect();

Then run:

java -cp ".:<your-classpath>" csc435.app.ClientProcessingEngine <SERVER IP> <PORT>

Running Benchmarks

The benchmarking utility simulates concurrent clients for stress testing.

java -cp ".:<your-classpath>" csc435.app.FileRetrievalBenchmark <serverIP> <serverPort> <numClients> <folder1> <folder2> ...

Example:

java -cp ".:libs/*" csc435.app.FileRetrievalBenchmark localhost 50051 3 ./docs1 ./docs2 ./docs3

## How It Works

Client connects to server

Registers and receives a unique client ID

Client indexes a folder

Word frequencies are computed and sent to the server

Server updates inverted index

Maps each word to a list of documents and frequencies

Client submits a search query

Server searches index and returns ranked results

Client disconnects

Server deregisters the client

## Notes

IndexStore uses ConcurrentHashMap and CopyOnWriteArrayList for thread safety

ServerProcessingEngine manages client sessions and dispatches indexing

FileRetrievalEngineService exposes gRPC methods

Shutdown is cleanly handled via RPCServerWorker.shutdown()

License

MIT License – free to use, modify, and distribute.

