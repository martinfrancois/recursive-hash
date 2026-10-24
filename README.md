# recursivehash

Writes a SHA-256 digest for every `.arw` file below a directory into `hashes.txt`.

## What it does

The tool walks the directory you pass it, picks every regular file whose name ends in `.arw` in any letter case, and hashes each one with SHA-256. It hashes files in parallel and writes one line per file to `hashes.txt` in the directory you ran it from. The output order follows the directory walk, so it is stable for the same tree on the same machine but not sorted.

Each line has three tab-separated columns and no trailing newline after the last line:

```
<parent directory>	<file name>	<sha256 as 64 lowercase hex characters>
```

The parent directory is printed the way you passed the root: a relative root gives relative parents, an absolute root gives absolute parents. A file the tool cannot read gets the text `Error!` in the digest column and the stack trace goes to stderr.

The `.arw` filter is fixed in `App.java`. Files with any other extension are skipped.

## Requirements

JDK 17 or newer. The Gradle wrapper downloads Gradle itself.

## Build

```
./gradlew build
```

This compiles, runs the tests and writes the runnable jar with all dependencies to `build/libs/recursivehash-1.0-SNAPSHOT-all.jar`. Run `./gradlew shadowJar` if you only want the jar.

## Usage

```
java -jar build/libs/recursivehash-1.0-SNAPSHOT-all.jar <directory>
```

`hashes.txt` is overwritten on every run.

## Example

`examples/photos` holds three small text files with `.arw` names and one `readme.txt` the tool must ignore.

```
java -jar build/libs/recursivehash-1.0-SNAPSHOT-all.jar examples/photos
cat hashes.txt
```

Output:

```
examples/photos/2021-09	DSC00002.ARW	5da8f23decf397b13f4f55b6fb8a61936238bfe08ed9d901132974f1beccc45c
examples/photos/2021-09	DSC00003.arw	999d1d048ee9123272dd9b718680551c83e867935b47c2650e6906dc22674e47
examples/photos	DSC00001.arw	b6a98d9ce9a2d9149288fa3df42d377c3e42737afdcdaf714e33c0a100b51060
```

`printf 'alpha\n' | sha256sum` gives the digest of `DSC00001.arw`, so you can check the tool against any other SHA-256 implementation.

## Tests

`./gradlew test` builds a temporary directory with files of known content and compares `hashes.txt` against digests computed outside the tool.

## Limitations

- Only `.arw` files. Change the `extension` constant in `App.java` for other file types.
- Output always goes to `hashes.txt` in the current directory.
- Symbolic links to directories are not followed. A link to a file is hashed through the link.
- The whole result is held in memory before it is written, so a tree with millions of files needs a matching heap.

## License

MIT, see `LICENSE`.
