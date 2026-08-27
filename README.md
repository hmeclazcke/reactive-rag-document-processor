# Reactive RAG Document Processor

A multi-module Java project for processing large text datasets in independent chunks, storing the results in MongoDB, and querying them through GraphQL.

Project status: Phase 1 complete as of August 2026.

The current implementation completes the first phase of the project: generate a large dataset, create a processing plan, process each chunk independently, persist word counts, and expose query APIs. The next phase will turn the processed datasets into a Retrieval-Augmented Generation pipeline with local embeddings, Qdrant, and Gemini.

## Current Pipeline

```text
file-generator
    -> large line-delimited dataset
    -> file-coordinator
    -> chunk plan + dataset metadata
    -> one or more file-processor runs
    -> MongoDB
    -> file-query-api
    -> GraphQL
```

Each module is an independent Spring Boot application with a specific runtime role.

## Modules

| Module | Runtime role | Responsibility |
| --- | --- | --- |
| `file-generator` | CLI job | Generates large text datasets by streaming seed lines to disk. Seeds can come from a local resource or from Gemini through Spring AI. |
| `file-coordinator` | CLI job | Reads file metadata, calculates byte-range chunks, and stores one dataset metadata document in MongoDB. |
| `file-processor` | worker job | Processes one chunk, counts UTF-8 words by streaming bytes from disk, and stores one document per dataset/chunk/word. |
| `file-query-api` | HTTP service | Exposes GraphQL queries for processed datasets and aggregated top words. |

## Architecture

The code follows a pragmatic Clean Architecture / Hexagonal Architecture style:

- `domain`: pure models and rules.
- `application`: use cases and ports.
- `adapter`: concrete inputs and outputs such as CLI, filesystem, MongoDB, GraphQL, and Gemini.
- `config`: Spring wiring.

Spring is kept at the edges. Core use cases can be tested directly with plain constructors and mocked ports.

## Data Model

The coordinator owns the `datasets` collection:

```json
{
  "_id": "dataset-6g-gemini",
  "path": "D:/datasets/reactive-rag-document-processor/dataset-6g-gemini.txt",
  "fileSizeBytes": 6442450944,
  "chunkSizeBytes": 2150000000,
  "chunkCount": 3
}
```

The processor owns the `chunk_word_counts` collection:

```json
{
  "_id": "dataset-6g-gemini:0:java",
  "datasetId": "dataset-6g-gemini",
  "chunkIndex": 0,
  "word": "java",
  "count": 3910
}
```

Word counts are stored as one document per dataset, chunk, and word instead of one large map per chunk. This avoids MongoDB's document size limit and makes aggregation by `datasetId` straightforward.

## Chunk Processing Semantics

Chunks use half-open byte ranges:

```text
[startByteInclusive, endByteExclusive)
```

The logical unit of ownership is a full line:

- if a chunk starts in the middle of a line, that partial line is skipped;
- if a line starts before `endByteExclusive`, the processor keeps reading until that line ends;
- this prevents double counting and avoids cutting words between chunks.

The processor uses `FileChannel`, `ByteBuffer`, and an incremental UTF-8 decoder. It does not load the whole chunk into memory and does not build one giant `String`.

## Configuration

Each module is configured through its own `application.yml`. The committed files use Spring Boot placeholders, so local values can be provided either through environment variables or a local profile file.

Common MongoDB configuration:

```yaml
spring:
  mongodb:
    uri: mongodb://root:root@localhost:27017/reactive_rag?authSource=admin
```

This project uses `spring.mongodb.uri`.

### Dataset generation

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

The Gemini API key is read from `GOOGLE_API_KEY`. As of August 2026, keys are created in Google AI Studio: https://aistudio.google.com/api-keys.

Gemini only generates a small set of seed lines. The generator cycles those lines and writes the dataset by streaming to disk, so it never asks the LLM to generate gigabytes of text.

### Chunk planning

`file-coordinator/src/main/resources/application.yml` controls:

```yaml
file-coordinator:
  dataset-id: local-dataset
  dataset-path: ./data/dataset.txt
  chunk-size-bytes: 5242880
```

The coordinator writes the parent dataset metadata to MongoDB.

### Chunk processing

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
```

The default runtime buffer is 4 MiB.

### Query API

`file-query-api/src/main/resources/application.yml` controls:

```yaml
file-query-api:
  top-words:
    max-limit: 100
```

## Running Locally

Start the local MongoDB infrastructure:

```bash
docker compose up -d
```

Run modules with the Gradle wrapper:

```bash
./gradlew :file-generator:bootRun
./gradlew :file-coordinator:bootRun
./gradlew :file-processor:bootRun
./gradlew :file-query-api:bootRun
```

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
  topWords(datasetId: "dataset-6g-gemini", limit: 20) {
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

A manual end-to-end run was completed with a dataset of about 6 GiB split into 3 chunks. The previous processor implementation took about 29-31 minutes per chunk; the current streaming implementation reduced that to about 1 minute per chunk while producing the same counts.

## Next Phase: RAG

The next phase will add Retrieval-Augmented Generation incrementally:

```text
dataset
    -> recoverable text chunks
    -> local embeddings
    -> Qdrant
    -> similarity search
    -> relevant chunk text
    -> Gemini
    -> GraphQL answer
```

MongoDB will continue to store structured metadata and processing results. Qdrant will be introduced only for vector search.
