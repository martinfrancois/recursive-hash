package com.martinfrancois.recursivehash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) throws IOException {
        String extension = ".arw";

        String pathsAndHashes = Files.walk(Path.of(args[0]))
                .filter(path -> !Files.isDirectory(path) && path.getFileName().toString().toLowerCase().endsWith(extension))
                .collect(Collectors.toList())
                .parallelStream()
                .map(path -> path.getParent().toString() + "\t" + path.getFileName().toString() + "\t" + HashFile.getHash(path))
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get("hashes.txt"), pathsAndHashes);
    }

}
