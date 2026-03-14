package com.kaspersky.fileindexer.core;

import com.kaspersky.fileindexer.api.FileIndexService;
import com.kaspersky.fileindexer.tokenizer.SimpleTokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryFileIndexServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void should_index_single_file() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Java Spring Java Boot");

        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());
        service.addFile(file);

        Set<Path> javaResult = service.search("java");
        Set<Path> springResult = service.search("spring");
        Set<Path> bootResult = service.search("boot");

        assertTrue(javaResult.contains(file));
        assertTrue(springResult.contains(file));
        assertTrue(bootResult.contains(file));
    }
    
    @Test
    void should_return_emptySet_for_missing_word() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Java Spring Boot");
        
        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());
        service.addFile(file);
        
        Set<Path> result = service.search("docker");
        assertTrue(result.isEmpty());
    }
    
    
    @Test
    void should_reindex_file_after_content_change() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Java Spring");
        
        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());
        service.addFile(file);
        
        assertTrue(service.search("java").contains(file));
        assertFalse(service.search("docker").contains(file));
        
        Files.writeString(file, "Docker Kubernetes");
        service.addFile(file);
        
        assertFalse(service.search("java").contains(file));
        assertTrue(service.search("docker").contains(file));
        assertTrue(service.search("kubernetes").contains(file));
    }

    @Test
    void should_index_all_files_in_directory() throws IOException {
        Path file1 = tempDir.resolve("a.txt");
        Path file2 = tempDir.resolve("b.txt");

        Files.writeString(file1, "Java Spring");
        Files.writeString(file2, "Docker Java");

        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());
        service.addDirectory(tempDir);

        Set<Path> javaResult = service.search("java");
        Set<Path> springResult = service.search("spring");
        Set<Path> dockerResult = service.search("docker");

        assertTrue(javaResult.contains(file1));
        assertTrue(javaResult.contains(file2));
        assertTrue(springResult.contains(file1));
        assertTrue(dockerResult.contains(file2));
    }

    @Test
    void should_remove_file_from_index() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Java Spring");

        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());
        service.addFile(file);

        assertTrue(service.search("java").contains(file));

        service.removeFile(file);

        assertFalse(service.search("java").contains(file));
        assertFalse(service.search("spring").contains(file));
    }

    @Test
    void should_ignore_non_txt_files() throws IOException {
        Path txtFile = tempDir.resolve("a.txt");
        Path dsStore = tempDir.resolve(".DS_Store");

        Files.writeString(txtFile, "Java");
        Files.writeString(dsStore, "Hidden system file");

        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());
        service.addDirectory(tempDir);

        assertTrue(service.search("java").contains(txtFile));
        assertTrue(service.search("hidden").isEmpty());
    }
}
