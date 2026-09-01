# Reactive RAG Document Processor

A Gradle multi-module Java project that processes large UTF-8 text datasets, stores recoverable document chunks in MongoDB, indexes local embeddings in Qdrant, and exposes GraphQL queries for word counts and RAG answers.

Current status: the end-to-end RAG flow is implemented and tested.

At query time, a GraphQL question is embedded, searched in Qdrant, recovered from MongoDB, and answered by Gemini using the retrieved context. The current development focus is retrieval quality: using a smaller, better corpus is more useful than generating a huge repetitive synthetic dataset.

## Stack

- Java 25
- Gradle 9.3.0 with Kotlin DSL
- Spring Boot 4.1.0
- Project Reactor
- Reactive MongoDB
- Spring GraphQL + WebFlux
- Spring AI 2.0.1
- Local Transformers embeddings
- Qdrant
- Google Gemini
- Docker Compose
- Testcontainers
- GitHub Actions

This project uses Gradle only. There is no Maven `pom.xml`.

## Modules

| Module | Runtime role | Responsibility |
| --- | --- | --- |
| `file-generator` | CLI job | Streams a line-delimited UTF-8 dataset to disk. |
| `file-coordinator` | CLI job | Calculates byte-range chunks and stores dataset metadata in MongoDB. |
| `file-processor` | worker job | Processes one source chunk, counts words, creates RAG chunks, and writes results to MongoDB. |
| `rag-indexer` | CLI job | Reads MongoDB `rag_chunks`, creates local embeddings, and writes vectors to Qdrant. |
| `file-query-api` | HTTP service | Exposes GraphQL queries for datasets, top words, retrieved context, and document answers. |

## System Flow

```mermaid
flowchart LR
    FG["file-generator"] --> DATA[("UTF-8 dataset")]
    DATA --> FC["file-coordinator"]
    FC --> DATASETS[("MongoDB datasets")]
    FC --> PLAN["ProcessingPlan"]
    PLAN --> FP["file-processor"]
    FP --> WORDS[("MongoDB chunk_word_counts")]
    FP --> RAG[("MongoDB rag_chunks")]

    RAG --> RI["rag-indexer"]
    RI --> EMB["Local Transformers embeddings"]
    EMB --> QDRANT[("Qdrant vectors")]

    CLIENT["GraphQL client"] --> API["file-query-api"]
    DATASETS --> API
    WORDS --> API
    API --> QDRANT
    QDRANT --> API
    API --> RAG
    API --> GEMINI["Gemini"]
    GEMINI --> API
    API --> CLIENT
```

MongoDB is the source of truth for document text. Qdrant is only the vector index used to find likely relevant chunks.

The arrows show data flow, not automatic orchestration. The batch modules are CLI jobs. `file-coordinator` stores and prints the processing plan, and each `file-processor` run receives one printed byte range explicitly.

## Data Ownership

```mermaid
flowchart TB
    subgraph MONGO["MongoDB"]
        DATASETS[("datasets<br/>dataset metadata")]
        WORDS[("chunk_word_counts<br/>word counts by chunk")]
        RAG[("rag_chunks<br/>source text + byte offsets")]
    end

    subgraph QDRANT["Qdrant"]
        POINTS[("points<br/>embedding vectors")]
        PAYLOAD["payload<br/>ragChunkId, datasetId,<br/>sourceChunkIndex, ragChunkIndex, text"]
    end

    DATASETS --> FQA["file-query-api"]
    WORDS --> FQA
    RAG --> RI["rag-indexer"]
    RI --> POINTS
    RI --> PAYLOAD

    POINTS -. "similarity search" .-> ASK["askDocument"]
    PAYLOAD -. "ragChunkId" .-> RAG
```

## Architecture

The code follows a pragmatic Clean Architecture / Hexagonal Architecture style:

- `domain`: pure records and domain rules.
- `application`: use cases and ports.
- `adapter/in`: GraphQL and CLI entry points.
- `adapter/out`: filesystem, MongoDB, Qdrant, Gemini, and Spring AI integrations.
- `config`: explicit Spring `@Configuration` / `@Bean` wiring.

Spring annotations are kept out of `domain` and `application`. Use cases depend on small ports, not on MongoDB, Qdrant, Gemini, or Spring AI directly.

Blocking external APIs are wrapped carefully. For example, Spring AI `VectorStore.similaritySearch(...)` and `ChatModel.call(...)` are synchronous calls, so their adapters run them on Reactor `boundedElastic`.

## Batch Processing

The processor uses half-open byte ranges:

```text
[startByteInclusive, endByteExclusive)
```

Line ownership is based on where the line starts:

- if a chunk starts in the middle of a line, that partial line is skipped;
- if a line starts inside the chunk but ends after the byte limit, it is still read completely;
- this avoids duplicate lines, lost lines, and broken UTF-8 characters.

`file-processor` reads each source chunk once. During that single pass it counts words and builds RAG chunks. RAG chunks are written to MongoDB in batches.

## Storage

Main MongoDB collections:

```text
datasets
chunk_word_counts
rag_chunks
```

Example `rag_chunks` document:

```json
{
  "_id": "dataset-books:rag:0:42",
  "datasetId": "dataset-books",
  "sourceChunkIndex": 0,
  "ragChunkIndex": 42,
  "text": "The original source text for this RAG chunk...",
  "startByteInclusive": 123456,
  "endByteExclusive": 131456
}
```

Qdrant stores vectors plus metadata copied from each Spring AI `Document`:

```json
{
  "datasetId": "dataset-books",
  "ragChunkId": "dataset-books:rag:0:42",
  "sourceChunkIndex": 0,
  "ragChunkIndex": 42,
  "text": "The original source text for this RAG chunk..."
}
```

The `ragChunkId` is the bridge from Qdrant search results back to MongoDB source text.

## RAG Indexer Flow

```mermaid
flowchart LR
    MONGO[("MongoDB<br/>rag_chunks")] --> MONGO_ADAPTER["MongoRagChunkQueryAdapter"]
    MONGO_ADAPTER --> QUERY_PORT["RagChunkQueryPort"]
    QUERY_PORT --> USE_CASE["IndexRagChunksUseCase"]

    USE_CASE --> BATCH["buffer(batchSize)"]
    BATCH --> SEQUENTIAL["concatMap<br/>one batch at a time"]
    SEQUENTIAL --> INDEX_PORT["RagChunkIndexPort"]
    INDEX_PORT --> INDEX_ADAPTER["SpringAiRagChunkIndexAdapter"]

    INDEX_ADAPTER --> DOC["Spring AI Document<br/>text + metadata"]
    DOC --> VECTOR_STORE["Spring AI VectorStore"]
    VECTOR_STORE --> MODEL["Transformers EmbeddingModel"]
    MODEL --> VECTOR["embedding vector"]
    VECTOR_STORE --> QDRANT[("Qdrant")]
```

## RAG Query Flow

```mermaid
sequenceDiagram
    participant Client as GraphQL Client
    participant API as file-query-api
    participant VectorStore as Spring AI VectorStore
    participant Embed as Local Transformers
    participant Qdrant as Qdrant
    participant Mongo as MongoDB rag_chunks
    participant Gemini as Gemini

    Client->>API: askDocument(datasetId, question)
    API->>VectorStore: similaritySearch(question, datasetId, limit)
    VectorStore->>Embed: embed(question)
    Embed-->>VectorStore: question vector
    VectorStore->>Qdrant: search filtered by datasetId
    Qdrant-->>VectorStore: ranked metadata
    VectorStore-->>API: ranked ragChunkIds
    API->>Mongo: find source chunks by ids
    Mongo-->>API: source text + byte offsets
    API->>API: keep similarity ranking
    API->>Gemini: question + recovered context
    Gemini-->>API: grounded answer
    API-->>Client: answer + sources
```

`searchDocumentContext(datasetId, question)` runs the same retrieval and MongoDB recovery steps, but stops before Gemini. It is useful when checking whether Qdrant is returning good context or when Gemini quota is unavailable.

## Running Locally

Prerequisites:

- JDK 25;
- Docker with the daemon running;
- a Gemini API key for `askDocument` or the optional `llm` generator mode.

Start infrastructure:

```powershell
docker compose -f compose.yml up -d mongo mongo-express qdrant
```

Useful local URLs:

```text
Mongo Express: http://localhost:8086
Qdrant Web UI: http://localhost:6333/dashboard
```

`compose.yml` starts MongoDB, Mongo Express, and Qdrant. The five project modules are not containerized; they run locally through the Gradle wrapper.

The following example runs a small pipeline manually. Each batch command starts one job, performs its work, and finishes.

1. Generate a local dataset:

```powershell
.\gradlew.bat :file-generator:bootRun --args='--file-generator.dataset-path=./data/demo.txt --file-generator.minimum-size-bytes=1048576 --file-generator.seed-provider=local'
```

2. Create and persist the processing plan:

```powershell
.\gradlew.bat :file-coordinator:bootRun --args='--file-coordinator.dataset-id=demo-v1 --file-coordinator.dataset-path=./data/demo.txt --file-coordinator.chunk-size-bytes=524288'
```

3. Run one processor for every range printed by the coordinator. This example shows the first range:

```powershell
.\gradlew.bat :file-processor:bootRun --args='--file-processor.dataset-id=demo-v1 --file-processor.dataset-path=./data/demo.txt --file-processor.chunk-index=0 --file-processor.start-byte-inclusive=0 --file-processor.end-byte-exclusive=524288'
```

Repeat that command with each `chunkIndex`, `startByteInclusive`, and `endByteExclusive` from the processing plan.

4. Index all stored RAG chunks for the dataset:

```powershell
.\gradlew.bat :rag-indexer:bootRun --args='--rag-indexer.dataset-id=demo-v1 --rag-indexer.batch-size=100'
```

5. Start the GraphQL API:


```powershell
.\gradlew.bat :file-query-api:bootRun --args='--spring.ai.google.genai.api-key=YOUR_API_KEY --file-query-api.ask-document.retrieved-chunk-limit=5 --spring.ai.vectorstore.qdrant.host=localhost --spring.ai.vectorstore.qdrant.port=6334 --spring.ai.vectorstore.qdrant.collection-name=rag_chunks'
```

On Linux/macOS, use `./gradlew` instead of `.\gradlew.bat`.

`searchDocumentContext` does not call Gemini, but the service is currently wired with a Gemini `ChatModel` because `askDocument` uses it.

## Configuration

Each module has its own `application.yml`.

Treat `datasetId` as an immutable processing-run identifier. If the source file or chunking configuration changes, use a new ID. Reusing an ID with different input can leave old derived records in MongoDB or Qdrant.

Common MongoDB URI:

```yaml
spring:
  mongodb:
    uri: mongodb://root:root@localhost:27017/reactive_rag?authSource=admin
```

Local database files should stay outside the repository when possible. `compose.yml` supports:

```env
MONGO_DATA_PATH=D:/docker-data/reactive-rag-document-processor/mongo-data
QDRANT_DATA_PATH=D:/docker-data/reactive-rag-document-processor/qdrant-storage
```

If those variables are not set, Compose uses ignored local folders under `./.docker/`.

Important runtime settings:

| Module | Key settings |
| --- | --- |
| `file-generator` | `file-generator.dataset-path`, `file-generator.minimum-size-bytes`, `file-generator.seed-provider` |
| `file-coordinator` | `file-coordinator.dataset-id`, `file-coordinator.dataset-path`, `file-coordinator.chunk-size-bytes` |
| `file-processor` | `file-processor.dataset-id`, `file-processor.chunk-index`, byte range, buffer size, RAG chunk size |
| `rag-indexer` | `rag-indexer.dataset-id`, `rag-indexer.batch-size`, Qdrant settings |
| `file-query-api` | `file-query-api.ask-document.retrieved-chunk-limit`, Gemini key, Qdrant settings |

For large local runs, the processor buffer is intentionally 4 MiB by default:

```yaml
file-processor:
  buffer-size-bytes: 4194304
  rag-chunk-max-text-length-characters: 8000
  rag-chunk-batch-size: 1000
```

## Dataset Notes

The normal Gemini seed mode generates a small seed block and cycles those lines to create large files. That is useful for infrastructure and performance tests, but it creates repetitive corpora and is not ideal for RAG quality.

For RAG demos, prefer a real or carefully curated 5-20 MiB corpus over a huge synthetic dataset.

## GraphQL API

GraphQL is served over HTTP:

```text
POST http://localhost:8080/graphql
```

Current schema:

```graphql
scalar Long

type Query {
  datasets: [Dataset!]!
  topWords(datasetId: String!, limit: Int!): [WordCount!]!
  searchDocumentContext(datasetId: String!, question: String!): [RagChunkSource!]!
  askDocument(datasetId: String!, question: String!): DocumentAnswer!
}

type DocumentAnswer {
  answer: String!
  sources: [RagChunkSource!]!
}

type RagChunkSource {
  rank: Int!
  ragChunkId: String!
  sourceChunkIndex: Int!
  ragChunkIndex: Int!
  startByteInclusive: Long!
  endByteExclusive: Long!
  textPreview: String!
  text: String!
}
```

Inspect retrieved context without calling Gemini:

```graphql
query {
  searchDocumentContext(
    datasetId: "dataset-books"
    question: "What is the difference between C and C++?"
  ) {
    rank
    ragChunkId
    sourceChunkIndex
    ragChunkIndex
    startByteInclusive
    endByteExclusive
    textPreview
  }
}
```

Ask a question with Gemini answer generation:

```graphql
query {
  askDocument(
    datasetId: "dataset-books"
    question: "What is the difference between C and C++?"
  ) {
    answer
    sources {
      rank
      ragChunkId
      textPreview
    }
  }
}
```

The custom `Long` scalar is used because GraphQL `Int` is 32-bit and cannot represent large file sizes.

## Testing

Run the full test suite:

```powershell
.\gradlew.bat --no-daemon test
```

The suite includes unit tests, GraphQL slice tests, MongoDB adapter tests, RAG indexing tests, RAG query tests, and a `file-query-api` integration test using Testcontainers with a real MongoDB container.

Docker must be available for Testcontainers.

## Current Status

Implemented:

- streaming dataset generation;
- byte-range chunk planning;
- one-pass UTF-8 chunk processing;
- MongoDB `datasets`, `chunk_word_counts`, and `rag_chunks`;
- dataset-scoped word aggregation;
- local embedding generation with Spring AI Transformers;
- Qdrant indexing through Spring AI `VectorStore`;
- GraphQL `datasets`, `topWords`, `searchDocumentContext`, and `askDocument`;
- source recovery from MongoDB after Qdrant retrieval;
- answer generation with Gemini;
- returned RAG sources for debugging and traceability;
- tests and GitHub Actions.

Validated locally:

- a 6 GiB synthetic dataset was processed in 3 source chunks after the processor performance refactor;
- `dataset-books` was processed as a 13.6 MiB real corpus with 1,740 RAG chunks and 1,740 Qdrant vectors;
- query-side retrieval can be tested without Gemini through `searchDocumentContext`.
