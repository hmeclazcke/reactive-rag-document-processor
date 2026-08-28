# Reactive RAG Document Processor

A multi-module Java project for generating large UTF-8 text datasets, processing them in independent chunks, storing structured results in MongoDB, indexing local embeddings in Qdrant, and exposing query APIs through GraphQL.

Project status: Phase 1 is complete. Phase 2A and the vector-indexing part of Phase 2B are implemented as of August 2026: the processor now creates recoverable RAG chunks, and `rag-indexer` can index those chunks into Qdrant with local embeddings. The GraphQL `askDocument` RAG query is still pending.

## Current Pipeline

Processing pipeline:

```text
file-generator
    -> large line-delimited UTF-8 dataset
    -> file-coordinator
    -> chunk plan + dataset metadata
    -> one or more file-processor runs
    -> MongoDB datasets
    -> MongoDB chunk_word_counts
    -> MongoDB rag_chunks
```

Current GraphQL query pipeline:

```text
file-query-api
    -> MongoDB datasets
    -> MongoDB chunk_word_counts
    -> GraphQL datasets/topWords
```

Vector indexing pipeline:

```text
rag-indexer
    -> reads MongoDB rag_chunks
    -> converts each chunk to a Spring AI Document
    -> Spring AI Transformers creates local embeddings
    -> Spring AI Qdrant VectorStore writes vectors to Qdrant
```

The next query-side RAG step will be:

```text
GraphQL question
    -> question embedding
    -> Qdrant similarity search
    -> ragChunkIds
    -> MongoDB rag_chunks
    -> relevant source text
    -> Gemini
    -> answer
```

Each module is an independent Spring Boot application with a specific runtime role.

## Architecture Diagrams

### System Flow

```mermaid
flowchart LR
    FG["file-generator"] --> DATA[("Large UTF-8 dataset")]
    DATA --> FC["file-coordinator"]
    FC --> PLAN["ProcessingPlan"]
    PLAN --> FP["file-processor runs"]

    FP --> WORDS[("MongoDB<br/>chunk_word_counts")]
    FP --> RAG[("MongoDB<br/>rag_chunks")]
    FC --> DATASETS[("MongoDB<br/>datasets")]

    DATASETS --> FQA["file-query-api"]
    WORDS --> FQA
    FQA --> GQL["GraphQL<br/>datasets / topWords"]

    RAG --> RI["rag-indexer"]
    RI --> EMB["Spring AI<br/>local embeddings"]
    EMB --> QDRANT[("Qdrant<br/>vectors + payload")]

    CLIENT["GraphQL client"] -. "future askDocument" .-> FQA
    FQA -. "question embedding" .-> EMB
    FQA -. "similarity search" .-> QDRANT
    QDRANT -. "ragChunkIds" .-> FQA
    FQA -. "fetch source text" .-> RAG
    FQA -. "question + context" .-> GEMINI["Gemini"]
    GEMINI -. "answer" .-> FQA
```

### Data Ownership

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

    POINTS -. "similarity search" .-> FUTURE["future askDocument"]
    PAYLOAD -. "ragChunkId" .-> RAG
```

### RAG Indexer Flow

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

### Future RAG Query

```mermaid
sequenceDiagram
    participant Client as GraphQL client
    participant API as file-query-api
    participant Embed as Question embedding model
    participant Qdrant as Qdrant
    participant Mongo as MongoDB rag_chunks
    participant Gemini as Gemini

    Client->>API: askDocument(datasetId, question)
    API->>Embed: embed question
    Embed-->>API: question vector
    API->>Qdrant: similarity search
    Qdrant-->>API: top ragChunkIds
    API->>Mongo: find chunks by ragChunkId
    Mongo-->>API: source text chunks
    API->>Gemini: question + retrieved context
    Gemini-->>API: grounded answer
    API-->>Client: answer
```

## Modules

| Module | Runtime role | Responsibility |
| --- | --- | --- |
| `file-generator` | CLI job | Generates large text datasets by streaming seed lines to disk. Seeds can come from a local resource or from Gemini through Spring AI. |
| `file-coordinator` | CLI job | Reads file metadata, calculates byte-range chunks, and stores one dataset metadata document in MongoDB. |
| `file-processor` | worker job | Processes one file chunk in a single pass, counts UTF-8 words, builds recoverable RAG chunks, and persists both outputs in MongoDB. |
| `file-query-api` | HTTP service | Exposes GraphQL queries for processed datasets and aggregated top words. |
| `rag-indexer` | CLI job | Reads recoverable RAG chunks from MongoDB and indexes local embeddings into Qdrant through Spring AI VectorStore. |

## Architecture

The code follows a pragmatic Clean Architecture / Hexagonal Architecture style:

- `domain`: pure models and rules.
- `application`: use cases and ports.
- `adapter`: concrete inputs and outputs such as CLI, filesystem, MongoDB, GraphQL, Spring AI, Gemini, and Qdrant.
- `config`: Spring wiring.

Spring is kept at the edges. Core use cases can be tested directly with plain constructors and mocked ports.

For example, `rag-indexer` application code depends on:

```text
RagChunkQueryPort
RagChunkIndexPort
```

It does not depend directly on MongoDB, Qdrant, or Spring AI. Those details live in adapters.

## Data Model

The coordinator owns the `datasets` collection:

```json
{
  "_id": "dataset-1g-gemini",
  "path": "D:/datasets/reactive-rag-document-processor/dataset-1g-gemini.txt",
  "fileSizeBytes": 1073741858,
  "chunkSizeBytes": 268435456,
  "chunkCount": 5
}
```

The processor owns the `chunk_word_counts` collection:

```json
{
  "_id": "dataset-1g-gemini:0:java",
  "datasetId": "dataset-1g-gemini",
  "chunkIndex": 0,
  "word": "java",
  "count": 3910
}
```

Word counts are stored as one document per dataset, chunk, and word instead of one large map per chunk. This avoids MongoDB's document size limit and makes aggregation by `datasetId` straightforward.

The processor also owns the `rag_chunks` collection:

```json
{
  "_id": "dataset-1g-gemini:rag:0:21479",
  "datasetId": "dataset-1g-gemini",
  "sourceChunkIndex": 0,
  "ragChunkIndex": 21479,
  "text": "The Spring Boot application initializes a Mono stream...",
  "startByteInclusive": 170000000,
  "endByteExclusive": 170007900
}
```

The RAG chunk id is stable and recoverable:

```text
datasetId:rag:sourceChunkIndex:ragChunkIndex
```

Qdrant stores vector points created by Spring AI. Each point contains:

```text
id      = UUID used by Qdrant/Spring AI
vector  = local embedding for the RAG chunk text
payload = metadata copied from the Spring AI Document
```

Payload currently includes:

```json
{
  "text": "The Spring Boot application initializes a Mono stream...",
  "datasetId": "dataset-1g-gemini",
  "sourceChunkIndex": 0,
  "ragChunkIndex": 21479,
  "ragChunkId": "dataset-1g-gemini:rag:0:21479"
}
```

MongoDB remains the source of truth for the original recoverable text. Qdrant is used for vector similarity search.

## Chunk Processing Semantics

Chunks use half-open byte ranges:

```text
[startByteInclusive, endByteExclusive)
```

The logical unit of ownership is a full line:

- if a chunk starts in the middle of a line, that partial line is skipped;
- if a line starts before `endByteExclusive`, the processor keeps reading until that line ends;
- this prevents double counting and avoids cutting words or RAG chunks between chunks.

The processor uses `FileChannel`, `ByteBuffer`, and an incremental UTF-8 decoder. It does not load the whole chunk into memory and does not build one giant `String`.

During the same pass over the file chunk, the processor:

```text
counts words
    +
accumulates complete lines into RAG chunks
    +
persists RAG chunks in batches
```

This avoids reading the same byte range twice.

## Configuration

Each module is configured through its own `application.yml`. The committed files use Spring Boot placeholders, so local values can be provided either through command-line arguments, environment variables, or a local profile file.

Common MongoDB configuration:

```yaml
spring:
  mongodb:
    uri: mongodb://root:root@localhost:27017/reactive_rag?authSource=admin
```

This project uses `spring.mongodb.uri`.

### Docker Data Paths

Local database files should stay outside the repository. The local `.env` file is ignored by Git and can point database storage to another drive:

```env
MONGO_DATA_PATH=D:/docker-data/reactive-rag-document-processor/mongo-data
QDRANT_DATA_PATH=D:/docker-data/reactive-rag-document-processor/qdrant-storage
```

If these variables are not set, Compose falls back to:

```text
./.docker/mongo-data
./.docker/qdrant-storage
```

The `.docker/` directory is ignored by Git.

### Dataset Generation

`file-generator/src/main/resources/application.yml` controls:

```yaml
file-generator:
  dataset-path: ./data/dataset.txt
  minimum-size-bytes: 1048576
  seed-resource-path: /dataset-seeds/local-seeds.txt
  seed-provider: local
```

For Gemini-backed seeds:

```yaml
file-generator:
  seed-provider: llm

spring:
  ai:
    model:
      chat: google-genai
    google:
      genai:
        api-key: ${GOOGLE_API_KEY}
        chat:
          model: gemini-3.6-flash
```

The Gemini API key is read from `GOOGLE_API_KEY`. Keys can be created in Google AI Studio: https://aistudio.google.com/api-keys.

Gemini only generates a small set of seed lines. The generator cycles those lines and writes the dataset by streaming to disk, so it never asks the LLM to generate gigabytes of text.

### Chunk Planning

`file-coordinator/src/main/resources/application.yml` controls:

```yaml
file-coordinator:
  dataset-id: local-dataset
  dataset-path: ./data/dataset.txt
  chunk-size-bytes: 5242880
```

The coordinator writes the parent dataset metadata to MongoDB.

### Chunk Processing

`file-processor/src/main/resources/application.yml` controls:

```yaml
file-processor:
  dataset-id: local-dataset
  dataset-path: ./data/dataset.txt
  chunk-index: 0
  start-byte-inclusive: 0
  end-byte-exclusive: 1048576
  max-line-length-bytes: 1048576
  buffer-size-bytes: 4194304
  rag-chunk-max-text-length-characters: 8000
  rag-chunk-batch-size: 1000
```

The default runtime buffer is 4 MiB. RAG chunks are built from complete lines and persisted in batches.

### RAG Indexing

`rag-indexer/src/main/resources/application.yml` controls:

```yaml
spring:
  ai:
    model:
      embedding: transformers
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        use-tls: false
        collection-name: rag_chunks
        content-field-name: text
        initialize-schema: true

rag-indexer:
  dataset-id: local-dataset
  batch-size: 100
```

`spring-ai-starter-model-transformers` provides a local embedding model. Spring AI uses it to convert RAG chunk text into embedding vectors. `spring-ai-starter-vector-store-qdrant` writes those vectors to Qdrant through Spring AI's `VectorStore` abstraction.

The Qdrant dashboard is available at:

```text
http://localhost:6333/dashboard
```

### Query API

`file-query-api/src/main/resources/application.yml` controls:

```yaml
file-query-api:
  top-words:
    max-limit: 100
```

## Running Locally

Start the local infrastructure:

```bash
docker compose up -d mongo mongo-express qdrant
```

Useful local URLs:

```text
Mongo Express: http://localhost:8086
Qdrant Web UI: http://localhost:6333/dashboard
```

Run modules with the Gradle wrapper:

```bash
./gradlew :file-generator:bootRun
./gradlew :file-coordinator:bootRun
./gradlew :file-processor:bootRun
./gradlew :file-query-api:bootRun
./gradlew :rag-indexer:bootRun
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`.

Example `rag-indexer` run for the 1 GiB dataset:

```powershell
.\gradlew.bat :rag-indexer:bootRun --args='--spring.profiles.active=local --rag-indexer.dataset-id=dataset-1g-gemini --rag-indexer.batch-size=100 --spring.ai.vectorstore.qdrant.host=localhost --spring.ai.vectorstore.qdrant.port=6334 --spring.ai.vectorstore.qdrant.collection-name=rag_chunks'
```

The indexer does not read the original dataset file. It reads MongoDB `rag_chunks` by `datasetId` and writes embeddings to Qdrant.

## GraphQL API

Current schema:

```graphql
scalar Long

type Query {
  topWords(datasetId: String!, limit: Int!): [WordCount!]!
  datasets: [Dataset!]!
}
```

List processed datasets:

```graphql
query {
  datasets {
    datasetId
    path
    fileSizeBytes
    chunkSizeBytes
    chunkCount
  }
}
```

Get the most frequent words for one dataset:

```graphql
query {
  topWords(datasetId: "dataset-1g-gemini", limit: 20) {
    word
    count
  }
}
```

The schema uses a custom `Long` scalar because GraphQL `Int` is 32-bit and cannot represent file sizes such as 6 GiB.

## Testing

Run the full test suite:

```bash
./gradlew test
```

The project includes:

- unit tests for domain rules and use cases;
- GraphQL slice tests with `@GraphQlTest`;
- MongoDB adapter tests;
- RAG indexer tests around batching and Spring AI `Document` conversion;
- a `file-query-api` integration test with Spring Boot, HTTP GraphQL, Testcontainers, and a real MongoDB container;
- GitHub Actions running the Gradle test suite with Java 25.

The integration test uses Testcontainers. Docker must be available, but no manually started local MongoDB instance is required for that test.

## Current Status

Phase 1 is complete:

- large dataset generation by streaming;
- local and Gemini seed providers;
- dataset metadata persistence;
- chunk planning with dataset IDs;
- UTF-8 streaming word counting;
- line ownership across byte-range chunks;
- dataset-scoped MongoDB aggregation;
- GraphQL `datasets` and `topWords(datasetId, limit)`;
- `Long` scalar support for large values;
- local tests, integration tests, and CI.

Phase 2A is implemented:

- recoverable RAG chunks are created by `file-processor`;
- word counts and RAG chunks are produced in one pass over the source file chunk;
- RAG chunks preserve dataset id, source chunk index, RAG chunk index, text, and byte offsets;
- RAG chunks are persisted to MongoDB in batches.

Phase 2B vector indexing is implemented:

- `rag-indexer` exists as a separate CLI job;
- it reads MongoDB `rag_chunks` lazily by `datasetId`;
- it indexes chunks in bounded batches;
- it uses Spring AI Transformers for local embeddings;
- it uses Spring AI Qdrant `VectorStore` for vector persistence;
- Qdrant is available in Docker Compose with persistent storage configurable through `.env`.

Manual runs completed locally:

- A 6 GiB dataset was split into 3 chunks. The optimized processor reduced chunk processing from about 29-31 minutes per chunk to about 1 minute per chunk while producing equivalent counts.
- A 1 GiB synthetic Gemini-seeded dataset produced about 135k RAG chunks.
- Indexing those RAG chunks into Qdrant with local embeddings took about 47 minutes on the local machine.

## Next Phase: RAG Querying

The next phase will add query-side Retrieval-Augmented Generation:

```text
askDocument GraphQL query
    -> embed the user's question
    -> search Qdrant for similar RAG chunks
    -> fetch original text from MongoDB by ragChunkId
    -> send question + context to Gemini
    -> return the answer through GraphQL
```

The first version should be a normal GraphQL query returning a single answer. Streaming/subscriptions can be added later.
