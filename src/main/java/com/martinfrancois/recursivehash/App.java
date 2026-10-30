package com.martinfrancois.recursivehash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {

    static final String EXTENSION = ".arw";
    static final String OUTPUT_FILE = "hashes.txt";

    public static void main(String[] args) throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(Path.of(args[0]))) {
            files = paths.filter(Files::isRegularFile)
                    .filter(App::hasRawExtension)
                    .toList();
        }
        String pathsAndHashes = files.parallelStream()
                .map(App::formatHash)
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get(OUTPUT_FILE), pathsAndHashes);
    }

    private static boolean hasRawExtension(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(EXTENSION);
    }

    private static String formatHash(Path path) {
        return path.getParent().toString() + "\t" + path.getFileName().toString() + "\t" + HashFile.getHash(path);
    }
}
