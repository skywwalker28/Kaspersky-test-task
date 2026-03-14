package com.kaspersky.fileindexer.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface FileIndexService {
    void addFile(Path path) throws IOException;
    void addDirectory(Path path) throws IOException;
    void removeFile(Path path);
    Set<Path> search(String word);
}
