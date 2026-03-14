package com.kaspersky.fileindexer.core;

import com.kaspersky.fileindexer.api.FileIndexService;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static java.nio.file.StandardWatchEventKinds.*;


public class DirectoryWatcher implements Closeable {
    private final FileIndexService fileIndexService;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();

    private volatile boolean running;
    private Thread workerThread;

    public DirectoryWatcher(FileIndexService fileIndexService) throws IOException {
        this.fileIndexService = fileIndexService;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    public void registerDirectory(Path directory) throws IOException {
        Path normalizedDir = directory.toAbsolutePath().normalize();

        if (!Files.exists(normalizedDir) || !Files.isDirectory(normalizedDir)) {
            throw new IllegalArgumentException("Directory does not exists: " + normalizedDir);
        }

        WatchKey key = normalizedDir.register(
                watchService,
                ENTRY_CREATE,
                ENTRY_MODIFY,
                ENTRY_DELETE
        );

        keys.put(key, normalizedDir);
    }

    public void start() {
        if (running) return;

        running = true;
        workerThread = new Thread(this::processEvents, "directory-watcher-thread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void processEvents() {
        while (running) {
            WatchKey key;

            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                break;
            }

            Path directory = keys.get(key);
            if (directory == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                WatchEvent<Path> ev = cast(event);
                Path fileName = ev.context();
                Path fullPath = directory.resolve(fileName).toAbsolutePath().normalize();

                try {
                    if (kind == ENTRY_CREATE) {
                        handleCreate(fullPath);
                    } else if (kind == ENTRY_MODIFY) {
                        handleModify(fullPath);
                    } else if (kind == ENTRY_DELETE) {
                        handleDelete(fullPath);
                    }
                } catch (Exception e) {
                    System.err.println("Watcher error for path " + fullPath + ": " + e.getMessage());
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
            }
        }
    }

    private void handleCreate(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            registerDirectory(path);
            fileIndexService.addDirectory(path);
        } else if (Files.isRegularFile(path)) {
            fileIndexService.addFile(path);
        }
    }

    private void handleModify(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            fileIndexService.addFile(path);
        }
    }

    private void handleDelete(Path path) throws IOException {
        fileIndexService.removeFile(path);
    }

    @SuppressWarnings("unchecked")
    private WatchEvent<Path> cast(WatchEvent<?> event) {
        return (WatchEvent<Path>) event;
    }

    @Override
    public void close() throws IOException {
        running = false;

        if (workerThread != null) workerThread.interrupt();
        watchService.close();
    }
}
