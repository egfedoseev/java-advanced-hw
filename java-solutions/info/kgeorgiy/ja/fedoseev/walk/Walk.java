package info.kgeorgiy.ja.fedoseev.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class Walk {
    private static int hashFile(Optional<Path> path) {
        return path.map(Walk::hashFile).orElse(0);
    }

    static int hashFile(Path path) {
        try (BufferedInputStream fileInputStream = new BufferedInputStream(Files.newInputStream(path))) {
            return FNWHasher.FNV1Hash(fileInputStream);
        } catch (Throwable e) {
            return 0;
        }
    }

    static Optional<Path> tryParsePath(String pathString) {
        if (pathString == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(pathString));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Usage: INPUT OUTPUT");
            return;
        }
        Optional<Path> parsedInput = tryParsePath(args[0]);
        if (parsedInput.isEmpty()) {
            System.err.println("Invalid input path: " + args[0]);
            return;
        }
        Path inputPath = parsedInput.get();

        Optional<Path> parsedOutput = tryParsePath(args[1]);
        if (parsedOutput.isEmpty()) {
            System.err.println("Invalid output path: " + args[1]);
            return;
        }
        Path outputPath = parsedOutput.get();

        try {
            final Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Can't create parent directories for output file: " + e.getMessage());
            return;
        }

        try (BufferedReader inputReader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            try (BufferedWriter outputWriter = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                processFiles(inputReader, outputWriter);
            } catch (IOException e) {
                System.err.println("Can't read from input file: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + args[0] + " not found");
        } catch (IOException e) {
            System.err.println("Something went wrong while trying to access input file: " + e.getMessage());
        }
    }

    private static void processFiles(BufferedReader inputReader, BufferedWriter outputWriter) throws IOException {
        String pathString = inputReader.readLine();
        while (pathString != null) {
            try {
                processRegularFile(outputWriter, tryParsePath(pathString), pathString);
            } catch (IOException e) {
                System.err.println("Can't write to output file: " + e.getMessage());
                return;
            }
            pathString = inputReader.readLine();
        }
    }

    static void processRegularFile(BufferedWriter outputWriter, Optional<Path> path, String pathString) throws IOException {
        int hash = hashFile(path);
        outputWriter.write(String.format("%08x %s", hash, pathString));
        outputWriter.newLine();
    }
}
