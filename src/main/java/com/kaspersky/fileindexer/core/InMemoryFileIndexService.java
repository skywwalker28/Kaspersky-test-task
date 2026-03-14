package com.kaspersky.fileindexer.core;

import com.kaspersky.fileindexer.api.FileIndexService;
import com.kaspersky.fileindexer.tokenizer.Tokenizer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class InMemoryFileIndexService implements FileIndexService {
    private final Tokenizer tokenizer;
    private final ConcurrentMap<String, Set<Path>> wordToFiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, Set<String>> fileToWords = new ConcurrentHashMap<>();

    public InMemoryFileIndexService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public void addFile(Path path) throws IOException {
        path = normalize(path);

        if (!isTextFile(path)) return;

        validateFile(path);

        String content = Files.readString(path);
        Set<String> words = tokenizer.tokenizer(content);

        removeFileFromIndex(path);

        fileToWords.put(path, words);
        for (String word : words) {
            wordToFiles
                    .computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet())
                    .add(path);
        }
    }

    @Override
    public void addDirectory(Path path) throws IOException {
        path = normalize(path);

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Directory does not exist: " + path);
        }

        try (Stream<Path> paths = Files.walk(path)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .forEach(file -> {
                        try {
                            addFile(file);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to index file: " + file, e);
                        }
                    });
        }
    }

    @Override
    public void removeFile(Path path) {
        if (path == null) return;

        path = normalize(path);

        Set<String> oldWords = fileToWords.remove(path);
        if (oldWords == null) return;

        for (String word : oldWords) {
            Set<Path> files = wordToFiles.get(word);
            if (files != null) {
                files.remove(path);
                if (files.isEmpty()) wordToFiles.remove(word, files);
            }
        }
    }

    @Override
    public Set<Path> search(String word) {
        if (word == null || word.isBlank()) return Collections.emptySet();


        String normalizeWord = word.toLowerCase(Locale.ROOT).trim();
        return Collections.unmodifiableSet(wordToFiles.getOrDefault(normalizeWord, Collections.emptySet()));
    }

    private void removeFileFromIndex(Path path) {
        Set<String> oldWords = fileToWords.remove(path);
        if (oldWords == null) return;

        for (String word : oldWords) {
            Set<Path> files = wordToFiles.get(word);
            if (files != null) {
                files.remove(path);
                if (files.isEmpty()) {
                    wordToFiles.remove(word, files);
                }
            }
        }
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private boolean isTextFile(Path path) {
        return path.toString().toLowerCase(Locale.ROOT).endsWith(".txt");
    }

    private void validateFile(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("File does not exists: " + path);
        }

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }
    }
}
