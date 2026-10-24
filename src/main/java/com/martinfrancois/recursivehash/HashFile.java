package com.martinfrancois.recursivehash;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.IOException;
import java.nio.file.Path;

public class HashFile {

    static final String ERROR_MARKER = "Error!";

    public static String getHash(Path path) {
        try {
            return Files.asByteSource(path.toFile()).hash(Hashing.sha256()).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return ERROR_MARKER;
        }
    }

}
