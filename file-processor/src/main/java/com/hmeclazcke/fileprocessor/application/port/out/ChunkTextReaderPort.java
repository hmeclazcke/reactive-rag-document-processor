package com.hmeclazcke.fileprocessor.application.port.out;

import com.hmeclazcke.fileprocessor.domain.FileChunk;

import java.nio.file.Path;
import java.util.List;

public interface ChunkTextReaderPort {

    List<String> readText(Path datasetPath, FileChunk chunk);
}