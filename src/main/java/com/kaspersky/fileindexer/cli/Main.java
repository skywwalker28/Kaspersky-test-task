package com.kaspersky.fileindexer.cli;

import com.kaspersky.fileindexer.api.FileIndexService;
import com.kaspersky.fileindexer.core.DirectoryWatcher;
import com.kaspersky.fileindexer.core.InMemoryFileIndexService;
import com.kaspersky.fileindexer.tokenizer.SimpleTokenizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        FileIndexService service = new InMemoryFileIndexService(new SimpleTokenizer());

        try(DirectoryWatcher watcher = new DirectoryWatcher(service)) {
            Scanner scanner = new Scanner(System.in);

            watcher.start();

            System.out.println("File Indexer CLI.\nCommands:\n add-file <path>" +
                    "\n add-dir <path>\n search <word>\n exit");
            while (true) {
                System.out.print(" > ");
                String line = scanner.nextLine();

                if (line == null || line.isBlank()) continue;
                if (line.equalsIgnoreCase("exit")) {
                    System.out.println("Bye!");
                    break;
                }

                String[] parts = line.split("\\s+", 2);
                String command = parts[0];

                try {
                    switch (command) {
                        case "add-file" -> {
                            if (parts.length < 2 || parts[1].isBlank()) {
                                System.out.println("Usage: add-file <path>");
                                continue;
                            }

                            Path path = Path.of(parts[1]);
                            service.addFile(path);
                            System.out.println("File indexed: " + path);
                        }

                        case "add-dir" -> {
                            if (parts.length < 2 || parts[1].isBlank()) {
                                System.out.println("Usage: add-dir <path>");
                                continue;
                            }

                            Path path = Path.of(parts[1]);
                            service.addDirectory(path);
                            watcher.registerDirectory(path);

                            System.out.println("Directory indexed: " + path);
                        }

                        case "search" -> {
                            if (parts.length < 2 || parts[1].isBlank()) {
                                System.out.println("Usage: search <word>");
                                continue;
                            }

                            Set<Path> result = service.search(parts[1]);
                            if (result.isEmpty()) System.out.println("No file found");
                            else {
                                System.out.println("Found in files:");
                                result.forEach(System.out::println);
                            }
                        }
                        default -> System.out.println("Unknown command");
                    }
                } catch (IllegalArgumentException | IOException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (RuntimeException e) {
                    System.out.println("Unexpected error: " + e.getMessage());
                }
            }
        }
    }
}
