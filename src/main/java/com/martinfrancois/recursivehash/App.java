package com.martinfrancois.recursivehash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class App {

    static final String EXTENSION = ".arw";
    static final String OUTPUT_FILE = "hashes.txt";

    public static void main(String[] args) throws IOException {
        String pathsAndHashes = Files.walk(Path.of(args[0]))
                .filter(path -> !Files.isDirectory(path) && path.getFileName().toString().toLowerCase().endsWith(EXTENSION))
                .collect(Collectors.toList())
                .parallelStream()
                .map(path -> path.getParent().toString() + "\t" + path.getFileName().toString() + "\t" + HashFile.getHash(path))
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get(OUTPUT_FILE), pathsAndHashes);
    }

}
