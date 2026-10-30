package com.martinfrancois.recursivehash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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

    @Test
    @EnabledOnOs(OS.LINUX)
    void closesWalkHandlesWhenTraversalFails(@TempDir Path dir) throws IOException {
        Path locked = Files.createDirectory(dir.resolve("locked"));
        var permissions = Files.getPosixFilePermissions(locked);
        Files.setPosixFilePermissions(locked, Set.of());
        try {
            assumeFalse(Files.isReadable(locked), "requires a user without permission bypass");
            long before = openHandlesUnder(dir);

            assertThrows(UncheckedIOException.class, () -> App.main(new String[] {dir.toString()}));

            assertEquals(before, openHandlesUnder(dir), "failed traversal must close its directory handles");
        } finally {
            Files.setPosixFilePermissions(locked, permissions);
        }
    }

    private static long openHandlesUnder(Path directory) throws IOException {
        long count = 0;
        try (Stream<Path> handles = Files.list(Path.of("/proc/self/fd"))) {
            var iterator = handles.iterator();
            while (iterator.hasNext()) {
                Path handle = iterator.next();
                try {
                    if (Files.readSymbolicLink(handle).startsWith(directory)) {
                        count++;
                    }
                } catch (NoSuchFileException ignored) {
                    // Another JVM thread closed this descriptor after it was listed.
                }
            }
        }
        return count;
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void skipsFifoAndKeepsSymlinksToRegularFiles(@TempDir Path dir) throws Exception {
        Path photos = Files.createDirectory(dir.resolve("photos"));
        Path regular = photos.resolve("regular.arw");
        Files.writeString(regular, "alpha\n");
        Path fifo = photos.resolve("blocked.arw");
        Process makeFifo = new ProcessBuilder("mkfifo", fifo.toString()).start();
        try {
            assertTrue(makeFifo.waitFor(5, TimeUnit.SECONDS), "mkfifo did not finish");
            assertEquals(0, makeFifo.exitValue());
        } finally {
            makeFifo.destroyForcibly();
            assertTrue(makeFifo.waitFor(5, TimeUnit.SECONDS), "mkfifo did not stop");
        }
        Path linked = Files.createSymbolicLink(photos.resolve("linked.arw"), regular.getFileName());
        Files.createSymbolicLink(photos.resolve("linked-pipe.arw"), fifo.getFileName());
        Files.createSymbolicLink(photos.resolve("broken.arw"), Path.of("missing.arw"));
        Path outside = Files.createDirectory(dir.resolve("outside"));
        Files.writeString(outside.resolve("not-visited.arw"), "bravo\n");
        Files.createSymbolicLink(photos.resolve("directory.arw"), outside);
        List<String> expected;
        Set<Path> includedFiles = Set.of(regular, linked);
        try (Stream<Path> traversal = Files.walk(photos)) {
            expected = traversal.filter(includedFiles::contains)
                    .map(AppTest::alphaRow)
                    .toList();
        }
        String classpath = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(Hashing.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classpath, App.class.getName(), photos.toString())
                .directory(dir.toFile()).redirectErrorStream(true)
                .redirectOutput(dir.resolve("process.log").toFile()).start();
        try {
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "hashing blocked on a special file");
            assertEquals(0, process.exitValue(), Files.readString(dir.resolve("process.log")));
            List<String> lines = Files.readAllLines(dir.resolve(App.OUTPUT_FILE));
            assertEquals(2, expected.size());
            assertEquals(expected, lines, "hashing must retain traversal encounter order");
        } finally {
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "child process did not stop");
        }
    }

    private static String alphaRow(Path path) {
        return path.getParent() + "\t" + path.getFileName() + "\t" + SHA256_ALPHA;
    }

}
