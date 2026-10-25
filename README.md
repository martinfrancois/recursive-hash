To package an executable fat jar with JDK 17 or newer, run:
```
./gradlew shadowJar
```
Then, you can find the built jar in `build/libs`.

Run with:
```
java -jar build/libs/recursive-hash-1.0-SNAPSHOT-all.jar /path/to/photos
```

The tool hashes `.arw` files recursively and overwrites `hashes.txt` in the current directory. Keep any earlier result before running it again. Each line contains the parent directory, filename and SHA-256 digest, separated by tabs. Unreadable files get `Error!` in the digest column.

Use `examples/photos` for a synthetic example. Run `./gradlew build` to build and test.

MIT, see [LICENSE](LICENSE).
