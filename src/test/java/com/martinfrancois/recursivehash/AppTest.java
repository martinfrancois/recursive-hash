package com.martinfrancois.recursivehash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppTest {

    // Digests computed independently with `printf '<content>\n' | sha256sum`.
    private static final String SHA256_ALPHA =
            "b6a98d9ce9a2d9149288fa3df42d377c3e42737afdcdaf714e33c0a100b51060";
    private static final String SHA256_BRAVO =
            "5da8f23decf397b13f4f55b6fb8a61936238bfe08ed9d901132974f1beccc45c";

    @Test
    void hashesArwFilesRecursivelyAndWritesOneTabSeparatedLinePerFile(@TempDir Path dir) throws IOException {
        Path alpha = dir.resolve("alpha.arw");
        Path bravo = dir.resolve("nested").resolve("bravo.ARW");
        Files.createDirectories(bravo.getParent());
        Files.writeString(alpha, "alpha\n");
        Files.writeString(bravo, "bravo\n");
        Files.writeString(dir.resolve("notes.txt"), "ignored\n");

        App.main(new String[] {dir.toString()});

        Path output = Path.of(App.OUTPUT_FILE);
        List<String> lines = Arrays.asList(Files.readString(output, StandardCharsets.UTF_8).split("\n"));
        Files.delete(output);

        List<String> expected = Arrays.asList(
                dir + "\talpha.arw\t" + SHA256_ALPHA,
                bravo.getParent() + "\tbravo.ARW\t" + SHA256_BRAVO);
        assertEquals(expected.stream().sorted().collect(Collectors.toList()),
                lines.stream().sorted().collect(Collectors.toList()));
    }

    @Test
    void getHashReturnsLowercaseHexSha256(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sample.arw");
        Files.writeString(file, "alpha\n");

        assertEquals(SHA256_ALPHA, HashFile.getHash(file));
    }

    @Test
    void getHashReportsUnreadableFileAsErrorMarker(@TempDir Path dir) {
        String result = HashFile.getHash(dir.resolve("missing.arw"));

        assertEquals(HashFile.ERROR_MARKER, result);
    }
}
